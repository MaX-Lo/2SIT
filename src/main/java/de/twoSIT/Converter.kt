package de.twoSIT

import de.twoSIT.models.Building
import de.twoSIT.models.Room
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
        val subsectionMap = mutableMapOf<Int, MutableList<Room>>().withDefault {
            mutableListOf()
        }
        for (room in building.rooms) {
            val level = room.level ?: continue

            val tmp = subsectionMap.getValue(level)
            for (nodeInd in 0 until room.nodes.size - 1) {
                tmp.add(room)
            }
            subsectionMap[level] = tmp
        }

        val twinSubSections = mutableListOf<Pair<SubSection, SubSection>>()
        val alreadyFound = mutableSetOf<SubSection>()
        val levelSubsections = mutableListOf<SubSection>()
        for ((level, rooms) in subsectionMap) {
            // todo here smt more clever has to happen

            levelSubsections.clear()
            for (room in rooms) {
                levelSubsections.addAll(room.subsections)
            }
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

        return building
    }


}