import AVFoundation
import CryptoKit
import Foundation
import HealthKit
import SwiftUI
import UIKit
import UserNotifications
import Vision

// The string identifiers in this file are the wire contract with the shared
// Kotlin module. They must stay equal to the Kotlin enum entry names in
// shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/health/. Shared code fails
// closed on any identifier it does not recognise, so a drift becomes a visible
// rejection instead of a silent widening.

enum NativeEvidenceIdentifiers {
    static let photoLibrary = "PHOTO_LIBRARY"
    static let camera = "CAMERA"

    static let authorized = "AUTHORIZED"
    static let denied = "DENIED"
    static let restricted = "RESTRICTED"
    static let notDetermined = "NOT_DETERMINED"
    static let provisional = "PROVISIONAL"

    static let temporaryInMemory = "TEMPORARY_IN_MEMORY"
}

struct NativeScanEvidence {
    let recognizedText: String
    let rawTextSHA256: String
    let barcode: String?
    let captureSourceId: String
    let captureAuthorizationId: String
    /// Pixels are held in memory for the length of one Vision request and never written to disk.
    let rawPixelRetentionId: String = NativeEvidenceIdentifiers.temporaryInMemory

    var summary: String {
        let lines = recognizedText
            .split(whereSeparator: \.isNewline)
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        let barcodePart = barcode.map { " Barcode: \($0.prefix(32))." } ?? " No barcode detected."
        return "Recognized \(lines.count) non-empty lines. Evidence hash: \(rawTextSHA256.prefix(12))…\(barcodePart) Confirm against the physical label; no dose was calculated."
    }
}

enum NativeScanError: Error {
    case missingCGImage
    case noRecognizedContent
}

/// Uses Apple's on-device Vision framework. The caller owns the UIImage in memory;
/// this service never writes it to disk and never treats OCR output as verified truth.
final class NativeLabelEvidenceBridge {
    func scan(
        image: UIImage,
        captureSourceId: String,
        captureAuthorizationId: String,
        completion: @escaping (Result<NativeScanEvidence, Error>) -> Void
    ) {
        guard let cgImage = image.cgImage else {
            completion(.failure(NativeScanError.missingCGImage))
            return
        }

        DispatchQueue.global(qos: .userInitiated).async {
            autoreleasepool {
                do {
                    let textRequest = VNRecognizeTextRequest()
                    textRequest.recognitionLevel = .accurate
                    textRequest.usesLanguageCorrection = true
                    textRequest.recognitionLanguages = ["zh-Hant", "zh-Hans", "en-US"]

                    let barcodeRequest = VNDetectBarcodesRequest()
                    let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
                    try handler.perform([textRequest, barcodeRequest])

                    let text = (textRequest.results ?? [])
                        .compactMap { $0.topCandidates(1).first?.string }
                        .joined(separator: "\n")
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    let barcode = barcodeRequest.results?
                        .compactMap(\.payloadStringValue)
                        .first

                    guard !text.isEmpty || barcode != nil else {
                        throw NativeScanError.noRecognizedContent
                    }

                    let digest = SHA256.hash(data: Data(text.utf8))
                        .map { String(format: "%02x", $0) }
                        .joined()
                    let evidence = NativeScanEvidence(
                        recognizedText: text,
                        rawTextSHA256: digest,
                        barcode: barcode,
                        captureSourceId: captureSourceId,
                        captureAuthorizationId: captureAuthorizationId
                    )
                    DispatchQueue.main.async {
                        completion(.success(evidence))
                    }
                } catch {
                    DispatchQueue.main.async {
                        completion(.failure(error))
                    }
                }
            }
        }
    }
}

