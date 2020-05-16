package de.twoSIT.models

class Response(rawResponse: RawResponse) {
    var relations: MutableMap<String, Relation> = mutableMapOf()
    var ways: MutableMap<String, Way> = mutableMapOf()
    var nodes: MutableMap<String, Node> = mutableMapOf()

    init {
        parseRelations(rawResponse.relations)
        parseWays(rawResponse.ways)
        parseNodes(rawResponse.nodes)
    }

    fun parseRelations(rawRelations: MutableList<Relation>) {
        for (relation in rawRelations){
            var id = ""
            for (tag in relation.tags){
                if (tag.k == "type" && tag.v == "building"){
                    for (member in relation.members){
                        when (member.type) {
                            "way" -> {
                                // todo get way from ref and add it to this.ways
                            }
                            "relation" -> {
                                // todo get relation from ref, check if it is already in this.relations and, if not,
                                //  add it to this.ways
                            }
                            else -> {
                                // log it
                            }
                        }
                    }
                    break
                } else if (tag.k == "id") {
                    id = tag.v
                }
            }
            relations[id] = relation
        }
    }

    fun parseWays(rawWays: MutableList<Way>) {
        for (way in rawWays) {
            var id = ""
            for (tag in way.tags) {
                if (tag.k == "id") {
                    id = tag.v
                }

            }
            ways[id] = way
        }
    }

    fun parseNodes(rawNodes: MutableList<Node>) {
        for (node in rawNodes) {
            var id = ""
            for (tag in node.tags) {
                if (tag.k == "id") {
                    id = tag.v
                }
            }
            nodes[id] = node
        }
    }
}