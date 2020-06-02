package de.twoSIT

import de.twoSIT.models.*

class Converter {
    fun convertBuildings(buildings: Iterable<Building>): Iterable<Building> {
        val convertedBuildings = mutableListOf<Building>()
        for (building in buildings) {
            convertedBuildings.add(convertBuilding(building))
        }
        return convertedBuildings
    }

    fun convertBuilding(building: Building): Building {
        val levelRoomMap = mutableMapOf<Int, MutableList<Room>>().withDefault {
            mutableListOf()
        }
        for (room in building.rooms) {
            val level = room.level ?: continue

            val tmp = levelRoomMap.getValue(level)
            tmp.add(room)
            levelRoomMap[level] = tmp
        }

        mergeRealTwins(levelRoomMap)
        // todo add Nodes into fake twins
        mergeNodes(levelRoomMap)

        return building
    }


    private fun mergeRealTwins(levelRoomMap: MutableMap<Int, MutableList<Room>>) {
        val subsectionRoomMap = mutableMapOf<SubSection, Room>()
        val twinSubSections = mutableListOf<Pair<SubSection, SubSection>>()
        for ((level, rooms) in levelRoomMap) {
            val levelSubsections = mutableListOf<SubSection>()

            // fill the lookups
            for (room in rooms) {
                for (subsection in room.subsections) {
                    subsectionRoomMap[subsection] = room
                    levelSubsections.add(subsection)
                }
            }

            // find the stuff
            val alreadyFound = mutableSetOf<SubSection>()
            for (subsection1 in levelSubsections) {
                for (subsection2 in levelSubsections) {
                    if (subsection1 === subsection2) continue
                    if (subsection1 in alreadyFound) continue
                    if (subsection1 == subsection2) {
                        alreadyFound.add(subsection1)
                        alreadyFound.add(subsection2)
                        twinSubSections.add(Pair(subsection1, subsection2))
                        break
                    }
                }
            }
        }

        // merge the stuff - real twins
        for (twinSubSection in twinSubSections) {
            val subSection1 = twinSubSection.first
            val subSection2 = twinSubSection.second
            val mergedSubSection = subSection1.getMerged(subSection2)

            val room1 = subsectionRoomMap[subSection1]!!
            room1.replaceSubsection(subSection1, mergedSubSection)

            val room2 = subsectionRoomMap[subSection2]!!
            room2.replaceSubsection(subSection2, mergedSubSection)
        }
    }

    private fun mergeNodes(levelRoomMap: MutableMap<Int, MutableList<Room>>) {
        val proximitNodes = mutableListOf<MutableSet<Node>>()
        val nodeSubsectionMap = mutableMapOf<Node, MutableSet<SubSection>>().withDefault {
            mutableSetOf()
        }
        for ((level, rooms) in levelRoomMap) {
            // fill the lookup
            val levelNodes = mutableListOf<Node>()
            for (room in rooms) {
                for (subsection in room.subsections) {
                    levelNodes.add(subsection.node1)
                    levelNodes.add(subsection.node2)

                    var tmp = nodeSubsectionMap.getValue(subsection.node1)
                    tmp.add(subsection)
                    nodeSubsectionMap[subsection.node1] = tmp

                    tmp = nodeSubsectionMap.getValue(subsection.node2)
                    tmp.add(subsection)
                    nodeSubsectionMap[subsection.node2] = tmp
                }
            }

            // find the stuff
            val alreadyVisit = mutableSetOf<Node>()
            for (node in levelNodes) {
                val proxies = mutableSetOf<Node>()
                for (node1 in levelNodes) {
                    if (node1 in alreadyVisit) continue
                    if (node.inProximity(node1)) {
                        proxies.add(node1)
                    }
                }
                if (proxies.isNotEmpty()){
                    proxies.add(node)
                    proximitNodes.add(proxies)
                }
            }

            // merge the stuff
            for (nodes in proximitNodes){
                val mergedNode = Node.getMerged(nodes)
                for (node in nodes){
                    for (subsection in nodeSubsectionMap[node]!!){
                        subsection.replaceNode(node, mergedNode)
                    }
                }
            }
        }

    }
}
