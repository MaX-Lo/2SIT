package de.twoSIT.models

class Way : AbstractNode() {
    val nodes = mutableMapOf<String, Node>()

    fun toRaw(): RawWay{
        val rawWay = RawWay()
        rawWay.id = id
        rawWay.visible = visible
        rawWay.tags = tags
        rawWay.user = user
        rawWay.uid = uid
        rawWay.timestamp = timestamp
        rawWay.changeset = changeset
        rawWay.version = version
        rawWay.nds = mutableListOf()
        for (nodeId in nodes.keys){
            val nr = NodeReference()
            nr.ref = nodeId
            rawWay.nds.add(nr)
        }
        return rawWay
    }
}

class Relation : AbstractNode(){
    val relationReferences = mutableListOf<String>()
    val ways = mutableMapOf<String, Way>()
}

class Response(rawResponse: RawResponse) {
    var relations: MutableMap<String, Relation> = mutableMapOf()
    var ways: MutableMap<String, Way> = mutableMapOf()
    var nodes: MutableMap<String, Node> = mutableMapOf()

    init {
        parseRelations(rawResponse)
        // now in relations [Relation] there should only be necessary relations, that composite only corresponding and
        // therefore necessary ways [Way], that again composite only corresponding and therefore necessary nodes [Node].
    }

    private fun parseRelations(rawResponse: RawResponse) {
        val allWays = parseWays(rawResponse)
        for (relation in rawResponse.relations){
            val cleanRelation = Relation()
            cleanRelation.id = relation.id
            cleanRelation.visible = relation.visible
            cleanRelation.version = relation.version
            cleanRelation.changeset = relation.changeset
            cleanRelation.timestamp = relation.timestamp
            cleanRelation.user = relation.user
            cleanRelation.uid = relation.uid
            cleanRelation.tags = relation.tags

            for (tag in relation.tags){
                if (tag.k == "type" && tag.v == "building"){
                    for (member in relation.members){
                        when (member.type) {
                            "way" -> {
                                val way = allWays[member.ref]!!
                                cleanRelation.ways[way.id] = way
                                ways[way.id] = way
                                for (node in way.nodes.values){
                                    nodes[node.id] = node
                                }
                            }
                            "relation" -> cleanRelation.relationReferences.add(member.ref)
                            else -> {
                                // log it
                            }
                        }
                    }
                    relations[relation.id] = cleanRelation
                    break
                }
            }
        }
    }

    private fun parseWays(rawResponse: RawResponse): MutableMap<String, Way> {
        val allWays: MutableMap<String, Way> = mutableMapOf()
        val allNodes = parseNodes(rawResponse.nodes)
        for (way in rawResponse.ways) {
            val cleanWay = Way()
            cleanWay.id = way.id
            cleanWay.visible = way.visible
            cleanWay.version = way.version
            cleanWay.changeset = way.changeset
            cleanWay.timestamp = way.timestamp
            cleanWay.user = way.user
            cleanWay.uid = way.uid
            cleanWay.tags = way.tags
            for (nodeRef in way.nds){
                cleanWay.nodes[nodeRef.ref] = allNodes[nodeRef.ref]!!
            }
            allWays[cleanWay.id] = cleanWay
        }
        return allWays
    }

    private fun parseNodes(rawNodes: MutableList<Node>): MutableMap<String, Node> {
        val allNodes = mutableMapOf<String, Node>()
        for (node in rawNodes) {
            allNodes[node.id] = node
        }
        return allNodes
    }
}