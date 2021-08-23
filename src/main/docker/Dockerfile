FROM gingerbreadz/jnvm-jdk:prebuilt

ENV NAME=transactions
ENV BUCKET=undefined
ENV BUCKET_KEY=undefined
ENV BUCKET_SECRET=undefined
ENV IP=127.0.0.1
ENV JAR=$NAME-1.0.jar
ENV BACKEND=MAP
ENV EVICTION=0
ENV NUMA_NODE=0
ENV JHEAP_SIZE=20g

RUN set -eux; apt-get update; apt-get install -y --no-install-recommends numactl; rm -rf /var/lib/apt/lists/*

WORKDIR $NAME

COPY target/$JAR .
COPY target/lib lib
COPY src/main/resources/default-jgroups-tcp.xml  .
COPY src/main/resources/default-jgroups-google.xml  .
COPY src/main/bin/run.sh  .
RUN chmod +x .

CMD ["bash", "/transactions/run.sh"]
