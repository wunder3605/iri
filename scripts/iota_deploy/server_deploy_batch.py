#!/usr/bin/python
# -*- coding: utf-8 -*-
from  subprocess import  call,Popen,PIPE,STDOUT
import sys
import re

def get_ip_list():
    ipdict = {}
    with open("ipinfo.txt",'r') as f:
        oret = f.readlines()
        for info in oret:
                ippub,ippvt = info.replace('\n','').split(',')
                ipdict[ippvt] = ippub
        return ipdict

#iri deploy
def deploy_iri_server():
    ip_total = get_ip_list()
    ip_pub = list(ip_total.values())
    for ip_address in ip_pub:
        oret = Popen(["/usr/local/bin/pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker ps |grep iota-node |wc -l "], shell=False, stdout=PIPE, stderr=STDOUT)
        num_exist = oret.stdout.readlines()[1].strip()
        if int(num_exist):
            call(["/usr/local/bin/pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb"])
            call(["/usr/local/bin/pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb.log"])
            call(["/usr/local/bin/pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker stop iota-node"])
            call(["/usr/local/bin/pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker rm iota-node"])
            call(["/usr/local/bin/pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo docker run  -d -p 14700:14700 -p 13700:13700 --name iota-node -v /home/trust/iri/scripts/examples/data:/iri/data -v /home/trust/iri/scripts/examples/conf/neighbors iota-node:v0.1-streamnet  /entrypoint.sh"])
        else:
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb"])
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb.log"])
            call(["pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker rm iota-node"])
            call(["pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker run  -d -p 14700:14700 -p 13700:13700 --name iota-node -v /home/trust/iri/scripts/examples/data:/iri/data -v /home/trust/iri/scripts/examples/conf/neighbors iota-node:v0.1-streamnet  /entrypoint.sh"])
    return 'success'

def clear_iri_server():
    ip_total = get_ip_list()
    ip_pub = list(ip_total.values())
    for ip_address in ip_pub:
        oret = Popen(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker ps |grep iota-node |wc -l "], shell=False, stdout=PIPE, stderr=STDOUT)
        num_exist = oret.stdout.readlines()[1].strip()
        if int(num_exist):
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb"])
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb.log"])
            call(["pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker stop iota-node"])
            call(["pssh", "-i", "-H", "trust@"+ip_address, "-x", "\"-oStrictHostKeyChecking=no\"", "sudo docker rm iota-node"])
        else:
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb"])
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo rm -rf ~/iri/scripts/examples/data/testnetdb.log"])
    return 'success'

#cli deploy
def deploy_cli_server():
    ip_total = get_ip_list()
    ip_pub = list(ip_total.values())
    for ip_address in ip_pub:
        oret = Popen(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo docker ps |grep iota-cli |wc -l "], shell=False, stdout=PIPE,stderr=STDOUT)
        num_exist = oret.stdout.readlines()[1].strip()
        if int(num_exist):
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo docker stop iota-cli"])
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo docker rm iota-cli"])
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo docker run -d -p 5000:5000 --name iota-cli iota-cli:v0.1-streamnet /docker-entrypoint.sh"])
        else:
            call(["pssh", "-i", "-H", "trust@" + ip_address, "-x", "\"-oStrictHostKeyChecking=no\"","sudo docker run -d -p 5000:5000 --name iota-cli iota-cli:v0.1-streamnet /docker-entrypoint.sh"])
    return 'success'

# add and remove neighbors
def link_iri_server(input_data):
    with open("topology.txt", 'r') as list_file:
        ip_list = list_file.read()
        moudle = re.compile(r'(?:(?:[0,1]?\d?\d|2[0-4]\d|25[0-5])\.){3}(?:[0,1]?\d?\d|2[0-4]\d|25[0-5]):\d{0,5}')
        result = re.findall(moudle, ip_list)
        add_ip = [x for x in result[::2]]
        desc_ip = [x for x in result[1::2]]
        num = len(add_ip)
        for i in range(0,num):
            k = add_ip[i].replace('\'','')
            v = desc_ip[i].replace('\'','')
            Popen(['/usr/bin/python', 'add_neighbors_batch.py', input_data, k, v])
        return 'success'



if __name__ == '__main__':
    input_p = sys.argv
    if input_p[1] == 'iri':
        deploy_iri_server()
    elif input_p[1] == 'cli':
        deploy_cli_server()
    elif input_p[1] == 'clear':
        clear_iri_server()
    elif input_p[1] in ['add','remove']:
        link_iri_server(input_p[1])
