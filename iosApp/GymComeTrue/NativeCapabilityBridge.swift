import CryptoKit
import Foundation
import UIKit
import UserNotifications
import Vision

struct NativeScanEvidence {
    let recognizedText: String
    let rawTextSHA256: String
    let barcode: String?

    var summary: String {
        let lines = recognizedText
            .split(whereSeparator: \.isNewline)
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        let barcodePart = barcode.map { " Barcode: \($0.prefix(32))." } ?? " No barcode detected."
        return "Recognized \(lines.count) non-empty lines. Evidence hash: \(rawTextSHA256.prefix(12))…\(barcodePart) Confirm against the physical label."
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
                        barcode: barcode
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

/// Local notifications are reminders, not guaranteed alarm-clock delivery.
/// AlarmKit and any challenge-to-dismiss flow require a separate reviewed phase.
final class NativeReminderBridge {
    func requestAuthorizationAndSchedule(
        after seconds: TimeInterval = 60,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, error in
            if let error {
                completion(.failure(error))
                return
            }
            guard granted else {
                completion(.success(()))
                return
            }

            let content = UNMutableNotificationContent()
            content.title = "Protocol checkpoint"
            content.body = "Confirm evidence and readiness before the next protocol step."
            content.sound = .default

            let trigger = UNTimeIntervalNotificationTrigger(
                timeInterval: max(1, seconds),
                repeats: false
            )
            let request = UNNotificationRequest(
                identifier: "protocol-checkpoint-\(UUID().uuidString)",
                content: content,
                trigger: trigger
            )
            center.add(request) { addError in
                if let addError {
                    completion(.failure(addError))
                } else {
                    completion(.success(()))
                }
            }
        }
    }
}
