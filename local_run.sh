./gradlew server:dockerBuildImage &&
  ./gradlew dashboard:dockerBuildDevImage &&
  docker-compose up
