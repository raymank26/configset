./gradlew server:dockerBuildImage --stacktrace &&
  ./gradlew dashboard:dockerBuildDevImage &&
  (docker-compose up & ./dashboard_npm_serve.sh)
