# ---- BUILD STAGE ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml để cache dependency
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline

# Copy source code
COPY src ./src

# Build jar file
RUN mvn -q -e -B clean package -DskipTests


# ---- RUN STAGE ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy jar file
COPY --from=build /app/target/*.jar app.jar

# JVM options
ENV JAVA_OPTS="-Xmx1G -Xms256m -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

# ENTRYPOINT shell mode
ENTRYPOINT sh -c "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"
