package de.twoSIT.models


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
            println("Could not parse levelConnection-way: no id")
            return false
        }
        if (indoorTag == null) {
            println("Could not parse levelConnection-way $id: no indoor tag")
            return false
        }
        if (indoorTag == null) {
            println("Could not parse levelConnection-way $id: wrong indoor tag '$indoorTag', " +
                    "has to be 'CORRIDOR' or 'AREA'")
            return false
        }
        if (doors.isEmpty()) {
            println("Could not parse levelConnection-way $id: no doors")
            return false
        }
        return true
    }
}

class Room {
    var id: String? = null
    var level: Int? = null
    var indoorTag: IndoorTag? = null

    val nodes = mutableListOf<Node>()
    var height: Float? = null
    var name: String? = null
    var ref: String? = null
    var additionalTags = mutableMapOf<String, String>()

    fun check(): Boolean {
        if (id == null) {
            println("Could not parse floor-relation: no id")
            return false
        }
        if (level == null) {
            println("Could not parse Room-way $id: no level")
            return false
        }
        if (indoorTag == null) {
            println("Could not parse Room-way $id: no indoor tag")
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
            println("Could not parse floor-relation: no id")
            return false
        }
        if (level == null) {
            println("Could not parse floor-relation $id: no level")
            return false
        }
        return true
    }
}


class Building {
    var id: String? = null
    var minLevel: Int? = null
    var maxLevel: Int? = null
    var floors = mutableListOf<Floor>()
    val connections = mutableListOf<LevelConnection>()
    var outline: Way? = null
    val rooms = mutableListOf<Room>()

    var mainWay: Way? = null
    var height: Float? = null
    var name: String? = null
    val nonExistingLevels: MutableList<Int> = mutableListOf()
    var additionalTags = mutableMapOf<String, String>()

    fun check(): Boolean {
        if (id == null) {
            println("Could not parse building-relation: no id")
            return false
        }
        if (minLevel == null) {
            println("Could not parse building-relation $id: no min-level")
            return false
        }
        if (maxLevel == null) {
            println("Could not parse building-relation $id: no max-level")
            return false
        }
        if (outline == null) {
            println("Could not parse building-relation $id: no outline")
            return false
        }
        if (floors.size == 0) {
            println("Could not parse building-relation ${id}: no floors")
            return false
        }
        return true
    }

    fun twoSit(): String {
        return ""
    }
}