package de.twoSIT

import de.twoSIT.const.responseCacheDir
import de.twoSIT.const.areaCacheFile
import de.twoSIT.const.wayCacheFile
import de.twoSIT.models.Area
import de.twoSIT.util.getLogger
import org.apache.http.client.fluent.Request
import java.io.File

class Requester(private val baseUrl: String) {
    companion object {
        @JvmStatic
        private val logger = getLogger(Requester::class.java)
    }

    fun requestWay(id: Int, useCache: Boolean = true): String {
        if (useCache) {
            val dirName = responseCacheDir
            File(dirName).mkdir()
            val cached = File("$dirName/${wayCacheFile(id)}")

            if (cached.exists()) {
                logger.debug("Loaded cached area $cached")
                return cached.readText()
            }

            val rawXml = requestWay(id)
            cached.printWriter().use { out ->
                val lines = rawXml.split("\n")
                for (line in lines) {
                    out.println(line)
                }
            }
            logger.debug("Saved way in $cached")
            return rawXml
        }
        return requestWay(id)
    }

    private fun requestWay(id: Int): String {
        val url = "${baseUrl}way/$id"
        return Request.Get(url).execute().returnContent().asString()
    }

    fun requestWays(ids: Iterable<Int>): String {
        val idString = ids.joinToString(separator = ",") { it -> "$it" }
        val url = "${baseUrl}ways?ways=$idString"
        return Request.Get(url).execute().returnContent().asString()
    }


    fun requestAreas(areas: Iterable<Area>, useCache: Boolean = true): MutableMap<Area, String> {
        val map = mutableMapOf<Area, String>()
        for (area in areas) {
            map[area] = requestArea(area, useCache)
        }
        return map
    }

    fun requestArea(area: de.twoSIT.models.Area, useCache: Boolean = true): String {
        if (useCache) {
            val dirName = responseCacheDir
            File(dirName).mkdir()
            val cached = File("$dirName/${areaCacheFile(area)}")

            if (cached.exists()) {
                logger.debug("Loaded cached area $cached")
                return cached.readText()
            }

            val rawXml = requestArea(area)
            cached.printWriter().use { out ->
                val lines = rawXml.split("\n")
                for (line in lines) {
                    out.println(line)
                }
            }
            logger.debug("Saved area in $cached")
            return rawXml
        }

        return requestArea(area)
    }

    private fun requestArea(area: Area): String {
        val url = "${baseUrl}map?bbox=${area.minLatitude},${area.minLongitude},${area.maxLatitude},${area.maxLongitude}"
        return Request.Get(url).execute().returnContent().asString()
    }
}