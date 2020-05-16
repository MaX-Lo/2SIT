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
    val latUL = 11.54
    val lonUL = 48.14
    val latLR = 11.543
    val lonLR = 48.145

    if (!DEBUG) {
        val url = "https://api.openstreetmap.org/api/0.6/map?bbox=$latUL,$lonUL,$latLR,$lonLR"
        return Request.Get(url).execute().returnContent().asString()
    }
    return File("response.xml").readText()
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
    if (!DEBUG){
        File("response.xml").printWriter().use { out ->
            val lines = rawXmlString.split("\n")
            for (line in lines){
                out.println(line)
            }
        }
    }
    val response = parseXml(rawXmlString)

}
