# 2SIT

## Indoor Mapping
[Indoor Mapping](https://wiki.openstreetmap.org/wiki/Indoor_Mapping)

[JOSM Plugin Dev](https://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins)

## Tools for indoor map visualization or editing
[OsmInEdit](https://osminedit.pavie.info/#19/48.13688/-1.69521/0) - editor for indoor maps

[OpenLevelUp](https://openlevelup.net/?l=0#19/53.55290/10.00693) - 2d indoor map viewer

[JOSM Indoorhelper](https://wiki.openstreetmap.org/wiki/JOSM/Plugins/indoorhelper) - Plugin to ease indoor mapping in JOSM

## Existing indoor tagging formats

[SIT](https://wiki.openstreetmap.org/wiki/Simple_Indoor_Tagging)

[Example](https://www.openstreetmap.org/edit#map=19/48.13956/11.56087)

Example (library with doors):

[OSM](https://www.openstreetmap.org/edit#map=20/42.80047/-1.63640)

[OpenLevelUp](https://openlevelup.net/?l=0#20/42.80052/-1.63628)

[AllDeprecated](https://wiki.openstreetmap.org/wiki/Indoor_Mapping#Previous_tagging_proposals)

[IndoorOSM (deprecated)](https://wiki.openstreetmap.org/wiki/Proposed_features/IndoorOSM)

[Example](https://www.openstreetmap.org/edit#map=20/49.41866/8.67497)

## API's
### python
https://github.com/mocnik-science/osm-python-tools

### java
Osmosis:
- [wiki](https://wiki.openstreetmap.org/wiki/Osmosis)
- [plugins](https://wiki.openstreetmap.org/wiki/Osmosis/Writing_Plugins)
- [maven](https://mvnrepository.com/artifact/org.openstreetmap.osmosis)

- [OSM API Client lib](https://github.com/westnordost/osmapi/)

# Project Approaches
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
- output: either commit changeset to a server or save it as xml

# Development workflow/setup
- local OSM instance running a pretty recent but not synchronizing copy of OSM
  - limit available area (e.g. europe) to reduce system load 
  - should provide access through osm api v0.6 or similar
 - transformation algorithm will output a xml changeset file which can be directly uploaded and evaluated onto the local osm instance
- to evaluate an id-editor instance could be setup besides the osm database
[Guide for database setup] (https://wiki.openstreetmap.org/wiki/Setting_up_a_local_copy_of_the_OpenStreetMap_database,_kept_up_to_date_with_minutely_diffs)
