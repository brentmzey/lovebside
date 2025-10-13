import SwiftUI
import shared

struct ContentView: View {
    let koin: Koin_coreKoin

    var body: some View {
        ComposeView(koin: koin)
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let koin: Koin_coreKoin

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(koin: koin)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}



