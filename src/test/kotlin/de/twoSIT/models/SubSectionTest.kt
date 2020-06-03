package de.twoSIT.models

import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubSectionTest {

    @Test
    fun testGetIntersectionNode() {
        /**               3
         *                |
         *        1 ----- 4 ----- 2
         */

        val node1 = Node("1", 0.0, 0.0)
        val node2 = Node("2", 1.0, 0.0)
        val node3 = Node("3", 2.0, 0.0)
        val subsection = SubSection(node1, node2)
        val node4 = subsection.getIntersection(node3)

        assertNull(node4)
    }

    // ToDo test true intersection not null and if intersection point as well as "t" is correct
}