package de.twoSIT.models

import Coordinate
import com.google.gson.Gson
import de.twoSIT.Mapper
import de.twoSIT.util.IdGenerator
import de.twoSIT.util.getLogger
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


private val logger = getLogger("SitElements")


enum class LevelConnectionType {
    STAIRS, CONVEYOR, ELEVATOR
}

enum class IndoorTag {
    ROOM, AREA, WALL, CORRIDOR
}

abstract class AbstractSitElement(val id: String, val additionalTags: MutableMap<String, String>) {
    abstract fun toOsm(): AbstractOsmElement

    abstract fun toRaw(): RawAbstractElement
}


class LevelConnection(id: String, val levels: MutableList<Int>, val nodes: MutableList<IndoorObject>,
                      val indoorTag: IndoorTag, val levelConnectionType: LevelConnectionType,
                      additionalTags: MutableMap<String, String>) : AbstractSitElement(id, additionalTags) {
    companion object {
        fun fromRaw(element: RawWay, allNodes: MutableMap<String, Node>): LevelConnection? {
            return fromOsm(Way.fromRaw(element), allNodes)
        }

        fun fromOsm(element: Way, allNodes: MutableMap<String, Node>): LevelConnection? {
            if (element.additionalTags.containsKey("buildingpart")) element.additionalTags.remove("buildingpart")

            if (!element.additionalTags.containsKey("buildingpart:verticalpassage")) {
                logger.warn("Could not parse Way ${element.id} to LevelConnection: No LevelConnectionType " +
                        "('buildingpart:verticalpassage')")
                return null
            }
            var connectionType: LevelConnectionType? = null
            when (element.additionalTags["buildingpart:verticalpassage"]){
                "stairway" -> connectionType = LevelConnectionType.STAIRS
                "elevator" -> connectionType = LevelConnectionType.ELEVATOR
            }
            if (connectionType == null) {
                logger.warn("Could not parse Way ${element.id} to LevelConnection: Invalid LevelConnectionType " +
                        "'${element.additionalTags["buildingpart:verticalpassage"]}' ('buildingpart:verticalpassage')")
                return null
            }
            element.additionalTags.remove("buildingpart:verticalpassage")

            if (!element.additionalTags.containsKey("buildingpart:verticalpassage:floorrange")) {
                logger.warn("Could not parse Way ${element.id} to LevelConnection: No FloorRange " +
                        "('buildingpart:verticalpassage:floorrange')")
                return null
            }
            val floorRangeOsm = element.additionalTags["buildingpart:verticalpassage:floorrange"]!!
            val levels = Regex("(?<=\\s|^)[-+]?\\d+(?=\\s|\$)").findAll(floorRangeOsm).map { it.value.toInt() }.toMutableList()
            if (levels.isEmpty()){
                logger.warn("Could not parse Way ${element.id} to LevelConnection: No FloorRange recognized " +
                        "'${element.additionalTags["buildingpart:verticalpassage:floorrange"]}' " +
                        "('buildingpart:verticalpassage:floorrange')")
                return null
            }
            element.additionalTags.remove("buildingpart:verticalpassage:floorrange")

            val levelRange = IntRange(levels.min()!!, levels.max()!!).toMutableList()
            val nodes = mutableListOf<IndoorObject>()
            for (nodeRef in element.nodeReferences){
                val indoorObject = IndoorObject.fromOsm(allNodes[nodeRef.ref]!!, levelRange)?: continue
                nodes.add(indoorObject)
            }
            return LevelConnection(element.id, levels, nodes, IndoorTag.ROOM, connectionType, element.additionalTags)
        }
    }

    override fun toOsm(): Way {
        val tags = mutableMapOf<String, String>()
        tags.putAll(additionalTags)

        when (levelConnectionType){
            LevelConnectionType.CONVEYOR -> {
                tags["stairs"] = "yes"
                tags["conveying"] = "yes"
            }
            LevelConnectionType.ELEVATOR -> tags["highway"] = "elevator"
            LevelConnectionType.STAIRS -> tags["stairs"] = "yes"
        }

        when (indoorTag){
            IndoorTag.ROOM -> tags["indoor"] = "room"
            IndoorTag.AREA -> tags["indoor"] = "area"
        }
        tags["level"] = "${levels.min()}-${levels.max()}"
        val nodeRefs = nodes.map { it.toOsm().toNodeReference() }.toMutableList()
        return Way(id, nodeRefs, tags)
    }

    override fun toRaw(): RawAbstractElement {
        return toOsm().toRaw()
    }

}


