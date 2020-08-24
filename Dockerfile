FROM openjdk:8-alpine

COPY target/uberjar/jamtap.jar /jamtap/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/jamtap/app.jar"]
