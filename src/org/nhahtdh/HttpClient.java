package org.nhahtdh;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.net.ssl.*;

/**
 * - Support HTTP and HTTPS protocol.
 * - Does NOT check the certificate in HTTPS protocol.
 * - Basic chunk-encoding is implemented.
 * - Pipelining is NOT implemented.
 * - Cookie is fully (?) supported.
 * - Most status code other than 200 will return an error.
 * 
 * @author Hong Dai Thanh
 * 
 */
class HttpClient {

	//----------
	// Debug
	//----------
	/*
	 * 0 - Basic debug statements
	 * 1 - Verbose
	 */
	private static final boolean debug[] = {true, false};

	//--------------
	// Constants
	//--------------
	public static final int DEFAULT_HTTP_PORT_NUMBER = 80;
	public static final int DEFAULT_HTTPS_PORT_NUMBER = 443;
	public static final String HTTP_HEADER_CHARSET = "US-ASCII";
	private static final String GET_METHOD = "GET";
	private static final String POST_METHOD = "POST";
	private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 (.NET CLR 3.5.30729)";
	private static final int MAX_REDIRECTION = 5;
	private static final String DEFAULT_FILE_NAME = "default_named.html";
	private static final String INVALID_WINDOWS_FILE_NAME_CHARACTER_SET = "/\\\\:\\*\\?\"<>"; 
	private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;

	//----------------
	// Data members
	//----------------
	/** Current working directory */
	private String workingDir;
	// Basic HTTP entries
	// TODO: Use URI and fileName fields only.
	private URI URL;
	private boolean isHttps;
	/** Host name */
	private String host;
	/** Port number to connect to */
	private int port;
	/** Path and query part of the URL */
	private String path;
	private String fileName;
	// HTTP 1.1 entries
	private CookieStore cookieStore;
	private boolean keepAlive;

	// IO interfaces
	private Socket socket;
	private OutputStreamWriter send;
	private RawStreamReader receive;

	// HTTP response header parser
	private HttpResponseHeader responseHeaderParser;

	//----------------
	// Constructors
	//----------------

	// Default constructor.
	public HttpClient() {
		this.workingDir = "";
		this.cookieStore = new CookieManager().getCookieStore();
		this.responseHeaderParser = new HttpResponseHeader();
	}

	/**
	 * This method will set all the parameters needed for connection establishment.
	 * @param address
	 *        The URI to the file.
	 * @param fileName
	 *        The name of the file to be downloaded. The file will be automatically named if set to null.
	 */
	public void setURL(String address, boolean keepAlive) throws URISyntaxException {
		if (debug[1]) {
			System.out.println("** Start URL Information **");
			if (URL != null)
				System.out.println("Current URL: " + URL);
			System.out.println("Input address: " + address);
		}
		// Resolve the relative address if fail to match the pattern of absolute path.
		if (!address.matches(".*://.*"))
			address = URL.resolve(address).toString();
		// Set the full URL
		this.URL = new URI(address);
		// Extract protocol identifier
		int int_t2 = address.indexOf("://");
		String protocol = int_t2 < 0 ? null : address.substring(0, int_t2);
		if (!protocol.equals("http") && !protocol.equals("https"))
			throw new IllegalArgumentException(protocol + " not supported.");
		else
			// Set secured HTTP connection flag
			this.isHttps = protocol.length() == 5;
		// Trim the protocol identifier
		address = address.substring(int_t2 < 0 ? 0 : int_t2 + "://".length());
		int int_t0, int_t1;
		if ((int_t0 = address.indexOf("/")) < 0) {
			// If requesting the root folder of the host.
			// Set host and port number
			if ((int_t1 = address.indexOf(":")) < 0) {
				this.host = address;
				this.port = this.isHttps ? DEFAULT_HTTPS_PORT_NUMBER : DEFAULT_HTTP_PORT_NUMBER;
			} else {
				this.host = address.substring(0, int_t1);
				this.port = Integer.parseInt(address.substring(int_t1 + 1));
			}
			// Set path
			this.path = "/";
		} else {
			// Set host and port number
			this.host = address.substring(0, int_t0);
			if ((int_t1 = this.host.indexOf(":")) < 0) {
				this.port = this.isHttps ? DEFAULT_HTTPS_PORT_NUMBER: DEFAULT_HTTP_PORT_NUMBER;
			} else {
				this.port = Integer.parseInt(this.host.substring(int_t1 + 1, int_t0));
				this.host = this.host.substring(0, int_t1);
			}
			// Set path
			this.path = address.substring(int_t0);
		}
		
		// Reset fileName field.
		this.fileName = null;
		
		// Set keep-alive boolean
		this.keepAlive = keepAlive;

		if (debug[1]) {
			System.out.println("Protocol: " + protocol);
			System.out.println("Host: " + this.host);
			System.out.println("Path: " + this.path);
			System.out.println("Port: " + this.port);
			System.out.println("** End URL Information **");
		}
	}

