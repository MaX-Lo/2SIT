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
    private var allRoomsOnLevel = mutableSetOf<Room>()

    fun convertBuildings(buildings: Iterable<Building>) {
        for (building in buildings) {
            convertBuilding(building)
        }
    }

    fun convertBuilding(building: Building) {
        val levelRoomMap = mutableMapOf<Int, MutableSet<Room>>().withDefault {
            mutableSetOf()
        }

        for (room in building.rooms) {
            val level = room.level ?: continue
            val tmp = levelRoomMap.getValue(level)
            tmp.add(room)
            levelRoomMap[level] = tmp
        }
        for ((level, rooms) in levelRoomMap) {
            convertLevel(rooms)
        }
    }

    private fun convertLevel(rooms: MutableSet<Room>) {
        allRoomsOnLevel = rooms
        insertSubsections()
        mergeNodes(allRoomsOnLevel)
        val x = allRoomsOnLevel.map { val y = it.nodes }

    }

    private fun getNodesForLevel(): MutableSet<Node> {
        return allRoomsOnLevel.map { it.nodes }.flatten().toMutableSet()
    }

    private fun insertSubsections(){
        val nodesOnLevel = getNodesForLevel()

        for (room in allRoomsOnLevel) {
            val newSubSections = mutableListOf<SubSection>()
            for (subsection in room.subsections) {
                val intersectionPairs = mutableListOf<Pair<Node, Double>>()
                for (node in nodesOnLevel) {
                    val intersection = subsection.getIntersection(node) ?: continue
                    if (intersection.first.inProximity(node) && intersection.second > 0 && intersection.second < 1) {
                        intersectionPairs.add(intersection)
                    }
                }
                intersectionPairs.sortBy { it.second }
                val intersectionNodes = intersectionPairs.map { it.first }

                // Idea: split the subsection with at the sorted interceptionPairs into smaller subsections.
                var currSubsection = subsection
                newSubSections.add(currSubsection)
                for (intersectionNode in intersectionNodes) {
                    val newSubSection = currSubsection.split(intersectionNode)
                    newSubSections.add(newSubSection)
                    currSubsection = newSubSection
                }
            }
            room.subsections.clear()
            room.subsections.addAll(newSubSections)
        }
    }

    /*
    approach:
        1. create a set of sets of nodes (nodesToMerge)
        2. iterate over all nodes on level
        3. for current node (curr):
            a. iterate again over all nodes on level (node1) -> if curr and node1 are close to each other, store node1 in a set (nodesNearby)
            b. find all sets in nodesToMerge that contain a node of nodesNearby and store them in (setsOfNearbyNodes)
            c. remove all sets of setsOfNearbyNodes from nodesToMerge
            d. add nodes of nodesNearby, that are not contained in a set and that set to setsOfNearbyNodes
            e. untie all sets of setsOfNearbyNodes and add the united set to nodesToMerge
             */
    private fun mergeNodes(roomsOnLevel : MutableSet<Room>) {
        val nodesToMerge = mutableSetOf<MutableSet<Node>>()
        val nodesOnLevel = getNodesForLevel()

        for (node in nodesOnLevel) {
            val nodesNearby = mutableSetOf<Node>()
            for (node1 in nodesOnLevel) {
                if (node === node1) continue
                if (node.inProximity(node1)) {
                    nodesNearby.add(node1)
                }
            }

            val setsOfNearbyNodes = mutableSetOf<MutableSet<Node>>()
            val nodesNotListedYet = mutableSetOf<Node>()
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
            val unitedSet = mutableSetOf<Node>()
            setsOfNearbyNodes.add(nodesNotListedYet)
            setsOfNearbyNodes.map { unitedSet.addAll(it) }
            nodesToMerge.add(unitedSet)
        }

        // create the new nodes and simultaneously create a map associating which nodes should be replaced by the
        // new node in the next step
        val oldToNewNodes = mutableMapOf<Node, Node>()
        for (nodes in nodesToMerge) {
            val merged = Node.getMerged(nodes)
            nodes.map { oldToNewNodes[it] = merged }
        }

        // replace old nodes if they got merged, if subsections contain the same nodes now, delete them
        for (room in allRoomsOnLevel) {
            val toDeleteSubsection = mutableListOf<SubSection>()
            for (subsection in room.subsections) {
                if (oldToNewNodes.containsKey(subsection.node1)) subsection.node1 = oldToNewNodes[subsection.node1]!!
                if (oldToNewNodes.containsKey(subsection.node2)) subsection.node2 = oldToNewNodes[subsection.node2]!!
                if (subsection.node1 === subsection.node2) {
                    toDeleteSubsection.add(subsection)
                }
            }
            room.subsections.removeAll(toDeleteSubsection)
        }
    }
}