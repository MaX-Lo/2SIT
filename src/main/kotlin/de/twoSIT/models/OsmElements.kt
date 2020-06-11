package de.twoSIT.models

import Coordinate
import GeoDistance
import com.google.gson.Gson
import de.twoSIT.util.IdGenerator
import de.twoSIT.util.getLogger

private val logger = getLogger("OsmElement")

data class Member(val ref: String, val role: String) {
    fun toRawMember(type: String): RawMember {
        val rawMember = RawMember()
        rawMember.type = type
        rawMember.ref = ref
        rawMember.role = role
        return rawMember
    }
}

abstract class AbstractOsmElement(val id: String, val additionalTags: MutableMap<String, String>) {

    companion object {
        fun fromRaw(element: RawAbstractElement): AbstractOsmElement {
            throw NotImplementedError()
        }
    }

    abstract fun toRaw(): RawAbstractElement

    fun mapCommonTags(rawElement: RawAbstractElement) {
        additionalTags["visible"] = rawElement.visible.toString()
        additionalTags["version"] = rawElement.version.toString()
        additionalTags["changeset"] = rawElement.changeset
        additionalTags["timestamp"] = rawElement.timestamp
        additionalTags["user"] = rawElement.user
        additionalTags["uid"] = rawElement.uid
    }

    fun enrichWithCommonTags(element: RawAbstractElement) {
        element.id = id
        // ToDo confirm that defaults are usefull for new created elements, check if some of these need to be updated
        //      for modified elements
        element.visible = if (additionalTags.containsKey("visible")) additionalTags.remove("visible")!!.toBoolean() else true
        element.version = if (additionalTags.containsKey("version")) additionalTags.remove("version")!!.toInt() else 1
        element.changeset = if (additionalTags.containsKey("changeset")) additionalTags.remove("changeset")!! else "42"
        element.timestamp = if (additionalTags.containsKey("timestamp")) additionalTags.remove("timestamp")!! else ""
        element.user = if (additionalTags.containsKey("user")) additionalTags.remove("user")!! else "2SIT"
        element.uid = if (additionalTags.containsKey("uid")) additionalTags.remove("uid")!! else "-1"

        element.tags = mutableListOf()
        for (tag in additionalTags) {
            val newTag = Tag()
            newTag.k = tag.key
            newTag.v = tag.value
            element.tags.add(newTag)
        }
    }
}

open class Node(id: String, var latitude: Double, var longitude: Double, additionalTags: MutableMap<String, String>) :
        AbstractOsmElement(id, additionalTags) {

    companion object {
        // FixMe is it task of a node to take care what is in proximity? Each node could have a different threshold?
        private const val proximityThreshold = 0.2 // meters

        fun fromRaw(rawNode: RawNode): Node {
            val additionalTags = mutableMapOf<String, String>()
            for (tag in rawNode.tags) {
                additionalTags[tag.k] = tag.v
            }
            val node = Node(rawNode.id, rawNode.lat, rawNode.lon, additionalTags)
            node.mapCommonTags(rawNode)
            return node
        }

        fun getMerged(others: Iterable<Node>): Node {
            val othersAsList = others.toList()
            for (i in 0 until othersAsList.size - 1) {
                val node1 = othersAsList[i]
                for (j in i + 1 until othersAsList.size) {
                    val node2 = othersAsList[j]
                    if (node1.distanceTo(node2) > proximityThreshold * 1.5) {
                        logger.warn("merging nodes ${node1.id}, ${node2.id} that are not in proximity! Distance: ${node1.distanceTo(node2)}")
                    }
                }
            }
            // ToDo tags contained in multiple nodes are overwritten - do we want that?
            val additionalTags = mutableMapOf<String, String>()
            others.map { additionalTags.putAll(it.additionalTags) }

            val latitude = others.map { it.latitude }.average()
            val longitude = others.map { it.longitude }.average()

            return Node(IdGenerator.getNewId(), latitude, longitude, additionalTags)
        }
    }

    fun getMerged(other: Node): Node {
        return getMerged(setOf(this, other))
    }

    fun inProximity(other: Node): Boolean {
        return distanceTo(other) < proximityThreshold
    }

    fun distanceTo(other: Node): Double {
        /**
         * @return the haversine distance to [other] in meters
         */
        return GeoDistance.haversineDistanceInM(Coordinate(latitude, longitude), Coordinate(other.latitude, other.longitude))
    }

    override fun toRaw(): RawNode {
        val rawNode = RawNode()
        enrichWithCommonTags(rawNode)
        rawNode.lat = this.latitude
        rawNode.lon = this.longitude
        return rawNode
    }

    fun toNodeReference(): RawNodeReference {
        val nodeRef = RawNodeReference()
        nodeRef.ref = this.id
        return nodeRef
    }

    fun toMember(): Member {
        return Member(id, "")
    }

    fun deepCopy(): Node {
        val JSON = Gson().toJson(this)
        return Gson().fromJson(JSON, Node::class.java)
    }
}

open class Way(id: String, additionalTags: MutableMap<String, String>) : AbstractOsmElement(id, additionalTags) {
    val subsections = mutableListOf<SubSection>()
    val nodes = mutableListOf<Node>()
        get() {
            field.clear()
            for (subsection in subsections) {
                field.add(subsection.node1)
            }
            return field
        }

    companion object {
        fun fromRaw(rawWay: RawWay, allNodes: MutableMap<String, Node>): Way {
            val additionalTags = mutableMapOf<String, String>()
            for (tag in rawWay.tags) {
                additionalTags[tag.k] = tag.v
            }

            val nodes = mutableListOf<Node>()
            for (nodeRef in rawWay.nds) {
                nodes.add(allNodes[nodeRef.ref]!!)
            }

            val way = Way(rawWay.id, additionalTags)
            way.mapCommonTags(rawWay)

            for (nodeInd in 0 until nodes.size - 1) {
                way.subsections.add(SubSection(nodes[nodeInd], nodes[nodeInd + 1]))
            }
            return way
        }
    }

    fun replaceSubsection(old: SubSection, new: SubSection) {
        if (old in subsections) {
            var tmp = new.copy()
            if (!old.node1.inProximity(new.node1)) {
                tmp = SubSection(new.node2, new.node1)
            }
            val ind = subsections.indexOf(old)
            subsections[ind] = tmp
        } else {
            logger.warn("$old is not a subsection of room $id")
        }
    }

    override fun toRaw(): RawWay {
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

    fun toMember(): Member {
        return Member(id, "")
    }
}

open class Relation(id: String, additionalTags: MutableMap<String, String>) :
        AbstractOsmElement(id, additionalTags) {
    var nodeMembers = mutableListOf<Member>()
    var wayMembers = mutableListOf<Member>()
    var relationMembers = mutableListOf<Member>()

    companion object {
        fun fromRaw(rawRelation: RawRelation): Relation {
            val additionalTags = mutableMapOf<String, String>()
            for (tag in rawRelation.tags) {
                additionalTags[tag.k] = tag.v
            }

            val relation = Relation(rawRelation.id, additionalTags)
            relation.mapCommonTags(rawRelation)

            for (rawMember in rawRelation.members) {
                val member = Member(rawMember.ref, rawMember.role)
                when (rawMember.type) {
                    "node" -> relation.nodeMembers.add(member)
                    "way" -> relation.wayMembers.add(member)
                    "relation" -> relation.relationMembers.add(member)
                    else -> logger.info("unexpected membertype found: ${rawMember.type}")
                }
            }

            return relation
        }
    }

    fun toMember(): Member {
        return Member(id, "")
    }


    override fun toRaw(): RawRelation {
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

