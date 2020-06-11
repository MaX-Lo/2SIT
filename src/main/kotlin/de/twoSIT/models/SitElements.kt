package de.twoSIT.models

import de.twoSIT.Mapper
import de.twoSIT.util.IdGenerator
import de.twoSIT.util.getLogger
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.sqrt


private val logger = getLogger("SitElements")


enum class LevelConnectionType {
    STAIRS, CONVEYOR, ELEVATOR
}

enum class IndoorTag {
    ROOM, AREA, WALL, CORRIDOR
}


class LevelConnection(id: String, val levels: MutableList<Int>, val doors: MutableList<IndoorObject>,
                      val indoorTag: IndoorTag, val levelConnectionType: LevelConnectionType,
                      additionalTags: MutableMap<String, String>) : AbstractOsmElement(id, additionalTags) {
    companion object {
        fun fromRaw(element: RawRelation): LevelConnection? {
            throw NotImplementedError()
        }

        fun fromOsm(element: Way): LevelConnection? {
            throw NotImplementedError()
        }
    }

    fun toOsm(): AbstractOsmElement {
        TODO("Not yet implemented")
    }

    override fun toRaw(): RawAbstractElement {
        TODO("Not yet implemented")
    }

}


class Room(id: String, val level: Int, val indoorTag: IndoorTag, additionalTags: MutableMap<String, String>) :
        Way(id, additionalTags) {
    var outline: Way? = null
    var height: Float? = null
    var name: String? = null
    var ref: String? = null

    companion object {
        fun fromRaw(element: RawWay, allNodes: MutableMap<String, Node>, level: Int): Room? {
            val way = Way.fromRaw(element, allNodes)
            return fromOsm(way, level)
        }

        fun fromOsm(element: Way, floorLevel: Int): Room? {
            var osmLevel: Int? = null
            var height: Float? = null
            var name: String? = null
            var ref: String? = null
            var indoorTag: IndoorTag? = null
            val additionalTags = mutableMapOf<String, String>()

            for ((key, value) in element.additionalTags.entries) {
                when (key) {
                    "level" -> osmLevel = value.toIntOrNull()
                    "height" -> height = value.toFloatOrNull()
                    "name" -> name = value
                    "ref" -> ref = value
                    "buildingpart" -> {
                        when (value) {
                            "corridor" -> indoorTag = IndoorTag.CORRIDOR
                            "room" -> indoorTag = IndoorTag.ROOM
                            "hall" -> indoorTag = IndoorTag.AREA
                            "verticalpassage" -> {}
                            else -> logger.info("Unrecognized IndoorTag '${value}' in Way ${element.id}")
                        }
                    }
                    else -> additionalTags[key] = value
                }
            }

            val roomLevel = osmLevel ?: floorLevel
            if (indoorTag == null) {
                logger.warn("Could not parse Way ${element.id} to room: No IndoorTag'")
                return null
            }

            val room = Room(element.id, roomLevel, indoorTag, additionalTags)
            room.height = height
            room.name = name
            room.ref = ref

            for (subsection in element.subsections) {
                val newSubSection = SubSection(
                        IndoorObject(subsection.node1.id, subsection.node1.latitude, subsection.node1.longitude,
                                floorLevel, subsection.node1.additionalTags),
                        IndoorObject(subsection.node2.id, subsection.node2.latitude, subsection.node2.longitude,
                                floorLevel, subsection.node2.additionalTags)
                )
                room.subsections.add(newSubSection)
            }

            return room
        }
    }

    fun toOsm(): Way {
        val way = Way(id, additionalTags)
        way.additionalTags["level"] = level.toString()
        way.additionalTags["indoorTag"] = indoorTag.toString().toLowerCase()
        if (height != null) way.additionalTags["height"] = height.toString()
        if (name != null) way.additionalTags["name"] = name!!
        if (ref != null) way.additionalTags["ref"] = ref!!
        return way
    }

    override fun toRaw(): RawWay {
        return toOsm().toRaw()
    }
}


