import SwiftUI

struct GetStartedButton: View {
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("onboarding.getStarted")
                .tmdbFont(.titleMedium)
                .foregroundStyle(TmdbColor.onPrimary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: TmdbSize.touchTarget)
                .padding(.horizontal, TmdbSpacing.sm)
        }
        .buttonStyle(GradientPillButtonStyle())
        .disabled(!enabled)
    }
}

private struct GradientPillButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                LinearGradient(
                    colors: [TmdbColor.primary, TmdbColor.primaryContainer],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: Capsule()
            )
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}
