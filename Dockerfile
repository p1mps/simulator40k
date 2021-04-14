FROM openjdk:8-alpine

COPY target/uberjar/simulator40k.jar /simulator40k/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/simulator40k/app.jar"]