class Floor(id: String, val level: Int, additionalTags: MutableMap<String, String>) :
        Relation(id, additionalTags) {
    var height: Float? = null
    var ref: String? = null
    var name: String? = null

    // Todo should be part of building root relation
    val usages = mutableMapOf<String, Way>()

    companion object {
        fun fromRaw(element: RawRelation, allWays: MutableMap<String, Way>): Floor? {
            return fromOsm(Relation.fromRaw(element), allWays)
        }

        fun fromOsm(element: Relation, allWays: MutableMap<String, Way>): Floor? {
            var level: Int? = null
            var height: Float? = null
            var name: String? = null
            var ref: String? = null
            val additionalTags = mutableMapOf<String, String>()

            for ((tag, value) in element.additionalTags.entries) {
                when (tag) {
                    "level" -> level = value.toIntOrNull()
                    "height" -> height = value.toFloatOrNull()
                    "name" -> name = value
                    else -> additionalTags[tag] = value
                }
            }
            if (level == null) {
                logger.warn("Could not parse Relation ${element.id} to Floor: No level'")
                return null
            }
            val floor = Floor(element.id, level, additionalTags)
            floor.height = height
            floor.name = name
            floor.ref = ref

            for (member in element.wayMembers) {
                val way = allWays[member.ref]!!
                if ("level:usage" in way.additionalTags.keys)
                    floor.usages[way.additionalTags["level:usage"]!!] = way
            }

            return floor
        }
    }

    override fun toRaw(): RawRelation {
        return toOsm().toRaw()
    }

    fun toOsm(): Relation {
        val relation = Relation(id, additionalTags)
        relation.additionalTags["level"] = level.toString()

        if (height != null) relation.additionalTags["height"] = height.toString()
        if (ref != null) relation.additionalTags["ref"] = ref!!
        if (name != null) relation.additionalTags["name"] = name!!

        return relation
    }
}

class IndoorObject(id: String, latitude: Double, longitude: Double, val level: Int, additionalTags: MutableMap<String, String>) :
        Node(id, latitude, longitude, additionalTags) {

    companion object {
        fun fromRaw(element: RawNode, level: Int): IndoorObject? {
            return fromOsm(Node.fromRaw(element), level)
        }

        fun fromOsm(element: Node, level: Int): IndoorObject? {
            return IndoorObject(element.id, element.latitude, element.longitude, level, element.additionalTags)
        }
    }

    override fun toRaw(): RawNode {
        return toOsm().toRaw()
    }

    fun toOsm(): Node {
        val node = Node(id, latitude, longitude, additionalTags)
        node.additionalTags["level"] = level.toString()
        return node
    }
}

class Building(id: String, val minLevel: Int, val maxLevel: Int, additionalTags: MutableMap<String, String>,
               val originalNodes: List<Node>, val originalWays: List<Way>, val originalRelations: List<Relation>,
               val entrances: MutableList<IndoorObject>) :
        AbstractOsmElement(id, additionalTags) {
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

    companion object {
        fun fromRaw(element: RawRelation, allNodes: MutableMap<String, Node>, allWays: MutableMap<String, Way>): Building? {
            return fromOsm(Relation.fromRaw(element), allNodes, allWays)
        }

        fun fromOsm(element: Relation, allNodes: MutableMap<String, Node>, allWays: MutableMap<String, Way>): Building? {
            val contained = Mapper.getContainedElements(element)
            val originalNodes = contained.first.map { it.deepCopy() }
            val originalWays = contained.second.map { it.deepCopy() }
            val originalRelations = contained.third.map { it.deepCopy() }

            var maxLevel: Int? = null
            var minLevel: Int? = null
            var name: String? = null
            var height: Float? = null
            val additionalTags = mutableMapOf<String, String>()

            for ((tag, value) in element.additionalTags.entries) {
                when (tag) {
                    "building:max_level" -> maxLevel = value.toIntOrNull()
                    "building:min_level" -> {
                        minLevel = value.toIntOrNull()
                        // keep tag since it's used not only by indoorOSM
                        additionalTags["building:min_level"] = value
                    }
                    "name" -> name = value
                    "height" -> height = value.toFloatOrNull()
                    else -> additionalTags[tag] = value
                }
            }
            var mainWay: Way? = null
            for (member in element.wayMembers) {
                if (mainWay == null) mainWay = allWays[member.ref]
                else logger.info("Multiple mainWays for building-relation ${element.id}")
            }

            val entrances = mutableListOf<IndoorObject>()
            for (member in element.nodeMembers) {
                if (member.role == "entrance") {
                    // todo this is a door
                    // fixme this is always level 0
                    IndoorObject.fromOsm(allNodes[member.ref]!!, 0)?.let { entrances.add(it) }
                }
            }

            if (maxLevel == null) {
                logger.warn("Could not parse Relation '${element.id}' to building: No maxLevel")
                return null
            }
            if (minLevel == null) {
                logger.warn("Could not parse Relation '${element.id}' to building: No minLevel")
                return null
            }

            val building = Building(element.id, minLevel, maxLevel, additionalTags, originalNodes, originalWays,
                    originalRelations, entrances)
            building.name = name
            building.height = height
            building.mainWay = mainWay

            return building
        }
    }

    override fun toRaw(): RawRelation {
        return toOsm().toRaw()
    }

    fun toOsm(): Relation {
        val relation = Relation(id, additionalTags)
        relation.additionalTags["minLevel"] = minLevel.toString()
        relation.additionalTags["maxLevel"] = maxLevel.toString()
        if (height != null) relation.additionalTags["height"] = height.toString()
        if (name != null) relation.additionalTags["name"] = name!!

        val nodeMembers = indoorObjects.map { it.toMember() }
        val wayMembers = rooms.map { it.toMember() } + floors.map { it.toMember() }
        relation.nodeMembers = nodeMembers.toMutableList()
        relation.wayMembers = wayMembers.toMutableList()

        // todo i dont think thats done. nonExisting levels, innerline, outerline etc

        return relation
    }
}

