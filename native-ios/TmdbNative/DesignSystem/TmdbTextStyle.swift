import SwiftUI

enum TmdbTextStyle: CaseIterable {
    case displayLarge, displayMedium, displaySmall
    case headlineLarge, headlineMedium, headlineSmall
    case titleLarge, titleMedium, titleSmall
    case bodyLarge, bodyMedium, bodySmall
    case labelLarge, labelMedium, labelSmall

    var size: CGFloat {
        switch self {
        case .displayLarge: 57
        case .displayMedium: 45
        case .displaySmall: 36
        case .headlineLarge: 32
        case .headlineMedium: 28
        case .headlineSmall: 24
        case .titleLarge: 22
        case .titleMedium, .bodyLarge: 16
        case .titleSmall, .bodyMedium, .labelLarge: 14
        case .bodySmall, .labelMedium: 12
        case .labelSmall: 11
        }
    }

    var lineHeight: CGFloat {
        switch self {
        case .displayLarge: 64
        case .displayMedium: 52
        case .displaySmall: 44
        case .headlineLarge: 40
        case .headlineMedium: 36
        case .headlineSmall: 32
        case .titleLarge: 28
        case .titleMedium, .bodyLarge: 24
        case .titleSmall, .bodyMedium, .labelLarge: 20
        case .bodySmall, .labelMedium, .labelSmall: 16
        }
    }

    var tracking: CGFloat {
        switch self {
        case .displayLarge: -0.25
        case .titleMedium: 0.15
        case .titleSmall, .labelLarge: 0.1
        case .bodyLarge, .labelMedium, .labelSmall: 0.5
        case .bodyMedium: 0.25
        case .bodySmall: 0.4
        default: 0
        }
    }

    var weight: Font.Weight {
        switch self {
        case .displayLarge, .displayMedium, .displaySmall,
             .headlineLarge, .headlineMedium, .headlineSmall, .titleLarge:
            .bold
        case .titleMedium, .titleSmall, .labelLarge, .labelMedium, .labelSmall:
            .semibold
        case .bodyLarge, .bodyMedium, .bodySmall:
            .regular
        }
    }

    var relativeTo: Font.TextStyle {
        switch self {
        case .displayLarge, .displayMedium, .displaySmall, .headlineLarge: .largeTitle
        case .headlineMedium, .headlineSmall: .title
        case .titleLarge: .title2
        case .titleMedium, .bodyLarge: .body
        case .titleSmall, .bodyMedium, .labelLarge: .subheadline
        case .bodySmall, .labelMedium: .footnote
        case .labelSmall: .caption
        }
    }
}

struct TmdbFont: ViewModifier {
    private let style: TmdbTextStyle
    private let weight: Font.Weight
    @ScaledMetric private var size: CGFloat
    @ScaledMetric private var lineSpacing: CGFloat

    init(style: TmdbTextStyle, weight: Font.Weight?) {
        self.style = style
        self.weight = weight ?? style.weight
        _size = ScaledMetric(wrappedValue: style.size, relativeTo: style.relativeTo)
        _lineSpacing = ScaledMetric(wrappedValue: style.lineHeight - style.size, relativeTo: style.relativeTo)
    }

    func body(content: Content) -> some View {
        content
            .font(.system(size: size, weight: weight))
            .lineSpacing(lineSpacing)
            .tracking(style.tracking)
    }
}

extension View {
    func tmdbFont(_ style: TmdbTextStyle, weight: Font.Weight? = nil) -> some View {
        modifier(TmdbFont(style: style, weight: weight))
    }
}
