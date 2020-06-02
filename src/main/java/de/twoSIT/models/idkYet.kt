package de.twoSIT.models


data class SubSection(var node1: Node, var node2: Node) {
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

    fun getMerged(other: SubSection): SubSection {
        return if (node1.inProximity(other.node1)){
            SubSection(node1.getMerged(other.node1), node2.getMerged(other.node2))
        } else {
            SubSection(node1.getMerged(other.node2), node2.getMerged(other.node1))
        }
    }
}

