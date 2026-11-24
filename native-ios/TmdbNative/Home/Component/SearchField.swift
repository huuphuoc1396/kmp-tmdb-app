import SwiftUI

struct SearchField: View {
    var body: some View {
        HStack {
            Text("home.searchHint")
                .tmdbFont(.bodyLarge)
                .foregroundStyle(TmdbColor.onSurfaceVariant)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Image(decorative: "SearchIcon")
                .scaleEffect(x: -1)
        }
        .padding(.leading, TmdbSpacing.lg)
        .padding(.trailing, TmdbSpacing.md)
        .frame(minHeight: 48)
        .background(TmdbColor.surfaceVariant, in: Capsule())
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
