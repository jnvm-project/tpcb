#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

trap "pkill -KILL -P $$; stop; wait; exit 255" SIGINT SIGTERM

source "${DIR}/utils_functions.sh"

TMP_DIR=$(config tmpdir) # FIXME

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
    ${DIR}/test.sh -continuous-run > ${TMP_DIR}/client.log &
    info "done"
}

tput(){
    if [ $# -ne 1 ]; then
        debug "usage: tput length"
        exit -1
    fi
    local length=$1
    local new=1
    for i in $(seq 1 $((10*${length})));
    do
	sleep 0.1
	new=$(grep -ao OK ${TMP_DIR}/client.log | wc -l)
	info "$((new-last))"
	last=${new}
    done &
}

crash(){
    ${DIR}/test.sh -crash > /dev/null
    info "crashed"
}

stop(){
    ${DIR}/test.sh -delete > /dev/null
    info "stopped"
}

exp(){
    if [ $# -ne 3 ]; then
        echo "usage: exp backend naccounts length"
        exit -1
    fi
    backend=$1
    naccounts=$2
    length=$3

    eviction=$((naccounts/10))
    [ $backend == "jnvm" ] && eviction=1
    
    cat ${DIR}/exp.config.tmpl |
        sed s/%BACKEND%/${backend}/g |
        sed s/%NACCOUNTS%/${naccounts}/g |
	sed s/%EVICTION%/${eviction}/g \
            > ${DIR}/exp.config

    rm -fr /pmem{0,1,2,3}/*
    start
    populate
    tput ${length}
    child1=$!
    run
    child2=$!
    sleep $((length/2)); crash
    start
    wait ${child1}
    kill ${child2}; wait
    stop
}

#N_ACCOUNTS=5000000
#N_ACCOUNTS=15000000
N_ACCOUNTS=10000000
LENGTH=120 # in sec.

for b in jnvm sfs mem;
#for b in map mem sfs;
do
    exp ${b} ${N_ACCOUNTS} ${LENGTH} > ${EXP_OUTDIR:-$DIR}/${b}.log
done