	/**
	 * Helper method for setURL method. This method decode the given URL.
	 * <p> A URL encoder must parse the URL and encode them separately before
	 * putting the fragments together.
	 * @param URL
	 *        The URL to be decoded.
	 * @return the corresponding decoded URL.
	 */
	private static String decodeURL(String URL) {
		StringBuilder name = new StringBuilder(URL);
		int index = -1, charCode;

		while ((index = name.indexOf("%", index + 1)) >= 0) { // indexOf() will work for any starting index
			charCode = Short.parseShort(name.substring(index + 1, index + 3), 16); // char type is 16-bit so we use parseShort
			name.replace(index, index + 3, Character.toString((char) charCode));
		}

		return name.toString();
	}
	
	/**
	 * Set the current working directory on the local system.
	 * <p> This method does not check whether the directory exists or not.
	 * @param relPath
	 *        Path of the directory to be changed to.
	 * @return
	 */
	// TODO: Check whether the directory exist before setting the directory.
	public String setWorkingDir(String relPath) {
		if (!relPath.matches("[^/].*/")) {
			if (debug[1])
				System.out.println("Working directory not set.");
			return null;
		}
		String ts0 = this.workingDir;
		this.workingDir = relPath;
		if (debug[0])
			System.out.println("Working directory set to: " + this.workingDir);
		return ts0;
	}

	/**
	 * Remove all cookies from cookie store.
	 */
	public void clearCookies() {
		cookieStore.removeAll();
	}

	/**
	 * Start downloading the file as specified by earlier call of setURL method.
	 * 
	 * @return a {@code java.io.File} object pointing to the received file.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public File download(String fileName) throws UnknownHostException, IOException, URISyntaxException {
		return this.request(GET_METHOD, null, fileName);
	}

	/**
	 * Send request to the server and receive response from the server. Currently,
	 * only GET and POST methods are implemented and the functionality is limited.
	 * 
	 * @param method
	 *        The method of the request to be made. Only GET and POST are accepted.
	 * @param postData
	 *        The data to be uploaded. It will be ignored when GET method is used.
	 * @return a pointer to the body of the response message from the server, if any.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public File request(String method, String postData, String fileName) throws UnknownHostException, IOException, URISyntaxException {
		int i = 0;
		while (i < MAX_REDIRECTION) {
			// Connect to the host. Get input and output streams.
			if (socket == null || socket.isClosed())
				this.connect();
			else if (!socket.getInetAddress().getHostName().equals(host) || socket.getPort() != port) {
				// Close the unused open connection.
				this.close();
				// Connect to new address or new port of the same address
				this.connect();
			}
			//-----------------------------
			// Send request to server
			String request = getRequestString(method, postData);
			if (debug[1])
				System.out.println("\n" + request);
			send.write(request);
			send.flush();
			//-----------------------------
			// Process response from server
			List<String> tl0; // Temporary variables
			// Parse the HTTP response header
			this.responseHeaderParser.parse(receive);

			// Add cookies to the cookie store. Set-Cookie2 is ignored de facto.
			if ((tl0 = this.responseHeaderParser.getValue("Set-Cookie")) != null) {
				if (debug[1])
					System.out.println("** Start Set-Cookie **");
				for (String s : tl0)
					setCookie(s);
				if (debug[1])
					System.out.println("** End Set-Cookie **");
			}

			/* 
			 * Set keep alive flag. We will close the connection for any HTTP 1.0 server
			 * and assume any HTTP 1.1 server will keep the connection alive unless 
			 * "Connection: close" is specified in the header.
			 * We will ignore Keep-Alive header since it is not a standard HTTP header.
			 */
			if (responseHeaderParser.getVersion().equals("1.0")
					|| ((tl0 = this.responseHeaderParser.getValue("Connection")) != null && tl0.get(0).toLowerCase().equals("close")))
				this.keepAlive = false;
			if (debug[1])
				System.out.println("Keep-Alive: " + this.keepAlive);
			
