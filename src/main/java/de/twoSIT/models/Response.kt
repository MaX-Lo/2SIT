package de.twoSIT.models

class Response(rawResponse: RawResponse) {
    var relations: MutableList<relation> = mutableListOf()
    var ways: MutableList<way> = mutableListOf()
    var nodes: MutableList<node> = mutableListOf()

    init {
        for (relation in rawResponse.relations){
            for (tag in relation.tags){
                if (tag.k == "type" && tag.v == "building"){
                    relations.add(relation)
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
                }
            }
        }

        ways = rawResponse.ways
        nodes = rawResponse.nodes
    }
}