data class SubSection(var node1: Node, var node2: Node) {
    val len = sqrt(abs(node2.latitude - node1.latitude)) + sqrt(abs(node2.longitude - node1.longitude))

    /**
     *
     *     https://stackoverflow.com/questions/10301001/perpendicular-on-a-line-segment-from-a-given-point
     * @param node the [Node] that should be projected
     * @return a [Pair] of the projection [Node] as first and the distance to the [SubSection].node1 as second. The distance is 0 if the projected node is node1 and 1 if it is node2
     * @return null if no projection is found within the [SubSection]
     *
     */
    fun getIntersection(node: Node): Pair<Node, Double>? {
        if (node.inProximity(node1)) return Pair(node1, 0.0)
        if (node.inProximity(node2)) return Pair(node2, 1.0)

        val t = ((node.latitude - node1.latitude) * (node2.latitude - node1.latitude) + (node.longitude - node1.longitude) * (node2.longitude - node1.longitude)) / ((node2.latitude - node1.latitude).pow(2) + (node2.longitude - node1.longitude).pow(2))
        if (t < 0 || t > 1) {
            return null
        }

        val latitude = node1.latitude + t * (node2.latitude - node1.latitude)
        val longitude = node1.longitude + t * (node2.longitude - node1.longitude)

        val additionalTags = mutableMapOf<String, String>()
        additionalTags.putAll(node.additionalTags)
        additionalTags.putAll(additionalTags)
        val projectionPoint = Node(IdGenerator.getNewId(), latitude, longitude, additionalTags)

        return Pair(projectionPoint, t)
    }

    override fun equals(other: Any?): Boolean {
        if (other is SubSection) {
            if ((node1.inProximity(other.node1) && node2.inProximity(other.node2)) ||
                    (node1.inProximity(other.node2) && node2.inProximity(other.node1))) {
                // same start and end nodes
                return true
            }
        }
        return super.equals(other)
    }

    fun split(middleNode: Node): SubSection {
        val tmp = node2.deepCopy()
        node2 = middleNode
        return SubSection(middleNode, tmp)
    }

    fun getMerged(other: SubSection): SubSection {
        return if (node1.inProximity(other.node1)) {
            SubSection(node1.getMerged(other.node1), node2.getMerged(other.node2))
        } else {
            SubSection(node1.getMerged(other.node2), node2.getMerged(other.node1))
        }
    }

    fun replaceNode(old: Node, new: Node) {
        when (old) {
            node1 -> {
                node1 = new
            }
            node2 -> {
                node2 = new
            }
            else -> {
                logger.warn("$old is not in this subsection...")
            }
        }
    }
}
