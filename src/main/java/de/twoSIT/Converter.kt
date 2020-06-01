package de.twoSIT

import de.twoSIT.models.Building
import de.twoSIT.models.SubSection

class Converter {
    fun convertBuildings(buildings: Iterable<Building>): Iterable<Building> {
        val convertedBuildings = mutableListOf<Building>()
        for (building in buildings) {
            convertedBuildings.add(convertBuilding(building))
        }
        return convertedBuildings
    }

    fun convertBuilding(building: Building): Building {
        val subsectionMap = mutableMapOf<Int, MutableList<SubSection>>().withDefault {
            mutableListOf()
        }
        for (room in building.rooms) {
            val level = room.level ?: continue

            val tmp = subsectionMap.getValue(level)
            for (nodeInd in 0 until room.nodes.size) {
                tmp.add(SubSection(room.nodes[nodeInd], room.nodes[nodeInd+1]))
            }
            subsectionMap[level] = tmp
        }

        val twinSubSections = mutableListOf<Pair<SubSection, SubSection>>()
        for ((level, subsections) in subsectionMap){
            for (subsection1 in subsections){
                for (subsection2 in subsections){
                    if (subsection1 == subsection2){
                        twinSubSections.add(Pair(subsection1, subsection2))
                        break
                    }
                }
            }
        }

        return building
    }


}