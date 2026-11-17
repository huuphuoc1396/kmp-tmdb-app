import SwiftUI

struct OnboardingView: View {
    @StateObject private var viewModel: OnboardingViewModel
    private let onFinished: () -> Void

    init(settings: any OnboardingSettings, onFinished: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: OnboardingViewModel(settings: settings))
        self.onFinished = onFinished
    }

    var body: some View {
        OnboardingContent(state: viewModel.state, onIntent: viewModel.onIntent)
            .task {
                for await effect in viewModel.effects {
                    switch effect {
                    case .navigateToHome:
                        onFinished()
                    }
                }
            }
    }
}

private let artwork = LinearGradient(
    stops: [
        .init(color: Color(red: 0x30 / 255, green: 0x32 / 255, blue: 0x43 / 255), location: 0),
        .init(color: Color(red: 0x3D / 255, green: 0x45 / 255, blue: 0x8F / 255), location: 0.56527),
    ],
    startPoint: .top,
    endPoint: .bottom
)

private let scrimColor = Color(red: 21 / 255, green: 21 / 255, blue: 29 / 255)

private let scrim = LinearGradient(
    stops: [
        .init(color: scrimColor.opacity(0), location: 0.3089),
        .init(color: scrimColor.opacity(0.85), location: 0.62685),
    ],
    startPoint: .top,
    endPoint: .bottom
)

struct OnboardingContent: View {
    let state: OnboardingUiState
    let onIntent: (OnboardingIntent) -> Void

    @ScaledMetric(relativeTo: .largeTitle) private var headlineSize: CGFloat = 32
    @ScaledMetric(relativeTo: .largeTitle) private var headlineLineSpacing: CGFloat = 5.3
    @ScaledMetric(relativeTo: .subheadline) private var bodySize: CGFloat = 15
    @ScaledMetric(relativeTo: .subheadline) private var bodyLineSpacing: CGFloat = 3.75

    var body: some View {
        ZStack {
            artwork
                .ignoresSafeArea()
                .accessibilityHidden(true)
            scrim
                .ignoresSafeArea()
                .accessibilityHidden(true)
            GeometryReader { geometry in
                ScrollView {
                    VStack(alignment: .leading, spacing: 19) {
                        Image(decorative: "TmdbLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(height: 15)
                        Text("onboarding.headline")
                            .font(.system(size: headlineSize, weight: .light))
                            .lineSpacing(headlineLineSpacing)
                            .fixedSize(horizontal: false, vertical: true)
                            .accessibilityAddTraits(.isHeader)
                        Text("onboarding.body")
                            .font(.system(size: bodySize))
                            .lineSpacing(bodyLineSpacing)
                            .fixedSize(horizontal: false, vertical: true)
                        GetStartedButton(enabled: !state.isNavigating) {
                            onIntent(.getStartedTapped)
                        }
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: 315, alignment: .leading)
                    .padding(.leading, 29)
                    .padding(.trailing, 31)
                    .padding(.bottom, 31)
                    .frame(maxWidth: .infinity, minHeight: geometry.size.height, alignment: .bottom)
                }
            }
        }
    }
}

#Preview("English") {
    OnboardingContent(state: OnboardingUiState(), onIntent: { _ in })
}

#Preview("Portuguese") {
    OnboardingContent(state: OnboardingUiState(), onIntent: { _ in })
        .environment(\.locale, Locale(identifier: "pt-BR"))
}

#Preview("Largest text") {
    OnboardingContent(state: OnboardingUiState(), onIntent: { _ in })
        .environment(\.dynamicTypeSize, .accessibility5)
}
