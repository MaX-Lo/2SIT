package de.twoSIT.const

import de.twoSIT.models.Area

const val responseCacheDir = "response_cache"
fun areaCacheFile(area: Area) : String{
    return "area_${area.minLatitude}_${area.minLongitude}_${area.maxLatitude}_${area.maxLongitude}.xml"
}
fun wayCacheFile(id: String) : String{
    return "way_${id}.xml"
}
fun nodeCacheFile(id: String) : String{
    return "node_${id}.xml"
}
fun relationCacheFile(id: String) : String{
    return "node_${id}.xml"
}

const val logDir = "logs"
const val defaultLogFile = "2sit.log"