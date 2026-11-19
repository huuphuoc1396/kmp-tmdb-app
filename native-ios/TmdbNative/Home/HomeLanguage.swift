import TmdbCore

enum HomeLanguage {
    /// The language of the app's copy (`Bundle.main.preferredLocalizations`), so title data matches the copy.
    static func from(preferredLocalizations: [String]) -> Language {
        Language.companion.fromLocaleTag(tag: preferredLocalizations.first ?? "en")
    }
}
