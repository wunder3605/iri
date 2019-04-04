package vue

import (
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
)

var o OCli
const MY_URL = "127.0.0.1:14700"
var str []string = make([]string, 3)

func TestAddAttestationInfoFunction(t *testing.T) {
	l, err := net.Listen("tcp", MY_URL)
	if err != nil {
		log.Fatal(err)
	}
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		//w.Write(weatherRespBytes)
		if r.Method != "POST" {
			t.Errorf("Except 'Get' got '%s'", r.Method)
		}

		if r.URL.EscapedPath() != "/" {
			t.Errorf("Except to path '/person',got '%s'", r.URL.EscapedPath())
		}

		bodyBytes, _ := ioutil.ReadAll(r.Body)
		fmt.Printf("received: %s", string(bodyBytes))
		str = append(str, string(bodyBytes))
		/*personList := make([]Person,0)
		err = json.Unmarshal(bodyBytes, &personList)
		if err != nil {
			logs.Error("decode data fail")
			return []Person{}, fmt.Errorf("decode data fail")
		}*/
	}))

	_ = ts.Listener.Close()
	ts.Listener = l
	ts.Start()
	defer ts.Close()

	bytes := []byte("{\"Attester\":\"192.168.130.102\",\"Attestee\":\"192.168.130.129\",\"Score\":\"1\"}")
	resp := o.AddAttestationInfoFunction(bytes)
	if resp.Code != 1 {
		fmt.Println("ERR")
		os.Exit(-1)
	}
}

func TestGetRankFunction(t *testing.T) {
	l, err := net.Listen("tcp", MY_URL)
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
		str := "{\"tee_num\":1,\"tee_content\":[{\"attester\":\"192.168.130.102\",\"attestee\":\"192.168.130.129\",\"score\":1}]}"
		_, _ = w.Write([]byte(str))
	}))

	_ = ts.Listener.Close()
	ts.Listener = l
	ts.Start()
	defer ts.Close()

	bytes := []byte("{\"period\":1,\"numRank\":100}")
	resp := o.GetRankFunction(bytes)
	if resp.Code != 1 {
		fmt.Printf("ERR: %s", resp.Message)
		os.Exit(-1)
	} else {
		fmt.Printf("msg: %s", resp.Data)
	}
}