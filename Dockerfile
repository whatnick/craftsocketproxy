FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew && ./gradlew --no-daemon clean jar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /src/build/libs/*.jar /opt/craftsocketproxy.jar
USER 65532:65532
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/craftsocketproxy.jar"]
CMD ["--s", "-host", "minecraft.minecraft.svc.cluster.local", "-port", "25565", "-proxy", "8080"]
