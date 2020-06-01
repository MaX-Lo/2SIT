package de.twoSIT

import de.twoSIT.models.*
import de.twoSIT.util.getLogger


class Mapper {
    companion object {
        @JvmStatic
        private val logger = getLogger(Mapper::class.java)
    }

    private val allWays = mutableMapOf<String, Way>()
    private val allRelations = mutableMapOf<String, Relation>()
    private val allNodes = mutableMapOf<String, Node>()

    private val buildings = mutableListOf<Building>()
    private val unparsedBuildings = mutableListOf<Building>()

    fun parseArea(rawArea: RawArea): List<Building> {
        // clear and refill maps
        allNodes.clear()
        allWays.clear()
        allRelations.clear()
        for (rawNode in rawArea.nodes) allNodes[rawNode.id] = Node.fromRaw(rawNode)
        for (way in rawArea.ways) allWays[way.id] = Way.fromRaw(way, allNodes)
        for (rawRelation in rawArea.relations) allRelations[rawRelation.id] = Relation.fromRaw(rawRelation)

        // fetch missing stuff
        fillMissing()

        // parse it
        parseBuildingRel()

        return buildings
    }

    private fun fillMissing() {
        val missingStuff = getMissingOSMElements()
        val requester = Requester.getInstance("https://api.openstreetmap.org/api/0.6/")

        val missingNodes = missingStuff.first
        val missingWays = missingStuff.second
        val missingRelations = missingStuff.third

        fun fetchNodes(missingNodes: List<String>){
            if (missingNodes.isNotEmpty()){
                logger.info("Fetching ${missingNodes.size} nodes")
                val nodeList = RawNode.multipleFromString(requester.requestNodes(missingNodes))
                for (rawNode in nodeList) allNodes[rawNode.id] = Node.fromRaw(rawNode)
            }
        }
        if (missingWays.isNotEmpty()){
            logger.info("Fetching ${missingWays.size} ways")
            val wayList = RawWay.multipleFromString(requester.requestWays(missingWays))
            val missingNodes = mutableListOf<String>()
            for (rawWay in wayList){
                for (nodeRef in rawWay.nds){
                    if (!allNodes.containsKey(nodeRef.ref)) missingNodes.add(nodeRef.ref)
                }
            }
            fetchNodes(missingNodes)
            for (rawWay in wayList) allWays[rawWay.id] = Way.fromRaw(rawWay, allNodes)
        }
        fetchNodes(missingNodes)
        if (missingRelations.isNotEmpty()){
            logger.info("Fetching ${missingRelations.size} relations")
            val relationList = RawRelation.multipleFromString(requester.requestNodes(missingRelations))
            for (rawRelation in relationList) allRelations[rawRelation.id] = Relation.fromRaw(rawRelation)
        }
    }

    private fun getMissingOSMElements(): Triple<MutableList<String>, MutableList<String>, MutableList<String>> {
        val missingNodes = mutableListOf<String>()
        val missingWays = mutableListOf<String>()
        val missingRelations = mutableListOf<String>()

        val buildingRelations = getAllBuildingRel()
        for (buildingRelation in buildingRelations) {
            val missingStuff = getMissingStuffBuilding(buildingRelation)
            missingNodes.addAll(missingStuff.first)
            missingWays.addAll(missingStuff.second)
            missingRelations.addAll(missingStuff.third)
        }

        return Triple(missingNodes, missingWays, missingRelations)
    }

    private fun getMissingStuffBuilding(buildingRelation: Relation): Triple<MutableList<String>, MutableList<String>, MutableList<String>> {
        val missingNodes = mutableListOf<String>()
        val missingWays = mutableListOf<String>()
        val missingRelations = mutableListOf<String>()

        for (member in buildingRelation.relationMembers) {
            if (!allRelations.containsKey(member.ref)) {
                missingRelations.add(member.ref)
            } else {
                val floorMissingStuff = getMissingStuffFloor(allRelations[member.ref]!!)
                missingNodes.addAll(floorMissingStuff.first)
                missingWays.addAll(floorMissingStuff.second)
                missingRelations.addAll(floorMissingStuff.third)
            }
        }
        for (member in buildingRelation.nodeMembers) {
            if (!allNodes.containsKey(member.ref)) missingNodes.add(member.ref)
        }
        for (member in buildingRelation.wayMembers) {
            if (!allNodes.containsKey(member.ref)) missingWays.add(member.ref)
        }

        return Triple(missingNodes, missingWays, missingRelations)
    }

