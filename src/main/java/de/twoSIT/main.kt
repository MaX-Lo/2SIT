package de.twoSIT

import de.twoSIT.models.Area
import de.twoSIT.models.RawArea
import de.twoSIT.models.RawNode
import de.twoSIT.models.RawWay


// bounding box examples for a buildings mapped as IndoorOSM and SIT
val indoorArea = Area(49.41689, 8.67180, 49.41969, 8.67695)
val sitArea = Area(42.79609, -1.63938, 42.80234, -1.63280)

// server addresses to easy switch.
const val uniServer = "http://141.76.16.34:8084/api/0.6/"
const val officialServer = "https://api.openstreetmap.org/api/0.6/"

fun main() {
    val requester = Requester.getInstance(officialServer)

    val rawXmlString = requester.requestArea(indoorArea)
    val rawArea = RawArea.fromString(rawXmlString)

    val mapper = Mapper()
    val buildings = mapper.parseArea(rawArea)

    val converter = Converter()
    val sitBuildings = converter.convertBuildings(buildings)
    mapper.exportBuildings()
    val x = ""
}
