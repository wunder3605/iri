#!/bin/sh

cd /code/iota_api/
if $ENABLE_BATCHING|tr '[A-Z]' '[a-z]';then
  sed -i "5s/False/True/g" conf  
fi
python modify_conf_file.py modify_iota_cli_conf
nohup  python chronic_txn_sync.py > synclog 2>&1 &
python app.py
