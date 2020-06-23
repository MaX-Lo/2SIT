package de.twoSIT.models

import org.junit.Test
import kotlin.test.assertEquals

class LevelStringParseTest {

    @Test
    fun testLevelStrSingle() {
        assertEquals(mutableSetOf(1f), levelFromStr("1"))
        assertEquals(mutableSetOf(-1f), levelFromStr("-1"))
        assertEquals(mutableSetOf(1.1f), levelFromStr("1.1 "))
    }

    @Test
    fun testLevelStrWithTo() {
        assertEquals(mutableSetOf(1f, 2f, 3f), levelFromStr("1 to 3"))
        assertEquals(mutableSetOf(1f, 2f, 3f), levelFromStr("3 to 1"))
    }

    @Test
    fun testLevelStrWithList() {
        assertEquals(mutableSetOf(1f, 2f, 3f), levelFromStr("1, 2, 3"))
        assertEquals(mutableSetOf(1f), levelFromStr("1"))
        assertEquals(mutableSetOf(1f), levelFromStr("1,"))
        assertEquals(mutableSetOf(1f, 2f, 3f), levelFromStr("1; 2; 3"))
        assertEquals(mutableSetOf(-1f, 2f, -3f), levelFromStr("-1, 2, -3"))
        assertEquals(mutableSetOf(-0.5f), levelFromStr("-0.5"))
    }

    @Test
    fun testLevelStrMixed() {
        assertEquals(mutableSetOf(1f, 3f, 4f), levelFromStr("1, 3 to 4"))
        assertEquals(mutableSetOf(1f, 3f, 4f), levelFromStr("1;3 to4"))
    }
}