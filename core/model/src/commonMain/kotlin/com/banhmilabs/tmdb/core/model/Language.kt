package com.banhmilabs.tmdb.core.model

enum class Language(val tag: String) {
    PT_BR("pt-BR"),
    EN_US("en-US"),
    ;

    companion object {
        fun fromLocaleTag(tag: String): Language =
            if (tag.replace('_', '-').equals(PT_BR.tag, ignoreCase = true)) PT_BR else EN_US
    }
}
