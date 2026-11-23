package com.banhmilabs.tmdb.nativeapp.home.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.service.title.domain.TitleCategory

private val TabsMinHeight = 48.dp
private val TabsStart = 33.dp
private val TabSpacing = 29.dp
private val PillShape = RoundedCornerShape(70.dp)
private val PillHorizontalPadding = 14.dp
private val TabVerticalPadding = 6.dp
private const val UNSELECTED_ALPHA = 0.4f

private val SelectedStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
private val UnselectedStyle = TextStyle(
    color = Color.White.copy(alpha = UNSELECTED_ALPHA),
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
)

@Composable
internal fun CategoryTabs(selected: TitleCategory, onSelect: (TitleCategory) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.selectableGroup().heightIn(min = TabsMinHeight),
        contentPadding = PaddingValues(start = TabsStart),
        horizontalArrangement = Arrangement.spacedBy(TabSpacing),
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
    val shape = if (selected) {
        Modifier
            .minimumInteractiveComponentSize()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.secondary)
    } else {
        Modifier.minimumInteractiveComponentSize()
    }
    Text(
        text = label,
        modifier = shape
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(
                horizontal = if (selected) PillHorizontalPadding else 0.dp,
                vertical = TabVerticalPadding,
            ),
        style = if (selected) SelectedStyle else UnselectedStyle,
        maxLines = 1,
    )
}

private val TitleCategory.label: Int
    @StringRes
    get() = when (this) {
        TitleCategory.MOVIES -> R.string.home_category_movies
        TitleCategory.SERIES -> R.string.home_category_series
        TitleCategory.ANIME -> R.string.home_category_anime
        TitleCategory.NOVELAS -> R.string.home_category_novelas
        TitleCategory.DOCUMENTARIES -> R.string.home_category_documentaries
    }
