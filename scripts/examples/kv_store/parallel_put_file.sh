#!/bin/bash
curl -k -s http://localhost:5000/put_file -X POST -H "Content-Type: application/json" -d @usr1_v1.json 
sleep 1
curl -k -s http://localhost:6000/put_file -X POST -H "Content-Type: application/json" -d @usr1_v2.json
sleep 1
curl -k -s http://localhost:5000/put_file -X POST -H "Content-Type: application/json" -d @usr2_v1.json
sleep 1
curl -k -s http://localhost:6000/put_file -X POST -H "Content-Type: application/json" -d @usr2_v2.json
sleep 1
curl -k -s http://localhost:5000/put_file -X POST -H "Content-Type: application/json" -d @usr1_sale_10.json &
curl -k -s http://localhost:6000/put_file -X POST -H "Content-Type: application/json" -d @usr1_sale_11.json
