import SwiftUI

struct GetStartedButton: View {
    let enabled: Bool
    let action: () -> Void

    @ScaledMetric(relativeTo: .body) private var labelSize: CGFloat = 16

    var body: some View {
        Button(action: action) {
            Text("onboarding.getStarted")
                .font(.system(size: labelSize, weight: .semibold))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 48)
                .padding(.horizontal, 12)
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
                    colors: [Theme.primary, Theme.primaryContainer],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: Capsule()
            )
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}
