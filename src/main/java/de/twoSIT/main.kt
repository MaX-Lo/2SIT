package de.twoSIT

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.twoSIT.models.RawResponse
import org.apache.http.client.fluent.Request


fun getResponse(): String {
    val latUL = 11.54
    val lonUL = 48.14
    val latLR = 11.543
    val lonLR = 48.145

    val url = "https://api.openstreetmap.org/api/0.6/map?bbox=$latUL,$lonUL,$latLR,$lonLR"

    return Request.Get(url).execute().returnContent().asString()
}

fun parseXml(raw: String) {
    val module = JacksonXmlModule()
    module.setDefaultUseWrapper(false)
    val xmlMapper = XmlMapper(module)

    val rawObj = xmlMapper.readValue(raw, RawResponse::class.java)
    val cleanedObj = ""
}

fun main() {
    val raw_xml_string = getResponse()
    parseXml(raw_xml_string)

}