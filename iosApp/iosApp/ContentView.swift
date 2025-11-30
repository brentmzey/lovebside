import SwiftUI
import ComposeApp

struct ContentView: View {
    let koin: Koin

    var body: some View {
        ComposeView(koin: koin)
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let koin: Koin

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(koin: koin)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
