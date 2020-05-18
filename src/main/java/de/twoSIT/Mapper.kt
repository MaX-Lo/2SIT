package de.twoSIT

import de.twoSIT.models.*

class Mapper {
    fun parse(rawResponse: RawResponse): List<Building> {
        val allWays = parseWays(rawResponse)
        val allRelations = mutableMapOf<String, RawRelation>()
        for (relation in rawResponse.relations) allRelations[relation.id] = relation

        val buildings = mutableListOf<Building>()

        for (relation in rawResponse.relations) {
            val tmpOutline = mutableListOf<Way>()
            val tmpFloors = mutableListOf<Floor>()
            val tmpAdditionalTags = mutableListOf<Tag>()
            val tmpIndoorObjects = mutableListOf<IndoorObject>()
            val tmpLevelConnections = mutableListOf<LevelConnection>()
            var tmpMinLevel: Int? = null
            var tmpMaxLevel: Int? = null
            var tmpHeight: Float? = null
            var tmpName: String? = null
            var tmpIsBuilding = false

            for (tag in relation.tags) {
                when (tag.k) {
                    "type" -> {
                        if (tag.v == "building"){
                            tmpIsBuilding = true
                            for (member in relation.members) {
                                when (member.type) {
                                    "way" -> {
                                        val way = allWays[member.ref]!!
                                        tmpIndoorObjects.addAll(parseIndoorObjects(way.nodes.values))
                                        if (!tmpOutline.contains(way)) {
                                            tmpOutline.add(way)
                                        } else {
                                            println("Outline for building-relation ${relation.id} is already defined")
                                        }
                                        if (tmpOutline.size > 1) {
                                            println("Multiple outline for building-relation ${relation.id}")
                                        }
                                    }
                                    "relation" -> {
                                        parseFloor(allRelations[member.ref]!!)?.let { tmpFloors.add(it) }
                                    }
                                    else -> println("Could not parse building-relation member ${member.type}")
                                }
                            }
                        }
                    }
                    "building:max_level" -> tmpMaxLevel = tag.v.toIntOrNull()
                    "building:min_level" -> tmpMinLevel = tag.v.toIntOrNull()
                    "name" -> tmpName = tag.v
                    "height" -> tmpHeight = tag.v.toFloatOrNull()
                    else -> tmpAdditionalTags.add(tag)
                }
            }

            if (tmpIsBuilding){
                var allNecessaryStuff = true
                if (tmpMinLevel == null) {
                    allNecessaryStuff = false
                    println("Could not parse building-relation ${relation.id}: no min-level")
                }
                if (tmpMaxLevel == null) {
                    allNecessaryStuff = false
                    println("Could not parse building-relation ${relation.id}: no max-level")
                }
                if (tmpOutline.size == 0) {
                    allNecessaryStuff = false
                    println("Could not parse building-relation ${relation.id}: no outline")
                }
                if (tmpOutline.size > 1) {
                    allNecessaryStuff = false
                    println("Could not parse building-relation ${relation.id}: too many outlines")
                }
                if (tmpFloors.size == 0) {
                    allNecessaryStuff = false
                    println("Could not parse building-relation ${relation.id}: no floors")
                }

                if (allNecessaryStuff){
                    val building = Building(tmpMinLevel!!, tmpMaxLevel!!, tmpFloors, tmpIndoorObjects, tmpLevelConnections,
                            tmpOutline[0])
                    if (tmpHeight != null) building.height = tmpHeight
                    if (tmpName != null) building.name = tmpName
                    if (tmpAdditionalTags.size > 0) building.additionalTags.addAll(tmpAdditionalTags)
                    buildings.add(building)
                }
            }
        }
        return buildings
    }

    private fun parseLevelConnections(): LevelConnection? {
        return null
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
            for (nodeRef in way.nds) {
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

    private fun parseIndoorObjects(nodes: MutableCollection<Node>): List<IndoorObject> {
        val indoorObjects = mutableListOf<IndoorObject>()

        for (node in nodes) {
            var objectLevel: Int? = null
            var objectHeight: Float? = null
            var objectName: String? = null
            var objectRef: String? = null
            var indoorTag: IndoorTag? = null
            val additionalObjectTags = mutableListOf<Tag>()
// todo parse indoorTag
            for (tag in node.tags) {
                when (tag.k) {
                    "level" -> objectLevel = tag.v.toIntOrNull()
                    "height" -> objectHeight = tag.v.toFloatOrNull()
                    "name" -> objectName = tag.v
                    "ref" -> objectRef = tag.v
                    else -> additionalObjectTags.add(tag)
                }
            }
            if (objectLevel != null && indoorTag != null) {
                val obj = IndoorObject(level = objectLevel, indoorTag = indoorTag)
                obj.height = objectHeight
                obj.name = objectName
                obj.ref = objectRef
                obj.additionalTags = additionalObjectTags
                indoorObjects.add(obj)
            } else {
                println("Could not parse indoorObject-node ${node.id}")
            }
        }
        return indoorObjects
    }

    private fun parseFloor(relation: RawRelation): Floor? {
        var floorLevel: Int? = null
        var floorHeight: Float? = null
        var floorRef: String? = null
        var floorName: String? = null
        val additionalFloorTags = mutableListOf<Tag>()

        // todo floorRef parsing

        for (tag in relation.tags) {
            when (tag.k) {
                "level" -> floorLevel = tag.v.toIntOrNull()
                "height" -> floorHeight = tag.v.toFloatOrNull()
                "name" -> floorName = tag.v
                "building" -> println("Floor-relation ${relation.id} has building tag o.O")
                else -> additionalFloorTags.add(tag) // add all unknown tags
            }
        }
        if (floorLevel == null) {
            println("Could not parse floor-relation ${relation.id}: no level tag")
            return null
        }

        val floor = Floor(level = floorLevel)
        floor.name = floorName
        floor.height = floorHeight
        floor.ref = floorRef

        return floor
    }
}