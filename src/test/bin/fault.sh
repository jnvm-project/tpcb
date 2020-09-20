#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

trap "pkill -KILL -P $$; stop; exit 255" SIGINT SIGTERM

source "${DIR}/utils_functions.sh"

TMP_DIR=$(config tmpdir)

if [ "$(config local)" == "false" ]
then
    info "k8s not supported"
fi

start(){
    info "starting"
    ${DIR}/test.sh -create
    local id=$(docker ps | grep transaction | awk '{print $1}')
    local up=0
    while [ ${up} != 1 ];
    do
    	up=$(2>&1 docker logs ${id} | grep "Server - Started" | wc -l);
    done
    info "started"
}

populate(){
    ${DIR}/test.sh -populate
    info "populated"
}

run(){    
    ${DIR}/test.sh -run > ${TMP_DIR}/client.log
    info "done"
}

tput(){
    local new=1
    while true;
    do
	sleep 1
	new=$(grep -o OK ${TMP_DIR}/client.log | wc -l)
	info "$((new-last))"
	last=${new}
    done &
}

crash(){
    ${DIR}/test.sh -crash > /dev/null
    info "crashed"
}

stop(){
    pkill -TERM -P $$ # FIXME
    ${DIR}/test.sh -delete > /dev/null
    info "stopped"
}

exp(){
    if [ $# -ne 3 ]; then
        echo "usage: exp backend naccounts nops"
        exit -1
    fi
    backend=$1
    naccounts=$2
    nops=$3

    cat ${DIR}/exp.config.tmpl |
        sed s/%BACKEND%/${backend}/g |
        sed s/%NACCOUNTS%/${naccounts}/g |
        sed s/%NOPS%/${nops}/g \
            > ${DIR}/exp.config

    start
    populate
    tput
    run &
    child=$!
    sleep 10; crash
    start
    wait ${child}
    stop    
}

N_ACCOUNTS=50000
N_OPS=500000

for b in map mem sfs;
do
    exp ${b} ${N_ACCOUNTS} ${N_OPS} > ${DIR}/${b}.log
done
