import CoreGraphics

enum NameLines {
    static let ellipsis = "…"

    static func lines(
        of name: String,
        width: CGFloat,
        lineLimit: Int?,
        measure: (String) -> CGFloat
    ) -> [String] {
        var lines: [String] = []
        var current = ""
        for word in name.split(whereSeparator: \.isWhitespace).map(String.init) {
            let candidate = current.isEmpty ? word : current + " " + word
            if measure(candidate) <= width {
                current = candidate
                continue
            }
            if !current.isEmpty {
                lines.append(current)
                current = ""
            }
            if measure(word) <= width {
                current = word
            } else {
                lines.append(cut(word, width: width, measure: measure))
                break
            }
        }
        if !current.isEmpty {
            lines.append(current)
        }
        guard let lineLimit, lines.count > lineLimit, lineLimit > 0 else { return lines }
        var visible = Array(lines.prefix(lineLimit))
        let last = visible[lineLimit - 1]
        let base = last.hasSuffix(ellipsis) ? String(last.dropLast()) : last
        visible[lineLimit - 1] = cut(base, width: width, measure: measure, forced: true)
        return visible
    }

    private static func cut(
        _ text: String,
        width: CGFloat,
        measure: (String) -> CGFloat,
        forced: Bool = false
    ) -> String {
        if !forced, measure(text) <= width {
            return text
        }
        var kept = text
        while kept.count > 1, measure(kept + ellipsis) > width {
            kept.removeLast()
            while kept.last?.isWhitespace == true {
                kept.removeLast()
            }
        }
        return kept + ellipsis
    }
}
