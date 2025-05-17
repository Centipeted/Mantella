package hu.bme.aut.mantella.data

import hu.bme.aut.mantella.model.CollectivePage
import hu.bme.aut.mantella.model.CollectiveWithEmoji

object CollectivePagesCache {
    private var cachedMap: Map<CollectiveWithEmoji, List<CollectivePage>>? = null

    fun insertCollectives(pages: Map<CollectiveWithEmoji, List<CollectivePage>>) {
        cachedMap = pages
    }

    fun addPage(collectiveName: String, page: CollectivePage) {
        val map = cachedMap ?: return
        val entry = map.entries.firstOrNull { it.key.name == collectiveName } ?: return

        cachedMap = map.toMutableMap().apply {
            this[entry.key] = entry.value + page
        }
    }

    fun getPages(collectiveName: String): List<CollectivePage> {
        val collective = cachedMap?.keys
            ?.firstOrNull { it.name == collectiveName }

        return collective?.let { cachedMap?.get(it) } ?: emptyList()
    }

    fun getCollectiveNames(): List<String> {
        return cachedMap?.keys?.map { it.name.lowercase().trim() } ?: emptyList()
    }

    fun removePages(paths: List<String>) {
        val map = cachedMap ?: return
        if (paths.isEmpty()) return

        val toRemove: Map<String, Set<String>> =
            paths.groupBy { it.substringBefore('/') }
                .mapValues { (_, list) -> list.toSet() }

        cachedMap = map.toMutableMap().apply {
            for ((collective, pages) in map) {
                val targetSet = toRemove[collective.name] ?: continue
                val filtered  = pages.filterNot { it.path in targetSet }

                if (filtered.isEmpty()) {
                    remove(collective)
                } else if (filtered.size != pages.size) {
                    this[collective] = filtered
                }
            }
        }
    }

    fun clearCache() {
        cachedMap = null
    }
}