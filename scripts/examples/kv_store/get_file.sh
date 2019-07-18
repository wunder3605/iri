#!/bin/bash
curl -k -s http://localhost:5000/get_file -X POST -H "Content-Type: application/json" -d @usr1_req.json 
echo ""
sleep 1
curl -k -s http://localhost:6000/get_file -X POST -H "Content-Type: application/json" -d  @usr2_req.json
echo ""
sleep 1
curl -k -s http://localhost:5000/get_file -X POST -H "Content-Type: application/json" -d  @usr1_sale_req.json
echo ""
