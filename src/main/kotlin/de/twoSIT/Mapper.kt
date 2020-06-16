package de.twoSIT

import de.twoSIT.models.*
import de.twoSIT.util.getLogger


private val logger = getLogger(Mapper::class.java)


object Mapper {
    private val allWays = mutableMapOf<String, Way>()
    private val allRelations = mutableMapOf<String, Relation>()
    private val allNodes = mutableMapOf<String, Node>()

    private val buildings = mutableListOf<Building>()
    private val unparsedBuildings = mutableListOf<Relation>()

    fun parseArea(rawArea: RawArea): List<Building> {
        // clear and refill maps
        allNodes.clear()
        allWays.clear()
        allRelations.clear()
        rawArea.nodes.map { allNodes[it.id] = Node.fromRaw(it) }
        rawArea.ways.map { allWays[it.id] = Way.fromRaw(it) }
        rawArea.relations.map { allRelations[it.id] = Relation.fromRaw(it) }

        // fetch missing stuff
        fillMissing()
        logger.debug("Parsing area with a total of ${allNodes.size} nodes; ${allWays.size} ways and " +
                "${allRelations.size} relations")

        // parse it
        parseBuildingRel()

        logger.debug("Parsed area into ${buildings.size} buildings with a total of " +
                "${buildings.map { it.rooms }.size} ways/rooms; ${buildings.map { it.floors }.size} relations/floors and " +
                "${buildings.map { it.indoorObjects.size + it.rooms.map { it.toOsm().nodeReferences.size }.sum() }.sum()} nodes")
        logger.warn("Could not parse ${unparsedBuildings.size} buildings")
        return buildings
    }

    /**
     * Checks for unfetched but referenced and therefore necessary objects and fetches them from server.
     */
    private fun fillMissing() {
        val missingStuff = getMissingOSMElements()
        val requester = Requester.getInstance("https://api.openstreetmap.org/api/0.6/")

        val missingNodes = missingStuff.first
        val missingWays = missingStuff.second
        val missingRelations = missingStuff.third

        fun fetchNodes(missingNodes: List<String>) {
            if (missingNodes.isNotEmpty()) {
                logger.info("Fetching ${missingNodes.size} nodes")
                val nodeList = RawNode.multipleFromString(requester.requestNodes(missingNodes))
                for (rawNode in nodeList) allNodes[rawNode.id] = Node.fromRaw(rawNode)
            }
        }
        if (missingWays.isNotEmpty()) {
            logger.info("Fetching ${missingWays.size} ways")
            val wayList = RawWay.multipleFromString(requester.requestWays(missingWays))
            val missingNodes = mutableListOf<String>()
            for (rawWay in wayList) {
                for (nodeRef in rawWay.nds) {
                    if (!allNodes.containsKey(nodeRef.ref)) missingNodes.add(nodeRef.ref)
                }
            }
            fetchNodes(missingNodes)
            for (rawWay in wayList) allWays[rawWay.id] = Way.fromRaw(rawWay)
        }
        fetchNodes(missingNodes)
        if (missingRelations.isNotEmpty()) {
            logger.info("Fetching ${missingRelations.size} relations")
            val relationList = RawRelation.multipleFromString(requester.requestNodes(missingRelations))
            for (rawRelation in relationList) allRelations[rawRelation.id] = Relation.fromRaw(rawRelation)
        }
    }

    /**
     * checks for all buildings if there are any unfetched but necessary objects.
     *
     * @return a [Triple] consisting of [MutableList] for unfetched [Node]s, [Way]s and [Relation]s
     */
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

    /**
     * checks for a given building if there are any unfetched but necessary objects.
     *
     * @param buildingRelation a [Relation] that represents the building to check
     * @return a [Triple] consisting of [MutableList] for unfetched [Node]s, [Way]s and [Relation]s
     */
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

    /**
     * checks for a given floor if there are any unfetched but necessary objects.
     *
     * @param floorRelation a [Relation] that represents the floor to check
     * @return a [Triple] consisting of [MutableList] for unfetched [Node]s, [Way]s and [Relation]s
     */
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

    /**
     * Extracts all [Relation]s that represent a building in the current [RawArea]
     *
     * @return a [MutableList] of [Relation]s that represent buildings
     */
    private fun getAllBuildingRel(): MutableList<Relation> {
        val buildingRelations = mutableListOf<Relation>()
        for (relation in allRelations.values) {
            if (relation.additionalTags.containsKey("type") && relation.additionalTags["type"] == "building") {
                buildingRelations.add(relation)
            }
        }
        return buildingRelations
    }

    /**
     * Parses all [Relation]s that represent buildings into a [Building] POJO
     */
    private fun parseBuildingRel() {
        val buildingRelations = getAllBuildingRel()
        for (relation in buildingRelations) {
            val building = Building.fromOsm(relation, allNodes, allWays)

            if (building != null) {
                for (member in relation.relationMembers) {
                    parseFloor(allRelations[member.ref]!!, building)
                }

                buildings.add(building)
            } else unparsedBuildings.add(relation)
        }

    }

