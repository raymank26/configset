./gradlew server:dockerBuildImage --stacktrace &&
  ./gradlew dashboard:dockerBuildDevImage &&
  docker-compose up
