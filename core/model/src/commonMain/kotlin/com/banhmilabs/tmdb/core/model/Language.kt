package com.banhmilabs.tmdb.core.model

enum class Language(val tag: String) {
    PT_BR("pt-BR"),
    EN_US("en-US"),
    VI_VN("vi-VN"),
    ;

    companion object {
        private const val VIETNAMESE = "vi"

        fun fromLocaleTag(tag: String): Language {
            val normalized = tag.replace('_', '-')
            return when {
                normalized.equals(PT_BR.tag, ignoreCase = true) -> PT_BR
                normalized.substringBefore('-').equals(VIETNAMESE, ignoreCase = true) -> VI_VN
                else -> EN_US
            }
        }
    }
}
