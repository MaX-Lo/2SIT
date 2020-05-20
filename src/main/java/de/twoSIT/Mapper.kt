package de.twoSIT

import de.twoSIT.models.*
import de.twoSIT.util.getLogger


class Mapper(rawResponse: RawResponse) {
    companion object {
        @JvmStatic
        private val logger = getLogger(Mapper::class.java)
    }


    private val allWays = mutableMapOf<String, Way>()
    private val allRelations = mutableMapOf<String, Relation>()
    private val allNodes = mutableMapOf<String, Node>()

    private val buildings = mutableListOf<Building>()
    private val unparsedBuildings = mutableListOf<Building>()

    private lateinit var currentBuilding: Building


    init {
        for (node in rawResponse.nodes) allNodes[node.id] = node
        parseWays(rawResponse)
        for (relation in rawResponse.relations) allRelations[relation.id] = relation
    }

    fun parse(): List<Building> {
        parseBuildingRel()

        return buildings
    }

    private fun getAllBuildingRel(): MutableList<Relation> {
        val buildingRelations = mutableListOf<Relation>()

        for (relation in allRelations.values) {
            for (tag in relation.tags) {
                if (tag.k == "type" && tag.v == "building") buildingRelations.add(relation)
            }
        }
        return buildingRelations
    }

    private fun parseBuildingRel() {
        val buildingRelations = getAllBuildingRel()
        for (relation in buildingRelations) {
            currentBuilding = Building()
            currentBuilding.id = relation.id

            for (tag in relation.tags) {
                when (tag.k) {
                    "building:max_level" -> currentBuilding.maxLevel = tag.v.toIntOrNull()
                    "building:min_level" -> currentBuilding.minLevel = tag.v.toIntOrNull()
                    "name" -> currentBuilding.name = tag.v
                    "height" -> currentBuilding.height = tag.v.toFloatOrNull()
                    else -> currentBuilding.additionalTags[tag.k] = tag.v
                }
            }

            for (member in relation.members) {
                when (member.type) {
                    "way" -> {
                        if (currentBuilding.mainWay == null) {
                            currentBuilding.mainWay = allWays[member.ref]
                        } else {
                            logger.info("Multiple mainWays for building-relation ${relation.id}")
                        }
                    }
                    "relation" -> {
                        if (allRelations.containsKey(member.ref)) {
                            parseFloor(allRelations[member.ref]!!)
                        } else {
                            logger.warn("FATAL: Could not find ${member.ref} in allRelations... oh nooo")
                        }
                    }
                    "node" -> {
                        if (member.role == "entrance") {
                            if (!allNodes.containsKey(member.ref)) {
                                logger.warn("FATAL: Could not find ${member.ref} in allNodes... oh nooo")
                            } else {
                                // todo this is a door
                                val door = allNodes[member.ref]!!
                            }
                        }
                    }
                    else -> logger.info("Could not parse building-relation member ${member.type}")
                }
            }

            if (currentBuilding.check()) {
                buildings.add(currentBuilding)
            } else {
                unparsedBuildings.add(currentBuilding)
            }
        }

    }

    private fun parseFloor(relation: Relation) {

        val floor = Floor()
        floor.id = relation.id

        // todo floorRef parsing

        for (tag in relation.tags) {
            when (tag.k) {
                "level" -> floor.level = tag.v.toIntOrNull()
                "height" -> floor.height = tag.v.toFloatOrNull()
                "name" -> floor.name = tag.v
                else -> floor.additionalTags[tag.k] = tag.v
            }
        }

        for (member in relation.members) {
            when (member.type) {
                "way" -> {
                    if (!allWays.containsKey(member.ref)) {
                        logger.warn("FATAL: Could not find ${member.ref} in allWays... oh nooo")
                    } else if (floor.level == null) {
                        logger.warn("Cannot parse room ${member.ref}: floor has no level")
                    } else {
                        parseRoom(allWays[member.ref]!!, floor.level!!)
                    }
                }
                "node" -> {
                    if (!allNodes.containsKey(member.ref)) {
                        logger.warn("FATAL: Could not find ${member.ref} in allNodes... oh nooo")
                    } else if (floor.level == null) {
                        logger.warn("Cannot parse room ${member.ref}: floor has no level")
                    } else {
                        parseIndoorObject(allNodes[member.ref]!!, floor.level!!)
                    }
                }
                "relation" -> {
                    if (!allRelations.containsKey(member.ref)) {
                        logger.warn("FATAL: Could not find ${member.ref} in allRelations... oh nooo")
                    } else {
                        val rel = allRelations[member.ref]!!
                        for (member in rel.members) {
                            when (member.role) {
                                "outer" -> {
                                    if (!allWays.containsKey(member.ref)) {
                                        logger.warn("FATAL: Could not find ${member.ref} in allWays... oh nooo")
                                    } else {
                                        currentBuilding.outline = allWays[member.ref]!!
                                    }
                                }
                                "inner" -> {
                                    if (!allWays.containsKey(member.ref)) {
                                        logger.warn("FATAL: Could not find ${member.ref} in allWays... oh nooo")
                                    } else {
                                        currentBuilding.innerline = allWays[member.ref]!!
                                    }
                                }
                            }
                        }
                    }
                }
                else -> logger.info("Unrecognized member type while parsing floor-relation ${relation.id}: '${member.type}'")
            }
        }
        if (floor.check()) {
            currentBuilding.floors.add(floor)
        }
    }

    private fun parseIndoorObject(node: Node, level: Int) {
        val indoorObject = IndoorObject()
        indoorObject.id = node.id
        indoorObject.level = level

        for (tag in node.tags) {
            indoorObject.additionalTags[tag.k] = tag.v
        }

        if (indoorObject.check()) currentBuilding.indoorObjects.add(indoorObject)
    }

    private fun parseLevelConnections(way: Way) {
        val levelConnection = LevelConnection()
        levelConnection.id = way.id

        if (levelConnection.check()) currentBuilding.connections.add(levelConnection)
    }

    private fun parseWays(rawResponse: RawResponse) {
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
    }

    private fun parseRoom(way: Way, level: Int) {
        val room = Room()
        room.id = way.id
        room.level = level

        for (tag in way.tags) {
            when (tag.k) {
                "level" -> room.level = tag.v.toIntOrNull()
                "height" -> room.height = tag.v.toFloatOrNull()
                "name" -> room.name = tag.v
                "ref" -> room.ref = tag.v
                "buildingpart" -> {
                    when (tag.v) {
                        "corridor" -> room.indoorTag = IndoorTag.CORRIDOR
                        "room" -> room.indoorTag = IndoorTag.ROOM
                        "hall" -> room.indoorTag = IndoorTag.AREA
                        "verticalpassage" -> {
                            parseLevelConnections(way)
                            return
                        }
                        "shell" -> {
                            room.outline = way
                            return
                        }
                        else -> logger.info("Unrecognized building part/indoor tag in room-way ${way.id}: '${tag.v}'")
                    }
                }
                else -> room.additionalTags[tag.k] = tag.v
            }
        }
        if (room.check()) {
            currentBuilding.rooms.add(room)
        }
    }

}