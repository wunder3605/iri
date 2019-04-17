package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	v "./vue"
)

func main() {
	http.HandleFunc("/AddNode", AddNode)
	http.HandleFunc("/QueryNodes", QueryNodes)
	err := http.ListenAndServe("0.0.0.0:8000", nil)

	if err != nil {
		fmt.Println(err)
	}
}

func AddNode(writer http.ResponseWriter, request *http.Request){
	var o v.OCli
	body, _ := ioutil.ReadAll(request.Body)
	response:=o.AddAttestationInfoFunction(body)
	if err := json.NewEncoder(writer).Encode(response); err != nil {
		fmt.Println(err)
	}
}

func QueryNodes(writer http.ResponseWriter, request *http.Request){
	var o v.OCli
	body, _ := ioutil.ReadAll(request.Body)
	response:=o.GetRankFunction(body)
	if err := json.NewEncoder(writer).Encode(response); err != nil {
		fmt.Println(err)
	}
}
