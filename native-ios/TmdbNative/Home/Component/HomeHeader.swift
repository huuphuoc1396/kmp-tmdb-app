import SwiftUI

struct HomeHeader: View {
    @ScaledMetric(relativeTo: .title) private var titleSize: CGFloat = 26

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("home.title")
                .font(.system(size: titleSize, weight: .bold))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            Avatar()
                .padding(.top, 7)
        }
    }
}

private struct Avatar: View {
    var body: some View {
        Image(systemName: "person.fill")
            .font(.system(size: 20))
            .foregroundStyle(.white.opacity(0.5))
            .frame(width: 40, height: 40)
            .background(Theme.surfaceVariant, in: Circle())
            .accessibilityHidden(true)
    }
}
