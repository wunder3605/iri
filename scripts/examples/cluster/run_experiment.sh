#!/bin/bash

TOPOLOGY=$1
echo $TOPOLOGY
sudo cp ../../iota_deploy/server_deploy_batch.py ./
sudo cp ../../iota_deploy/add_neighbors_batch.py ./
sudo cp conf_info/ipinfo.txt ./
sudo cp conf_info/topology/${TOPOLOGY} topology.txt
python server_deploy_batch.py iri
python server_deploy_batch.py cli $2
python server_deploy_batch.py add
