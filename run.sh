#!/bin/bash

git pull || true
npm i || true
JAVA_OPTS="-Xmx512m -Xms512m" sbt runAll
