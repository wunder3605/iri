import json
import urllib2
import sys

url = "http://localhost:14266"
folder = './'


#Utils:
all_nines = '9' * 81
TIMEOUT = 60
def API(request,url=url):

    stringified = json.dumps(request)
    headers = {'content-type': 'application/json', 'X-IOTA-API-Version': '1'}

    try:
        request = urllib2.Request(url=url, data=stringified, headers=headers)
        returnData = urllib2.urlopen(request, timeout=TIMEOUT).read()
        response = json.loads(returnData)

    except:
        print url, "Timeout!"
        print '\n    ' + repr(sys.exc_info())
        return ""
    if not response:
        response = ""
    return response

def getNodeInfo(url):
    cmd = {
        "command": "getNodeInfo"
    }
    return API(cmd, url)

def getTrytes(url, hash):
    cmd = {
        "command": "getTrytes",
        "hashes" : [hash]
    }
    return API(cmd, url)

def attachToTangle(url, trunk_tx, branch_tx, mwm, unattached_trytes):
    cmd = {
        "command": "attachToTangle",
        "trunkTransaction": trunk_tx,
        "branchTransaction": branch_tx,
        "minWeightMagnitude": mwm,
        "trytes": [unattached_trytes]
    }
    return API(cmd, url)

def storeTransactions(url, attached_trytes):
    cmd = {
        "command": "storeTransactions",
        "trytes": [attached_trytes]
    }
    return API(cmd, url)

def broadcastTransactions(url, attached_trytes):
    cmd = {
        "command": "broadcastTransactions",
        "trytes": [attached_trytes]
    }
    return API(cmd, url)

def findTransactions(url, tags):
    cmd = {
        "command": "findTransactions",
        "tags": [tags]
    }
    return API(cmd, url)

def getTransactionsToApprove(url):
    cmd = {
        "command": "getTransactionsToApprove",
        "depth" : 10 
    }
    return API(cmd, url)

def storeMessage(url, address, message, tag):
    cmd = {
        "command": "storeMessage",
        "address": address,
        "message": message,
        "tag": tag
    }
    return API(cmd, url)

def getBalance(url, address, coinType):
    cmd = {
        "command": "getBalances",
        "cointype": coinType
    }
    return API(cmd, url)
