package de.twoSIT.const

import de.twoSIT.models.Area

const val responseCacheDir = "responses"
fun responseFileName(area: Area) : String{
    return "response_${area.minLatitude}_${area.minLongitude}_${area.maxLatitude}_${area.maxLongitude}.xml"
}

const val logDir = "logs"
const val defaultLogFile = "2sit.log"