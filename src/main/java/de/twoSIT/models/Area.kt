package de.twoSIT.models

class Area(val minLongitude: Double, val minLatitude: Double, val maxLongitude: Double, val maxLatitude: Double) {
    init {
        assert(minLongitude < maxLongitude)
        assert(minLatitude < maxLatitude)
    }
}