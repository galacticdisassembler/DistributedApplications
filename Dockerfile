FROM maven:3.8.4-jdk-11-slim AS build
COPY .. /home/app/
RUN chmod +x /home/app/mvnw
RUN /home/app/mvnw -f ./proxy-app/pom.xml package -Dmaven.test.skip

FROM openjdk:11.0.13-jre-slim-buster
COPY --from=build /home/app/proxy-app/data-warehouse/target/data-warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/local/lib/data-warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar
COPY --from=build /home/app/proxy-app/data-warehouse/src/main/resources/log4j2.xml /usr/local/lib/log4j2.xml
EXPOSE 8081
ENTRYPOINT ["java","-jar",'-Dlog4j.configurationFile="/usr/local/lib/log4j2.xml"',"/usr/local/lib/data-warehouse-1.0-SNAPSHOT-jar-with-dependencies.jar","port=8081"]