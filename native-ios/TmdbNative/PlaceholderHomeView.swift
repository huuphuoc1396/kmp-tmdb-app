import SwiftUI

struct PlaceholderHomeView: View {
    var body: some View {
        ZStack {
            Theme.night.ignoresSafeArea()
            Text("Home").foregroundStyle(.white)
        }
    }
}
