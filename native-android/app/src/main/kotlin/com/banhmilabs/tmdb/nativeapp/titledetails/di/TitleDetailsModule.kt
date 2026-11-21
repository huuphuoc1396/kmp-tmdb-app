package com.banhmilabs.tmdb.nativeapp.titledetails.di

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.nativeapp.titledetails.TitleDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val titleDetailsModule = module {
    viewModel { (titleId: TitleId) ->
        TitleDetailsViewModel(titleId = titleId, repository = get(), favorites = get())
    }
}
