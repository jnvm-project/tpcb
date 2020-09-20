#!/usr/bin/env bash

JVMARGS="-Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=${IP} -Djgroups.use.jdk_logger=true -verbose:gc"

CONFIG="default-jgroups-tcp.xml"
LOG_DIR="/tmp/bank"

if [[ "${BACKEND}" == "" ]];

then
    BACKEND="MAP"
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

java -cp ${JAR}:lib/* ${JVMARGS} eu.tsp.transactions.Server -backend ${BACKEND}