			// Get status code.
			int statusCode = responseHeaderParser.getStatusCode();

			// Check the status code and take action against it.
			switch (statusCode) {
			case 100: // Continue
			case 101: // Switching Protocols
			case 204: // No Content
			case 304: // Not Modified
				// _TODO: Implement the correct reaction when receiving these status codes.
				// These status codes do not expect message body.
				return null;
			case 302: // Found
				/*
				 * Getting 302 Found for POST means our request has reached the
				 * destination and has been processed. Therefore, we can reset
				 * the method to GET.
				 * 
				 * The same request method should be kept for code 301 and 307.
				 */
				if (method.equals(POST_METHOD)) {
					method = GET_METHOD;
					postData = null;
				}
				// Continue.
			case 301: // Moved permanently
			case 307: // Temporary Redirect
				i++; // Increment the number of redirection done.
				if (debug[0])
					System.out.println("Redirecting... ");
				// Continue.
			case 200: // OK
				// Only status code 200, 301, 302, 307 can enter this portion of the code.
				
				// Set the file name for the response body. A file is yet to be created at this stage.
				setFileName(fileName);
				if (debug[1])
					System.out.println("File name: " + this.fileName);
				
				// Get body of the response.
				File outFile = getBody(statusCode == 200); // Write to file only if status code is 200

				// Close the connection if needed.
				if (!this.keepAlive)
					this.close();

				if (statusCode == 200)
					// Return the File object.
					return outFile;
				else {
					/* 
					 * If the status code is 301, 302 or 307, do redirection.
					 * Assume Location field is always present in the header
					 * for these 3 status codes.
					 * 
					 * The file received will be automatically named, regardless
					 * of the file name supplied.
					 */
					setURL(this.responseHeaderParser.getValue("Location").get(0), true);
					continue;
				}
				// BREAK.
			default:
				// Throw exception for the rest of the statuses.
				// Unimplemented statuses are listed under
				// _UNIMPLEMENTED_STATUSES comment
				throw new SocketException(statusCode + " " + this.responseHeaderParser.getReason());
			}
		}
		
		throw new SocketException("Maximum number of redirections reached");
	}

	/**
	 * Private helper method for request. Connects to the host as specified
	 * by setURL method. 
	 * @throws IOException
	 * @throws SocketException
	 */
	private void connect() throws IOException, SocketException {
		if (this.isHttps)
			socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(this.host, this.port);
		else
			socket = new Socket(host, port);
		this.send = new OutputStreamWriter(socket.getOutputStream(), HTTP_HEADER_CHARSET); // By standard, the HTTP header should not contain non-US-ASCII characters.
		this.receive = new RawStreamReader(socket.getInputStream());
		if (debug[1]) {
			System.out.println("Connection successfully established");
			System.out.println("InetAddress: " + socket.getInetAddress() + ":" + socket.getPort());
		}
	}
	
	/**
	 * Private helper method for request. Generates the request message to be sent to
	 * the server with the given method and message body, if applicable.
	 * <p> This method currently partially support GET and POST method. Other methods
	 * are considered invalid when passed to this method and will generate an Exception.
	 * @param method
	 *        The method of the request. Currently only GET and POST are valid method.
	 * @param upData
	 *        The data to be sent. Only used in POST method.
	 * @return
	 */
	private String getRequestString(String method, String upData) {
		String request;
		if (method.equals(GET_METHOD) || method.equals(POST_METHOD)) {
			request =
				method + " " + path + " HTTP/1.1\r\n" + 
				"Host: " + host + "\r\n" +
				// Use User-Agent of Mozilla Firefox on Windows 7.
				"User-Agent: " + DEFAULT_USER_AGENT + "\r\n";

			// Cookie field.
			List<HttpCookie> cookieList = cookieStore.get(this.URL);
			if (cookieList.size() > 0) {
				request += "Cookie: ";
				for (int j = 0; j < cookieList.size(); j++)
					request += cookieList.get(j) + (j == cookieList.size() - 1 ? "\r\n" : "; ");
			}

			if (method.equals(POST_METHOD)) {
				request += 
					"Content-Type: application/x-www-form-urlencoded\r\n" +
					// Length of the body
					"Content-Length: " + upData.length() + "\r\n";
			}

			request +=
				(keepAlive ? "Connection: Keep-Alive\r\n" : "Connection: close\r\n") +
				// Empty line
				"\r\n";

			// Include body if the method is POST
			if (method.equals(POST_METHOD))
				request += upData;
		} else
			throw new IllegalArgumentException("Unsupported or invalid method: " + method);

		return request;
	}

	private void setCookie(String line) {
		List<HttpCookie> cookieList = HttpCookie.parse(line);
		// Add cookie to the cookie store
		for (HttpCookie entry : cookieList) {
			cookieStore.add(this.URL, entry);
			if (debug[1])
				System.out.println(entry);
		}
	}
	
	private void setFileName(String fileName) {
		/*
		 * If fileName is not specified (is null OR is empty string OR contains
		 * illegal characters), we will try these steps to assign the file name:
		 * - Look for filename field in Content-Disposition header field.
		 * - Name the file based on the request URL.
		 * - Use the default name.
		 * Otherwise, we will name the file with fileName.
		 * _TODO: Support valid file name on OS's other than Windows.
		 */
		if (fileName == null || !fileName.matches("[^" + INVALID_WINDOWS_FILE_NAME_CHARACTER_SET + "]+")) {
			// If the file name is not specified.
			List<String> tl0; String ts0; int ti0; // Temporary variables
			/*
			 * Assume only one Content-Disposition header is specified, if any.
			 * We will ignore any extra Content-Disposition header.
			 */
			if ((tl0 = this.responseHeaderParser.getValue("Content-Disposition")) != null
					&& (ti0 = (ts0 = tl0.get(0)).indexOf("filename=")) >= 0) {
				// If there is Content-Disposition header and filename parameter is specified.
				// Assume that a value is present if filename field appears in the header.
				if (ts0.charAt(ti0 += "filename=".length()) == '"') {
					// If value of filename parameter is quoted-string
					// Assume the file name does not contain double-quotation mark "
					this.fileName = 
						ts0.substring(ti0 += 1, ts0.indexOf("\"", ti0))
						// Replace all invalid characters with underscore _
						.trim().replaceAll("[" + INVALID_WINDOWS_FILE_NAME_CHARACTER_SET + "]", "_");
				} else {
					// If value of filename parameter is token
					// A token should not contain semicolon ;
					this.fileName = 
						ts0.substring(ti0, (ti0 = ts0.indexOf(";", ti0)) < 0 ? ts0.length() : ti0)
						// Replace all invalid characters with underscore _
						.trim().replaceAll("[" + INVALID_WINDOWS_FILE_NAME_CHARACTER_SET + "]", "_");
				}
				
				if (!this.fileName.isEmpty())
					return;
			} 
			
			// If we cannot specify a valid file name from Content-Disposition header field.
			if (this.path.equals("/"))
				// If the URL points to root folder of the host
				this.fileName = DEFAULT_FILE_NAME;
			else {
				this.fileName =
					// Decode according to percentage-coding scheme.
					decodeURL(
							/* 
							 * Extract the part between the last "/" before the last "?" 
							 * and the last "?" in the link.
							 */
							(this.fileName = this.path.substring(0, (ti0 = this.path.lastIndexOf("?")) < 0 ? this.path.length() : ti0))
							.substring(this.fileName.lastIndexOf("/") + 1));
				
				if (this.fileName.isEmpty())
					// Assign a default file name if empty.
					// This case occurs with paths ending with slash /
					this.fileName = DEFAULT_FILE_NAME;
				else if (this.fileName.indexOf(".") < 0)
					// Add a default .html extension to file name without dot
					// One example of this case is Wikipedia.
					this.fileName += ".html";
			}
		} else 
			// If file name is specified and valid.
			this.fileName = fileName;
	}

	/**
	 * Private helper method for request. This method will read the message body
	 * of the response message and write to disk or ignore it based on the flag.
	 * <p>
	 * This method supports basic chunk encoding and normal encoding.
	 * <p>
	 * This method will never overwrite an existing file with the same name as
	 * the receiving file.
	 * 
	 * @param writeToDisk
	 *            Write the received message body to file or not.
	 * @return a {@code java.io.File} object pointing to the received file, if
	 *         writeToDisk flag is set. Otherwise, null is returned.
	 * @throws IOException
	 */
	private File getBody(boolean writeToDisk) throws IOException {
		List<String> tl0; // Temporary variable
	
		// Get length of message body.
		long contentLength = -1;
		if ((tl0 = this.responseHeaderParser.getValue("Content-Length")) != null)
			contentLength = Long.parseLong(tl0.get(0));
	
		// Get transfer encoding
		boolean chunkEncoding = false;
		if ((tl0 = this.responseHeaderParser.getValue("Transfer-Encoding")) != null && !(chunkEncoding = tl0.get(0).toLowerCase().equals("chunked")))
			throw new IOException("Unsupported transfer encoding: " + tl0.get(0));
	
		if (debug[1]) {
			System.out.println("** Start Message Body **");
			System.out.println("Content-Length: " + contentLength);
			if (chunkEncoding)
				System.out.println("Transfer-Encoding: chunked");
			System.out.println("** End Message Body **");
		}
	
		// Write file to disk
		if (debug[0] && writeToDisk)
			System.out.println("\n" + this.fileName + " is being downloaded. Please wait.");
	
		File outFile = null;
		FileOutputStream toFile = null;
		if (writeToDisk) {
			// Set file name
			outFile = resolveFileNameConflict(workingDir, fileName);
	
			// Set output stream
			toFile = new FileOutputStream(outFile);
		}
	
		byte[] data = new byte[DEFAULT_RECEIVE_BUFFER_SIZE];
		int ti0; // Temporary variable
		if (chunkEncoding) {
			// If Transfer-Encoding field is present, ignore Content-Length field.
			long chunkLength;
			String ts0; // Temporary variable
			while (true) {
				// Extract length of the chunk data
				ts0 = new String(receive.readLine(), HTTP_HEADER_CHARSET).trim();
				chunkLength = Long.parseLong(ts0.substring(0, (ti0 = ts0.indexOf(";")) < 0 ? ts0.length() : ti0).trim(), 16);
				if (chunkLength == 0) {
					// Last chunk (zero chunk) encountered.
					break;
				}
	
				while (chunkLength > 0) {
					// Read data into the buffer.
					// read: If the function returns, the number of bytes read is always > 0.
					// Math.min: The result is always in int range so the conversion is safe. Result of this function is always > 0 because chunkLength > 0.
					chunkLength -= ti0 = receive.read(data, 0, (int) Math.min(DEFAULT_RECEIVE_BUFFER_SIZE, chunkLength));
					if (writeToDisk)
						toFile.write(data, 0, ti0);
				}
				receive.readLine(); // Discard CRLF that terminates chunk data.
			}

			// Discard all trailers and the CRLF that terminates the whole chunk body.
			while (!new String(receive.readLine(), HTTP_HEADER_CHARSET).trim().isEmpty());
		} else if (contentLength > 0) { // Transfer-Encoding field is not present in the header
			/*
			 * Assume that when the server does not specify to close the
			 * connection after transfer completes, and Transfer-Encoding header
			 * field is not included, the server always includes Content-Length
			 * header field in the response.
			 */
			// Read data into the buffer.
			// read: If the function returns, the number of bytes read is always > 0.
			// Math.min: The result is always in int range so the conversion is safe. Result of this function is always > 0 because chunkLength > 0.
			while (contentLength > 0) {
				contentLength -= ti0 = receive.read(data, 0, (int) Math.min(DEFAULT_RECEIVE_BUFFER_SIZE, contentLength));
				if (writeToDisk)
					toFile.write(data, 0, ti0);
			}
		} else { // Both Content-Length and Transfer-Encoding fields are missing.
			/*
			 * Assume there is no message body if both of them are missing and
			 * the connection is to be left open.
			 */
			if (!this.keepAlive) {
				// Read until EOF is encountered.
				while ((ti0 = receive.read(data, 0, DEFAULT_RECEIVE_BUFFER_SIZE)) != -1)
					if (writeToDisk)
						toFile.write(data, 0, ti0);
			}
		}
	
		// Close the file.
		if (writeToDisk)
			toFile.close();
		if (debug[0] && writeToDisk)
			System.out.println("File written to disk.");
	
		return outFile;
	}
	
	private File resolveFileNameConflict(String path, String fileName) {
		File outFile = new File(path + fileName);
		
		if (!outFile.exists())
			return outFile;
		
		// Resolve conflict to prevent overwriting existing file.
		int ti0;
		String name = fileName.substring(0, (ti0 = fileName.lastIndexOf(".")) < 0 ? fileName.length() : ti0);
		String ext = fileName.substring(ti0 < 0 ? fileName.length() : ti0);
		
		for (int i = 2; outFile.exists(); i++)
			outFile = new File(path + name + " (" + i + ")" + ext);
		
		if (debug[0])
			System.out.println("Another file with same name found. File name changed to " + outFile.getName());
		
		return outFile;
	}

	public void close() throws IOException {
		this.send.close();
		this.receive.close();
		this.socket.close();
		if (debug[1])
			System.out.println("Connection to " + socket.getInetAddress() + ":" + socket.getPort() + " closed.");
	}

	/**
	 * Get the port number of the target address of the previous connection if
	 * the socket is closed; get the port number of the target address of the
	 * current connection if the socket is open.
	 * 
	 * @return port number of the target address.
	 */
	public int getRemotePort() {
		return this.socket.getPort();
	}

	// Debug code
	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner(System.in);
		HttpClient client = new HttpClient();
		File receivedFile;
		while (true) {
			client.setURL(sc.nextLine(), false);
			receivedFile = client.download(null);
			System.out
					.println(receivedFile.getName() + " has been downloaded.");
		}
	}
}


// _UNIMPLEMENTED_STATUSES
/*
case 201: // Created
case 202: // Accepted
case 203: // Non-Authoritative Information
case 205: // Reset Content
case 206: // Partial Content
case 300: // Multiple Choices
case 303: // See Other
case 305: // Use Proxy
case 400: // Bad Request
case 401: // Unauthorized
case 403: // Forbidden
case 404: // Not Found
case 405: // Method Not Allowed
case 406: // Not Acceptable
case 407: // Proxy Authentication Required
case 408: // Request Timeout
case 409: // Conflict
case 410: // Gone
case 411: // Length Required
case 412: // Precondition Failed
case 413: // Request Entity Too Large
case 414: // Request-URI Too Long
case 415: // Unsupported Media Type
case 416: // Requested Range Not Satisfiable
case 417: // Expectation Failed
case 500: // Internal Server Error
case 501: // Not Implemented
case 502: // Bad Gateway
case 503: // Service Unavailable
case 504: // Gateway Timeout
case 505: // HTTP Version Not Supported
 */