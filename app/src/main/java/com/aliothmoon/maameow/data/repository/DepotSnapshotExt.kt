package com.aliothmoon.maameow.data.repository

import com.aliothmoon.maameow.data.model.toolbox.DepotItem
import com.aliothmoon.maameow.data.resource.ItemInfo

val DepotSnapshot.hasRecognition: Boolean
    get() = syncTimeMillis > 0L

val DepotSnapshot.estimatedItems: Map<String, Int>
    get() = if (formatVersion < DepotRepository.FORMAT_VERSION) items else estimatedItemsWith(fightDropDeltas)

fun DepotSnapshot.estimatedCountOf(itemId: String): Int = estimatedItems[itemId] ?: 0

fun DepotSnapshot.effectiveCountOf(itemId: String): Int =
    if (hasRecognition) estimatedCountOf(itemId) else 0

/** 将 v1 的单一 items map 归入其原本的语义字段。 */
fun DepotSnapshot.migrateToV2(): DepotSnapshot {
    if (formatVersion >= DepotRepository.FORMAT_VERSION) return this
    val baseline = if (syncTimeMillis > 0L) items else emptyMap()
    val delta = if (syncTimeMillis > 0L) emptyMap() else items
    return copy(
        baselineItems = baseline,
        fightDropDeltas = delta,
        items = items,
        formatVersion = DepotRepository.FORMAT_VERSION,
    )
}

internal fun DepotSnapshot.estimatedItemsWith(
    deltas: Map<String, Int>,
): Map<String, Int> = buildMap {
    for ((itemId, count) in baselineItems) put(itemId, count)
    for ((itemId, count) in deltas) {
        put(itemId, (get(itemId) ?: 0) + count)
    }
}

/**
 * 将持久化 map 转为展示/导出用列表，排序与识别回调一致：
 * 游戏 sortId 升序，查不到的靠后并按 id 兜底。
 */
fun DepotSnapshot.toSortedItems(itemMap: Map<String, ItemInfo>): List<DepotItem> =
    estimatedItems.map { (id, count) -> DepotItem(id, count) }
        .sortedWith(
            compareBy(
                { itemMap[it.id]?.sortId ?: Int.MAX_VALUE },
                { it.id },
            )
        )
