import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        SharedCoreKt.setUpSharedCore()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