    private fun getMissingStuffFloor(floorRelation: Relation): Triple<MutableList<String>, MutableList<String>, MutableList<String>> {
        val missingNodes = mutableListOf<String>()
        val missingWays = mutableListOf<String>()
        val missingRelations = mutableListOf<String>()

        for (member in floorRelation.nodeMembers) {
            if (!allNodes.containsKey(member.ref)) missingNodes.add(member.ref)
        }
        for (member in floorRelation.wayMembers) {
            if (!allWays.containsKey(member.ref)) missingWays.add(member.ref)
        }
        for (member in floorRelation.relationMembers) {
            if (!allRelations.containsKey(member.ref)) missingWays.add(member.ref)
        }

        return Triple(missingNodes, missingWays, missingRelations)
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
            val building = Building(relation.id)
            val contained = getContainedElements(relation)
            building.originalNodes = contained.first.map { it.deepCopy() }
            building.originalWays = contained.second.map { it.deepCopy() }
            building.originalRelations = contained.third.map { it.deepCopy() }

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
                    // todo this is a door
                    val door = allNodes[member.ref]!!
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
                parseFloor(allRelations[member.ref]!!, building)
            }

            if (building.check()) {
                buildings.add(building)
            } else {
                unparsedBuildings.add(building)
            }
        }

    }

    private fun parseFloor(relation: Relation, building: Building) {
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
            if (floor.level == null) {
                logger.warn("Cannot parse room ${member.ref}: floor has no level")
            } else {
                parseIndoorObject(allNodes[member.ref]!!, floor.level!!, building)
            }
        }

        for (member in relation.wayMembers) {
            if (floor.level == null) {
                logger.warn("Cannot parse room ${member.ref}: floor has no level")
            } else {
                val way = allWays[member.ref]!!
                if ("level:usage" in way.additionalTags.keys) {
                    floor.usages[way.additionalTags["level:usage"]!!] = way
                } else {
                    parseRoom(way, floor.level!!, building)
                }
            }
        }

        for (member in relation.relationMembers) {
            val rel = allRelations[member.ref]!!
            for (relationMember in rel.wayMembers) {
                when (relationMember.role) {
                    "outer" -> building.outline = allWays[relationMember.ref]!!
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

        if (floor.check()) {
            building.floors.add(floor)
        }
    }

    private fun parseIndoorObject(node: Node, level: Int, building: Building) {
        val indoorObject = IndoorObject(node.id)
        indoorObject.level = level
        indoorObject.additionalTags.putAll(node.additionalTags)

        if (indoorObject.check()) building.indoorObjects.add(indoorObject)
    }

    private fun parseLevelConnections(way: Way, building: Building) {
        val levelConnection = LevelConnection(way.id)
        if (levelConnection.check()) building.connections.add(levelConnection)
    }

    private fun parseRoom(way: Way, level: Int, building: Building) {
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
                            parseLevelConnections(way, building)
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

    fun getContainedElements(relation: Relation): Triple<List<Node>, List<Way>, List<Relation>> {
        /** get a list of all (recursively) referenced OSM Elements (Way, Node, Relation), including this */
        val containedNodes = mutableListOf<Node>()
        val containedWays = mutableListOf<Way>()
        val containedRelations = mutableListOf<Relation>()

        for (node in relation.nodeMembers) {
            if (allNodes.containsKey(node.ref)) containedNodes.add(allNodes[node.ref]!!)
        }

        for (way in relation.wayMembers) {
            if (allWays.containsKey(way.ref)) {
                containedNodes.addAll(getContainedElements(allWays[way.ref]!!))
                containedWays.add(allWays[way.ref]!!)
            }
        }

        for (innerRelation in relation.relationMembers) {
            if (allRelations.containsKey(innerRelation.ref)) {
                val contained = getContainedElements(allRelations[innerRelation.ref]!!)
                containedNodes.addAll(contained.first)
                containedWays.addAll(contained.second)
                containedRelations.addAll(contained.third)
                containedRelations.add(allRelations[innerRelation.ref]!!)
            }
        }

        return Triple(containedNodes, containedWays, containedRelations)
    }

    fun getContainedElements(way: Way): List<Node> {
        return way.nodes.values.toList()
    }

    fun exportBuildings() {
        // Todo implement for building currently just a test with ways
        /**
         * Requirements:
         * - figure out which OSM Elements have changed, got deleted or were newly created
         * - untouched elements should not appear in the exported diff
         *
         * Approach 1)
         * - when initially creating a building object all contained OSM Elements (Nodes, Ways, Relations)
         *   are stored as a copy
         * - after parsing the building object into SIT
         *   - convert the buildings indoorOSM objects back to the default OSM Elements
         *   - create a diff between original OSM Element lists and resulting converted OSM Elements list
         *   - for each Element:
         *      - difference in Attributes -> create a modify entry with element and all its attributes
         *      - Element not found in original lists -> create a create entry with element and negative id
         *      - Element from copy missing in converted list -> create a deleted entry for element
         *
         * Approach 2)
         * - each Element Type has a flag having one of three possible values:
         *   1 - modify: set if an attribute or child element got modified
         *   2 - create: set if the element got newly created
         *   3 - delete: set if the element got removed since it isn't needed anymore
        **/
        val osmChange = OsmChange()
        val modify = Modify()
        val create = Create()
        val delete = Delete()

        for (building in buildings) {

        }

        for (way in allWays.entries) {
            osmChange.modify.ways.add(way.value.toRawWay())
        }
        val xmlStr = osmChange.toXMLString()
    }

}