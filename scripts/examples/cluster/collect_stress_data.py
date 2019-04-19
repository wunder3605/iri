#!/usr/bin/python
# -*- coding: utf-8 -*-
import os
import csv
import sys

log_file = sys.argv[1]
file_name = sys.argv[2]
total_topology = ['3_clique','4_circle','4_clique','7_circle','7_clique','7_bridge','7_star']
txn_num = [5000,10000,15000,20000]

total_num = []
cmd = "cat %s |grep -v 15000 |grep -v configure|grep 5000 |awk '{print $7}'"%log_file
oret = os.popen(cmd,'r',1).read().split()
i = 0
for tps in oret:
    tps_num = tps.replace('/s','')
    total_num.append([5000,tps_num,total_topology[i]])
    i+=1

for tx_nm in txn_num[1:]:
    cmd = "cat %s |grep -v configure|grep %s |awk '{print $7}'"%(log_file,tx_nm)
    oret = os.popen(cmd, 'r', 1).read().split()
    i = 0
    for tps in oret:
        tps_num = tps.replace('/s', '')
        total_num.append([tx_nm, tps_num, total_topology[i]])
        i += 1

with open(file_name,"w") as csvfile:
      writer = csv.writer(csvfile)
      writer.writerow(['num_txn','TPS','cluster_size'])
      writer.writerows(sorted(total_num,key=lambda stu:stu[2]))
