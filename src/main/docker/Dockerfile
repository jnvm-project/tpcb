FROM openjdk:8-slim

ENV NAME=transactions
ENV BUCKET=undefined
ENV BUCKET_KEY=undefined
ENV BUCKET_SECRET=undefined
ENV IP=127.0.0.1
ENV JAR=$NAME-1.0.jar
ENV BACKEND=MAP
ENV EVICTION=0
ENV JAVA_HOME=/jdk8u/build/linux-x86_64-normal-server-release/jdk

WORKDIR $NAME

COPY jdk8u /jdk8u
COPY target/$JAR .
COPY target/lib lib
COPY src/main/resources/default-jgroups-tcp.xml  .
COPY src/main/resources/default-jgroups-google.xml  .
COPY src/main/bin/run.sh  .
RUN chmod +x .

CMD ["bash", "/transactions/run.sh"]
