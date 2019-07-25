#!/bin/sh

cd /code/iota_api/

if $ENABLE_BATCHING;then
  sed -i "5s/False/True/g" conf  
fi
if true
then
  hostip=$(echo $HOST_IP|awk '{print $3}')
  echo $hostip
  sed -i "2s/localhost/${hostip}/g" conf
fi

python app.py
