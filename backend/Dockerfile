# ETAP 1: Budowanie (Maven + JDK 21)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Kopiujemy pliki projektu
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
# Budujemy aplikację
RUN mvn clean package -DskipTests

# ETAP 2: Uruchamianie (JRE 21 - lekki obraz produkcyjny)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Kopiujemy wynikowy plik JAR
COPY --from=build /app/target/*.jar app.jar

# Port dla Spring Boota
EXPOSE 8080

# Uruchomienie z optymalizacją pod kontenery
ENTRYPOINT ["java", "-jar", "app.jar"]