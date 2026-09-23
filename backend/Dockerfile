# syntax=docker/dockerfile:1



##
# Stage 1: Build the Spring Boot backend
##
FROM gradle:9.2.1-jdk21 AS backend-build
WORKDIR /workspace/backend

# Copy backend build configuration and sources
COPY backend/gradle ./gradle
COPY backend/gradlew backend/build.gradle backend/settings.gradle backend/gradle.properties ./
COPY backend/src ./src
COPY backend/cfg ./cfg
COPY backend/deploy ./deploy



# Build the Spring Boot application (skip tests for faster container builds)
RUN gradle -x test bootJar

##
# Stage 2: Explode the fat JAR into a flat classpath structure
##
FROM container-registry.oracle.com/graalvm/jdk:21 AS explode
WORKDIR /app

# Copy the fat JAR
COPY --from=backend-build /workspace/backend/build/libs/dynamic-mock-server-*.jar /app/app.jar

# Explode the fat JAR into a standard directory layout
# This removes the nested JAR complexity that confuses GraalVM's
# ClassPathIsolation.collectClassPathJars logic, preventing the NPE.
RUN cd /app && jar -xf app.jar && rm app.jar

##
# Stage 3: Runtime image with exploded classpath
##
FROM container-registry.oracle.com/graalvm/jdk:21
WORKDIR /app

# Copy the exploded JAR contents (BOOT-INF/classes, BOOT-INF/lib/, etc.)
COPY --from=explode /app/ ./

ENV SERVER_PORT=8080
EXPOSE 8080

# Run using the flat classpath (not -jar) so GraalVM can see all jars directly.
ENTRYPOINT java \
    --add-exports=org.graalvm.truffle/com.oracle.truffle.api.polyglot=ALL-UNNAMED \
    --add-exports=org.graalvm.truffle/com.oracle.truffle.api.instrumentation=ALL-UNNAMED \
    --add-exports=org.graalvm.truffle.compiler/com.oracle.truffle.compiler=ALL-UNNAMED \
    --add-opens=org.graalvm.truffle/com.oracle.truffle.api.nodes=ALL-UNNAMED \
    --add-opens=org.graalvm.truffle/com.oracle.truffle.api=ALL-UNNAMED \
    -Dpolyglotimpl.DisableClassPathIsolation=true \
    -Dpolyglot.engine.WarnInterpreterOnly=false \
    -Dspring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI} \
    -Dspring.data.redis.host=${SPRING_DATA_REDIS_HOST} \
    -Dspring.data.redis.port=${SPRING_DATA_REDIS_PORT} \
    -Dserver.port=${SERVER_PORT} \
    -cp "BOOT-INF/classes:BOOT-INF/lib/*" \
    com.dynamicmock.DynamicMockApplication
