import TmdbCore

enum TitleDetailsFormat {
    static func releaseDate(_ date: ReleaseDate, language: Language) -> String {
        let day = padded(date.day, to: 2)
        let month = padded(date.month, to: 2)
        let year = padded(date.year, to: 4)
        switch language {
        case .ptBr:
            return "\(day)/\(month)/\(year)"
        case .enUs:
            return "\(month)/\(day)/\(year)"
        }
    }

    static func runtime(minutes: Int) -> String {
        let hours = minutes / 60
        let rest = minutes % 60
        if hours == 0 { return "\(rest)m" }
        if rest == 0 { return "\(hours)h" }
        return "\(hours)h \(rest)m"
    }

    static func nameWithYear(_ name: String, year: Int?) -> String {
        guard let year else { return name }
        return "\(name)\u{00A0}(\(year))"
    }

    private static func padded(_ value: Int, to width: Int) -> String {
        let text = String(value)
        return String(repeating: "0", count: max(0, width - text.count)) + text
    }
}
