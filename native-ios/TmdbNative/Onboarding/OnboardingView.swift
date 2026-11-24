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
        .init(color: Color("OnboardingArtworkTop"), location: 0),
        .init(color: Color("OnboardingArtworkBottom"), location: 0.56527),
    ],
    startPoint: .top,
    endPoint: .bottom
)

private let scrim = LinearGradient(
    stops: [
        .init(color: TmdbColor.surface.opacity(0), location: 0.3089),
        .init(color: TmdbColor.surface.opacity(0.85), location: 0.62685),
    ],
    startPoint: .top,
    endPoint: .bottom
)

struct OnboardingContent: View {
    let state: OnboardingUiState
    let onIntent: (OnboardingIntent) -> Void

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
                    VStack(alignment: .leading, spacing: TmdbSpacing.lg) {
                        Image(decorative: "TmdbLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(height: 15)
                        Text("onboarding.headline")
                            .tmdbFont(.headlineLarge, weight: .light)
                            .fixedSize(horizontal: false, vertical: true)
                            .accessibilityAddTraits(.isHeader)
                        Text("onboarding.body")
                            .tmdbFont(.bodyLarge)
                            .fixedSize(horizontal: false, vertical: true)
                        GetStartedButton(enabled: !state.isNavigating) {
                            onIntent(.getStartedTapped)
                        }
                    }
                    .foregroundStyle(TmdbColor.onSurface)
                    .frame(maxWidth: 320, alignment: .leading)
                    .padding(.horizontal, TmdbSpacing.xxl)
                    .padding(.bottom, TmdbSpacing.xxl)
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
