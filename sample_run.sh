./gradlew server:dockerBuildImage --stacktrace &&
  ./gradlew dashboard:dockerBuildDefaultImage &&
  ./gradlew sample:dockerBuildImage &&
  docker-compose -p sample -f sample/docker-compose.yml up
