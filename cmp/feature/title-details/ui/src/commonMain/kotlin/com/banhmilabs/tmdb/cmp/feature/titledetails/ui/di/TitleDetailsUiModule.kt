package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.di

import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.TitleDetailsViewModel
import com.banhmilabs.tmdb.core.model.TitleId
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val titleDetailsUiModule = module {
    viewModel { (titleId: TitleId) -> TitleDetailsViewModel(titleId = titleId, repository = get()) }
}
