from __future__ import print_function
import sys
sys.path.append("..")
import ConfigParser
import time
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


# txs buffer. dequeue is thread-safe
txn_cache = deque()

# timer interval
TIMER_INTERVAL = 20

BATCH_SIZE = 100

COMPRESSED_SIZE = 7

lock = threading.Lock()
def get_cache():
    # timer
    global timer_thread
    timer_thread = threading.Timer(TIMER_INTERVAL, get_cache)
    timer_thread.start()

    global lock

    with lock:
        nums = min(len(txn_cache), BATCH_SIZE)
        if nums == 0:
            return

        all_txs = ""
        all_trytes = ""
        for i in range(nums):
            tx = txn_cache.popleft()
            all_txs += tx
            if (i + 1) % COMPRESSED_SIZE == 0 or i == nums -1:
                out = StringIO.StringIO()
                with gzip.GzipFile(fileobj=out, mode="w") as f:
                    f.write(all_txs)
                compressed_data = out.getvalue()
                trytes = TryteString.from_bytes(compressed_data).__str__()
                if len(trytes) < 2187:
                    trytes += '9' * (2187 - len(trytes))
                all_trytes += trytes
                all_txs = ""
        cache.cache_txn_in_tangle_message(all_trytes)
        print("[INFO]Cache data in tangle, the tangle tag is %s." % (TagGenerator.get_current_tag("TR")), file=sys.stderr)


app = Flask(__name__)

cf = ConfigParser.ConfigParser()
cf.read("conf")
iota_addr = cf.get("iota", "addr")
iota_seed = cf.get("iota", "seed")
cache = IotaCache(iota_addr, iota_seed)


@app.route('/')
def hello_world():
    return 'Hello World!'

@app.route('/put_file', methods=['POST'])
def put_file():
    req_json = request.get_json()

    if req_json is None:
        return 'error'

    req_json["timestamp"] = str(time.time())

    cache.cache_txn_in_tangle_message(TryteString.from_string(json.dumps(req_json, sort_keys=True)).__str__())

    print("[INFO]Cache data in tangle, the tangle tag is %s." % (TagGenerator.get_current_tag("TR")), file=sys.stderr)

    return 'ok'

@app.route('/put_cache', methods=['POST'])
def put_cache():
    # get json
    req_json = request.get_json()
    if req_json is None:
        return 'error'

    req_json["timestamp"] = str(time.time())

    tx_string = json.dumps(req_json, sort_keys=True)

    # cache in local ring-buffer
    txn_cache.append(tx_string)

    if len(txn_cache) >= BATCH_SIZE:
        # ring-buffer is full, send to ipfs and iota directly.
        threading.Thread(target=get_cache).start()

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
