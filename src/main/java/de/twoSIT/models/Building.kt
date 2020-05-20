package de.twoSIT.models

import de.twoSIT.util.getLogger


enum class LevelConnectionType {
    STAIRS, CONVEYOR, ELEVATOR
}

enum class IndoorTag {
    ROOM, AREA, WALL, CORRIDOR
}



class LevelConnection(id: String? = null) : AbstractElement(id) {
    val levels = mutableListOf<Int>()
    var indoorTag: IndoorTag? = null
    val doors = mutableListOf<RawNode>()
    var levelConnectionType: LevelConnectionType? = null

    fun check(): Boolean {
        if (id == null) {
            Building.logger.warn("Could not parse levelConnection-way: no id")
            return false
        }
        if (indoorTag == null) {
            Building.logger.info("Could not parse levelConnection-way $id: no indoor tag")
            return true
        }
        if (levelConnectionType == null) {
            Building.logger.warn("Could not parse levelConnection-way $id: no levelConnectionType")
            return true
        }
        if (doors.isEmpty()) {
            Building.logger.info("Could not parse levelConnection-way $id: no doors")
            return true
        }
        return true
    }
}


class Room(id: String? = null) : AbstractElement(id) {
    var level: Int? = null
    var indoorTag: IndoorTag? = null

    var outline: Way? = null
    val nodes = mutableListOf<RawNode>()
    var height: Float? = null
    var name: String? = null
    var ref: String? = null

    fun check(): Boolean {
        if (id == null) {
            Building.logger.warn("Could not parse floor-relation: no id")
            return false
        }
        if (level == null) {
            Building.logger.warn("Could not parse Room-way $id: no level")
            return false
        }
        if (indoorTag == null) {
            Building.logger.info("Could not parse Room-way $id: no indoor tag")
            return true
        }
        return true
    }
}


class Floor(id: String? = null) : AbstractElement(id) {
    var level: Int? = null
    var height: Float? = null
    var ref: String? = null
    var name: String? = null
    val usages = mutableMapOf<String, Way>()

    fun check(): Boolean {
        if (id == null) {
            Building.logger.warn("Could not parse floor-relation: no id")
            return false
        }
        if (level == null) {
            Building.logger.warn("Could not parse floor-relation $id: no level")
            return false
        }
        return true
    }
}

class IndoorObject(id: String? = null) : AbstractElement(id) {
    var level: Int? = null

    fun check(): Boolean {
        if (id == null) {
            Building.logger.warn("Could not parse indoorObject-node: no id")
            return false
        }
        if (level == null) {
            Building.logger.warn("Could not parse indoorObject-node $id: no level")
            return false
        }
        return true
    }
}

class Building(id: String? = null): AbstractElement(id) {
    companion object {
        @JvmStatic
        val logger = getLogger(Building::class.java)
    }

    var minLevel: Int? = null
    var maxLevel: Int? = null
    var floors = mutableListOf<Floor>()
    val connections = mutableListOf<LevelConnection>()
    var outline: Way? = null
    val rooms = mutableListOf<Room>()
    val indoorObjects = mutableListOf<IndoorObject>()

    var innerline: Way? = null
    var mainWay: Way? = null
    var height: Float? = null
    var name: String? = null
    val nonExistingLevels: MutableList<Int> = mutableListOf()

    fun check(): Boolean {
        if (id == null) {
            logger.warn("Could not parse building-relation: no id")
            return false
        }
        if (minLevel == null) {
            logger.warn("Could not parse building-relation $id: no min-level")
            return false
        }
        if (maxLevel == null) {
            logger.warn("Could not parse building-relation $id: no max-level")
            return false
        }
        if (outline == null) {
            logger.info("Could not parse building-relation $id: no outline")
            return true
        }
        if (floors.size == 0) {
            logger.info("Could not parse building-relation ${id}: no floors")
            return true
        }
        return true
    }

    fun twoSit(): String {
        return ""
    }
}