class Room(id: String, val level: Int, val indoorTag: IndoorTag, val nodes: MutableList<IndoorObject>,
           additionalTags: MutableMap<String, String>) : AbstractSitElement(id, additionalTags) {
    var height: Float? = null
    var name: String? = null
    var ref: String? = null

    companion object {
        fun fromRaw(element: RawWay, allNodes: MutableMap<String, Node>, level: Int): Room? {
            val way = Way.fromRaw(element)
            return fromOsm(way, level, allNodes)
        }

        fun fromOsm(element: Way, floorLevel: Int, allNodes: MutableMap<String, Node>): Room? {
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

            val nodes = element.nodeReferences.map { allNodes[it.ref]!! }
            val indoorObjects = mutableListOf<IndoorObject>()
            for (node in nodes){
                val obj = IndoorObject.fromOsm(node, mutableListOf(floorLevel))
                if (obj == null) {
                    logger.warn("Could not parse way ${element.id} to Room: Parse error on Node '${node.id}'")
                    return null
                }
                indoorObjects.add(obj)
            }
            val room = Room(element.id, roomLevel, indoorTag, indoorObjects, additionalTags)
            room.height = height
            room.name = name
            room.ref = ref

            return room
        }
    }

    override fun toOsm(): Way {
        val way = Way(id, nodes.map { it.toOsm().toNodeReference() }.toMutableList(), additionalTags)
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
        AbstractSitElement(id, additionalTags) {
    var height: Float? = null
    var ref: String? = null
    var name: String? = null
    var shell: Room? = null

    // Todo should be part of building root relation
    val usages = mutableMapOf<String, Way>()

    companion object {
        fun fromRaw(element: RawRelation, allNodes: MutableMap<String, Node>, allWays: MutableMap<String, Way>): Floor? {
            return fromOsm(Relation.fromRaw(element), allNodes, allWays)
        }

        fun fromOsm(element: Relation, allNodes: MutableMap<String, Node>, allWays: MutableMap<String, Way>): Floor? {
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
                if (member.role == "shell") {
                    floor.shell = Room.fromOsm(allWays[member.ref]!!, level, allNodes)
                }
            }

            return floor
        }
    }

    override fun toRaw(): RawRelation {
        return toOsm().toRaw()
    }

    override fun toOsm(): Relation {
        val relation = Relation(id, additionalTags)
        relation.additionalTags["level"] = level.toString()

        if (height != null) relation.additionalTags["height"] = height.toString()
        if (ref != null) relation.additionalTags["ref"] = ref!!
        if (name != null) relation.additionalTags["name"] = name!!

        return relation
    }
}

class IndoorObject(id: String, val latitude: Double, val longitude: Double, val levels: MutableList<Int>, additionalTags: MutableMap<String, String>) :
        AbstractSitElement(id, additionalTags) {

    companion object {
        // FixMe is it task of a node to take care what is in proximity? Each node could have a different threshold?
        private const val proximityThreshold = 0.5 // meters

        fun fromRaw(element: RawNode, levels: MutableList<Int>): IndoorObject? {
            return fromOsm(Node.fromRaw(element), levels)
        }

        fun fromOsm(element: Node, levels: MutableList<Int>): IndoorObject? {
            if (element.additionalTags.containsKey("door"))
                print(element)
            return IndoorObject(element.id, element.latitude, element.longitude, levels, element.additionalTags)
        }

        fun getMerged(others: Iterable<IndoorObject>): IndoorObject {
            val othersAsList = others.toList()

            // FixMe othersAsList is sometimes of size 0 but at least one node should be in each set of to be merged nodes
            //       could be because how we merge Transitive relations

            if (othersAsList.isEmpty()) {
                logger.info("Can't merge list of size 0")
            }
            for (i in 0 until othersAsList.size - 1) {
                val node1 = othersAsList[i]

                for (j in i + 1 until othersAsList.size) {
                    val node2 = othersAsList[j]
                    if (node1.distanceTo(node2) > IndoorObject.proximityThreshold * 1.5) {
                        logger.warn("merging nodes ${node1.id}, ${node2.id} that are not in proximity! Distance: ${node1.distanceTo(node2)}")
                    }
                }
            }
            // ToDo tags contained in multiple nodes are overwritten - do we want that?
            val additionalTags = mutableMapOf<String, String>()
            others.map { additionalTags.putAll(it.additionalTags) }

            val latitude = others.map { it.latitude }.average()
            val longitude = others.map { it.longitude }.average()
            val level = othersAsList[0].levels

            return IndoorObject(IdGenerator.getNewId(), latitude, longitude, level, additionalTags)
        }

    }

    override fun toRaw(): RawNode {
        return toOsm().toRaw()
    }

    override fun toOsm(): Node {
        val node = Node(id, latitude, longitude, additionalTags)

        val levelString: String = if (levels.size == 1)
            levels[0].toString()
        else
            "${levels.min()}-${levels.max()}"

        node.additionalTags["level"] = levelString
        return node
    }

    fun getMerged(other: IndoorObject): IndoorObject {
        return getMerged(setOf(this, other))
    }

    fun inProximity(other: IndoorObject): Boolean {
        return distanceTo(other) < proximityThreshold
    }

    fun distanceTo(other: IndoorObject): Double {
        /**
         * @return the haversine distance to [other] in meters
         */
        return GeoDistance.haversineDistanceInM(Coordinate(latitude, longitude), Coordinate(other.latitude, other.longitude))
    }

    fun deepCopy(): IndoorObject {
        val JSON = Gson().toJson(this)
        return Gson().fromJson(JSON, IndoorObject::class.java)
    }
}

class Building(id: String, val minLevel: Int, val maxLevel: Int, additionalTags: MutableMap<String, String>,
               val originalNodes: List<Node>, val originalWays: List<Way>, val originalRelations: List<Relation>,
               val entrances: MutableList<IndoorObject>) :
        AbstractSitElement(id, additionalTags) {
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
                    IndoorObject.fromOsm(allNodes[member.ref]!!, mutableListOf(0))?.let { entrances.add(it) }
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

    override fun toOsm(): Relation {
        val relation = Relation(id, additionalTags)
        relation.additionalTags["minLevel"] = minLevel.toString()
        relation.additionalTags["maxLevel"] = maxLevel.toString()
        if (height != null) relation.additionalTags["height"] = height.toString()
        if (name != null) relation.additionalTags["name"] = name!!

        val nodeMembers = indoorObjects.map { it.toOsm().toMember() }
        val wayMembers = rooms.map { it.toOsm().toMember() }
        val relationMembers = floors.map { it.toOsm().toMember() }
        relation.nodeMembers = nodeMembers.toMutableList()
        relation.wayMembers = wayMembers.toMutableList()
        relation.relationMembers = relationMembers.toMutableList()

        // todo i dont think thats done. nonExisting levels, innerline, outerline etc

        return relation
    }

    fun getContainedElements(): Triple<List<Node>, List<Way>, List<Relation>> {
        val containedNodes = indoorObjects.map { it.toOsm() }.toMutableList()
        containedNodes.addAll(entrances.map { it.toOsm() })

        val containedWays = rooms.map { it.toOsm() }
        rooms.map { containedNodes.addAll(it.nodes.map { it.toOsm() }) }

        val containedRelations = floors.map { it.toOsm() }

        return Triple(containedNodes, containedWays, containedRelations)
    }
}

data class WallSection(var start: IndoorObject, var end: IndoorObject) {
    val len = sqrt(abs(end.latitude - start.latitude)) + sqrt(abs(end.longitude - start.longitude))

    /**
     *
     *     https://stackoverflow.com/questions/10301001/perpendicular-on-a-line-segment-from-a-given-point
     * @param node the [Node] that should be projected
     * @return a [Pair] of the projection [Node] as first and the distance to the [SubSection].node1 as second. The distance is 0 if the projected node is node1 and 1 if it is node2
     * @return null if no projection is found within the [SubSection]
     *
     */
    fun getIntersection(node: IndoorObject): Pair<IndoorObject, Double>? {
        if (node.inProximity(start)) return Pair(start, 0.0)
        if (node.inProximity(end)) return Pair(end, 1.0)

        val t = ((node.latitude - start.latitude) * (end.latitude - start.latitude) + (node.longitude - start.longitude) * (end.longitude - start.longitude)) / ((end.latitude - start.latitude).pow(2) + (end.longitude - start.longitude).pow(2))
        if (t < 0 || t > 1) {
            return null
        }

        val latitude = start.latitude + t * (end.latitude - start.latitude)
        val longitude = start.longitude + t * (end.longitude - start.longitude)

        val additionalTags = mutableMapOf<String, String>()
        additionalTags.putAll(node.additionalTags)
        additionalTags.putAll(additionalTags)
        val projectionPoint = IndoorObject(IdGenerator.getNewId(), latitude, longitude, start.levels, additionalTags)

        return Pair(projectionPoint, t)
    }

    fun split(middleNode: IndoorObject): WallSection {
        val tmp = end.deepCopy()
        end = middleNode
        return WallSection(middleNode, tmp)
    }

    fun getMerged(other: WallSection): WallSection {
        return if (start.inProximity(other.start)) {
            WallSection(start.getMerged(other.start), end.getMerged(other.end))
        } else {
            WallSection(start.getMerged(other.end), end.getMerged(other.start))
        }
    }

    fun replaceNode(old: IndoorObject, new: IndoorObject) {
        when (old) {
            start -> {
                start = new
            }
            end -> {
                end = new
            }
            else -> {
                logger.warn("$old is not in this subsection...")
            }
        }
    }
}
