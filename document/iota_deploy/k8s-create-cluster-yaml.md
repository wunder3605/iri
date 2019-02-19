# k8s������Ⱥ����  

## (1)����������Ⱥ

ͨ��yaml������Ⱥ���½�deployment�ļ���iota-deploy.yaml��������:
```
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
name: trias-cli-deployment
  labels:
    app: trias-cli
spec:
  replicas: 1
  selector:
    matchLabels:
      app: trias-cli
  template:
    metadata:
      labels:
        app: trias-cli
    spec:
      restartPolicy: Always
      containers:
      - name: trias-cli
        image: 172.31.23.215:5000/trias-cli:StreamNet_v1.0.6 
    ```  
�½�service�ļ���iota-service.yaml  
```
apiVersion: v1
kind: Service
metadata:
  name: trias-cli-service
spec:
    ports:
      - port: 4999
        targetPort: 4999
        name: "trias-cli"
        protocol: TCP
    selector:
        app: trias-cli
    externalIPs:
    - 172.31.28.12
```  
������Ⱥ  
```
sudo kubectl create -f iota-deploy.yaml  
sudo kubectl create -f iota-service.yaml
```  
ע��:  
spec.selector.matchLabels.app��spec.template.metadata.labels.appҪһ��  
service�ļ��е�spec.selector.app ��deployment��labelsҪ��Ӧ  

���ͨ�� clusterip ��port���ʼ�Ⱥ

## (2)���������໥���õ�������Ⱥ
#### ��Ⱥ2���ʼ�Ⱥ1�еķ���ӿ�  
������Ⱥ1  
�½�iota_node_rc.yaml�ļ�
```
apiVersioe: v1
kind: ReplicationController
metadata:
  name: iota-node
spec:
  replicas: 1
  selector:
    app: iota-node
  template:
    metadata:
      labels:
        app: iota-node
    spec:
      containers:
      - name: iota-node
        image: stplaydog/iota-node:StreamNet_v1.0
        ports:
        - containerPort: 14700
        env:
        - name: API_PORT
          value: "14700"
        - name: UDP_PORT
          value: "13600"
        - name: TCP_PORT
          value: "13600"
```  
�½�iota_node_sc.yaml
```
apiVersion: v1

kind: Service
metadata:
  name: iota-node
spec:
  ports:
  - name: iota-node
    port: 14700
    targetPort: 14700
    nodePort: 31000
  selector:
    app: iota-node
  type: NodePort
```  
```
sudo kubectl create -f iota_cli_dp.yaml  
sudo kubectl create -f iota_cli_sc.yaml
```  
������Ⱥ2  
�½�iota_cli_dp.yaml�ļ�  
```
apiVersion: v1
kind: ReplicationController
metadata:
  name: iota-cli
spec:
  replicas: 1
  selector:
    app: iota-cli
  template:
    metadata:
      labels:
        app: iota-cli
    spec:
      containers:
        - name: iota-cli
          image: 172.31.23.215:5000/trias-cli:StreamNet_v1.0.6
          ports:
          - containerPort: 4999
          env:
          - name: IOTA_NODE_SERVICE_HOST
            value: '192.16.30.12'
          - name: IOTA_NODE_SERVICE_PORT
            value: '14700'
```  
�½�iota_cli_sc.yaml�ļ�  
```
apiVersion: v1
kind: Service
metadata:
  name: iota-cli
spec:
  type: NodePort
  ports:
    - port: 4999
      nodePort: 31499
  selector:
    app: iota-cli
```  
������Ⱥ  
```
sudo kubectl create -f iota_cli_dp.yaml  
sudo kubectl create -f iota_cli_sc.yaml
```  

���ͨ�� clusterip ��port���ʼ�Ⱥ