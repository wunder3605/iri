FROM golang:1.11
RUN apt-get -yqq update && \
 apt-get install -yqq python && \
 apt-get -yqq install python-pip
WORKDIR /go/src/app
VOLUME ["/go/src/app"]
COPY . .
COPY ./entrypoint.sh /go/src/app
RUN go get github.com/awalterschulze/gographviz && \
 go get github.com/kylelemons/go-gypsy/yaml && \
 go get github.com/wunder3605/pagerank
RUN pip install -r requirements.txt
RUN chmod +x modify_yaml_file.py
ENTRYPOINT ["/go/src/app/entrypoint.sh"]
