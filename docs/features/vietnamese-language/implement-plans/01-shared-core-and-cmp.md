# Plan: Shared core and cmp in Vietnamese

Requirement: [01 - Shared core and cmp in Vietnamese](../subtasks/01-shared-core-and-cmp.md)
Status: Done

## Summary
Add `VI_VN("vi-VN")` to `Language` and map every `vi` locale to it. Because the cache already keys rows by
`language.tag` (TEXT columns), Vietnamese gets its own cache with no schema change, and TMDB requests already send
`language.tag`. The exhaustive `when`s on `Language` (trailer language, release date format) gain a Vietnamese branch,
runtime formatting becomes language-aware, the favorites fallback becomes an explicit order (en-US, then pt-BR), and
every `cmp` string file gets a `values-vi` copy. The `cmp` iOS app declares `vi` so iOS reports it as the locale.

## Decisions
- Any locale whose language subtag is `vi` maps to `VI_VN`; pt-BR keeps its exact `pt-BR` match (AC-6: `pt-PT`
  and `pt` still map to English) — from task.md constraints.
- Compose resource qualifier `values-vi` (language only), so `vi-VN`, `vi` and `vi-US` all resolve to it (auto) —
  matches the "region ignored" rule; rejected: `values-vi-rVN`, which would miss `vi-US`.
- No database migration: `language` columns are TEXT holding `Language.tag`, so `vi-VN` rows sit beside the others
  (auto) — rejected: a language table or enum column; not needed.
- Trailer language code for Vietnamese is `vi` (TMDB `iso_639_1`), English fallback unchanged (auto) — mirrors `pt`.
- Vietnamese release date: `"<day> thg <month>, <year>"`, day and month not padded (`26 thg 11, 2026`,
  `5 thg 3, 1999`), as in the AC-5 example and the CLDR Vietnamese medium date (auto); rejected: numeric
  `dd/MM/yyyy`, which would not match the confirmed AC-5 example.
- Vietnamese runtime: `"2 giờ 10 phút"`, `"2 giờ"`, `"45 phút"`; English and pt-BR stay `"2h 10m"` (auto) —
  AC-5 example; rejected: a shared string resource, since the existing formatter is a pure function tested in
  `commonTest`.
- Favorites fallback order: `EN_US`, `PT_BR`, `VI_VN`, minus the current language; the first non-null summary wins
  (auto) — AC-4 for Vietnamese; for en and pt-BR the first fallback is the same as today (pt-BR and en-US), so AC-6
  holds; Vietnamese is only a last resort for them.
- Vietnamese copy drafted here from the English strings (auto) — task.md open question says Claude drafts and the user
  reviews; listed for review in the foundry report. `details_not_rated` keeps the short badge `NR`, its spoken
  description is Vietnamese (auto) — the badge has room for two or three letters only.
- `favorites_count` in Vietnamese has only the `other` plural item (auto) — Vietnamese has no singular plural form.
- Previews: add a `Tiếng Việt` (`locale = "vi"`) entry to `TmdbPreviews` and `TmdbComponentPreviews` (auto) — every
  screen and component then shows Vietnamese without touching each preview; rejected: per-file previews.
- `cmp/iosApp` `Info.plist` `CFBundleLocalizations` gains `vi` (auto) — without it iOS reports English for a
  Vietnamese device, so `Locale.current` would never be `vi`.
- `native-android` and `native-ios` are not changed here; they keep building against the previously published core
  until 02 / 03 republish it.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Language model and locale mapping | `Language`, `Language.fromLocaleTag` | `core/model/.../Language.kt` |
| Per-language cache | `language` TEXT key in `TitleList.sq`, `TitleDetails.sq` | `service/title/data/.../generated/` |
| Trailer choice | `trailerKey`, `videoLanguage` | `service/title/data/.../details/remote/TitleDetailsRemoteDataSource.kt` |
| Favorites with fallback | `ObserveFavoriteTitles` | `service/title/domain/.../ObserveFavoriteTitles.kt` |
| Date / runtime formatting | `formatReleaseDate`, `formatRuntime` | `cmp/feature/title-details/ui/.../TitleDetailsFormat.kt` |
| Previews | `TmdbPreviews`, `TmdbComponentPreviews` | `cmp/designsystem/.../preview/` |
| Test doubles | `FakeTitleDetailsRepository`, `FakeFavoritesRepository`, `DetailsDtos` | `service/title/fake`, `service/favorites/fake`, data tests |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `core:model` | `Language.kt` (mod), `LanguageTest.kt` (mod) | `VI_VN`, `vi` mapping |
| `service:title:data` | `TitleDetailsRemoteDataSource.kt` (mod), `TrailerSelectionTest.kt` (mod) | Vietnamese trailer language |
| `service:title:domain` | `ObserveFavoriteTitles.kt` (mod), `ObserveFavoriteTitlesTest.kt` (mod) | Fallback order |
| `cmp:feature:title-details:ui` | `TitleDetailsFormat.kt`, `DetailsHeader.kt`, `TitleDetailsFormatTest.kt` (mod); `values-vi/strings.xml` (new) | Vietnamese date, runtime, copy |
| `cmp:feature:home:ui`, `cmp:feature:favorites:ui`, `cmp:feature:onboarding:ui`, `cmp:designsystem` | `values-vi/strings.xml` (new); designsystem `preview/*.kt` (mod) | Vietnamese copy, previews |
| `cmp/iosApp` | `iosApp/Info.plist` (mod) | Declare `vi` |
| Affected (public API of `core:model`) | every module that switches on `Language` | compiled in Step 1 |

