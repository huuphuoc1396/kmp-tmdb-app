import SwiftUI

struct HomeHeader: View {
    var body: some View {
        HStack(alignment: .top, spacing: TmdbSpacing.xs) {
            Text("home.title")
                .tmdbFont(.headlineMedium)
                .foregroundStyle(TmdbColor.onSurface)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            Avatar()
                .padding(.top, TmdbSpacing.xs)
        }
    }
}

private struct Avatar: View {
    var body: some View {
        Image(systemName: "person.fill")
            .font(.system(size: TmdbSize.iconMedium))
            .foregroundStyle(TmdbColor.onSurface.opacity(0.5))
            .frame(width: 40, height: 40)
            .background(TmdbColor.surfaceVariant, in: Circle())
            .accessibilityHidden(true)
    }
}
