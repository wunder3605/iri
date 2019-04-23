#!/bin/bash

TOPOLOGY=$1
echo $TOPOLOGY
sudo cp ../../iota_deploy/server_deploy_batch.py ./
python server_deploy_batch.py iri
python server_deploy_batch.py cli true
sudo cp conf_info/topology/${TOPOLOGY} topology.txt
python server_deploy_batch.py add
