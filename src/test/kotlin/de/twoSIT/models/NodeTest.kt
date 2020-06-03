package de.twoSIT.models

import org.junit.Test
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.test.assertTrue

class NodeTest {

    @Test
    fun testDistanceToSameCoordinates() {
        val node1 = Node("1", 10.020134, 10.2356)
        val node2 = Node("2", 10.020134, 10.2356)

        assertTrue { node1.distanceTo(node2) == 0.0 }
        assertTrue { node2.distanceTo(node1) == 0.0 }
    }

    @Test
    fun testDistanceToDifferentCoordinates() {
        val node1 = Node("1", 10.0, 10.0)
        val node2 = Node("2", 0.0, 0.0)

        assertTrue { node1.distanceTo(node2).roundToInt() == 1568521 }
        assertTrue { node2.distanceTo(node1).roundToInt() == 1568521 }
    }
}