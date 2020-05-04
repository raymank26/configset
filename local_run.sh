./gradlew server:dockerBuildImage &&
  ./gradlew dashboard:dockerBuildImage &&
  docker-compose up