    /**
     * Parses a [Relation] that represents a floor into a [Floor] POJO and adds it to the [building]
     *
     * @param relation the [Relation] that represents the floor
     * @param building the [Building] to add the [Floor] to
     */
    private fun parseFloor(relation: Relation, building: Building) {
        val floor = Floor.fromOsm(relation, allNodes, allWays) ?: return

        for (member in relation.nodeMembers) {
            parseIndoorObject(allNodes[member.ref]!!, floor.level, building)
        }

        for (member in relation.wayMembers) {
            val way = allWays[member.ref]!!
            if ("level:usage" in way.additionalTags.keys) {
                continue
            } else {
                parseRoom(way, floor.level, building)
            }

        }

        for (member in relation.relationMembers) {
            val rel = allRelations[member.ref]!!
            for (relationMember in rel.wayMembers) {
                when (relationMember.role) {
                    "outer" -> building.outline = allWays[relationMember.ref]!!
                    "inner" -> building.innerline = allWays[relationMember.ref]!!
                }
            }
        }
        building.floors.add(floor)
    }

    /**
     * Parses a [Node] that represents a basic indoor object into a [IndoorObject] POJO and adds it to the [building]
     *
     * @param node the [Node] that represents the indoor object
     * @param level the level the indoor object is on
     * @param building the [Building] to add the [IndoorObject] to
     */
    private fun parseIndoorObject(node: Node, level: Int, building: Building) {
        val indoorObject = IndoorObject.fromOsm(node, mutableListOf(level)) ?: return
        building.indoorObjects.add(indoorObject)
    }

    /**
     * Parses a [Way] that represents a level connection into a [LevelConnection] POJO and adds it to the [building]
     *
     * @param way the [Way] that represents the level connection
     * @param building the [Building] to add the [LevelConnection] to
     */
    private fun parseLevelConnections(way: Way, building: Building) {
        val levelConnection = LevelConnection.fromOsm(way, allNodes) ?: return
        building.connections.add(levelConnection)
    }

    /**
     * Parses a [Way] that represents a room into a [Room] POJO and adds it to the [building]
     *
     * @param way the [Way] that represents the room
     * @param level the level the room is in
     * @param building the [Building] to add the [Floor] to
     */
    private fun parseRoom(way: Way, level: Int, building: Building) {
        for ((key, value) in way.additionalTags.entries) {
            if (key == "buildingpart" && value == "verticalpassage"){
                parseLevelConnections(way, building)
                return
            }
        }

        val room = Room.fromOsm(way, level, allNodes)?: return
        building.rooms.add(room)
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
        return way.nodeReferences.map { allNodes[it.ref]!! }
    }

    fun exportBuildings() {
        val osmChange = OsmChange()
        val modify = Modify()
        val create = Create()
        val delete = Delete()

        // Todo implement diff calculation
        for (building in buildings) {
            logger.info("Building ${building.id} backparsing result")

            val buildingAsOsmRel = building.toOsm()
            //val resultingOsmElements = getContainedElements(buildingAsOsmRel)
            val resultingOsmElements = building.getContainedElements()
            val nodes = resultingOsmElements.first
            val ways = resultingOsmElements.second
            val relations = resultingOsmElements.third

            // populate create elements
            val originalNodeIds = building.originalNodes.map { it.id }.toSet()
            val originalWayIds = building.originalWays.map { it.id }.toSet()
            val originalRelationIds = building.originalRelations.map { it.id }.toSet()

            for (node in nodes) {
                if (node.id !in originalNodeIds) create.nodes.add(node.toRaw())
            }
            for (way in ways) {
                if (way.id !in originalWayIds) create.ways.add(way.toRaw())
            }
            for (relation in relations) {
                if (relation.id !in originalRelationIds) create.relations.add(relation.toRaw())
            }
            osmChange.create = create
            logger.info("\t${create.nodes.size} nodes, ${create.ways.size} ways, ${create.relations.size} relations that got created")

            // populate delete elements
            val newNodeIds = nodes.map { it.id }.toSet()
            val newWayIds = nodes.map { it.id }.toSet()
            val newRelationIds = nodes.map { it.id }.toSet()

            for (node in building.originalNodes) {
                if (node.id !in newNodeIds) delete.nodes.add(node.toRaw())
            }
            for (way in building.originalWays) {
                if (way.id !in newWayIds) delete.ways.add(way.toRaw())
            }
            for (relation in building.originalRelations) {
                if (relation.id !in newRelationIds) delete.relations.add(relation.toRaw())
            }
            osmChange.delete = delete
            logger.info("\t${delete.nodes.size} nodes, ${delete.ways.size} ways, ${delete.relations.size} relations that got deleted")

            // populate modify elements
            // currently we assume everything inside a building that's not created or deleted was modified
            // ToDo for commiting a new changeset metadata as changeset, version, ... needs to be updated
            for (node in nodes) {
                if (node.id in originalNodeIds) modify.nodes.add(node.toRaw())
            }
            for (way in ways) {
                if (way.id in originalWayIds) modify.ways.add(way.toRaw())
            }
            for (relation in relations) {
                if (relation.id in originalRelationIds) modify.relations.add(relation.toRaw())
            }
            osmChange.modify = modify
            logger.info("\t${modify.nodes.size} nodes, ${modify.ways.size} ways, ${modify.relations.size} relations that got modified")
        }

        osmChange.createExportFile()
    }
}