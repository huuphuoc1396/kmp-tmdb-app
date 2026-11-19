import SwiftUI

/// Stands in for the title details screen until that feature exists: shows the title's name.
struct TitleDetailsPlaceholderView: View {
    let title: TitleItem

    var body: some View {
        ZStack {
            Theme.night.ignoresSafeArea()
            Text(title.name)
                .font(.title2.weight(.semibold))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .padding(homeScreenPadding)
        }
        .toolbar(.visible, for: .navigationBar)
    }
}
