
/**
 * HttpClient Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author Jeremy Kimotho
 * @version 21/10/2022
 */


import java.util.logging.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;


public class HttpClient {

	private static final Logger logger = Logger.getLogger("HttpClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public HttpClient() {
		// nothing to do!
	}
	
    /**
     * Splits up the user provided url into host, port, and filepath. 
     * 
     * @param url URL to be split into the three parts
     */

    String [] urlSplitter(String url)
    {
        String [] base = url.split("//", 2);
        String [] parts = base[1].split("/", 2);
        String [] splits = {"host", "port", "filepath"};
        String host;
        String port;
        String filepath;

        // we know there's a port value if : exists
        if(parts[0].contains(":"))
        {
            String [] hostandport = parts[0].split(":", 2);
            host = hostandport[0];
            port = hostandport[1];
        }
        else
        {
            host = parts[0];
            port = "80";
        }

        /*
         * if length is 1 we know there's no pathname. however if user adds a / to the
         * url after hostname and port but no filename, empty string "" will also be added by split 
         * so this catches both http://host/ as well as http://host and adds index.html
         */ 
        if(parts.length == 1 || parts[1] == "")
        {
            filepath = "/index.html";
        }
        else 
        {
            filepath = "/" + parts[1];
        }

        splits[0] = host;
        splits[1] = port;
        splits[2] = filepath;

        return splits;
    };

    /**
     * Sends a get request to the server using the output stream. Creates the request using host and filepath
     * after each line is constructed they're joined and converted to bytes to send through stream
     * 
     * @param socket - stream we send bytes through
     * @param host - name of the host
     * @param filepath - filepath of the object we'll download
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    void sendGetRequest(OutputStream socket, String host, String filepath)
    {
        String get_request;
        String line_terminators = "\r\n";
        String line_1 = "GET " + filepath + " HTTP/1.1" + line_terminators;
        String line_2 = "Host: " + host + line_terminators;
        String line_3 = "Connection: close" + line_terminators;
        get_request = line_1 + line_2 + line_3 + line_terminators;

        try {
            byte[] request = get_request.getBytes("US-ASCII");
            socket.write(request);
            socket.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException();
        }

        System.out.println(get_request);
    }

    /**
     * This reads from the socket the header info byte-by-byte and parses out the useful parts to us which 
     * are the response code line and the conten length line. The other lines are printed to 
     * console but are not saved
     * 
     * @param socket - output stream of socket
     * @return - a string array with the server response and content length
     * @throws IOException
     */
    String [] readHeader(InputStream socket)
    {
        // arbitrary buffer size
        int header_buffer_size = 10000;
        // each line from the stream temporarily stored here as we accumulate the entire line
        char [] line = new char[header_buffer_size];

        String [] output = {"server response", "content_length"};

        int i = 0;
        int read_bytes = 0;
        boolean terminate = false;
        try {
            // read the first byte 
            int read_byte = socket.read();
            // we terminate once we find a blank line signifying the end of header data
            while (!terminate) {
                if ((char) read_byte == '\n') {
                    line[i] = '\n';
                    // convert char array into a string
                    String r_line = String.copyValueOf(line, 0, read_bytes);
                    // prints the header info line-by-line
                    System.out.println(r_line);
                    // once the last line (blank line) has been read we can terminate the loop
                    if (r_line.isBlank())
                    {
                        terminate = true;
                        break;
                    }
                    // save the relevant content length and status code
                    else if (r_line.contains("Content-Length"))
                        output[1] = r_line;
                    else if (r_line.contains("HTTP/1.1"))
                        output[0] = r_line;
                    i = 0;
                    read_bytes = 0;
                    // read next byte
                    read_byte = socket.read();
                } else {
                    // read byte and save it to line as a char
                    line[i] = (char) read_byte;
                    i++;
                    read_bytes++;
                    // read next byte
                    read_byte = socket.read();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return output;
    }

    /*
     * Splits up the header data that we need from the Strings
     */
    int headerSplitter(String input)
    {
        String [] parts = input.split(" ");
        return Integer.parseInt(parts[1].strip());
    }

    /**
     * reads the body of the response from the output stream. firstly splits up the file name
     * from the filepath as that will be the name of the local file. Then writes contents of body to 
     * output file
     * 
     * @param socket -input stream from socket
     * @param filepath 
     * @param buffer_size - that we extracted from header info
     * @throws FileNotFoundException
     * @throws IOException
     */
    void readBody(InputStream socket, String filepath, int buffer_size)
    {
        String [] parts = filepath.split("/");
        String filename = parts[parts.length - 1].strip();
        try
        {
            FileOutputStream outputFile = new FileOutputStream(filename);
            BufferedOutputStream file = new BufferedOutputStream(outputFile);

            int readBytes = 0;
            byte [] buff = new byte[buffer_size];

            while((readBytes = socket.read(buff)) != -1)
            {
                file.write(buff, 0, readBytes);
                file.flush();
            }

            file.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            throw new RuntimeException();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /*
     * Read output stream and determine the status code of header.
     * If header returns response of ok we can save body of response
     */
    void readResponse(InputStream socket, String filepath)
    {
        // Read header data and print to console
        String [] header_info = readHeader(socket);
        String response_code = header_info[0];
        String content_length = header_info[1];

        // Extract from the strings the data we need
        int status_code = headerSplitter(response_code);
        int buffer_size = headerSplitter(content_length);

        // if server response is ok
        if(status_code == 200)
        {
            readBody(socket, filepath, buffer_size);
        }
        else
        {
            System.out.printf("Status code: %d", response_code);
            throw new RuntimeException();
        }
    }

    /**
     * Downloads the object specified by the parameter url. We first parse the url from the user
     * that is, we extract the hostname, port, and filepath. Then we establish a TCP connection
     * and open the streams we'll need tr/w from socket. The get request is then sent and after we 
     * immediately begin reading response. This will consist of header response and then the object we want
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UnknownHostException
     * @throws SocketException
     * @throws IOException
     */
	public void get(String url)
    {
        // Method that parses the url for us
        String [] parts = urlSplitter(url);
        String hostname = parts[0];
        int port = Integer.parseInt(parts[1]);
        String filepath = parts[2];

        // initialise server address from the arguments specified by user
        InetSocketAddress serverAddress = new InetSocketAddress(hostname, port);

       // initialise a new socket to communicate with host
        Socket socket = new Socket();

        try
        {
            // establish connection to the server address specified by the user
            socket.connect(serverAddress);

            // streams we will need to read from, and write to the socket
            OutputStream socketWriter = socket.getOutputStream();
            InputStream socketReader = socket.getInputStream();
            
            // Method that send the get request using the hostname and filepath
            sendGetRequest(socketWriter, hostname, filepath);
            // We read the header response and then if ok, save file
            readResponse(socketReader, filepath);

            // clean up streams we're no longer using
            socketWriter.close();
            socketReader.close();
            // terminate connection with server
            socket.close();
        }
        catch(UnknownHostException e)
        {
            e.printStackTrace();
            throw new RuntimeException();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
            throw new RuntimeException();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException();
        }

    };

}
