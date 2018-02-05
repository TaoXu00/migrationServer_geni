#!/bin/bash
echo "start program..."
mvn verify
mvn exec:java -Dexec.mainClass=ServerApplication