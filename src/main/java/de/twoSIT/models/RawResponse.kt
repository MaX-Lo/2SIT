package de.twoSIT.models

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

class tag {
    @JacksonXmlProperty(isAttribute = true)
    var k: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var v: String = "not available"
}

abstract class abstract_node {
    @JacksonXmlProperty(isAttribute = true)
    var id: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var visible: Boolean = false

    @JacksonXmlProperty(isAttribute = true)
    var version: Int = 0

    @JacksonXmlProperty(isAttribute = true)
    var changeset: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var timestamp: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var user: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var uid: String = "not available"
}

class way : abstract_node() {

}

class node : abstract_node() {
    @JacksonXmlProperty(isAttribute = true)
    var lat: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var lon: Float = 0f

    @JacksonXmlProperty(localName = "tag")
    var tags: List<tag> = mutableListOf()
}

class bounds {
    @JacksonXmlProperty(isAttribute = true)
    var minlat: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var minlon: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var maxlat: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var maxlon: Float = 0f
}

// raw response
@JacksonXmlRootElement(localName = "osm")
class RawResponse {
    @JacksonXmlProperty(isAttribute = true)
    var version: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var generator: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var copyright: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var attribution: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var license: String = "not available"

    @JacksonXmlProperty(localName = "bounds")
    var bounds: bounds = bounds()

    @JacksonXmlProperty(localName = "node")
    var nodes: List<abstract_node> = mutableListOf()
}