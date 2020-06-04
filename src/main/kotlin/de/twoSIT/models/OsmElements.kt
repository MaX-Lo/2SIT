package de.twoSIT.models

import Coordinate
import GeoDistance
import com.google.gson.Gson
import de.twoSIT.util.IdGenerator
import de.twoSIT.util.getLogger

private val logger = getLogger("OsmElement")

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

open class Node(id: String? = null, var latitude: Double, var longitude: Double) : AbstractElement(id) {
    private val proximityThreshold = 0.2 // meters

    companion object {
        fun fromRaw(rawNode: RawNode): Node {
            val node = Node(rawNode.id, rawNode.lat, rawNode.lon)
            node.mapCommonTags(rawNode)
            for (tag in rawNode.tags) {
                node.additionalTags[tag.k] = tag.v
            }
            return node
        }

        fun getMerged(others: Iterable<Node>): Node {
            val othersAsList = others.toList()
            for (i in 0 until othersAsList.size - 1) {
                val node1 = othersAsList.get(i)
                for (j in i + 1 until othersAsList.size) {
                    val node2 = othersAsList.get(j)
                    if (!node1.inProximity(node2)) {
                        logger.warn("merging nodes ${node1.id}, ${node2.id} that are not in proximity!!")
                    }
                }
            }

            val latitude = others.map { it.latitude }.average()
            val longitude = others.map { it.longitude }.average()
            val additionalTags = mutableMapOf<String, String>()
            // ToDo tags contained in multiple nodes are overwritten - do we want that?
            others.map { additionalTags.putAll(it.additionalTags) }

            val newNode = Node(IdGenerator.getNewId(), latitude, longitude)
            newNode.additionalTags.putAll(additionalTags)

            return newNode
        }
    }

    fun getMerged(other: Node): Node {
        return getMerged(setOf(this, other))
    }

    fun inProximity(other: Node): Boolean {
        // FixMe is it task of a node to take care what is in proximity? Each node could have a different threshold?
        return distanceTo(other) < proximityThreshold
    }

    fun distanceTo(other: Node): Double {
        /**
         * @return the haversine distance to [other] in meters
         */
        return GeoDistance.haversineDistanceInM(Coordinate(latitude, longitude), Coordinate(other.latitude, other.longitude))
    }

    fun toRawNode(): RawNode {
        val rawNode = RawNode()
        enrichWithCommonTags(rawNode)
        rawNode.lat = this.latitude
        rawNode.lon = this.longitude
        return rawNode
    }

    fun toNodeReference(): RawNodeReference {
        val nodeRef = RawNodeReference()
        assert(id != null) { "Conversion to NodeReference impossible, id is missing" }
        nodeRef.ref = this.id!!
        return nodeRef
    }

    fun toMember(): Member {
        return Member(id!!, "")
    }

    fun deepCopy(): Node {
        val JSON = Gson().toJson(this)
        return Gson().fromJson(JSON, Node::class.java)
    }
}

open class Way(id: String? = null) : AbstractElement(id) {
    val subsections = mutableListOf<SubSection>()
    val nodes = mutableListOf<Node>()
        get() {
            field.clear()
            for (subsection in subsections) {
                field.add(subsection.node1)
                //field.add(subsection.node2)
            }
            return field
        }

    companion object {
        fun fromRaw(rawWay: RawWay, allNodes: MutableMap<String, Node>): Way {
            val way = Way(rawWay.id)
            val nodes = mutableListOf<Node>()
            for (nodeRef in rawWay.nds) {
                nodes.add(allNodes[nodeRef.ref]!!)
            }
            for (nodeInd in 0 until nodes.size - 1) {
                way.subsections.add(SubSection(nodes[nodeInd], nodes[nodeInd + 1]))
            }
            way.mapCommonTags(rawWay)
            for (tag in rawWay.tags) {
                way.additionalTags[tag.k] = tag.v
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

    fun toMember(): Member {
        return Member(id!!, "")
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

open class Relation(id: String? = null) : AbstractElement(id) {
    var nodeMembers = mutableListOf<Member>()
    var wayMembers = mutableListOf<Member>()
    var relationMembers = mutableListOf<Member>()

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
            for (tag in rawRelation.tags) {
                relation.additionalTags[tag.k] = tag.v
            }

            return relation
        }
    }

    fun toMember(): Member {
        return Member(id!!, "")
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

