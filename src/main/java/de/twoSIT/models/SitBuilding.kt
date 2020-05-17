package de.twoSIT.models

class SitBuilding {
    val mainRelation = Relation()

    /*
    checks if all SIT conditions are fulfilled
     */
    fun check() : Boolean {
        // todo check tag values

        var containsMinLevel = false
        var containsMaxLevel = false
        for (tag in mainRelation.tags){
            if (tag.k == "min_level"){
                containsMinLevel = true
            } else if (tag.k == "max_level"){
                containsMaxLevel = true
            }
            if (containsMaxLevel && containsMinLevel) break
        }

        var everyNodeHasLevel = true
        var everyWayHasIndoor = true
        var everyWayHasLevel = true
        for (way in mainRelation.ways.values){

            var wayContainsLevel = false
            var wayContainsIndoor = false
            for (tag in way.tags){
                if (tag.k == "level"){
                    wayContainsLevel = true
                } else if (tag.k == "indoor"){
                    wayContainsIndoor = true
                }

                if (wayContainsIndoor && wayContainsLevel) break
            }
            everyWayHasLevel = everyWayHasLevel && wayContainsLevel
            everyWayHasIndoor = everyWayHasIndoor && wayContainsIndoor

            for (node in way.nodes.values){
                var nodeContainsLevel = false
                for (tag in node.tags){
                    if (tag.k == "level"){
                        nodeContainsLevel = true
                    }

                    if (nodeContainsLevel) break
                }
                everyNodeHasLevel = everyNodeHasLevel && nodeContainsLevel
            }
        }

        return containsMinLevel && containsMaxLevel && everyNodeHasLevel && everyWayHasLevel && everyWayHasIndoor
    }
}