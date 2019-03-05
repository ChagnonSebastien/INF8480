#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./balancer.sh remote_balancer_ip
	- remote_balancer_ip: (OPTIONAL) l'addresse ip du repartiteur
	  Si l'arguement est non fourni, on conisdere que le repartiteur est local (ip_address = 127.0.0.1)

EndOfMessage

IPADDR=$1
if [ -z "$1" ]
  then
    IPADDR="127.0.0.1"
fi

java -cp "$basepath"/balancer.jar:"$basepath"/shared.jar \
  -Djava.security.manager \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  ca.polymtl.inf8480.tp2.balancer.Balancer
