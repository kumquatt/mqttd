FROM lukasz/docker-scala:latest


RUN git clone https://github.com/xperi/mqttd.git /opt/akka-mqttd

WORKDIR /opt/akka-mqttd

RUN sbt stage

