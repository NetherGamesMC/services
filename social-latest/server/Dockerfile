FROM openjdk:17-jdk-slim

EXPOSE 8080/tcp

WORKDIR /home

RUN apt update && apt dist-upgrade -y && apt autoremove -y

COPY target/SocialHub-jar-with-dependencies.jar /home/Server.jar

ENTRYPOINT ["java", "-jar", "Server.jar"]

