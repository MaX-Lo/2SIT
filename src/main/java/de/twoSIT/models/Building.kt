package de.twoSIT.models

import de.twoSIT.util.getLogger


enum class LevelConnectionType {
    STAIRS, CONVEYOR, ELEVATOR
}

enum class IndoorTag {
    ROOM, AREA, WALL, CORRIDOR
}

class LevelConnection {
    var id: String? = null
    val levels = mutableListOf<Int>()
    var indoorTag: IndoorTag? = null
    val doors = mutableListOf<Node>()

    fun check(): Boolean {
        if (id == null) {
            Building.logger.warn("Could not parse levelConnection-way: no id")
            return false
        }
        if (indoorTag == null) {
            Building.logger.warn("Could not parse levelConnection-way $id: no indoor tag")
            return false
        }
        if (indoorTag == null) {
            Building.logger.warn("Could not parse levelConnection-way $id: wrong indoor tag '$indoorTag', " +
                    "has to be 'CORRIDOR' or 'AREA'")
            return false
        }
        if (doors.isEmpty()) {
            Building.logger.warn("Could not parse levelConnection-way $id: no doors")
            return false
        }
        return true
    }
}

class Room {
    var id: String? = null
    var level: Int? = null
    var indoorTag: IndoorTag? = null

    var outline: Way? = null
    val nodes = mutableListOf<Node>()
    var height: Float? = null
    var name: String? = null
    var ref: String? = null
    var additionalTags = mutableMapOf<String, String>()

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
            Building.logger.warn("Could not parse Room-way $id: no indoor tag")
            return false
        }
        return true
    }
}


class Floor {
    var id: String? = null
    var level: Int? = null
    var height: Float? = null
    var ref: String? = null
    var name: String? = null
    val additionalTags = mutableMapOf<String, String>()

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

class IndoorObject{
    var id: String? = null
    var level: Int? = null
    val additionalTags = mutableMapOf<String, String>()

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

class Building {
    companion object {
        @JvmStatic
        val logger = getLogger(Building::class.java)
    }

    var id: String? = null
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
    var additionalTags = mutableMapOf<String, String>()

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
            logger.warn("Could not parse building-relation $id: no outline")
            return false
        }
        if (floors.size == 0) {
            logger.warn("Could not parse building-relation ${id}: no floors")
            return false
        }
        return true
    }

    fun twoSit(): String {
        return ""
    }
}