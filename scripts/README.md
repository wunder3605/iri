## How to build docker

```bash
docker build -t <name>:<tag> .
```

## How to run docker

```bash
sudo docker run -d -p 5000:5000 -e "ENABLE_BATCHING=$1" -e "HOST_IP=$SSH_CONNECTION" --name ${CLINAME} ${CLINAME}:${TAG} /docker-entrypoint.sh
```
