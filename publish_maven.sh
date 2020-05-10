#!/bin/bash

# The script expects following in local.properties:
#mavenUrl=
#mavenLogin=
#mavenPassword=

./gradlew sdk:publish && ./gradlew client:publish
