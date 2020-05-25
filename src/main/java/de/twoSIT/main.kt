package de.twoSIT

import de.twoSIT.models.Area
import de.twoSIT.models.RawArea
import de.twoSIT.models.RawNode
import de.twoSIT.models.RawWay


// bounding box examples for a buildings mapped as IndoorOSM and SIT
val indoorArea = Area(49.41689, 8.67180, 49.41969, 8.67695)
val sitArea = Area(42.79609, -1.63938, 42.80234, -1.63280)


fun main() {
    // val requester = Requester("http://141.76.16.34:8084/api/0.6/")  // uni-server
    val requester = Requester("https://api.openstreetmap.org/api/0.6/")

    val rawXmlString = requester.requestArea(indoorArea)
    val rawArea = RawArea.fromString(rawXmlString)

    val mapper = Mapper()
    val buildings = mapper.parseArea(rawArea)

    val x = ""
}
