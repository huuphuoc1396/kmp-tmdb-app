import SwiftUI

/// The heart alone, like the system back button: the navigation bar draws the button's background. A `Toggle`, so
/// VoiceOver reads it as the "Favorite" switch with its on/off value; each tap reports once and the shown value is
/// always the stored one.
struct FavoriteToggle: View {
    let isOn: Bool
    let onTap: () -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: { _ in onTap() })) {
            Text("details.favorite")
        }
        .toggleStyle(FavoriteToggleStyle())
    }
}

private struct FavoriteToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            Image(configuration.isOn ? "FavoriteOn" : "FavoriteOff")
                .resizable()
                .frame(width: 32, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}
