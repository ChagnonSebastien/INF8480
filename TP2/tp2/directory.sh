#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./directory.sh directory_address
	- directory_address: (OPTIONAL) L'addresse ip du service de repertoire de noms.
	  Si l'argument est non fourni, on considere que le service est local (ip_address = 127.0.0.1)

EndOfMessage

IPADDR=$1
if [ -z "$1" ]
  then
    IPADDR="127.0.0.1"
fi

java -cp "$basepath"/directory.jar:"$basepath"/shared.jar \
  -Djava.rmi.server.codebase=file:"$basepath"/shared.jar \
  -Djava.security.manager \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  ca.polymtl.inf8480.tp2.directory.Directory
