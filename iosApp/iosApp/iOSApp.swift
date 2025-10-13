import SwiftUI
import shared

@main
struct iOSApp: App {
    private let koinApp = KoinIOSKt.doInitKoin()

    var body: some Scene {
        WindowGroup {
            ContentView(koin: koinApp.koin)
        }
    }
}