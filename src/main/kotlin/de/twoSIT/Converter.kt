package de.twoSIT

import de.twoSIT.models.*
import de.twoSIT.util.getLogger

private val logger = getLogger(Converter::class.java)

/*
approach:
    1. We iterate over all buildings
    2. Within a building iterate over the levels
    3. In current level fill a list of all needed nodes, rooms
    4. Add additional nodes where they are needed for merging
        a. iterate over rooms
        b. in current room iterate over all subsections
        c. for current subsection iterate over all nodes on floor
        d. check whether a projection on the current subsection exists, if so add it to current room nodes
    5. merge all nodes that are close to each other
        a.
 */
object Converter {
    /**
     * Converts a given [Iterable] of [Building]s. This includes the merge of the [Room]s as well as the
     * [LevelConnection]s of each building
     *
     * @param buildings a [Iterable] of [Building]s to convert
     */
    fun convertBuildings(buildings: Iterable<Building>) {
        for (building in buildings) {
            logger.debug("Start to convert building '${building.id}'")
            convertBuilding(building)
        }
    }

    /**
     * Converts a given [Building]. This includes the merge of the [Room]s as well as the [LevelConnection]s
     *
     * @param building the [Building] to convert
     */
    fun convertBuilding(building: Building) {
        val levelRoomMap = mutableMapOf<Int, MutableSet<Room>>().withDefault {
            mutableSetOf()
        }

        for (room in building.rooms) {
            val level = room.level
            val tmp = levelRoomMap.getValue(level)
            tmp.add(room)
            levelRoomMap[level] = tmp
        }

        logger.debug("Found a total of ${levelRoomMap.size} levels and ${levelRoomMap.map { it.value.size }.size} rooms")
        for ((level, rooms) in levelRoomMap) {
            logger.debug("Start to convert level '${level}' of building ${building.id}")
            convertLevel(rooms)
        }
    }

    private fun convertLevel(roomsOnLevel: MutableSet<Room>) {
        val nodeCount = getNodesForLevel(roomsOnLevel)
        insertNodes(roomsOnLevel)
        mergeNodes(roomsOnLevel)
        logger.debug("Merged ${getNodesForLevel(roomsOnLevel) - nodeCount} nodes")
    }

    /**
     * This function extracts all [Node]s of all [Room]s.
     *
     * @param roomsOnLevel a [MutableSet] of all [Room]s on the level
     * @return a [MutableSet] of all [Node]s on the level
     *
     */
    private fun getNodesForLevel(roomsOnLevel: MutableSet<Room>): MutableSet<IndoorObject> {
        return roomsOnLevel.map { it.nodes }.flatten().toMutableSet()
    }

    /**
     * This function splits a [SubSection] into two following [SubSection]s if a [Node] of any [Room] of the level has a
     * projection-[Node] on the [SubSection], which is close to the [Node] itself.
     *
     * @param roomsOnLevel a [MutableSet] of all [Room]s on the level
     */
    private fun insertNodes(roomsOnLevel: MutableSet<Room>) {
        val nodesOnLevel = getNodesForLevel(roomsOnLevel)

        for (room in roomsOnLevel) {
            val newNodes = mutableListOf<IndoorObject>()
            newNodes.add(room.nodes[0])
            for (currentNodeInd in 0 until room.nodes.size - 1) {
                val wallSection = WallSection(room.nodes[currentNodeInd], room.nodes[currentNodeInd + 1])

                val intersectionPairs = mutableListOf<Pair<IndoorObject, Double>>()
                for (node in nodesOnLevel) {
                    val intersection = wallSection.getIntersection(node) ?: continue
                    if (intersection.first.inProximity(node) && intersection.second > 0 && intersection.second < 1) {
                        intersectionPairs.add(intersection)
                    }
                }
                intersectionPairs.sortBy { it.second }
                val intersectionNodes = intersectionPairs.map { it.first }
                newNodes.addAll(intersectionNodes)
                newNodes.add(wallSection.end)
            }
            room.nodes.clear()
            room.nodes.addAll(newNodes)
        }
    }

    private fun mergeNodes(roomsOnLevel: MutableSet<Room>) {
        val nodesToMerge = nodesToMerge(roomsOnLevel)
        logger.debug("Merging ${nodesToMerge.map { it.size }.sum()} into ${nodesToMerge.size} nodes")
        replaceNodes(nodesToMerge, roomsOnLevel)
    }

