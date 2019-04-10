package vue

import (
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"regexp"
	"strings"
	"testing"
)

var o OCli
const MyURL = "127.0.0.1:14700"
var nodesCache = make([]string,3)
var index int = 0
var number int=1

func TestAddAttestationInfoFunction(t *testing.T) {
	l, err := net.Listen("tcp", MyURL)
	if err != nil {
		log.Fatal(err)
	}
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		if r.Method != "POST" {
			t.Errorf("Except 'Get' got '%s'", r.Method)
		}

		if r.URL.EscapedPath() != "/" {
			t.Errorf("Except to path '/person',got '%s'", r.URL.EscapedPath())
		}

		bodyBytes, _ := ioutil.ReadAll(r.Body)
		handleData(bodyBytes)

	}))

	_ = ts.Listener.Close()
	ts.Listener = l
	ts.Start()
	defer ts.Close()

	bytes := []byte("{\"Attester\":\"192.168.130.102\",\"Attestee\":\"192.168.130.129\",\"Score\":\"1\"}")
	resp := o.AddAttestationInfoFunction(bytes)
	if resp.Code != 1 {
		fmt.Printf("failed to call AddAttestationInfoFunction: %s\n", resp.Message)
		os.Exit(-1)
	}
}

func TestGetRankFunction(t *testing.T) {
	l, err := net.Listen("tcp", MyURL)
	if err != nil {
		log.Fatal(err)
	}
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != "POST" {
			t.Errorf("Except 'Get' got '%s'", r.Method)
		}

		if r.URL.EscapedPath() != "/" {
			t.Errorf("Except to path '/person',got '%s'", r.URL.EscapedPath())
		}

		w.WriteHeader(http.StatusOK)
		w.Header().Set("Content-Type", "application/json")
		str := `{"blocks":"[\"%7B%22tee_num%22%3A1%2C%22tee_content%22%3A%5B%7B%22attester%22%3A%22`+nodesCache[0]+
			`%22%2C%22attestee%22%3A%22`+nodesCache[1]+`%22%2C%22score%22%3A`+nodesCache[2]+`%7D%5D%7D\"]","duration":5}`
		_, _ = io.WriteString(w, str)
	}))

	_ = ts.Listener.Close()
	ts.Listener = l
	ts.Start()
	defer ts.Close()
	bytes := []byte("{\"period\":1,\"numRank\":100}")
	resp := o.GetRankFunction(bytes)
	if resp.Code != 1 {
		fmt.Printf("failed to call GetRankFunction: %s\n", resp.Message)
		os.Exit(-1)
	}

	result:=checkData(resp.Data.DataCtx)
	if result == 1{
		fmt.Println("Data detection correct")
	}else {
       t.Error("Data detection failure")
	}

}

func handleData(bodyBytes []byte){
	nodes := make([]string,15)
	data1 := strings.Split(string(bodyBytes),",")
	nodes = strings.Split(data1[2],"%22")
	reg := regexp.MustCompile(`[%ACD]`)
	data2 := reg.ReplaceAllString(nodes[14], "0")
	nodes[14] = strings.Split(data2,"0")[2]

	if number==1 {
		for k := range nodes {
			if k == 7 || k == 11 || k == 14 {
				nodesCache[index] = nodes[k]
				index++
			}
		}
		number ++
	}else{
		for k := range nodes {
			if k == 7 || k == 11 || k == 14 {
				nodesCache=append(nodesCache,nodes[k])
			}
		}
	}

}

func checkData(a interface{}) int{
	str := fmt.Sprintf("%v", a)
	reg := regexp.MustCompile(`[{\[\]}]`)
	ss := reg.ReplaceAllString(str, " ")
	data := strings.Split(ss," ")
	nodes := make([]string,3)
	j := 0
	for k:=range data{
		if k==2||k==3||k==4{
			nodes[j] = data[k]
			j++
		}
	}

	for i := 0;i<3;i++{
		if nodes[i] != nodesCache[i]{
			return 0
		}
	}
	return 1
}


