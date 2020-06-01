package de.twoSIT.models

import Coordinate
import GeoDistance
import com.google.gson.Gson
import de.twoSIT.util.getLogger

val logger = getLogger("Clean")

abstract class AbstractElement(var id: String? = null) {
    var additionalTags = mutableMapOf<String, String>()

    fun mapCommonTags(rawElement: RawAbstractElement) {
        additionalTags["visible"] = rawElement.visible.toString()
        additionalTags["version"] = rawElement.version.toString()
        additionalTags["changeset"] = rawElement.changeset
        additionalTags["timestamp"] = rawElement.timestamp
        additionalTags["user"] = rawElement.user
        additionalTags["uid"] = rawElement.uid
    }

    fun enrichWithCommonTags(element: RawAbstractElement) {
        element.id = id!!
        element.visible = additionalTags.remove("visible")!!.toBoolean()
        element.version = additionalTags.remove("version")!!.toFloat()
        element.changeset = additionalTags.remove("changeset")!!
        element.timestamp = additionalTags.remove("timestamp")!!
        element.user = additionalTags.remove("user")!!
        element.uid = additionalTags.remove("uid")!!

        element.tags = mutableListOf()
        for (tag in additionalTags) {
            val newTag = Tag()
            newTag.k = tag.key
            newTag.v = tag.value
            element.tags.add(newTag)
        }
    }
}

class Node(id: String? = null, val latitude: Double, val longitude: Double): AbstractElement(id), Comparable<Node> {

    val proximityThreshold = 0.2 // meters

    companion object {
        fun fromRaw(rawNode: RawNode): Node {
            val node = Node(rawNode.id, rawNode.lat, rawNode.lon)
            node.mapCommonTags(rawNode)
            for (tag in rawNode.tags){
                node.additionalTags[tag.k] = tag.v
            }
            return node
        }
    }

    override fun compareTo(other: Node): Int {
        if (id!! < other.id!!) return -1
        if (id!! > other.id!!) return 1
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other is Node){
            val distance = GeoDistance.haversineDistanceInM(Coordinate(latitude, longitude), Coordinate(other.latitude, other.longitude))
            return distance < proximityThreshold
        }
        return super.equals(other)
    }

    fun toRawNode(): RawNode {
        val rawNode = RawNode()
        enrichWithCommonTags(rawNode)
        rawNode.lat = this.latitude
        rawNode.lon = this.longitude
        return rawNode
    }

    fun toNodeReference(): NodeReference {
        val nodeRef = NodeReference()
        assert(id != null) { "Conversion to NodeReference impossible, id is missing" }
        nodeRef.ref = this.id!!
        return nodeRef
    }

    fun deepCopy(): Node {
        val JSON = Gson().toJson(this)
        return Gson().fromJson(JSON, Node::class.java)
    }
}

open class Way(id: String? = null): AbstractElement(id) {
    val nodes = mutableListOf<Node>()
    val subsections = mutableListOf<SubSection>()

    companion object {
        fun fromRaw(rawWay: RawWay, nodes: MutableMap<String, Node>): Way {
            val way = Way(rawWay.id)
            for (nodeRef in rawWay.nds) {
                way.nodes.add(nodes[nodeRef.ref]!!)
            }
            for (nodeInd in 0 until way.nodes.size-1) {
                way.subsections.add(SubSection(way.nodes[nodeInd], way.nodes[nodeInd+1]))
            }
            way.mapCommonTags(rawWay)
            for (tag in rawWay.tags){
                way.additionalTags[tag.k] = tag.v
            }


            return way
        }
    }

    fun compareWay(way: Way) {
        for (node in nodes){

        }
    }

    fun toRawWay(): RawWay {
        val rawWay = RawWay()
        enrichWithCommonTags(rawWay)
        rawWay.nds = mutableListOf()
        for (node in nodes) {
            rawWay.nds.add(node.toNodeReference())
        }
        return rawWay
    }

    fun deepCopy(): Way {
        val JSON = Gson().toJson(this)
        return Gson().fromJson(JSON, Way::class.java)
    }
}


data class Member(val ref: String, val role: String) {
    fun toRawMember(type: String): RawMember {
        val rawMember = RawMember()
        rawMember.type = type
        rawMember.ref = ref
        rawMember.role = role
        return rawMember
    }
}

class Relation(id: String? = null): AbstractElement(id) {
    val nodeMembers = mutableListOf<Member>()
    val wayMembers = mutableListOf<Member>()
    val relationMembers = mutableListOf<Member>()

    companion object {
        fun fromRaw(rawRelation: RawRelation): Relation {
            val relation = Relation(rawRelation.id)
            for (rawMember in rawRelation.members) {
                val member = Member(rawMember.ref, rawMember.role)
                when (rawMember.type) {
                    "node" -> relation.nodeMembers.add(member)
                    "way" -> relation.wayMembers.add(member)
                    "relation" -> relation.relationMembers.add(member)
                    else -> logger.info("unexpected membertype found: ${rawMember.type}")
                }
            }
            relation.mapCommonTags(rawRelation)
            for (tag in rawRelation.tags){
                relation.additionalTags[tag.k] = tag.v
            }

            return relation
        }
    }

    fun toRawRelation(): RawRelation {
        val rawRelation = RawRelation()
        enrichWithCommonTags(rawRelation)
        rawRelation.members = mutableListOf()
        for (member in nodeMembers) rawRelation.members.add(member.toRawMember("node"))
        for (member in wayMembers) rawRelation.members.add(member.toRawMember("way"))
        for (member in relationMembers) rawRelation.members.add(member.toRawMember("relation"))

        return rawRelation
    }

    fun deepCopy(): Relation {
        val JSON = Gson().toJson(this)
        return Gson().fromJson(JSON, Relation::class.java)
    }
}

