package com.aliothmoon.maameow.maa.callback

import com.alibaba.fastjson2.JSON
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.data.model.toolbox.DepotItem
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.FakePreferencesDataStore
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import androidx.datastore.preferences.core.stringPreferencesKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolboxResultCollectorTest {

    @Test
    fun depotRecognition_writesToProfileCapturedAtSessionStart() {
        val depotRepository = mockk<DepotRepository>(relaxed = true)
        val collector = ToolboxResultCollector(
            resourceDataManager = mockk<ResourceDataManager>(),
            achievementRepository = mockk<AchievementRepository>(relaxed = true),
            depotRepository = depotRepository,
            operBoxRepository = mockk<OperBoxRepository>(),
        )

        collector.onSessionStart("profile-at-start")
        collector.onDepotResult(
            JSON.parseObject("""{"done":true,"data":"{\"30011\":42}"}"""),
        )

        verify {
            depotRepository.replaceRecognition(
                "profile-at-start",
                listOf(DepotItem("30011", 42)),
            )
        }
    }

    @Test
    fun operBoxRecognition_writesToProfileCapturedAtSessionStartAfterProfileSwitch() = runBlocking {
        val activeProfileId = MutableStateFlow("profile-at-start")
        val profileDeleted = MutableSharedFlow<String>()
        val taskChainState = mockk<TaskChainState> {
            every { profileId } returns activeProfileId
            every { isLoaded } returns MutableStateFlow(true)
            every { this@mockk.profileDeleted } returns profileDeleted
        }
        val store = FakePreferencesDataStore()
        val resourceDataManager = mockk<ResourceDataManager> {
            every { operators } returns MutableStateFlow(emptyMap())
        }
        val operBoxRepository = OperBoxRepository(store, taskChainState)
        val collector = ToolboxResultCollector(
            resourceDataManager = resourceDataManager,
            achievementRepository = mockk<AchievementRepository>(relaxed = true),
            depotRepository = mockk<DepotRepository>(relaxed = true),
            operBoxRepository = operBoxRepository,
        )

        collector.onSessionStart("profile-at-start")
        activeProfileId.value = "profile-after-switch"
        collector.onOperBoxResult(
            JSON.parseObject(
                """{"done":true,"own_opers":[{"id":"char_002_amiya","name":"阿米娅","rarity":5,"elite":2,"level":80,"potential":1}]}""",
            ),
        )

        val profileAtStartKey = stringPreferencesKey("operbox_profile-at-start")
        withTimeout(5_000) {
            while (store.data.first()[profileAtStartKey] == null) yield()
        }

        assertTrue(store.data.first()[profileAtStartKey] != null)
        assertNull(store.data.first()[stringPreferencesKey("operbox_profile-after-switch")])
    }
}
