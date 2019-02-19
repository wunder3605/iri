from __future__ import print_function
import sys
sys.path.append("..")
import ConfigParser
import commands
import threading
import json
from flask import Flask, request
from iota_cache.iota_cache import IotaCache
from tag_generator import TagGenerator
from collections import deque
import StringIO
import gzip
from iota import TryteString

cf = ConfigParser.ConfigParser()
cf.read("conf")
iota_addr = cf.get("iota", "addr")
iota_seed = cf.get("iota", "seed")
enable_ipfs = cf.getboolean("iota", "enableIpfs")
enable_compression = cf.getboolean("iota", "enableCompression")
enable_batching = cf.getboolean("iota", "enableBatching")
cache = IotaCache(iota_addr, iota_seed)

# txs buffer. dequeue is thread-safe
txn_cache = deque()
TIMER_INTERVAL = 20
BATCH_SIZE = 20
COMPRESSED_SIZE = 7

cache_lock = threading.Lock()
lock = threading.Lock()


if (enable_ipfs == True and enable_compression == True) or (enable_batching == False and enable_compression == True):
    print("Error configure!", file=sys.stderr)
    sys.exit(-1)

def compress_str(data):
    if enable_compression == True:
        out = StringIO.StringIO()
        with gzip.GzipFile(fileobj=out, mode="w") as f:
            f.write(data)
        compressed_data = out.getvalue()
        return TryteString.from_bytes(compressed_data).__str__()
    else:
        return data

def send(tx_string, tx_num=1, tag='TR'):
    if enable_ipfs == True:
        send_to_ipfs_iota(tx_string, tx_num, tag)
    else:
        print(tag,file=sys.stderr)
        send_to_iota(tx_string, tx_num, tag)

def send_to_ipfs_iota(tx_string, tx_num, tag):
    global lock
    with lock:
        filename = 'json'
        f = open(filename, 'w')
        f.write(tx_string)
        f.flush()
        f.close()

        ipfs_hash = commands.getoutput(' '.join(['ipfs', 'add', filename, '-q']))
        print("[INFO]Cache json %s in ipfs, the hash is %s." % (tx_string, ipfs_hash), file=sys.stderr)

        if tx_num == 1:
            data = ipfs_hash
        else:
            data = json.dumps({"address": ipfs_hash, "tx_num": tx_num}, sort_keys=True)

        cache.cache_txn_in_tangle_simple(data, TagGenerator.get_current_tag(tag))
        print("[INFO]Cache hash %s in tangle, the tangle tag is %s." % (ipfs_hash, TagGenerator.get_current_tag("TR")), file=sys.stderr)

def send_to_iota(tx_string, tx_num, tag):
    global lock
    with lock:
        data = json.dumps({"txn_content": tx_string, "tx_num": tx_num}, sort_keys=True)

        if enable_batching is False:
            cache.cache_txn_in_tangle_simple(data, TagGenerator.get_current_tag(tag))
        else:
            compressed_data = compress_str(data)
            cache.cache_txn_in_tangle_message(compressed_data)

        print("[INFO]Cache data in tangle, the tangle tag is %s." % (TagGenerator.get_current_tag(tag)), file=sys.stderr)

def get_cache():
    if enable_batching is False:
        return

    # timer
    global timer_thread
    timer_thread = threading.Timer(TIMER_INTERVAL, get_cache)
    timer_thread.start()

    global cache_lock
    with cache_lock:
        nums = min(len(txn_cache), BATCH_SIZE)
        if nums == 0:
            return

        list = []
        for i in range(nums):
            tx = txn_cache.popleft()
            list.append(tx)

    all_txs = json.dumps(list)
    send(all_txs, nums)


app = Flask(__name__)


@app.route('/')
def hello_world():
    return 'Hello World!'

@app.route('/get_balance', methods=['GET'])
def get_balance():
    cache.get_balance('StreamNetCoin')
    return 'ok'

@app.route('/put_file', methods=['POST'])
def put_file():
    req_json = request.get_json()

    if req_json is None:
        return 'error'

    if req_json[u'tag'] is None: 
        send(json.dumps(req_json, sort_keys=True))
    else:
        send(json.dumps(req_json, sort_keys=True), tag=req_json[u'tag'])

    return 'ok'

@app.route('/put_cache', methods=['POST'])
def put_cache():
    if enable_batching is False:
        return 'error'

    req_json = request.get_json()
    if req_json is None:
        return 'error'

    tx_string = json.dumps(req_json, sort_keys=True)

    # cache in local ring-buffer
    txn_cache.append(tx_string)

    if len(txn_cache) >= BATCH_SIZE:
        # ring-buffer is full, send to ipfs and iota directly.
        t = threading.Thread(target=get_cache)
        t.start()

    return 'ok'

@app.route('/post_contract', methods=['POST'])
def post_contract():
    req_json = request.get_json()

    if req_json is None:
        return 'request error'
    print("now come here to post contract")

    cache.cache_txn_in_tangle_simple(req_json['ipfs_addr'], TagGenerator.get_current_tag("SC"))
    return 'ok'

@app.route('/post_action', methods=['POST'])
def post_action():
    req_json = request.get_json()

    if req_json is None:
        return 'request error'

    cache.cache_txn_in_tangle_simple(req_json['ipfs_addr'], TagGenerator.get_current_tag("SA"))
    return 'ok'

@app.route('/put_contract', methods=['PUT'])
def put_contract():
    req_json = request.get_json()

    if req_json is None:
        return 'request error'

    msg = Fragment(TryteString(req_json['ipfs_addr']))
    ipfs_addr = msg.decode()
    wasm.set_contract(ipfs_addr)
    return 'ok'

@app.route('/put_action', methods=['PUT'])
def put_action():
    req_json = request.get_json()

    if req_json is None:
        return 'request error'

    msg = Fragment(TryteString(req_json['ipfs_addr']))
    ipfs_addr = msg.decode()
    wasm.exec_action(ipfs_addr)
    return 'ok'

if __name__ == '__main__':
    get_cache()
    app.run()
