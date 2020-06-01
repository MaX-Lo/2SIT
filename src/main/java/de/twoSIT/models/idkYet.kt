package de.twoSIT.models


data class SubSection(val node1: Node, val node2: Node) {
    override fun equals(other: Any?): Boolean {
        if (other is SubSection) {
            if ((node1 == other.node1 && node2 == other.node2) ||
                    (node1 == other.node2 && node2 == other.node1)) {
                // same start and end nodes
                return true
            }
        }

        return super.equals(other)
    }
}

