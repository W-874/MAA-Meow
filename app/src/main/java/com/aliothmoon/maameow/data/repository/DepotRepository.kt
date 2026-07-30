package com.aliothmoon.maameow.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.aliothmoon.maameow.data.model.toolbox.DepotItem
import com.aliothmoon.maameow.data.preferences.TaskChainState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * 仓库快照 v2：全量识别是基线，战斗掉落是识别后的增量估算。
 *
 * [items] 保留为 v1 读取方的估算数量镜像；新代码应使用 [baselineItems]、
 * [fightDropDeltas] 或 [estimatedItems]。
 */
@Serializable
data class DepotSnapshot(
    val baselineItems: Map<String, Int> = emptyMap(),
    val fightDropDeltas: Map<String, Int> = emptyMap(),
    val items: Map<String, Int> = emptyMap(),
    val syncTimeMillis: Long = 0L,
    val formatVersion: Int = 1,
)

/** 仓库分片：内存权威，set/merge 同步写内存并排队落盘。 */
class DepotRepository(
    store: DataStore<Preferences>,
    private val taskChainState: TaskChainState,
) {
    private val shards = ProfileShardStore(
        store = store,
        taskChainState = taskChainState,
        keyPrefix = KEY_PREFIX,
        serializer = DepotSnapshot.serializer(),
        empty = ::DepotSnapshot,
        normalize = DepotSnapshot::migrateToV2,
    )

    val snapshot: StateFlow<DepotSnapshot> get() = shards.snapshot

    val isLoaded: StateFlow<Boolean> get() = shards.isLoaded

    fun start() = shards.start()

    /** 用本次全量仓库识别替换基线，并丢弃此前的战斗掉落估算。 */
    fun replaceRecognition(profileId: String, items: List<DepotItem>) {
        val baseline = items.associate { it.id to it.count }
        shards.mutate(profileId) {
            DepotSnapshot(
                baselineItems = baseline,
                items = baseline,
                syncTimeMillis = System.currentTimeMillis(),
                formatVersion = FORMAT_VERSION,
            )
        }
    }

    /** 以调用时的活跃配置档写入全量仓库识别。 */
    fun replaceRecognition(items: List<DepotItem>) =
        replaceRecognition(taskChainState.profileId.value, items)

    /** 记录本次战斗掉落；仅累加估算增量，不改变全量识别时间。 */
    fun recordFightDrops(profileId: String, drops: List<Pair<String, Int>>) {
        val valid = drops.filter { (itemId, add) -> add > 0 && !shouldExclude(itemId) }
        if (valid.isEmpty()) return
        shards.mutate(profileId) { current ->
            val merged = current.fightDropDeltas.toMutableMap()
            for ((itemId, add) in valid) {
                merged[itemId] = (merged[itemId] ?: 0) + add
            }
            current.copy(
                fightDropDeltas = merged,
                items = current.estimatedItemsWith(merged),
                formatVersion = FORMAT_VERSION,
            )
        }
    }

    /** 以调用时的活跃配置档记录战斗掉落。 */
    fun recordFightDrops(drops: List<Pair<String, Int>>) =
        recordFightDrops(taskChainState.profileId.value, drops)

    /** 包含战斗掉落估算的数量；未全量识别时仍可用于展示。 */
    fun estimatedCountOf(itemId: String): Int = snapshot.value.estimatedCountOf(itemId)

    /** 可用于库存目标的数量；未全量识别时返回 0。 */
    fun effectiveCountOf(itemId: String): Int = snapshot.value.effectiveCountOf(itemId)

    val hasRecognition: Boolean get() = snapshot.value.hasRecognition

    @Deprecated("Use replaceRecognition")
    fun set(items: List<DepotItem>) = replaceRecognition(items)

    @Deprecated("Use recordFightDrops")
    fun merge(drops: List<Pair<String, Int>>) = recordFightDrops(drops)

    @Deprecated("Use effectiveCountOf")
    fun countOf(itemId: String): Int = effectiveCountOf(itemId)

    private fun shouldExclude(itemId: String): Boolean =
        itemId.isEmpty() || !itemId.all { it in '0'..'9' } || itemId in EXCLUDED_ITEM_IDS

    companion object {
        internal const val FORMAT_VERSION = 2
        private const val KEY_PREFIX = "depot_"

        private val Context.depotStore: DataStore<Preferences> by preferencesDataStore(
            name = "depot",
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        )

        fun create(context: Context, taskChainState: TaskChainState) =
            DepotRepository(context.depotStore, taskChainState)

        private val EXCLUDED_ITEM_IDS = setOf(
            "3401",
            "3112", "3113", "3114",
            "5001",
        )
    }
}
