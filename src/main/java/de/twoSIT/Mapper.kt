package de.twoSIT

import de.twoSIT.models.*
import de.twoSIT.util.getLogger


class Mapper(rawArea: RawArea) {
    companion object {
        @JvmStatic
        private val logger = getLogger(Mapper::class.java)
    }


    private val allWays = mutableMapOf<String, Way>()
    private val allRelations = mutableMapOf<String, Relation>()
    private val allNodes = mutableMapOf<String, Node>()

    private val buildings = mutableListOf<Building>()
    private val unparsedBuildings = mutableListOf<Building>()

    private lateinit var building: Building


    init {
        for (rawNode in rawArea.nodes) allNodes[rawNode.id] = Node.fromRawNode(rawNode)
        for (way in rawArea.ways) allWays[way.id] =  Way.fromRawWay(way, allNodes)
        for (rawRelation in rawArea.relations) allRelations[rawRelation.id] = Relation.fromRawRelation(rawRelation)
    }

    fun parse(): List<Building> {
        parseBuildingRel()
        return buildings
    }

    private fun getAllBuildingRel(): MutableList<Relation> {
        val buildingRelations = mutableListOf<Relation>()
        for (relation in allRelations.values) {
            if (relation.additionalTags.containsKey("type") && relation.additionalTags["type"] == "building") {
                buildingRelations.add(relation)
            }
        }
        return buildingRelations
    }

    private fun parseBuildingRel() {
        val buildingRelations = getAllBuildingRel()
        for (relation in buildingRelations) {
            building = Building(relation.id)

            for ((tag, value) in relation.additionalTags.entries) {
                when (tag) {
                    "building:max_level" -> building.maxLevel = value.toIntOrNull()
                    "building:min_level" -> {
                        building.minLevel = value.toIntOrNull()
                        // keep tag since it's used not only by indoorOSM
                        building.additionalTags["building:min_level"] = value
                    }
                    "name" -> building.name = value
                    "height" -> building.height = value.toFloatOrNull()
                    else -> building.additionalTags[tag] = value
                }
            }

            for (member in relation.nodeMembers) {
                if (member.role == "entrance") {
                    if (!allNodes.containsKey(member.ref)) {
                        logger.warn("FATAL: Could not find ${member.ref} in allNodes... oh nooo")
                    } else {
                        // todo this is a door
                        val door = allNodes[member.ref]!!
                    }
                }
            }

            for (member in relation.wayMembers) {
                if (building.mainWay == null) {
                    building.mainWay = allWays[member.ref]
                } else {
                    logger.info("Multiple mainWays for building-relation ${relation.id}")
                }
            }

            for (member in relation.relationMembers) {
                if (allRelations.containsKey(member.ref)) {
                    parseFloor(allRelations[member.ref]!!)
                } else {
                    logger.warn("FATAL: Could not find ${member.ref} in allRelations... oh nooo")
                }
            }

            if (building.check()) {
                buildings.add(building)
            } else {
                unparsedBuildings.add(building)
            }
        }

    }

    private fun parseFloor(relation: Relation) {

        val floor = Floor(relation.id)

        // todo floorRef parsing

        for ((tag, value) in relation.additionalTags.entries) {
            when (tag) {
                "level" -> floor.level = value.toIntOrNull()
                "height" -> floor.height = value.toFloatOrNull()
                "name" -> floor.name = value
                else -> floor.additionalTags[tag] = value
            }
        }

        for (member in relation.nodeMembers) {
            if (!allNodes.containsKey(member.ref)) {
                logger.warn("FATAL: Could not find ${member.ref} in allNodes... oh nooo")
            } else if (floor.level == null) {
                logger.warn("Cannot parse room ${member.ref}: floor has no level")
            } else {
                parseIndoorObject(allNodes[member.ref]!!, floor.level!!)
            }
        }

        for (member in relation.wayMembers) {
            if (!allWays.containsKey(member.ref)) {
                logger.warn("FATAL: Could not find ${member.ref} in allWays... oh nooo")
            } else if (floor.level == null) {
                logger.warn("Cannot parse room ${member.ref}: floor has no level")
            } else {
                val way = allWays[member.ref]!!
                if ("level:usage" in way.additionalTags.keys){
                    floor.usages[way.additionalTags["level:usage"]!!] = way
                } else {
                    parseRoom(way, floor.level!!)
                }
            }
        }

        for (member in relation.relationMembers) {
            if (!allRelations.containsKey(member.ref)) {
                logger.warn("FATAL: Could not find ${member.ref} in allRelations... oh nooo")
            } else {
                val rel = allRelations[member.ref]!!
                for (relationMember in rel.wayMembers) {
                    when (relationMember.role) {
                        "outer" -> {
                            if (!allWays.containsKey(relationMember.ref)) {
                                logger.warn("FATAL: Could not find ${relationMember.ref} in allWays... oh nooo")
                            } else {
                                building.outline = allWays[relationMember.ref]!!
                            }
                        }
                        "inner" -> {
                            if (!allWays.containsKey(relationMember.ref)) {
                                logger.warn("FATAL: Could not find ${relationMember.ref} in allWays... oh nooo")
                            } else {
                                building.innerline = allWays[relationMember.ref]!!
                            }
                        }
                    }
                }
            }
        }

        if (floor.check()) {
            building.floors.add(floor)
        }
    }

    private fun parseIndoorObject(node: Node, level: Int) {
        val indoorObject = IndoorObject(node.id)
        indoorObject.level = level
        indoorObject.additionalTags.putAll(node.additionalTags)

        if (indoorObject.check()) building.indoorObjects.add(indoorObject)
    }

    private fun parseLevelConnections(way: Way) {
        val levelConnection = LevelConnection(way.id)
        if (levelConnection.check()) building.connections.add(levelConnection)
    }

    private fun parseRoom(way: Way, level: Int) {
        val room = Room(way.id)
        room.level = level

        for ((key, value) in way.additionalTags.entries) {
            when (key) {
                "level" -> room.level = value.toIntOrNull()
                "height" -> room.height = value.toFloatOrNull()
                "name" -> room.name = value
                "ref" -> room.ref = value
                "buildingpart" -> {
                    when (value) {
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
                        else -> logger.info("Unrecognized building part/indoor tag in room-way ${way.id}: '${value}'")
                    }
                }
                else -> room.additionalTags[key] = value
            }
        }
        if (room.check()) {
            building.rooms.add(room)
        }
    }

}