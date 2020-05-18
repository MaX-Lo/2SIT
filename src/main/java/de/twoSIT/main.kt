package de.twoSIT

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.twoSIT.models.Area
import de.twoSIT.models.Building
import de.twoSIT.models.RawResponse
import org.apache.http.client.fluent.Request
import java.io.File


const val DEBUG = true

val indoorArea = Area(49.41689, 8.67180, 49.41969, 8.67695)
val sitArea = Area(42.79609, -1.63938, 42.80234, -1.63280)

fun getResponse(area: Area): String {
    // todo log and time the shit
    // todo 2 add some fault tolerance, maybe check network access, website availability and all the good stuff up front

    val dirName = "responses"
    File(dirName).mkdir()
    val cached = File("$dirName/response_${area.minLatitude}_${area.minLongitude}_${area.maxLatitude}_" +
            "${area.maxLongitude}.xml")

    if (DEBUG && cached.exists()) {
        return cached.readText()
    }

    val url = "https://api.openstreetmap.org/api/0.6/map?bbox=${area.minLatitude},${area.minLongitude}," +
            "${area.maxLatitude},${area.maxLongitude}"
    val rawXml = Request.Get(url).execute().returnContent().asString()

    if (DEBUG){
        cached.printWriter().use { out ->
            val lines = rawXml.split("\n")
            for (line in lines){
                out.println(line)
            }
        }
    }
    return rawXml
}

fun parse2Xml(raw: String): RawResponse {
    // todo add some fault tolerance here too
    val module = JacksonXmlModule()
    module.setDefaultUseWrapper(false)
    val xmlMapper = XmlMapper(module)

    return xmlMapper.readValue(raw, RawResponse::class.java)
}

fun main() {
    val rawXmlString = getResponse(indoorArea)
    val rawResponse = parse2Xml(rawXmlString)

    val mapper = Mapper()
    val buildings = mapper.parse(rawResponse)

    val x = ""
}
