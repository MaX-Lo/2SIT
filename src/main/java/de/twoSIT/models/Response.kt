package de.twoSIT.models

class Way : AbstractNode() {
    val nodes = mutableMapOf<String, Node>()
}

class Relation : AbstractNode(){
    val relationReferences = mutableListOf<String>()
    val ways = mutableMapOf<String, Way>()
}

class Response(rawResponse: RawResponse) {
    var relations: MutableMap<String, Relation> = mutableMapOf()
    private var allWays: MutableMap<String, Way> = mutableMapOf()
    private var allNodes: MutableMap<String, Node> = mutableMapOf()

    init {
        parseNodes(rawResponse.nodes)
        parseWays(rawResponse.ways)
        parseRelations(rawResponse.relations)
        // now in relations [Relation] there should only be necessary relations, that composite only corresponding and
        // therefore necessary ways [Way], that again composite only corresponding and therefore necessary nodes [Node].
        // allWays and allNodes are not necessary anymore
    }

    private fun parseRelations(rawRelations: MutableList<RawRelation>) {
        for (relation in rawRelations){
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
                            "way" -> cleanRelation.ways[member.ref] = allWays[member.ref]!!
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

    private fun parseWays(rawWays: MutableList<RawWay>) {
        for (way in rawWays) {
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
    }

    private fun parseNodes(rawNodes: MutableList<Node>) {
        for (node in rawNodes) {
            allNodes[node.id] = node
        }
    }
}