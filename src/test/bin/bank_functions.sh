#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")
source ${DIR}/utils_functions.sh

proxy=$(get_proxy)

create_account(){
    if [ $# -ne 1 ]; then
        debug "usage: create_account id"
        exit -1
    fi
    local id=$1

    res=$(curl -s -X post "${proxy}/${id}")
    log "create_account(${id}) @ ${proxy} -> ${res}"
}


create_accounts(){
    if [ $# -ne 2 ]; then
       debug "usage: create_accounts  start end"
       exit -1
    fi
    local start=$1
    local end=$2

    curl -s -X post "${proxy}/${start}/${end}" > /dev/null
}

get_balance(){
    if [ $# -ne 1 ]; then
        debug "usage: get_balance id"
        exit -1
    fi
    local id=$1
    local res=$(curl -s -X get "${proxy}/${id}")
    log "get_balance(${id}) @ ${proxy} -> ${res}"
    echo ${res}
}

transfer(){
    if [ $# -ne 3 ]; then
        debug "usage: transfer from to amount"
        exit -1
    fi
    local from=$1
    local to=$2
    local amount=$3
    local res=$(curl -s -X put "${proxy}/${from}/${to}/${amount}")
    log "transfer(${from},${to},${amount}) @ ${proxy} -> ${res}"
}

continuous_transfers(){
    if [ $# -ne 1 ]; then
        debug "usage: continuous_transfers #acounts"
        exit -1
    fi

    local accounts=$1
    local max_amount=$(config max_amount)

    # generate request (beforehand)
    local max=50000 # due to ARG_MAX
    local from=0
    local to=1
    local amount=1
    local urls=()
    # start=`date +%s%N`
    for i in $(seq 1 ${max});
    do
	from=$((RANDOM % ${accounts}))
	to=$((RANDOM % ${accounts}))
	amount=$((RANDOM % max_amount))
	urls+=(${proxy}/${from}/${to}/${amount})
    done
    # end=`date +%s%N`
    # echo $(($((end-start))/1000000))

    # execute them
    while true;
    do
	curl -s -X put $(echo ${urls[@]})
    done
}


clear_accounts(){
    if [ $# -ne 0 ]; then
        debug "usage: clear"
        exit -1
    fi
    res=$(curl -s -X post "${proxy}/clear")
    log "create_account(${id}) @ ${proxy} -> ${res}"
}