    /**
     *
     * This function returns a set of sets, in which all nodes are contained, that are close to each other and therefore
     * should be merged. These sets are distinct, so a node in one set will not appear in another.
     * The proximity of the nodes is calculated transitive, meaning if
     *      1. node A is close to node B
     *      2. node B is close to node C
     *          --> A is close to C
     *
     * @param roomsOnLevel a [MutableSet] of all [Room]s on the level
     * @return a [MutableSet] of distinct sets which contain [Node]s that are in proximity of each other
     *
     * approach:
     * 1. create a set of sets of nodes (nodesToMerge)
     * 2. iterate over all nodes on level
     * 3. for current node (curr):
     * a. iterate again over all nodes on level (node1) -> if curr and node1 are close to each other, store node1 in a set (nodesNearby)
     * b. find all sets in nodesToMerge that contain a node of nodesNearby and store them in (setsOfNearbyNodes)
     * c. remove all sets of setsOfNearbyNodes from nodesToMerge
     * d. add nodes of nodesNearby, that are not contained in a set and that set to setsOfNearbyNodes
     * e. untie all sets of setsOfNearbyNodes and add the united set to nodesToMerge
     *
     */
    private fun nodesToMerge(roomsOnLevel: MutableSet<Room>): MutableSet<MutableSet<IndoorObject>> {
        val nodesToMerge = mutableSetOf<MutableSet<IndoorObject>>()
        val nodesOnLevel = getNodesForLevel(roomsOnLevel)

        for (node in nodesOnLevel) {
            val nodesNearby = mutableSetOf<IndoorObject>()
            for (node1 in nodesOnLevel) {
                if (node === node1) { continue }
                if (node.inProximity(node1)) {
                    nodesNearby.add(node1)
                }
            }

            if (nodesNearby.isEmpty()) { continue }

            // if a nearby node is already processed and has therefore already a set in nodesToMerge the sets
            // need to be merged
            val setsOfNearbyNodes = mutableSetOf<MutableSet<IndoorObject>>()
            val nodesNotListedYet = mutableSetOf<IndoorObject>()
            for (nodeNearby in nodesNearby) {
                var setFound = false
                for (s in nodesToMerge) {
                    if (nodeNearby in s) {
                        setsOfNearbyNodes.add(s)
                        setFound = true
                        break
                    }
                }
                if (!setFound) {
                    nodesNotListedYet.add(nodeNearby)
                }
            }
            for (s in setsOfNearbyNodes) {
                nodesToMerge.remove(s)
            }
            val unitedSet = mutableSetOf<IndoorObject>()
            setsOfNearbyNodes.add(nodesNotListedYet)
            setsOfNearbyNodes.map { unitedSet.addAll(it) }
            unitedSet.add(node)
            nodesToMerge.add(unitedSet)
        }
        return nodesToMerge
    }

    /**
     * This function merges the nodes in [nodesToMerge] and updates the references of each subsection.
     * It furthermore removes SubSections of len 0 and updates the references in the according rooms.
     *
     * @param nodesToMerge a [MutableSet] of distinct sets that contain [Node]s that should be merged
     * @param roomsOnLevel a [MutableSet] of all [Room]s on the level
     *
     */
    private fun replaceNodes(nodesToMerge: MutableSet<MutableSet<IndoorObject>>, roomsOnLevel: MutableSet<Room>) {
        val oldToNewNodes = mutableMapOf<IndoorObject, IndoorObject>()
        for (nodes in nodesToMerge) {
            val merged = IndoorObject.getMerged(nodes)
            nodes.map { oldToNewNodes[it] = merged }
        }

        for (room in roomsOnLevel) {

            val toDeleteNodes = mutableListOf<IndoorObject>()
            for (nodeIdx in 0 until room.nodes.size) {
                val node = room.nodes[nodeIdx]
                if (oldToNewNodes.containsKey(node)) {
                    room.nodes[nodeIdx] = oldToNewNodes[node]!!
                }
            }

            // remove duplicates, except the last node which should be a duplicate of the first node
            var nodeIdx = 0
            while ( nodeIdx < room.nodes.size - 1) {
                val node = room.nodes[nodeIdx]
                val nextNode = room.nodes[nodeIdx + 1]
                if (node === nextNode) { room.nodes.removeAt(nodeIdx + 1) }
                nodeIdx += 1
            }
        }
    }

}