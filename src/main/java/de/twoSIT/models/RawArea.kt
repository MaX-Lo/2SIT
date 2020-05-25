package de.twoSIT.models

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

class RawMember {
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

abstract class RawAbstractElement {
    @JacksonXmlProperty(isAttribute = true)
    var id: String = "not available"

    @JacksonXmlProperty(isAttribute = true)
    var visible: Boolean = false

    @JacksonXmlProperty(isAttribute = true)
    var version: Float = 0f

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


class RawRelation : RawAbstractElement() {
    @JacksonXmlProperty(localName = "member")
    var members: MutableList<RawMember> = mutableListOf()
}

@JacksonXmlRootElement(localName = "osm")
class RawWay : RawAbstractElement() {
    companion object {
        @JvmStatic
        fun fromString(rawXmlString: String): RawWay{
            // todo add some fault tolerance here too
            val module = JacksonXmlModule()
            module.setDefaultUseWrapper(false)
            val xmlMapper = XmlMapper(module)

            return xmlMapper.readValue(rawXmlString, RawWay::class.java)
        }
    }


    @JacksonXmlProperty(localName = "nd")
    var nds: MutableList<NodeReference> = mutableListOf()
}


class RawNode : RawAbstractElement() {
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
class RawArea {
    companion object {
        @JvmStatic
        fun fromString(rawXmlString: String): RawArea{
            // todo add some fault tolerance here too
            val module = JacksonXmlModule()
            module.setDefaultUseWrapper(false)
            val xmlMapper = XmlMapper(module)

            return xmlMapper.readValue(rawXmlString, RawArea::class.java)
        }
    }

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
    var nodes: MutableList<RawNode> = mutableListOf()

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "way")
    var ways: MutableList<RawWay> = mutableListOf()

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "relation")
    var relations: MutableList<RawRelation> = mutableListOf()
}