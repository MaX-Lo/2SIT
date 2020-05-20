package de.twoSIT.models

import de.twoSIT.Mapper
import de.twoSIT.util.getLogger

val logger = getLogger("Clean")

abstract class AbstractElement(var id: String? = null) {
    var additionalTags = mutableMapOf<String, String>()

}

class Way(id: String? = null): AbstractElement(id) {
    val nodes = mutableMapOf<String, Node>()

    companion object {
        fun fromRawWay(rawWay: RawWay, nodes: MutableMap<String, Node>): Way {
            val way = Way(rawWay.id)
            for (nodeRef in rawWay.nds) {
                way.nodes[nodeRef.ref] = nodes[nodeRef.ref]!!
            }
            way.additionalTags["visible"] = rawWay.visible.toString()
            way.additionalTags["version"] = rawWay.version.toString()
            way.additionalTags["changeset"] = rawWay.changeset
            way.additionalTags["timestamp"] = rawWay.timestamp
            way.additionalTags["user"] = rawWay.user
            way.additionalTags["uid"] = rawWay.uid
            for (tag in rawWay.tags){
                way.additionalTags[tag.k] = tag.v
            }
            return way
        }
    }
}

class Node(id: String? = null): AbstractElement(id) {

    companion object {
        fun fromRawNode(rawNode: RawNode): Node {
            val node = Node(rawNode.id)
            for (tag in rawNode.tags){
                node.additionalTags[tag.k] = tag.v
            }
            return node
        }
    }
}

data class Member(val ref: String, val role: String)

class Relation(id: String? = null): AbstractElement(id) {
    val nodeMembers = mutableListOf<Member>()
    val wayMembers = mutableListOf<Member>()
    val relationMembers = mutableListOf<Member>()

    companion object {
        fun fromRawRelation(rawRelation: RawRelation): Relation {
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

            for (tag in rawRelation.tags){
                relation.additionalTags[tag.k] = tag.v
            }

            return relation
        }
    }
}

