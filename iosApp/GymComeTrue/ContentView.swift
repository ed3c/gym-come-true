import GymComeTrueShared
import PhotosUI
import SwiftUI
import UIKit

struct ComposeHostView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Shared state is owned by the Kotlin presentation layer.
    }
}

struct ContentView: View {
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var nativeStatus = "iOS Vision creates unverified evidence. Confirm every field."
    @State private var isScanning = false
    @State private var isShowingCamera = false

    private let scanner = NativeLabelEvidenceBridge()
    private let reminders = NativeReminderBridge()
    private let health = NativeHealthReadBridge()

    var body: some View {
        ZStack(alignment: .bottom) {
            ComposeHostView()
                .ignoresSafeArea(.keyboard)

            VStack(alignment: .leading, spacing: 10) {
                Text(nativeStatus)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(4)

                HStack {
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Label(isScanning ? "Scanning…" : "Scan label photo", systemImage: "doc.text.viewfinder")
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isScanning)

                    Button {
                        startCameraCapture()
                    } label: {
                        Label("Camera", systemImage: "camera")
                    }
                    .buttonStyle(.bordered)
                    .disabled(isScanning)
                }

                HStack {
                    Button {
                        scheduleProtocolReminders()
                    } label: {
                        Label("Protocol reminders", systemImage: "bell")
                    }
                    .buttonStyle(.bordered)

                    Button {
                        requestHealthReads()
                    } label: {
                        Label("Health reads", systemImage: "heart.text.square")
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(14)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
            .padding()
        }
        .sheet(isPresented: $isShowingCamera) {
            NativeCameraCaptureView { image in
                isShowingCamera = false
                guard let image else {
                    nativeStatus = "Camera capture was cancelled. Nothing was stored."
                    return
                }
                scan(image: image, sourceId: NativeEvidenceIdentifiers.camera, authorizationId: NativeCameraAccess.authorizationId)
            }
            .ignoresSafeArea()
        }
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            isScanning = true
            nativeStatus = "Reading the selected image on device…"

            Task {
                do {
                    guard
                        let data = try await item.loadTransferable(type: Data.self),
                        let image = UIImage(data: data)
                    else {
                        throw NativeScanError.missingCGImage
                    }

                    // PhotosPicker runs out of process: the explicit pick is the
                    // authorization, and no photo-library permission is requested.
                    scan(
                        image: image,
                        sourceId: NativeEvidenceIdentifiers.photoLibrary,
                        authorizationId: NativeEvidenceIdentifiers.authorized
                    )
                    selectedPhoto = nil
                } catch {
                    isScanning = false
                    nativeStatus = "Unable to load the selected image: \(error.localizedDescription)"
                    selectedPhoto = nil
                }
            }
        }
    }

    private func startCameraCapture() {
        guard NativeCameraAccess.isCameraAvailable else {
            nativeStatus = "No camera is available on this device. Use a label photo instead."
            return
        }
        NativeCameraAccess.requestAccess { authorizationId in
            if authorizationId == NativeEvidenceIdentifiers.authorized {
                self.isShowingCamera = true
            } else {
                self.nativeStatus = "Camera access is \(authorizationId). Nothing was captured."
            }
        }
    }

    /// The single seam into shared code: Vision output crosses as primitives and
    /// shared `EvidenceHandoff` decides whether it may become evidence at all.
    private func scan(image: UIImage, sourceId: String, authorizationId: String) {
        isScanning = true
        scanner.scan(
            image: image,
            captureSourceId: sourceId,
            captureAuthorizationId: authorizationId
        ) { result in
            self.isScanning = false
            switch result {
            case .success(let evidence):
                let handoff = EvidenceHandoff.shared.acceptFromNative(
                    sourceId: evidence.captureSourceId,
                    authorizationId: evidence.captureAuthorizationId,
                    recognizedText: evidence.recognizedText,
                    rawTextSha256: evidence.rawTextSHA256,
                    barcode: evidence.barcode,
                    retentionId: evidence.rawPixelRetentionId
                )
                self.nativeStatus = handoff.summary
            case .failure(let error):
                self.nativeStatus = "Label extraction failed: \(error.localizedDescription)"
            }
        }
    }

    /// Recurrence is decided by shared `ReminderPlanner`; iOS only registers the
    /// wall-clock components it returns. Denied access produces an empty plan.
    private func scheduleProtocolReminders() {
        reminders.requestAuthorization { authorizationId in
            let plan = ReminderPlanner.shared.planForNative(
                variantId: "AFTERNOON_1600",
                dayIds: ["MONDAY", "WEDNESDAY", "FRIDAY"],
                authorizationId: authorizationId,
                cancelledRequestIds: []
            )
            let requests = plan.occurrences.map { occurrence in
                NativeReminderRequest(
                    requestId: occurrence.requestId,
                    appleWeekday: Int(occurrence.appleWeekday),
                    hour: Int(occurrence.hour),
                    minute: Int(occurrence.minute),
                    title: occurrence.title,
                    body: occurrence.body
                )
            }
            self.reminders.replaceSchedule(
                with: requests,
                cancelledRequestIds: plan.cancelledRequestIds
            ) { result in
                switch result {
                case .success(let count):
                    let warning = plan.warnings.first ?? ""
                    self.nativeStatus = "Scheduled \(count) recurring reminder(s). \(warning)"
                case .failure(let error):
                    self.nativeStatus = "Reminder scheduling failed: \(error.localizedDescription)"
                }
            }
        }
    }

    /// The read set comes from shared `HealthReadPolicy`, which is the tested owner
    /// of least privilege. iOS never reveals read authorization, so an empty result
    /// is reported as ambiguous rather than as "no data".
    private func requestHealthReads() {
        let identifiers = HealthReadPolicy.shared.appleReadIdentifiers(
            enabledFeatureIds: ["WEIGHT_TREND_CARD"]
        )
        health.requestReadAccess(appleTypeIdentifiers: identifiers) { state in
            guard state.availabilityId == NativeHealthReadBridge.availableId else {
                self.nativeStatus = state.detail
                return
            }
            self.health.readLatestBodyMassKilograms { outcomeId, value in
                if outcomeId == NativeHealthReadBridge.samplesReturnedId, let value {
                    self.nativeStatus = String(
                        format: "Latest body mass on this device: %.1f kg. Health data stays local.",
                        value
                    )
                } else {
                    self.nativeStatus = "The Health read returned nothing. iOS does not reveal read permission, "
                        + "so this can mean either no samples or no access."
                }
            }
        }
    }
}
