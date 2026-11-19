package com.banhmilabs.tmdb.core.model

/** Language of TMDB content; [tag] is sent as `language`. */
enum class Language(val tag: String) {
    PT_BR("pt-BR"),
    EN_US("en-US"),
    ;

    companion object {
        /** `pt-BR` (any case, `-` or `_`) -> [PT_BR]; every other device language -> [EN_US]. */
        fun fromLocaleTag(tag: String): Language =
            if (tag.replace('_', '-').equals(PT_BR.tag, ignoreCase = true)) PT_BR else EN_US
    }
}
