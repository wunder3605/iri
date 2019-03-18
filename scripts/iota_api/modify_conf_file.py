import socket
import ConfigParser
import yaml
import sys,os
cf = ConfigParser.ConfigParser()
modify_param = sys.argv[1]


def get_host_ip():
    oret = os.popen("/usr/bin/curl ifconfig.io",'r',1)
    return oret.read().replace('\n','')

def parase_param_method():
    with open('ipinfo.txt','r') as f:
        ipdict ={}
        oret = f.readlines()
        for info in oret:
                ippub,ippvt = info.replace('\n','').split(',')
                ipdict[ippub] = ippvt
        return ipdict

def get_pvt_ip():
    ip_pub = get_host_ip();
    ip_total = parase_param_method();
    if ip_pub in ip_total:
        ip_pvt = ip_total[ip_pub]
        return ip_pvt
    else:
        return 'localhost'

def modify_iota_cli_conf():
    cf.read("conf")
    cf.set('iota','addr','http://'+get_pvt_ip()+':14700')
    confile = open('conf','wb')
    cf.write(confile)
    confile.close()
def modify_go_yaml():
    with open('config.yaml') as f:
        content = yaml.safe_load(f)
        content.update({'url': 'http://'+get_pvt_ip()+':14700'})
    with open('config.yaml', 'w') as nf:
        yaml.dump(content, nf,default_flow_style=False)


if __name__ == '__main__':
     if "modify_go_yaml" in modify_param:
         modify_go_yaml()
     elif "modify_iota_cli_conf" in modify_param:
         modify_iota_cli_conf()

