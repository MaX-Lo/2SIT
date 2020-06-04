package de.twoSIT.models

import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SubSectionTest {

    @Test
    fun testGetIntersectionNodeIsNull() {
        /**
         *   1 ------- 2 ------- 3
         */

        val node1 = Node("1", 0.0, 0.0)
        val node2 = Node("2", 1.0, 0.0)
        val node3 = Node("3", 2.0, 0.0)
        val subsection = SubSection(node1, node2)
        val node4 = subsection.getIntersection(node3)

        assertNull(node4)
    }

    @Test
    fun testGetIntersectionNodeFarFromIntersectionPoint() {
        /**               3
         *                |
         *        1 ----- 4 ----- 2
         */

        val node1 = Node("1", 0.0, 0.0)
        val node2 = Node("2", 1.0, 0.0)
        val node3 = Node("3", 0.5, 0.5)
        val subsection = SubSection(node1, node2)

        val intersection = subsection.getIntersection(node3)
        assertNotNull(intersection, "Can't perform intersection test since intersection was not found")

        val expectedNode = Node("-1", 0.5, 0.0)
        val expectedT = 0.5
        assertEquals(intersection.first.longitude, expectedNode.longitude)
        assertEquals(intersection.first.latitude, expectedNode.latitude)
        assertEquals(intersection.second, expectedT)
    }

    @Test
    fun testGetIntersectionNodeCloseToProjectionPoint() {
        /**               3
         *                |
         *        1 ----- 4 ----- 2
         */

        val node1 = Node("1", 51.0255184, 13.7223363)
        val node2 = Node("2", 51.0255451, 13.7221857)
        val node3 = Node("3", 51.0255614, 13.7222899)
        val subsection = SubSection(node1, node2)

        val intersection = subsection.getIntersection(node3)
        assertNotNull(intersection, "Can't perform intersection test since intersection was not found")

        // expected results are measured by hand, precession is therefore not very high
        val expectedNode = Node("-1", 51.0255, 13.7222)
        val expectedT = 0.35
        Assert.assertEquals(expectedNode.longitude, intersection.first.longitude, 0.0001)
        Assert.assertEquals(expectedNode.latitude, intersection.first.latitude, 0.0001)
        Assert.assertEquals(expectedT, intersection.second, 0.05)
    }
}