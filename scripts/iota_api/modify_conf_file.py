import socket
import ConfigParser

def get_host_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip
cf = ConfigParser.ConfigParser()
cf.read("conf")
cf.set('iota','addr','http://'+get_host_ip()+':14700')
confile = open('conf','wb')
cf.write(confile)
