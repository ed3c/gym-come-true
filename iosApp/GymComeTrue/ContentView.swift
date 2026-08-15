import SwiftUI
import UIKit
import GymComeTrueShared

struct ComposeHostView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Shared state is owned by the Kotlin presentation layer.
    }
}

struct ContentView: View {
    var body: some View {
        ComposeHostView()
            .ignoresSafeArea(.keyboard)
    }
}
