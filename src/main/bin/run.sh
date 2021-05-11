#!/usr/bin/env bash

JVMARGS="-Xmx20g -XX:+UseG1GC -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Djgroups.use.jdk_logger=true -verbose:gc"

CONFIG="default-jgroups-tcp.xml"
LOG_DIR="/tmp/bank"

if [[ "${BACKEND}" == "" ]];
then
    BACKEND="MAP"
fi

if [[ "${EVICTION}" == "" ]];
then
    EVICTION=0
fi

if [[ "${IP}" != "127.0.0.1" ]]
then
    CONFIG=default-jgroups-google.xml
    cat ${CONFIG} \
    | sed s,%BUCKET%,${BUCKET},g \
    | sed s,%BUCKET_KEY%,${BUCKET_KEY},g \
    | sed s,%BUCKET_SECRET%,${BUCKET_SECRET},g \
    | sed s,%IP%,${IP},g > tmp
    mv tmp ${CONFIG}
fi

mv ${CONFIG} jgroups.xml

trap stopstart SIGHUP
child=""

stopstart() {
    [ ! -z $child ] && kill -KILL $child && wait $child
    run
}

run() {
    $JAVA_HOME/bin/java -cp ${JAR}:lib/* ${JVMARGS} eu.tsp.transactions.Server -backend ${BACKEND} -eviction ${EVICTION} &
    child=$!
}

run
wait $child
