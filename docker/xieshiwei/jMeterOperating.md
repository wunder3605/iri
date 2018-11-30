# 1. Download and install jMeter
  You can download the archive from the official website, and then use the decompression command to extract it to the corresponding file.After decompression, go to /jmeter/bin in the decompression directory.Empower jmeter.sh with the command: chmod 777 jmeter.sh(If there are insufficient permissions, add sudo in front of the command).Then use the command: sh jmeter.sh -v to check if it is available.
  
# 2. Start jMeter
  * The command to start jMeter is: java -jar ApacheJMeter.jar
  * A graphical interface will appear after successful launch of jMeter
  
# 3. New test plan
  * Steps:Click file-->new 
  * To create a new test plan and name the test plan(For example: IOTAPerfTest)
  
# 4. Create a thread group
  * Steps:Right click Add-->Threads(Users)-->Thread Group
  * Among them you need to fill in some parameters as follows:
    * Number of Threads (Users): number of virtual users (for example: 10 people)
    * Ramp-Up Period: Equivalent to the time range of the first loop used by the thread (for example: 1 second)
    * Loop count: the number of virtual times (for example: 10 times)
  * The meaning it expresses is: 1 person requests 10 times in one second, 10 people request 100 times in 1 second.
  
# 5. Create an Http Request
  * Steps:Right click Add-->Sampler-->Http Request
  * Server name or IP: This refers to the domain name of the target host you want to access.Note that because http (or https, the default is http) has been defined in the previous protocol, don't add http:// in front of the server name, if you write localhost directly, Followed by the port number of the target server
  * At the bottom are Parameters, Body Data, and Files Upload:
    * Parameters refers to the parameters in the function definition, and argument refers to the actual parameters when the function is called. In general, the two can be mixed.
    * Files Upload refers to: Get all the embedded resources from the HTML file: When selected, send the HTTP request and get the response of the HTML file content, then parse the HTML and get all the resources contained in the HTML (picture, flash Etc): (not selected by default)
    * Body Data refers to the entity data, which is the content of the subject entity in the request message. Generally, we send the request to the server, and the entity body parameters carried can be written here. In general, Body Data is used.

# 6. Create an HTTP Header Manager
  * Steps:Right click Add-->Config Element-->Http Header Manager
  * The role of the header manager is:The content of the request header used to customize the HTTP request issued by Sampler. HTTP requests from different browsers have different Agents. The correct Refer is required to access certain pages with anti-theft chains... In these cases, the HTTP Header Manager is required to ensure that the HTTP request sent is correct.
  * Then you need to fill in the name of the information header and the corresponding value. I filled in the Content-Type when I tested it. The meaning can be understood as the parameter name and type. Enter the corresponding parameter type below the value. Here I need to transfer the json type when testing. So fill in the application/json

# 7. Create an Summary Report
  * Steps:Right click Add-->listener-->summary Report
  * After the test monitoring Summary Report is created, after you test it, you can see some parameters of your test here:
    * Successes: Saves the successful part of the log.
    * Configure: Sets the result attribute, which is the result field to save to the file. Generally save the necessary field information, the more you save, the impact on the IO of the load machine.
    * Label: The sampler name (or transaction name).
    * #Samples: Number of sampler runs (how many transactions were submitted).
    * Average: The average response time of the request (transaction) in milliseconds.
    * Min: The minimum response time of the request, in milliseconds.
    * Max: The maximum response time of the request, in milliseconds.
    * Std.Dev: The standard deviation of response time.
    * Error%: Transaction error rate.
    * Throughput: throughput rate (TPS).
    * KB/sec: Packet traffic per second in KB.
    * Avg.Bytes: Average data traffic in Bytes

# 8. Create an View Results Tree
  * Steps:Right click Add-->listener-->view Results Tree
  * It contains three pieces: sampler results, request and response data.
  * After the test is over, if our request is successfully sent to the server, the simulation request in the result tree will be displayed in green. It can be judged by the response status code information in the sampler result or by clicking the request module to view the request we sent. If the request fails, the simulation request will be displayed in red and feedback error.
    
  