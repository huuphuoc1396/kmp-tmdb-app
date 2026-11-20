import SwiftUI
import TmdbCore

@main
struct TmdbNativeApp: App {
    init() {
        SharedCore.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            AppRootView(settings: SharedCoreOnboardingSettings(), titleLists: SharedCoreTitleLists())
        }
    }
}
