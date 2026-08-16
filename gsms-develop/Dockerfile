FROM eclipse-temurin:17-jdk

WORKDIR /home

RUN apt update && apt-get install -y wget unzip && apt dist-upgrade -y && apt autoremove -y

ADD server/target/GSMS.jar /home

ENTRYPOINT ["java", "-jar", "GSMS.jar"]
