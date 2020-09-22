#!/usr/bin/env bash

DIR=$(dirname "${BASH_SOURCE[0]}")

source "${DIR}/bank_functions.sh"

trap "pkill -KILL -P $$; wait; exit 255" SIGINT SIGTERM

TMP_DIR=$(config tmpdir)
N_ACCOUNTS=$(config naccounts)
N_OPS=$(config nops)
N_PAR=$(config npar)

random_transfer(){    
    from=$((RANDOM % N_ACCOUNTS))
    to=$((RANDOM % N_ACCOUNTS))
    amount=$((RANDOM))
    transfer ${from} ${to} ${amount}
}

if [[ "$1" == "-create" ]]
then
    if [ "$(config local)" == "false" ]
    then
	k8s_create_all_pods
	gsutil rm -r gs://$(config bucket)/* >&/dev/null # clean bucket
    else
	docker run --rm -d \
	       --env BACKEND=$(config backend) \
       	       --env EVICTION=$(config eviction) \
	       --mount type=bind,source=${TMP_DIR}/bank,destination=/tmp/bank \
	       --net host \
	       0track/transactions:latest > /dev/null
    fi
elif [[ "$1" == "-delete" ]]
then    
    if [ "$(config local)" == "false" ]
    then
    	k8s_delete_all_pods
    fi
    docker kill $(docker ps | grep transaction | awk '{print $1}')
elif [[ "$1" == "-crash" ]]
then    
    if [ "$(config local)" == "false" ]
    then
	info "k8s not supported"
    fi
    docker kill -s KILL $(docker ps | grep transaction | awk '{print $1}')
elif [[ "$1" == "-populate" ]]
then
    create_accounts 0 $((N_ACCOUNTS-1))
elif [[ "$1" == "-clear" ]]
then    
    clear_accounts
elif [[ "$1" == "-run" ]]
then
    for i in $(seq 1 ${N_OPS});
    do
    	random_transfer
    done
    wait
elif [[ "$1" == "-continuous-run" ]]
then
    continuous_transfers ${N_ACCOUNTS}
elif [[ "$1" == "-concurrent-run" ]]
then
    for i in $(seq 1 $((N_OPS/N_PAR)));
    do
	for j in $(seq 1 ${N_PAR});
	do
	    random_transfer &
	done
	wait
    done
elif [[ "$1" == "-check" ]]
then
    total=0
    for i in $(seq 0 $((N_ACCOUNTS-1)));
    do
	balance=$(get_balance $i)
	total=$((total+balance))
    done	
    info "Total=${total}"    
fi

