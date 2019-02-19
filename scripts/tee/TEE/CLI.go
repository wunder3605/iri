package TEE

import (
    "fmt"
    "net/http"
    "os"
    "flag"
    "log"
    "strings"
)

type CLI struct {}
var url = "http://localhost:14266"

func printUsage()  {

    fmt.Println("Usage:")
    fmt.Println("\taddattestationinfo -info <detail> -- add attestation information.")
    fmt.Println("\tgetrank -num <NUMBER> -period <WHICH_PERIOD> -- get TEE rank of a specific period.")
    fmt.Println("\tprinthcgraph -period <WHICH_PEIOD> -- output HCGraph information.")

}

func isValidArgs()  {
    if len(os.Args) < 2 {
        printUsage()
        os.Exit(1)
    }
}

func (cli *CLI) addAttestationInfo(info []string)  {
//storeMessage
//http.PostForm
    _, err := http.Get(url + "?command=storeMessage&storeMessage&address="+ info[0] + "&message=" + info[1])
    if err != nil {
        //request error
        log.Fatal(err)
    }
}

func (cli *CLI) getRank(num string, period []string)  {
    _, err := http.Get(url + "?command=getBlocksInPeriodStatement&period=" + period[0])
    if err != nil {
        //request error
        log.Fatal(err)
    }
}

func (cli *CLI) printHCGraph(period string){
    _, err := http.Get(url + "?command=getBlocksInPeriodStatement&period=" + period)
    if err != nil {
        //request error
        log.Panic(err)
    }
}

func (cli *CLI) Run()  {

    isValidArgs()

    addAttestationInfoCmd := flag.NewFlagSet("taddattestationinfo",flag.ExitOnError)
    getRankCmd := flag.NewFlagSet("getrank",flag.ExitOnError)
    printHCGraphCmd := flag.NewFlagSet("printhcgraph",flag.ExitOnError)

    flagInfo := addAttestationInfoCmd.String("info","","TEE info......")
    flagNum := getRankCmd.String("num","","number of ranked nodes......")
    flagPeriod := getRankCmd.String("period","","which period to rank......")
    flagPeriod1 := printHCGraphCmd.String("period","","which period to rank......")

    switch os.Args[1] {
    case "taddattestationinfo":
        err := addAttestationInfoCmd.Parse(os.Args[2:])
        if err != nil {
            log.Panic(err)
        }
    case "getrank":
        err := getRankCmd.Parse(os.Args[2:])
        if err != nil {
            log.Panic(err)
        }
    case "printhcgraph":
        err := printHCGraphCmd.Parse(os.Args[2:])
        if err != nil {
            log.Panic(err)
        }
    default:
        printUsage()
        os.Exit(1)
    }

    if addAttestationInfoCmd.Parsed() {
        if *flagInfo == ""{
            printUsage()
            os.Exit(1)
        }

        info := strings.Split(*flagInfo,",")
        cli.addAttestationInfo(info)
    }

    if getRankCmd.Parsed() {
        if *flagNum == "" ||  *flagPeriod == "" {
            printUsage()
            os.Exit(1)
        }
        period := strings.Split(*flagPeriod, " ")
        cli.getRank(*flagNum, period)
    }

    if printHCGraphCmd.Parsed() {
        if *flagPeriod1 == "" {
            printUsage()
            os.Exit(1)
        }
        period := *flagPeriod1
        cli.printHCGraph(period)
    }
}