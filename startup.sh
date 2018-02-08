#!/bin/bash
mkdir systemfiles
mkdir systemfiles/containerStop
mkdir systemfiles/containerRestore
mkdir systemfiles/log
mkdir systemfiles/lookupTable
echo "start program..."
mvn clean install
mvn exec:java -Dexec.mainClass=ServerApplication