## Steps
### Step 1: Vietnamese language in the shared core and exhaustive `when`s  (AC-2, AC-3, AC-5, AC-6, EC-1, EC-3) ✅
- Files: `core/model/src/commonMain/.../Language.kt` (modify),
  `service/title/data/src/commonMain/.../details/remote/TitleDetailsRemoteDataSource.kt` (modify),
  `cmp/feature/title-details/ui/src/commonMain/.../TitleDetailsFormat.kt` (modify),
  `cmp/feature/title-details/ui/src/commonMain/.../component/DetailsHeader.kt` (modify)
- What:
  - `enum class Language(val tag: String) { PT_BR("pt-BR"), EN_US("en-US"), VI_VN("vi-VN") }`.
  - `fromLocaleTag(tag)`: normalize `_` to `-`; exact `pt-BR` (ignore case) → `PT_BR`; language subtag
    (before the first `-`) equal to `vi` (ignore case) → `VI_VN`; otherwise `EN_US`.
  - `videoLanguage`: `VI_VN -> "vi"`.
  - `formatReleaseDate`: `VI_VN -> "${date.day} thg ${date.month.number}, $year"`.
  - `formatRuntime(minutes: Int, language: Language)`: Vietnamese units `giờ` / `phút` joined by spaces; others keep
    `h` / `m`. `DetailsHeader` passes its `language`: `details.runtimeMinutes?.let { formatRuntime(it, language) }`.
- Unit tests:
  - `LanguageTest`: `vietnameseMapsToViVn` (`vi-VN`, `vi`, `vi-US`, `vi_VN`, `VI`), `tagsAreTmdbLanguages`
    → `["pt-BR", "en-US", "vi-VN"]`; existing `otherLanguagesMapToEnUs` unchanged, plus `"vietnamese"`-like
    non-matches such as `"vin"`.
  - `TrailerSelectionTest`: `trailerPrefersVietnamese`, `vietnameseTrailerFallsBackToEnglish`.
  - `TitleDetailsFormatTest`: `vietnameseDateIsDayThgMonthYear` (`26 thg 11, 2026`, `5 thg 3, 1999`),
    `vietnameseRuntime` (`2 giờ 10 phút`, `2 giờ`, `45 phút`); existing runtime tests pass `Language.EN_US`.
- Compile: `./gradlew :core:model:compileAndroidMain :service:title:data:compileAndroidMain :service:title:domain:compileAndroidMain :service:favorites:data:compileAndroidMain :core:di:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidMain :cmp:feature:favorites:ui:compileAndroidMain :cmp:composeApp:compileAndroidMain`

### Step 2: Favorites fallback order  (AC-4, AC-6, EC-5) ✅
- Files: `service/title/domain/src/commonMain/.../ObserveFavoriteTitles.kt` (modify)
- What: replace the single fallback with
  `private val FALLBACK_ORDER = listOf(Language.EN_US, Language.PT_BR, Language.VI_VN)`;
  in `observeOne`, `val fallbacks = FALLBACK_ORDER - language`; `combine(listOf(current) + fallbacks.map { observeSummary })`
  → `FavoriteTitle(id, values.firstNotNullOfOrNull { it })`. The refresh-when-missing launch is unchanged.
- Unit tests (`ObserveFavoriteTitlesTest`): `vietnameseFallsBackToEnglishFirst` (data stored in en-US and pt-BR →
  en-US shown), `vietnameseFallsBackToPortugueseWithoutEnglish`, `vietnameseReplacesFallbackAfterRefresh`;
  existing `fallsBackToOtherLanguageThenReplacesAfterRefresh` still passes.
- Compile: `./gradlew :service:title:domain:compileAndroidMain`

