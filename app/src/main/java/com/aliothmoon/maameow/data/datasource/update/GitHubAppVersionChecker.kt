package com.aliothmoon.maameow.data.datasource.update

import com.aliothmoon.maameow.constant.MaaApi
import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.api.model.GitHubRelease
import com.aliothmoon.maameow.data.datasource.AppDownloader
import com.aliothmoon.maameow.data.model.update.UpdateChannel
import com.aliothmoon.maameow.data.model.update.UpdateCheckResult
import com.aliothmoon.maameow.data.model.update.UpdateError
import com.aliothmoon.maameow.data.model.update.UpdateInfo
import com.aliothmoon.maameow.domain.service.update.checker.AppVersionChecker
import com.aliothmoon.maameow.utils.JsonUtils
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class GitHubAppVersionChecker(
    private val httpClient: HttpClientHelper,
) : AppVersionChecker {

    override suspend fun check(
        current: String,
        channel: UpdateChannel,
    ): UpdateCheckResult = try {
        val releasesUrl = when (channel) {
            UpdateChannel.STABLE -> MaaApi.APP_GITHUB_RELEASES
            UpdateChannel.BETA -> MaaApi.APP_GITHUB_RELEASES_BETA
        }
        val release = httpClient.get(releasesUrl).use { response ->
            check(response.isSuccessful) { "GitHub Release API returned HTTP ${response.code}" }
            JsonUtils.common.decodeFromString<List<GitHubRelease>>(response.body.string())
                .firstOrNull { channel == UpdateChannel.BETA || !it.prerelease }
                ?: error("GitHub Release API returned no matching release")
        }
        val version = release.tagName.removePrefix("v").removePrefix("V")

        if (version.isEmpty() || AppDownloader.compareVersions(current, version) >= 0) {
            UpdateCheckResult.UpToDate(current)
        } else {
            UpdateCheckResult.Available(
                UpdateInfo(version = version, releaseNote = release.body.orEmpty())
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.e(error, "GitHub App Release check failed: $channel")
        UpdateCheckResult.Error(UpdateError.NetworkError(error.message))
    }
}
