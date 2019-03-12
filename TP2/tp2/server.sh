#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./server.sh server_address directory_address
	- server_address: (OPTIONAL) L'addresse ip du serveur.
	  Si l'argument est non fourni, on considere que le serveur est local (ip_address = 127.0.0.1)
	- directory_address: (OPTIONAL) L'addresse ip du service de repertoire de nom.
	  Si l'argument est non fourni, on considere que le service est local (ip_address = 127.0.0.1)

EndOfMessage

IPADDR=$1
if [ -z "$1" ]
  then
    IPADDR="127.0.0.1"
fi

IPADDR2=$2
if [ -z "$2" ]
  then
    IPADDR2="127.0.0.1"
fi

java -cp "$basepath"/server.jar:"$basepath"/shared.jar \
  -Djava.security.policy="$basepath"/policy \
  -Djava.security.manager \
  -Djava.rmi.server.hostname="$IPADDR" \
  ca.polymtl.inf8480.tp2.server.Server $*
