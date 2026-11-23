import TmdbCore

enum HomeLanguage {
    static func from(preferredLocalizations: [String]) -> Language {
        Language.companion.fromLocaleTag(tag: preferredLocalizations.first ?? "en")
    }
}
