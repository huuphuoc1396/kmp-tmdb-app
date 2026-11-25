import SwiftUI
import TmdbCore
import UIKit

@main
struct TmdbNativeApp: App {
    init() {
        SharedCore.shared.start()
        Self.hideNavigationBarSeparator()
    }

    private static func hideNavigationBarSeparator() {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.shadowColor = .clear
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some Scene {
        WindowGroup {
            AppRootView(
                settings: SharedCoreOnboardingSettings(),
                titleLists: SharedCoreTitleLists(),
                titleDetails: SharedCoreTitleDetails(),
                favorites: SharedCoreFavorites(),
                favoriteTitles: SharedCoreFavoriteTitles()
            )
        }
    }
}
