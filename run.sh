#!/bin/bash

cd $(dirname $0)
git pull || true
npm i && npm start || true
cd common
npm i && npm start || true
cd ../plugins/servo/
npm i && npm start || true
cd ../temperature
npm i && npm start || true
cd $(dirname $0)
JAVA_OPTS="-Xmx512m -Xms512m" sbt runAll d

