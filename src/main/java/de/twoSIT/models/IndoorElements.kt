package de.twoSIT.models

import de.twoSIT.Mapper
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
    val doors = mutableListOf<Node>()
    var levelConnectionType: LevelConnectionType? = null

    fun check(): Boolean {
        if (id == null) {
            Building.logger.warn("Could not parse levelConnection-way: no id")
            return false
        }
        if (indoorTag == null) {
            Building.logger.info("levelConnection-way $id: no indoor tag")
            return true
        }
        if (levelConnectionType == null) {
            Building.logger.warn("levelConnection-way $id: no levelConnectionType")
            return true
        }
        if (doors.isEmpty()) {
            Building.logger.info("levelConnection-way $id: no doors")
            return true
        }
        return true
    }
}


class Room(id: String? = null) : Way(id) {
    var level: Int? = null
    var indoorTag: IndoorTag? = null

    var outline: Way? = null
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
            Building.logger.info("Room-way $id: no indoor tag")
            return true
        }
        return true
    }

    fun toWay(): Way {
        val way = Way(id)
        way.additionalTags = additionalTags
        way.additionalTags["level"] = level.toString()
        if (indoorTag != null) way.additionalTags["indoorTag"] = indoorTag.toString().toLowerCase()
        if (height != null) way.additionalTags["height"] = height.toString()
        if (name != null) way.additionalTags["name"] = name!!
        if (ref != null) way.additionalTags["ref"] = ref!!
        return way
    }
}


class Floor(id: String? = null) : Way(id) {
    var level: Int? = null
    var height: Float? = null
    var ref: String? = null
    var name: String? = null
    // Todo should be part of building root relation
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

    fun toWay(): Way {
        var way = Way(id)
        way.additionalTags = additionalTags
        if (level != null) way.additionalTags["level"] = level.toString()
        if (height != null) way.additionalTags["height"] = height.toString()
        if (ref != null) way.additionalTags["ref"] = ref!!
        if (name != null) way.additionalTags["name"] = name!!
        return way
    }
}

class IndoorObject(id: String? = null, latitude: Double, longitude: Double, val level: Int?) : Node(id, latitude, longitude) {

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

    fun toNode(): Node {
        val node = Node(id, latitude, longitude)
        node.additionalTags = additionalTags
        node.additionalTags["level"] = level.toString()
        return node
    }
}

class Building(id: String? = null): AbstractElement(id) {
    companion object {
        @JvmStatic
        val logger = getLogger(Building::class.java)
    }

    var minLevel: Int? = null
    var maxLevel: Int? = null
    var height: Float? = null
    var name: String? = null

    val indoorObjects = mutableListOf<IndoorObject>()
    val rooms = mutableListOf<Room>()
    var floors = mutableListOf<Floor>()
    val connections = mutableListOf<LevelConnection>()
    var outline: Way? = null

    var innerline: Way? = null
    var mainWay: Way? = null

    val nonExistingLevels: MutableList<Int> = mutableListOf()

    // used to create the diff for later export
    var originalNodes = listOf<Node>()
    var originalWays = listOf<Way>()
    var originalRelations = listOf<Relation>()

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
            logger.info("building-relation $id: no outline")
            return true
        }
        if (floors.size == 0) {
            logger.info("building-relation ${id}: no floors")
            return true
        }
        return true
    }

    fun toRelation(): Relation {
        val relation = Relation(id)
        relation.additionalTags = additionalTags
        if (minLevel != null) relation.additionalTags["minLevel"] = minLevel.toString()
        if (maxLevel != null) relation.additionalTags["maxLevel"] = maxLevel.toString()
        if (height != null) relation.additionalTags["height"] = height.toString()
        if (name != null) relation.additionalTags["name"] = name!!

        val nodeMembers = indoorObjects.map { it.toMember() }
        val wayMembers = rooms.map { it.toMember() } + floors.map { it.toMember() }
        relation.nodeMembers = nodeMembers.toMutableList()
        relation.wayMembers = wayMembers.toMutableList()

        return relation
    }
}