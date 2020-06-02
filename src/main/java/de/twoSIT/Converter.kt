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
        val levelRoomMap = mutableMapOf<Int, MutableList<Room>>().withDefault {
            mutableListOf()
        }
        for (room in building.rooms) {
            val level = room.level ?: continue

            val tmp = levelRoomMap.getValue(level)
            tmp.add(room)
            levelRoomMap[level] = tmp
        }

        val alreadyFound = mutableSetOf<SubSection>()
        val levelSubsections = mutableListOf<SubSection>()
        val subsectionRoomMap = mutableMapOf<SubSection, Room>()
        val twinSubSections = mutableListOf<Pair<SubSection, SubSection>>()
        for ((level, rooms) in levelRoomMap) {
            alreadyFound.clear()
            levelSubsections.clear()
            // todo here smt more clever has to happen

            // fill the lookups
            for (room in rooms) {
                for (subsection in room.subsections){
                    subsectionRoomMap[subsection] = room
                    levelSubsections.add(subsection)
                }
            }

            // find the stuff
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
        for (twinSubSection in twinSubSections){
            val subSection1 = twinSubSection.first
            val subSection2 = twinSubSection.second
            val mergedSubSection = subSection1.getMerged(subSection2)

            val room1 = subsectionRoomMap[subSection1]!!
            room1.replaceSubsection(subSection1, mergedSubSection)

            val room2 = subsectionRoomMap[subSection2]!!
            room2.replaceSubsection(subSection2, mergedSubSection)
            val x = ""
        }

        return building
    }


}