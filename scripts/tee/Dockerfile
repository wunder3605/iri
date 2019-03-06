FROM golang:1.11

WORKDIR /go/src/app
COPY . .
COPY ./entrypoint.sh /go/src/app 

RUN go get github.com/awalterschulze/gographviz 
RUN go get github.com/kylelemons/go-gypsy/yaml 
RUN go get github.com/wunder3605/pagerank 

#CMD ["sh entrypoint.sh"]
ENTRYPOINT ["/go/src/app/entrypoint.sh"]

