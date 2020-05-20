package de.twoSIT.models

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

class Member {
    @JacksonXmlProperty(isAttribute = true)
    var type: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var ref: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var role: String = "not available"
}

class NodeReference {
    @JacksonXmlProperty(isAttribute = true)
    var ref: String = "not available"
}

class Tag {
    @JacksonXmlProperty(isAttribute = true)
    var k: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var v: String = "not available"
}

abstract class AbstractNode {
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

    @JacksonXmlProperty(localName = "tag")
    var tags: MutableList<Tag> = mutableListOf()
}


class Relation : AbstractNode() {
    @JacksonXmlProperty(localName = "member")
    var members: MutableList<Member> = mutableListOf()
}


class RawWay : AbstractNode() {
    @JacksonXmlProperty(localName = "nd")
    var nds: MutableList<NodeReference> = mutableListOf()
}

class Node : AbstractNode() {
    @JacksonXmlProperty(isAttribute = true)
    var lat: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var lon: Float = 0f
}

class BoundingBox {
    @JacksonXmlProperty(isAttribute = true)
    var minlat: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var minlon: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var maxlat: Float = 0f

    @JacksonXmlProperty(isAttribute = true)
    var maxlon: Float = 0f
}

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
    var bounds: BoundingBox = BoundingBox()

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "node")
    var nodes: MutableList<Node> = mutableListOf()

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "way")
    var ways: MutableList<RawWay> = mutableListOf()

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "relation")
    var relations: MutableList<Relation> = mutableListOf()
}