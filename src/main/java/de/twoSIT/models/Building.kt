package de.twoSIT.models

enum class IndoorTag {
    ROOM, AREA, WALL, CORRIDOR
}

class LevelConnection(indoorTag: IndoorTag, val levels: List<Int>, val doors: List<Node>){
    init {
        assert(indoorTag == IndoorTag.AREA || indoorTag == IndoorTag.CORRIDOR){"'indoorTag' has to be 'CORRIDOR' " +
                "or 'AREA"}
    }
}

class Floor(val level: Int){
    var ref: String? = null
    var height: Float? = null
    var name: String? = null
    var additionalTags: MutableList<Tag> = mutableListOf()
}

class IndoorObject(val level: Int, val indoorTag: IndoorTag) {
    var height: Float? = null
    var name: String? = null
    var ref: String? = null
    var additionalTags: MutableList<Tag> = mutableListOf()
}

class Building(val id: String, val minLevel: Int, val maxLevel: Int, val floors: List<Floor>, val objects: List<IndoorObject>,
               connections: List<LevelConnection>, val outline: Way){
    var height: Float? = null
    var name: String? = null
    val nonExistingLevels: MutableList<Int> = mutableListOf()
    val additionalTags: MutableList<Tag> = mutableListOf()

    fun twoSit(): String{
        return ""
    }
}