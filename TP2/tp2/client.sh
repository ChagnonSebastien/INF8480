#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./client.sh ops_filename remote_balancer_ip
	- ops_filename: le nom du fichier qui contient les operations
	- remote_balancer_ip: (OPTIONAL) l'addresse ip du repartiteur

EndOfMessage

java -cp "$basepath"/client.jar:"$basepath"/shared.jar -Djava.security.policy="$basepath"/policy ca.polymtl.inf8480.tp2.client.Client $*
