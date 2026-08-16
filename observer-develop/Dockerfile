FROM eclipse-temurin:17-jdk

EXPOSE 8080/tcp

WORKDIR /home

RUN apt update && apt dist-upgrade -y && apt autoremove -y


RUN apt-get install -y wget unzip && apt dist-upgrade -y && apt autoremove -y

RUN wget https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2024.3-docker.zip -P /tmp/ && \
  unzip /tmp/YourKit-JavaProfiler-2024.3-docker.zip -d /usr/local && \
  rm /tmp/YourKit-JavaProfiler-2024.3-docker.zip


COPY server/target/Observer.jar /home/Observer.jar

ENTRYPOINT ["java", "-agentpath:/usr/local/YourKit-JavaProfiler-2024.3/bin/linux-x86-64/libyjpagent.so=broker_url=https://broker.yourkit.com/q4U3Mr3er4tQ3v2UYQuc/,broker_token=8MrpMyFGQCjWQuCN9j2Z", "-XX:+AlwaysPreTouch", "-jar", "Observer.jar"]