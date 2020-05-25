package de.twoSIT.models

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.io.FileInputStream
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.Source


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

class Tag(
        @JacksonXmlProperty(isAttribute = true)
        var k: String = "not available",
        @JacksonXmlProperty(isAttribute = true)
        var v: String = "not available"
)

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

@JacksonXmlRootElement(localName = "way")
class RawWay : RawAbstractElement() {
    companion object {
        @JvmStatic
        fun fromString(rawXmlString: String): RawWay {
            // todo add some fault tolerance here too
            val f = XMLInputFactory.newFactory()
            val sr: XMLStreamReader = f.createXMLStreamReader(StringReader(rawXmlString))
            sr.next()
            sr.next()

            val xmlMapper = XmlMapper()
            xmlMapper.setDefaultUseWrapper(false)
            xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

            return xmlMapper.readValue(sr, RawWay::class.java)
        }

        @JvmStatic
        fun multipleFromString(rawXmlString: String): MutableList<RawWay> {
            val resultList = mutableListOf<RawWay>()

            val xmlMapper = XmlMapper()
            xmlMapper.setDefaultUseWrapper(false)
            xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            val f = XMLInputFactory.newFactory()
            val sr: XMLStreamReader = f.createXMLStreamReader(StringReader(rawXmlString))
            sr.next()

            try {
                while (sr.hasNext()){
                    sr.next()
                    resultList.add(xmlMapper.readValue(sr, RawWay::class.java))
                }
            } catch (e: NoSuchElementException){
                // stupid fucking xml parsing shit fuuuuu
            }

            return resultList
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
        fun fromString(rawXmlString: String): RawArea {
            // todo add some fault tolerance here too
            val xmlMapper = XmlMapper()
            xmlMapper.setDefaultUseWrapper(false)

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