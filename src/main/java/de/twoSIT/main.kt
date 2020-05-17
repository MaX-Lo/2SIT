package de.twoSIT

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.twoSIT.models.RawResponse
import de.twoSIT.models.Response
import org.apache.http.client.fluent.Request
import java.io.File


const val DEBUG = true

fun getResponse(): String {
    // todo log and time the shit
    // todo 2 add some fault tolerance, maybe check network access, website availability and all the good stuff up front
    val minLongitude = 49.41689 //11.54
    val minLatitude =  8.67180//48.14
    val maxLongitude = 49.41969 //11.543
    val maxLatitude = 8.67695 //48.145
    assert(minLongitude < maxLongitude)
    assert(minLatitude < maxLatitude)

    val dirName = "responses"
    File(dirName).mkdir()
    val cached = File("$dirName/response_${minLatitude}_${minLongitude}_${maxLatitude}_${maxLongitude}.xml")

    if (DEBUG && cached.exists()) {
        return cached.readText()
    }

    val url = "https://api.openstreetmap.org/api/0.6/map?bbox=$minLatitude,$minLongitude,$maxLatitude,$maxLongitude"
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

fun parseXml(raw: String): Response {
    // todo add some fault tolerance here too
    val module = JacksonXmlModule()
    module.setDefaultUseWrapper(false)
    val xmlMapper = XmlMapper(module)

    val rawObj = xmlMapper.readValue(raw, RawResponse::class.java)
    return Response(rawObj)
}

fun main() {
    val rawXmlString = getResponse()
    val response = parseXml(rawXmlString)
    val x = ""
}
