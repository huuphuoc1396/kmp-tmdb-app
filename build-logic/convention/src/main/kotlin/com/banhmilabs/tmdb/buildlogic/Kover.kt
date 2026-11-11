package com.banhmilabs.tmdb.buildlogic

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

private const val MIN_LINE_COVERAGE = 80

/** Kover with an 80% line coverage threshold. DI, DTOs and generated code are excluded. */
internal fun Project.configureKover() {
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    extensions.configure<KoverProjectExtension> {
        reports {
            filters {
                excludes {
                    packages("*.di", "*.di.*")
                    classes("*Dto", "*Dto$*", "*.generated.*", "*BuildKonfig*")
                }
            }
            verify {
                rule {
                    minBound(MIN_LINE_COVERAGE, CoverageUnit.LINE)
                }
            }
        }
    }
}
