# 2SIT

## Indoor Mapping
*Indoor Mapping*: 
https://wiki.openstreetmap.org/wiki/Indoor_Mapping

*JOSM Plugin Dev*: 
https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins

## Tools for indoor map visualization or editing
OsmInEdit - editor for indoor maps - https://osminedit.pavie.info/#19/48.13688/-1.69521/0

OpenLevelUp - 2d indoor map viewer - https://openlevelup.net/?l=0#19/53.55290/10.00693

JOSM Indoorhelper - Plugin to ease indoor mapping in JOSM - https://wiki.openstreetmap.org/wiki/JOSM/Plugins/indoorhelper

## Existing indoor tagging formats

*SIT*: 
https://wiki.openstreetmap.org/wiki/Simple_Indoor_Tagging

Example: https://www.openstreetmap.org/edit#map=19/48.13956/11.56087

Example (library with doors):

https://www.openstreetmap.org/edit#map=20/42.80047/-1.63640

https://openlevelup.net/?l=0#20/42.80052/-1.63628

*IndoorOSM (deprecated)*: 
https://wiki.openstreetmap.org/wiki/Proposed_features/IndoorOSM

Example: https://www.openstreetmap.org/edit#map=20/49.41866/8.67497

## Buildings with indoor maps
...


## Project Approaches
...
*JOSM plugin*
transforming the map but requiring direct feedback from the user in a WYSIWYG style to reduce risks of transformation errors

## 1) Data aquisition
- download area via OverpassApi or import downloaded area from JOSM (via coordinates, name or address)
- consider downloading only indoor relevant data
- multiple buldings at once - list inputs

## 2) Transformation into SIT conform data
- identify used indoor mapping convention
- continue based on result
- beginn with parsing features on one level
- if this is working start parsing connections between different levels

## 3) Evaluation and contribution
- save changeset in a format that can be understood by OSM
- import changeset into JOSM (cli? How does JOSM store changesets?)
- compare with old version and test for SIT conformity
