package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_category_anime
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_category_documentaries
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_category_movies
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_category_novelas
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_category_series
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CategoryTabs(selected: TitleCategory, onSelect: (TitleCategory) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.selectableGroup().heightIn(min = TmdbSize.touchTarget),
        contentPadding = PaddingValues(start = TmdbSpacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(TitleCategory.entries) { category ->
            CategoryTab(
                label = stringResource(category.label),
                selected = category == selected,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pill = if (selected) {
        Modifier
            .clip(CircleShape)
            .background(colors.secondary)
    } else {
        Modifier
    }
    val pillPadding = if (selected) Modifier.padding(horizontal = TmdbSpacing.md) else Modifier
    Text(
        text = label,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .then(pill)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .then(pillPadding)
            .padding(vertical = TmdbSpacing.xs),
        color = if (selected) colors.onSecondary else colors.onSurfaceVariant,
        style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
        maxLines = 1,
    )
}

private val TitleCategory.label: StringResource
    get() = when (this) {
        TitleCategory.MOVIES -> Res.string.home_category_movies
        TitleCategory.SERIES -> Res.string.home_category_series
        TitleCategory.ANIME -> Res.string.home_category_anime
        TitleCategory.NOVELAS -> Res.string.home_category_novelas
        TitleCategory.DOCUMENTARIES -> Res.string.home_category_documentaries
    }
