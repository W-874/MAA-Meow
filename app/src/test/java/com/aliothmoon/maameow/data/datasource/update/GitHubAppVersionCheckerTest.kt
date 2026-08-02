package com.aliothmoon.maameow.data.datasource.update

import com.aliothmoon.maameow.constant.MaaApi
import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.model.update.UpdateChannel
import com.aliothmoon.maameow.data.model.update.UpdateCheckResult
import com.aliothmoon.maameow.data.model.update.UpdateError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAppVersionCheckerTest {

    private val httpClient = mockk<HttpClientHelper>()
    private val checker = GitHubAppVersionChecker(httpClient)

    @Test
    fun stable_ignoresPrereleases_andReturnsLatestStableRelease() = runBlocking {
        coEvery { httpClient.get(MaaApi.APP_GITHUB_RELEASES) } returns response(
            """
            [
              {"tag_name":"v2.1.0-beta.1","body":"Beta notes","prerelease":true},
              {"tag_name":"v2.0.0","body":"Stable notes","prerelease":false}
            ]
            """.trimIndent()
        )

        val result = checker.check(current = "1.0.0", channel = UpdateChannel.STABLE)

        val available = result as UpdateCheckResult.Available
        assertEquals("2.0.0", available.info.version)
        assertEquals("Stable notes", available.info.releaseNote)
    }

    @Test
    fun beta_includesPrereleases_andUsesFirstRelease() = runBlocking {
        coEvery { httpClient.get(MaaApi.APP_GITHUB_RELEASES_BETA) } returns response(
            """
            [
              {"tag_name":"v2.1.0-beta.1","body":"Beta notes","prerelease":true},
              {"tag_name":"v2.0.0","body":"Stable notes","prerelease":false}
            ]
            """.trimIndent()
        )

        val result = checker.check(current = "2.0.0", channel = UpdateChannel.BETA)

        val available = result as UpdateCheckResult.Available
        assertEquals("2.1.0-beta.1", available.info.version)
        assertEquals("Beta notes", available.info.releaseNote)
    }

    @Test
    fun invalidReleaseResponse_mapsToNetworkError() = runBlocking {
        coEvery { httpClient.get(MaaApi.APP_GITHUB_RELEASES) } returns response("not json")

        val result = checker.check(current = "1.0.0", channel = UpdateChannel.STABLE)

        assertTrue((result as UpdateCheckResult.Error).error is UpdateError.NetworkError)
    }

    @Test
    fun cancellation_isPropagated() {
        coEvery { httpClient.get(MaaApi.APP_GITHUB_RELEASES) } throws CancellationException()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                checker.check(current = "1.0.0", channel = UpdateChannel.STABLE)
            }
        }
    }

    private fun response(body: String): Response = Response.Builder()
        .request(Request.Builder().url("https://api.github.com/releases").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody())
        .build()
}
