import SwiftUI

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
            FavoriteHeart(isFavorite: configuration.isOn)
                .frame(width: TmdbSize.touchTarget, height: TmdbSize.touchTarget)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}
