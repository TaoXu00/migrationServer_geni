#!/bin/bash
cd ..
mkdir ContainerStop
mkdir ContainerResotre
cd migrationServer_geni
mkdir log
mkdir lookupTable
echo "start program..."
mvn verify
mvn exec:java -Dexec.mainClass=ServerApplication