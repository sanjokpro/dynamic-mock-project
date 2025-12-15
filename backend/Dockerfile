# syntax=docker/dockerfile:1

##
# Stage 1: Build the Angular UI
##
FROM node:20-alpine AS ui-build
WORKDIR /app/frontend

# Install dependencies first (better cache)
COPY frontend/package*.json ./
RUN npm ci --legacy-peer-deps

# Copy the rest of the frontend and build
COPY frontend/ .
RUN npm run build

##
# Stage 2: Build the Spring Boot backend and embed the built UI
##
FROM gradle:8.14.1-jdk21 AS backend-build
WORKDIR /workspace/backend

# Copy backend build configuration and sources
COPY backend/gradle ./gradle
COPY backend/gradlew backend/gradlew.bat backend/build.gradle backend/settings.gradle backend/gradle.properties ./
COPY backend/src ./src
COPY backend/cfg ./cfg
COPY backend/deploy ./deploy

# Copy the built UI into Spring Boot static resources so it's served at /
RUN mkdir -p src/main/resources/static
COPY --from=ui-build /app/frontend/dist/dynamic-mock-frontend/browser/ ./src/main/resources/static/

# Build the Spring Boot application (skip tests for faster container builds)
RUN chmod +x ./gradlew
RUN ./gradlew -x test bootJar

##
# Stage 3: Runtime image (GraalVM JDK)
##
FROM ghcr.io/graalvm/jdk-community:21
WORKDIR /app

COPY --from=backend-build /workspace/backend/build/libs/dynamic-mock-server-*.jar /app/app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

