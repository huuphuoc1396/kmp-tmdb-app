import SwiftUI

struct SectionHeading: View {
    let text: LocalizedStringKey

    init(_ text: LocalizedStringKey) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .tmdbFont(.titleLarge)
            .foregroundStyle(TmdbColor.onSurface)
            .lineLimit(1)
            .truncationMode(.tail)
            .accessibilityAddTraits(.isHeader)
    }
}
