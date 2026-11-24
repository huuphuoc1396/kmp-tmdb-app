import SwiftUI
import UIKit

struct NameText: View {
    private let name: String
    private let style: TmdbTextStyle
    private let lineLimit: Int?
    private let alignment: TextAlignment

    @ScaledMetric private var size: CGFloat
    @ScaledMetric private var lineSpacing: CGFloat
    @State private var width: CGFloat?

    init(_ name: String, style: TmdbTextStyle, lineLimit: Int?, alignment: TextAlignment = .leading) {
        self.name = name
        self.style = style
        self.lineLimit = lineLimit
        self.alignment = alignment
        _size = ScaledMetric(wrappedValue: style.size, relativeTo: style.relativeTo)
        _lineSpacing = ScaledMetric(wrappedValue: style.lineSpacing, relativeTo: style.relativeTo)
    }

    var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: frameAlignment)
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .onAppear { width = geometry.size.width }
                        .onChange(of: geometry.size.width) { width = $0 }
                }
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: name))
    }

    @ViewBuilder
    private var content: some View {
        if let width {
            VStack(alignment: stackAlignment, spacing: lineSpacing) {
                ForEach(Array(lines(width: width).enumerated()), id: \.offset) { line in
                    Text(verbatim: line.element)
                        .tmdbFont(style)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
            }
        } else {
            Text(verbatim: name)
                .tmdbFont(style)
                .multilineTextAlignment(alignment)
                .lineLimit(lineLimit)
        }
    }

    private func lines(width: CGFloat) -> [String] {
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: size, weight: style.uiWeight),
            .kern: style.tracking,
        ]
        return NameLines.lines(of: name, width: width, lineLimit: lineLimit) {
            ceil(NSAttributedString(string: $0, attributes: attributes).size().width)
        }
    }

    private var stackAlignment: HorizontalAlignment {
        switch alignment {
        case .leading: .leading
        case .center: .center
        case .trailing: .trailing
        }
    }

    private var frameAlignment: Alignment {
        switch alignment {
        case .leading: .leading
        case .center: .center
        case .trailing: .trailing
        }
    }
}

private extension TmdbTextStyle {
    var uiWeight: UIFont.Weight {
        switch weight {
        case .bold: .bold
        case .semibold: .semibold
        default: .regular
        }
    }
}
