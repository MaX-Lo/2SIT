package de.twoSIT.models

import de.twoSIT.util.IdGenerator
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


data class SubSection(var node1: Node, var node2: Node) {
    val len = sqrt(abs(node2.latitude - node1.latitude)) + sqrt(abs(node2.longitude - node1.longitude))

    /*
    https://stackoverflow.com/questions/10301001/perpendicular-on-a-line-segment-from-a-given-point
                       C
                       |
          A ---------- D ----------- B
     */
    fun getInterception(node: Node): Node? {
        if (node.inProximity(node1)) return node1
        if (node.inProximity(node2)) return node2

        val t = ((node.latitude - node1.latitude) * (node2.latitude - node1.latitude) + (node.longitude - node1.longitude) * (node2.longitude - node1.longitude)) / ((node2.latitude - node1.latitude).pow(2) + (node2.longitude - node1.longitude).pow(2))
        if (0 < t || 1 < t) {
            return null
        }

        val latitude = node1.latitude + t * (node2.latitude - node1.latitude)
        val longitude = node1.longitude + t * (node2.longitude - node1.longitude)
        return Node(IdGenerator.getNewId(), latitude, longitude)
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