/// Camera capture. `PhotosPicker` runs out of process and needs no library
/// permission, so only the camera path has an authorization state to report.
enum NativeCameraAccess {
    static var isCameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    static var authorizationId: String {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            return NativeEvidenceIdentifiers.authorized
        case .denied:
            return NativeEvidenceIdentifiers.denied
        case .restricted:
            return NativeEvidenceIdentifiers.restricted
        case .notDetermined:
            return NativeEvidenceIdentifiers.notDetermined
        @unknown default:
            return NativeEvidenceIdentifiers.notDetermined
        }
    }

    static func requestAccess(completion: @escaping (String) -> Void) {
        guard isCameraAvailable else {
            completion(NativeEvidenceIdentifiers.restricted)
            return
        }
        AVCaptureDevice.requestAccess(for: .video) { _ in
            DispatchQueue.main.async {
                completion(authorizationId)
            }
        }
    }
}

struct NativeCameraCaptureView: UIViewControllerRepresentable {
    let onImage: (UIImage?) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let controller = UIImagePickerController()
        controller.sourceType = .camera
        controller.allowsEditing = false
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {
        // The picker owns its own state.
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onImage: onImage)
    }

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        private let onImage: (UIImage?) -> Void

        init(onImage: @escaping (UIImage?) -> Void) {
            self.onImage = onImage
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            onImage(info[.originalImage] as? UIImage)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onImage(nil)
        }
    }
}

struct NativeHealthReadState {
    let availabilityId: String
    let authorizationId: String
    let detail: String
}

/// Least-privilege HealthKit reads (Issue #28).
///
/// iOS deliberately never reports read authorization: `requestAuthorization`
/// only tells the app that the sheet was answered. This bridge therefore reports
/// `REQUEST_PRESENTED_OUTCOME_UNKNOWABLE` and lets shared code decide what may
/// be displayed. Nothing is written back to Health and nothing leaves the device.
final class NativeHealthReadBridge {
    static let availableId = "AVAILABLE"
    static let unavailableId = "UNAVAILABLE_ON_DEVICE"
    static let requestPresentedId = "REQUEST_PRESENTED_OUTCOME_UNKNOWABLE"
    static let restrictedId = "RESTRICTED_BY_POLICY"
    static let samplesReturnedId = "SAMPLES_RETURNED"
    static let emptyOutcomeId = "EMPTY_INDISTINGUISHABLE"

    private let store = HKHealthStore()

    static func objectType(forAppleIdentifier identifier: String) -> HKObjectType? {
        if identifier == "HKWorkoutTypeIdentifier" {
            return HKObjectType.workoutType()
        }
        return HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier(rawValue: identifier))
    }

    /// `appleTypeIdentifiers` must come from shared `HealthReadPolicy`, which is the
    /// tested owner of the least-privilege set. This bridge never widens it.
    func requestReadAccess(
        appleTypeIdentifiers: [String],
        completion: @escaping (NativeHealthReadState) -> Void
    ) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(
                NativeHealthReadState(
                    availabilityId: Self.unavailableId,
                    authorizationId: NativeEvidenceIdentifiers.notDetermined,
                    detail: "Health data is unavailable on this device."
                )
            )
            return
        }

        let types = Set(appleTypeIdentifiers.compactMap(Self.objectType(forAppleIdentifier:)))
        guard !types.isEmpty else {
            completion(
                NativeHealthReadState(
                    availabilityId: Self.availableId,
                    authorizationId: NativeEvidenceIdentifiers.notDetermined,
                    detail: "No Health feature is enabled, so no read type was requested."
                )
            )
            return
        }

        store.requestAuthorization(toShare: [], read: types) { answered, error in
            DispatchQueue.main.async {
                if let error {
                    completion(
                        NativeHealthReadState(
                            availabilityId: Self.availableId,
                            authorizationId: Self.restrictedId,
                            detail: "Health authorization could not be requested: \(error.localizedDescription)"
                        )
                    )
                    return
                }
                completion(
                    NativeHealthReadState(
                        availabilityId: Self.availableId,
                        authorizationId: answered
                            ? Self.requestPresentedId
                            : NativeEvidenceIdentifiers.notDetermined,
                        detail: "iOS does not disclose read permission; only the query outcome is observable."
                    )
                )
            }
        }
    }

    /// Returns a shared `HealthQueryOutcome` identifier. An empty result stays
    /// ambiguous on purpose: it can mean no access or no samples.
    func readLatestBodyMassKilograms(completion: @escaping (String, Double?) -> Void) {
        guard
            HKHealthStore.isHealthDataAvailable(),
            let bodyMass = HKObjectType.quantityType(forIdentifier: .bodyMass)
        else {
            completion(Self.emptyOutcomeId, nil)
            return
        }

        let newestFirst = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        let query = HKSampleQuery(
            sampleType: bodyMass,
            predicate: nil,
            limit: 1,
            sortDescriptors: [newestFirst]
        ) { _, samples, _ in
            let value = (samples?.first as? HKQuantitySample)?
                .quantity
                .doubleValue(for: HKUnit.gramUnit(with: .kilo))
            DispatchQueue.main.async {
                if let value {
                    completion(Self.samplesReturnedId, value)
                } else {
                    completion(Self.emptyOutcomeId, nil)
                }
            }
        }
        store.execute(query)
    }
}

