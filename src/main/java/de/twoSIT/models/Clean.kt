package de.twoSIT.models

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

}

class Way(id: String? = null): AbstractElement(id) {
    val nodes = mutableMapOf<String, Node>()

    companion object {
        fun fromRawWay(rawWay: RawWay, nodes: MutableMap<String, Node>): Way {
            val way = Way(rawWay.id)
            for (nodeRef in rawWay.nds) {
                way.nodes[nodeRef.ref] = nodes[nodeRef.ref]!!
            }
            way.mapCommonTags(rawWay)
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
            node.mapCommonTags(rawNode)
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
            relation.mapCommonTags(rawRelation)
            for (tag in rawRelation.tags){
                relation.additionalTags[tag.k] = tag.v
            }

            return relation
        }
    }
}

