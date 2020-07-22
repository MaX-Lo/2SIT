# 2SIT
A tool to transform OSM indoor maps mapped with the IndoorOSM schema to the SIT schema

## Usage 
* Set the parameters in config.json to your requirements
* Execute the script. This will generate 2 files:
  1) export/export_xx.osc - this is the osm change file containing information what has changed during the transformation
  2) logs/xx.log - contains logs about the transformation process, e.g. stats or elements that couldn't be transformed
* the next step is to load the osc file into the editor of your choice and check if the transformation was successful. 
Maybe smaller corrections need to be done here.
* If you checked the data for correctness the osc data can be committed directly to osm or be integrated with the data in
your editor

## Compilation
When executing maven compile one needs to add set the settings file explicitly.
```mvn -s settings.xml compile```

See the following for details:
https://stackoverflow.com/questions/44265547/how-to-properly-specify-jcenter-repository-in-maven-config