/// Local notifications are reminders, not guaranteed alarm-clock delivery.
///
/// Recurrence uses `UNCalendarNotificationTrigger` with wall-clock components
/// computed by shared `ReminderPlanner`, so the system re-resolves them after a
/// time-zone change, a DST transition, or a reboot. A fixed time-interval trigger
/// cannot do that and is not used. AlarmKit is assessed in shared code and is not
/// linked here; no challenge-to-dismiss behaviour is implemented or claimed.
struct NativeReminderRequest {
    let requestId: String
    let appleWeekday: Int
    let hour: Int
    let minute: Int
    let title: String
    let body: String
}

final class NativeReminderBridge {
    private let center = UNUserNotificationCenter.current()

    func currentAuthorizationId(completion: @escaping (String) -> Void) {
        center.getNotificationSettings { settings in
            DispatchQueue.main.async {
                completion(Self.identifier(for: settings.authorizationStatus))
            }
        }
    }

    func requestAuthorization(completion: @escaping (String) -> Void) {
        center.requestAuthorization(options: [.alert, .sound]) { _, _ in
            self.currentAuthorizationId(completion: completion)
        }
    }

    /// `requests` must come from a shared `ReminderPlan`; an empty plan schedules
    /// nothing, which is the correct outcome for denied or undetermined access.
    func replaceSchedule(
        with requests: [NativeReminderRequest],
        cancelledRequestIds: [String],
        completion: @escaping (Result<Int, Error>) -> Void
    ) {
        if !cancelledRequestIds.isEmpty {
            center.removePendingNotificationRequests(withIdentifiers: cancelledRequestIds)
        }
        guard !requests.isEmpty else {
            completion(.success(0))
            return
        }

        let group = DispatchGroup()
        var failure: Error?

        for request in requests {
            var components = DateComponents()
            components.weekday = request.appleWeekday
            components.hour = request.hour
            components.minute = request.minute

            let content = UNMutableNotificationContent()
            content.title = request.title
            content.body = request.body
            content.sound = .default

            let notification = UNNotificationRequest(
                identifier: request.requestId,
                content: content,
                trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
            )

            group.enter()
            center.add(notification) { error in
                // Serialised on main so the shared `failure` slot has one writer.
                DispatchQueue.main.async {
                    if let error, failure == nil {
                        failure = error
                    }
                    group.leave()
                }
            }
        }

        group.notify(queue: .main) {
            if let failure {
                completion(.failure(failure))
            } else {
                completion(.success(requests.count))
            }
        }
    }

    func cancelAll() {
        center.removeAllPendingNotificationRequests()
    }

    private static func identifier(for status: UNAuthorizationStatus) -> String {
        switch status {
        case .authorized:
            return NativeEvidenceIdentifiers.authorized
        case .provisional, .ephemeral:
            return NativeEvidenceIdentifiers.provisional
        case .denied:
            return NativeEvidenceIdentifiers.denied
        case .notDetermined:
            return NativeEvidenceIdentifiers.notDetermined
        @unknown default:
            return NativeEvidenceIdentifiers.notDetermined
        }
    }
}
