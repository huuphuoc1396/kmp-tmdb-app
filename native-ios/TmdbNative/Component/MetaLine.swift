func metaLine(year: Int?, genre: String?) -> String {
    [year.map(String.init), genre].compactMap { $0 }.joined(separator: " · ")
}
