package com.aliothmoon.maameow.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.aliothmoon.maameow.BuildConfig

fun Modifier.benchmarkTestTag(tag: String): Modifier =
    if (BuildConfig.BENCHMARK_BUILD) testTag(tag) else this

fun Modifier.enableBenchmarkTestTags(): Modifier =
    if (BuildConfig.BENCHMARK_BUILD) {
        semantics { testTagsAsResourceId = true }
    } else {
        this
    }