### Step 3: Vietnamese copy for cmp  (AC-1, EC-2, EC-4) ✅
- Files (new): `values-vi/strings.xml` under `composeResources/` of `cmp/designsystem`,
  `cmp/feature/onboarding/ui`, `cmp/feature/home/ui`, `cmp/feature/favorites/ui`, `cmp/feature/title-details/ui`,
  each with every key of its `values/strings.xml`:
  - designsystem: `remove_favorite_title` Xóa khỏi danh sách yêu thích?; `remove_favorite_message` “%1$s” sẽ bị xóa
    khỏi danh sách của bạn. Bạn có thể thêm lại sau.; `remove_favorite_message_unknown` Phim này sẽ bị xóa khỏi danh
    sách của bạn. Bạn có thể thêm lại sau.; `remove_favorite_cancel` Hủy; `remove_favorite_confirm` Xóa;
    `remove_favorite_heart` Xóa khỏi yêu thích
  - onboarding: `onboarding_headline` Tất cả về phim lẻ, phim bộ, anime và nhiều hơn nữa.; `onboarding_body` Luôn cập
    nhật phim lẻ, phim bộ, anime và nhiều hơn nữa.; `onboarding_get_started` Bắt đầu
  - home: `home_title` Hôm nay bạn muốn xem gì?; `home_search_hint` Tìm kiếm; `home_popular_heading` Phổ biến nhất;
    `home_category_movies` Phim lẻ; `home_category_series` Phim bộ; `home_category_anime` Anime;
    `home_category_novelas` Telenovela; `home_category_documentaries` Phim tài liệu; `home_row_empty` Chưa có gì để
    hiển thị.; `home_row_error` Không tải được các phim này.; `home_retry` Thử lại; `home_favorites_title` Yêu thích;
    `home_favorites_subtitle` Đã lưu để xem bất cứ lúc nào.; `home_favorites_view_all` Xem tất cả;
    `home_favorites_loading` Đang tải phim
  - favorites: `favorites_back` Quay lại; `favorites_title` Yêu thích của tôi; `favorites_subtitle` Tất cả phim bạn
    đã lưu, ở cùng một nơi.; `favorites_loading` Đang tải phim; `favorites_empty` Chưa có phim yêu thích nào. Chạm vào
    trái tim trên một phim để lưu vào đây.; `favorites_count` other: %1$d phim
  - title-details: `details_back` Quay lại; `details_watch_trailer` Xem trailer; `details_cast` Diễn viên chính;
    `details_genres` Thể loại; `details_recommendations` Đề xuất; `details_no_synopsis` Chưa có tóm tắt.;
    `details_not_rated` NR; `details_not_rated_description` Chưa có đánh giá; `details_rating_description` Đánh giá
    %1$d%%; `details_error` Không tải được phim này.; `details_retry` Thử lại; `details_favorite` Yêu thích
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain :cmp:feature:onboarding:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidMain :cmp:feature:favorites:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidMain`

### Step 4: Previews and iOS declaration  (AC-1, EC-4) ✅
- Files: `cmp/designsystem/src/commonMain/.../preview/` file holding `TmdbPreviews` (modify),
  `cmp/iosApp/iosApp/Info.plist` (modify)
- What: add `@Preview(name = "Tiếng Việt", locale = "vi", device = Devices.PHONE)` to `TmdbPreviews` and
  `@Preview(name = "Tiếng Việt", locale = "vi")` to `TmdbComponentPreviews`, after the pt-BR entries; add
  `<string>vi</string>` to `CFBundleLocalizations`.
- Unit tests: none (manual check in previews and on the simulator).
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain`

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 | 3, 4 | Manual: How to test 2–3 (cmp), Vietnamese previews |
| AC-2 | 1 | `LanguageTest.vietnameseMapsToViVn` (request and cache use `tag`); manual step 4 |
| AC-3 | 1 | `TrailerSelectionTest.trailerPrefersVietnamese`, `vietnameseTrailerFallsBackToEnglish` |
| AC-4 | 2 | `ObserveFavoriteTitlesTest.vietnameseFallsBackToEnglishFirst`, `…ToPortugueseWithoutEnglish`, `…ReplacesFallbackAfterRefresh` |
| AC-5 | 1 | `TitleDetailsFormatTest.vietnameseDateIsDayThgMonthYear`, `vietnameseRuntime` |
| AC-6 | 1, 2 | Existing `LanguageTest`, `TitleDetailsFormatTest`, `TrailerSelectionTest`, `ObserveFavoriteTitlesTest` cases |
| EC-1 | 1 | Existing per-language cache tests (`TitleListsRepositoryImplTest`, `TitleDetailsRepositoryImplTest`); manual step 8 |
| EC-2 | 3 | Manual: offline first launch in Vietnamese |
| EC-3 | 1 | Existing repository per-language tests; manual |
| EC-4 | 3, 4 | Manual: largest font in Vietnamese, large-text preview |
| EC-5 | 2 | Existing `listsFavoritesNewestFirst` (favorites not per language) |

## Out of scope
- `native-android` copy, formatting and previews -> 02.
- `native-ios` copy, formatting and `vi` declaration -> 03.
- README -> 04.

## Risks
- Republishing the shared core breaks the native apps' exhaustive `switch`/`when` on `Language` until 02 and 03
  land — they are not built by CI and 02/03 follow directly.
- Vietnamese copy is a draft — the user reviews it (foundry report); changes are string-only.
- Long Vietnamese labels (for example "Phim tài liệu" tab, "Xóa khỏi danh sách yêu thích?") may wrap at large
  text — covered by the existing large-text layout; checked manually.

## Deviations
- The three Vietnamese favorites tests went into a new `ObserveFavoriteTitlesVietnameseTest` instead of
  `ObserveFavoriteTitlesTest`, which would have exceeded Detekt's `TooManyFunctions` limit (11).
- `formatRuntime` also gained `portugueseRuntimeIsUnchanged` in `TitleDetailsFormatTest` (AC-6).
