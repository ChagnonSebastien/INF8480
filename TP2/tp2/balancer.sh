#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./balancer.sh secure_mode balancer_ip directory_ip
	- secure_mode: le mode de securite du repartiteur (true:securise|false:non securise)
	- balancer_ip: (OPTIONAL) l'addresse ip du repartiteur
	  Si l'argument est non fourni, on considere que le repartiteur est local (ip_address = 127.0.0.1)
	- directory_ip: (OPTIONAL) l'addresse ip du service de repertoire de noms
	  Si l'argument est non fourni, on considere que le service est local (ip_address = 127.0.0.1)

EndOfMessage

IPADDR=$2
if [ -z "$2" ]
  then
    IPADDR="127.0.0.1"
fi

IPADDR2=$3
if [ -z "$3" ]
  then
    IPADDR2="127.0.0.1"
fi

java -cp "$basepath"/balancer.jar:"$basepath"/shared.jar \
  -Djava.security.manager \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  ca.polymtl.inf8480.tp2.balancer.Balancer $*
