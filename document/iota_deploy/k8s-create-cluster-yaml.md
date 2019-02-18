<<<<<<< HEAD
# k8så¯åŠ¨é›†ç¾¤æ­¥éª¤ #

## å¯åŠ¨å•ä¸ªé›†ç¾¤ ##

é€šè¿‡yamlåˆ›å»ºé›†ç¾¤ï¼Œæ–°å»ºdeploymentæ–‡ä»¶ï¼Œiota-deploy.yamlå†…å®¹å¦‚ä¸‹:

=======
# k8sÆô¶¯¼¯Èº²½Öè  

## (1)Æô¶¯µ¥¸ö¼¯Èº

Í¨¹ýyaml´´½¨¼¯Èº£¬ÐÂ½¨deploymentÎÄ¼þ£¬iota-deploy.yamlÄÚÈÝÈçÏÂ:
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
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
<<<<<<< HEAD
```  

æ–°å»ºserviceæ–‡ä»¶ï¼Œiota-service.yaml  

=======
    ```  
ÐÂ½¨serviceÎÄ¼þ£¬iota-service.yaml  
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
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
<<<<<<< HEAD
```

åˆ›å»ºé›†ç¾¤ 

```
sudo kubectl create -f iota-deploy.yaml  
sudo kubectl create -f iota-service.yaml
```

æ³¨æ„:  
spec.selector.matchLabels.appå’Œspec.template.metadata.labels.appè¦ä¸€è‡´  
serviceæ–‡ä»¶ä¸­çš„spec.selector.app å’Œdeploymentä¸­labelsè¦å¯¹åº”  

æœ€åŽé€šè¿‡ clusterip å’Œportè®¿é—®é›†ç¾¤

## å¯åŠ¨æœåŠ¡ç›¸äº’è°ƒç”¨çš„ä¸¤ä¸ªé›†ç¾¤ ##

### é›†ç¾¤2è®¿é—®é›†ç¾¤1ä¸­çš„æœåŠ¡æŽ¥å£ ###

åˆ›å»ºé›†ç¾¤1

æ–°å»ºiota_node_rc.yamlæ–‡ä»¶

=======
```  
´´½¨¼¯Èº  
```
sudo kubectl create -f iota-deploy.yaml  
sudo kubectl create -f iota-service.yaml
```  
×¢Òâ:  
spec.selector.matchLabels.appºÍspec.template.metadata.labels.appÒªÒ»ÖÂ  
serviceÎÄ¼þÖÐµÄspec.selector.app ºÍdeploymentÖÐlabelsÒª¶ÔÓ¦  

×îºóÍ¨¹ý clusterip ºÍport·ÃÎÊ¼¯Èº

## (2)Æô¶¯·þÎñÏà»¥µ÷ÓÃµÄÁ½¸ö¼¯Èº
#### ¼¯Èº2·ÃÎÊ¼¯Èº1ÖÐµÄ·þÎñ½Ó¿Ú  
´´½¨¼¯Èº1  
ÐÂ½¨iota_node_rc.yamlÎÄ¼þ
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
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
<<<<<<< HEAD
```

æ–°å»ºiota_node_sc.yaml

=======
```  
ÐÂ½¨iota_node_sc.yaml
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
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
<<<<<<< HEAD
```

åˆ›å»ºé›†ç¾¤2

æ–°å»ºiota_cli_dp.yamlæ–‡ä»¶

=======
```  
´´½¨¼¯Èº2  
ÐÂ½¨iota_cli_dp.yamlÎÄ¼þ  
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
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
<<<<<<< HEAD
```

æ–°å»ºiota_cli_sc.yamlæ–‡ä»¶.

=======
```  
ÐÂ½¨iota_cli_sc.yamlÎÄ¼þ  
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
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
<<<<<<< HEAD
```

åˆ›å»ºé›†ç¾¤ã€‚

```
sudo kubectl create -f iota_cli_dp.yaml;
sudo kubectl create -f iota_cli_sc.yaml;
```

æœ€åŽé€šè¿‡ clusterip å’Œportè®¿é—®é›†ç¾¤.
=======
```  
´´½¨¼¯Èº  
```
sudo kubectl create -f iota_cli_dp.yaml  
sudo kubectl create -f iota_cli_sc.yaml
```  

×îºóÍ¨¹ý clusterip ºÍport·ÃÎÊ¼¯Èº
>>>>>>> 9e0ed38... [fix #129] Creating clusters through yaml files
