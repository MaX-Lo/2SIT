package de.twoSIT.models


data class SubSection(val node1: Node, val node2: Node): Comparable<SubSection>{
    override fun compareTo(other: SubSection): Int {
        if ((node1.inProximity(other.node1) && node2.inProximity(other.node2)) ||
                (node1.inProximity(other.node2) && node2.inProximity(other.node1))){
            // same start and end node
            return 0
        }
        // todo other checks for unreal twins
        return 1
    }
}