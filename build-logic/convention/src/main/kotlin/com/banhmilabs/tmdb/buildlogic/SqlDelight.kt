package com.banhmilabs.tmdb.buildlogic

import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun sqlDelightDatabaseName(path: String): String =
    path.removePrefix(":").split(":")[1].replaceFirstChar { it.uppercase() } + "Database"

internal fun Project.configureSqlDelight() {
    pluginManager.apply("app.cash.sqldelight")

    extensions.configure<SqlDelightExtension> {
        databases.create(sqlDelightDatabaseName(path)) {
            packageName.set("${derivedNamespace()}.generated")
        }
    }
}
