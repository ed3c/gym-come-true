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

    private let scanner = NativeLabelEvidenceBridge()
    private let reminders = NativeReminderBridge()

    var body: some View {
        ZStack(alignment: .bottom) {
            ComposeHostView()
                .ignoresSafeArea(.keyboard)

            VStack(alignment: .leading, spacing: 10) {
                Text(nativeStatus)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)

                HStack {
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Label(isScanning ? "Scanning…" : "Scan label photo", systemImage: "doc.text.viewfinder")
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isScanning)

                    Button {
                        reminders.requestAuthorizationAndSchedule(after: 60) { result in
                            nativeStatus = result.fold(
                                success: "Reminder scheduled. Delivery is not guaranteed like a system alarm.",
                                failurePrefix: "Reminder failed"
                            )
                        }
                    } label: {
                        Label("Test reminder", systemImage: "bell")
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(14)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
            .padding()
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

                    scanner.scan(image: image) { result in
                        isScanning = false
                        nativeStatus = result.fold(
                            success: { $0.summary },
                            failurePrefix: "Label extraction failed"
                        )
                        selectedPhoto = nil
                    }
                } catch {
                    isScanning = false
                    nativeStatus = "Unable to load the selected image: \(error.localizedDescription)"
                    selectedPhoto = nil
                }
            }
        }
    }
}

private extension Result where Failure == Error {
    func fold(
        success: @autoclosure () -> String,
        failurePrefix: String
    ) -> String {
        switch self {
        case .success:
            return success()
        case .failure(let error):
            return "\(failurePrefix): \(error.localizedDescription)"
        }
    }

    func fold(
        success: (Success) -> String,
        failurePrefix: String
    ) -> String {
        switch self {
        case .success(let value):
            return success(value)
        case .failure(let error):
            return "\(failurePrefix): \(error.localizedDescription)"
        }
    }
}
