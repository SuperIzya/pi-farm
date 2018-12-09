#!/bin/bash

cd /home/pi/pi-farm
git pull || true

JAVA_OPTS="-Xmx512m -Xms512m" sbt runAll

