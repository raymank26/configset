gradle build && unzip -o build/distributions/app-0.1.zip -d build/tmp && JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=5004,server=y,suspend=n" ./build/tmp/app-0.1/bin/app
