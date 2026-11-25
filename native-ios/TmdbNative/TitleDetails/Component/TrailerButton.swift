import SwiftUI

private let gradient = LinearGradient(
    colors: [TmdbColor.primary, TmdbColor.primaryContainer],
    startPoint: .topLeading,
    endPoint: .bottomTrailing
)

struct TrailerButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: TmdbSpacing.sm) {
                Image("PlayCircle")
                    .resizable()
                    .frame(width: TmdbSize.iconSmall, height: TmdbSize.iconSmall)
                    .accessibilityHidden(true)
                Text("details.watchTrailer")
                    .tmdbFont(.titleMedium)
                    .foregroundStyle(TmdbColor.onPrimary)
                    .multilineTextAlignment(.center)
            }
            .padding(TmdbSpacing.sm)
            .frame(maxWidth: .infinity, minHeight: TmdbSize.touchTarget)
            .background(gradient, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
