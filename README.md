
# Downloading Objects via HTTP request
#### A java application that takes an http url as input and downloads the referenced object, 22/10/2022
#### By **Jeremy Kimotho**
## Description
We take as input, from the command line, an http url in the form: http://hostname[:port]/[filepath] as input and download the referenced object. Port and filepath are optional parameters, if port is not specified 80 will be used and index.html is the default filepath if none is specified. A TCP connection is established to the host if it is valid and then streams are established to read and write from and to the socket. Only the get request and the response header are printed to the command line. The file is then saved with the same name as specified in the filepath
## Setup/Installation Requirements
* javac HttpDriver.java
* java HttpDriver {valid_http_url}
## Technologies Used
* java
## Support and contact details
Email me at projectsjeremy1000 (at) gmail.com for any questions or anything.
### License
