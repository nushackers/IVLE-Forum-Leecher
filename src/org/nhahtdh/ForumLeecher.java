package org.nhahtdh;

import java.io.*;
import java.util.*;
import java.net.*;
import org.htmlparser.*;
import org.htmlparser.util.*;
import org.htmlparser.nodes.*;
import org.htmlparser.filters.*;

public class ForumLeecher {
	// TODO: Run multiple connections (multiple threads) to speed up download.
	
	//---------
	// Debug
	//---------
	/*
	 * 0 - login
	 * 1 - parse
	 * 2 - set folder
	 * 3 - download
	 */
	private static final boolean debug[] = {false, false, false, true};

	//--------------
	// Constants
	//--------------
	private static final String DISPLAY_POST_REGEX = "displayPost\\('[-0-9a-f]+'.*\\).*";
	private static final String FORUM_ID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String ATTACHMENT_REGEX = "/forum/download_file\\.aspx.*";
	
	private static final String IVLE_ADDRESS = "https://ivle.nus.edu.sg/";
	private static final String FORUM_ADDRESS = "http://ivle.nus.edu.sg/forum/";
	
	private static final String DEFAULT_LOCAL_FOLDER = "forum/";
	
	private static String currentWorkingDir;

	public static void main(String args[]) throws Exception {
		final URI FORUM_URI = new URI("http://ivle.nus.edu.sg/forum/");
		Scanner sc = new Scanner(System.in);
		HttpClient client = new HttpClient();
		boolean tb0; // Temporary variables
		// _TODO: Let user configure the working directory.
		setCurrentWorkingDir(DEFAULT_LOCAL_FOLDER, client, true);
		
		// Run the login routine.
		if (!login(sc, client))
			return;
		
		//--------------------------------
		// Loop for user to choose forum to archive
		while (true) {
			File receivedFile;
			
			// Get forum ID from user input.
			String forumId;
			if ((forumId = getForumId(sc)) == null)
				return;
			//--------------------------------
			// Set current working directory
			setCurrentWorkingDir(DEFAULT_LOCAL_FOLDER + forumId + "/", client, false);
			
			// Download the main forum page
			client.setURL(FORUM_ADDRESS + "forum.aspx?forumId=" + forumId, true);
			receivedFile = client.download(null);
			// Check for error page returned by server.
			if (receivedFile.getName().matches("error\\.aspx.*")) {
				System.out.println("Your action caused an error to occur on IVLE.");
				receivedFile.delete();
				tb0 = new File(currentWorkingDir).delete();
				if (debug[2])
					System.out.println("Folder " + currentWorkingDir + (tb0 ? "" : " not") + " deleted.");
				setCurrentWorkingDir(DEFAULT_LOCAL_FOLDER, client, false);
				
				continue; // Loop back to get new forum ID
			}
			// Assume that no error will returned by the server from this point onwards.
			// _TODO: Error checking.
			
			/*
			 * 0 - Links to forum postings.
			 * 1 - Links to images and attachments.
			 */
			HeapList<String> listLinks = new HeapList<String>(2);
			String link; // Temporary variable
			// Download the major frame pages of the forum.
			LinkedList<String> frameLinks = parseLinks(receivedFile, new TagNameFilter("frame"), "src", null);
			while (!frameLinks.isEmpty()) {
				link = frameLinks.remove();
				client.setURL(FORUM_URI.resolve(link).toString(), true);
				receivedFile = client.download(null);
				if (link.matches(".*menu\\.aspx.*")) {
					// If the links points to menu.aspx
					// Check for the existence of forum archive.
					if (parseLinks(receivedFile, new HasAttributeFilter("id", "ibtnArchive"), "onclick", ".*forum_archive\\.aspx.*").size() != 0) {
						if (debug[3])
							System.out.println("Found forum archive.");
						// Download main page of forum archive
						client.setURL(FORUM_ADDRESS + "forum_archive.aspx?forumId=" + forumId, true);
						receivedFile = client.download(null);
						// Parse and add the links of major frame pages of archive forum  
						frameLinks.addAll(parseLinks(receivedFile, new TagNameFilter("frame"), "src", null));
					}
				} else if (link.matches(".*list(_archive)?\\.aspx.*")) {
					// Parse for number of pages of list of forum postings.
					int numPages = parseNumPages(receivedFile);
					String pageName = receivedFile.getName();
					if (debug[3])
						System.out.println("There are " + numPages + " pages of forum post listing.");
					
					int i = 0;
					while (true) {
						// Parse the list for links to posts.
						if (debug[3])
							System.out.println("Parsing the list " + receivedFile.getPath());
						listLinks.addAll(parseLinks(receivedFile, new TagNameFilter("a"), "onclick", DISPLAY_POST_REGEX), 0);
						if (++i >= numPages)
							break;
						// Download other pages of forum post listing.
						client.setURL(FORUM_ADDRESS + pageName + "?forumid=" + forumId + "&currpage=" + i, true);
						receivedFile = client.download(null);
					}
				} 
				// If the link points to menu_archive.aspx or main(_archive).aspx, just download the page. 
			}
			//--------------------------------
			
			/*
			 * Download the forum postings and extract extra links (images and attachments)
			 * to be downloaded.
			 */
			if (debug[3])
				System.out.println("Downloading " + listLinks.size(0) + " forum posts.");
			int numAttach = 0, numImage = 0;
			while (listLinks.getNextPollIndex() == 0) { // Only download the posts in this round.
				int ti0; // Temporary variable 
				link = listLinks.poll();
				String postId = link.substring(ti0 = link.indexOf("'") + 1, ti0 = link.indexOf("'", ti0 + 1));
				// We will download the archive version of the post by default.
				client.setURL(FORUM_ADDRESS + "read_archive.aspx?forumid=" + forumId + "&postid=" + postId, true);
				receivedFile = client.download(postId + ".html");
				if (debug[3])
					System.out.println("Downloaded file: " + receivedFile.getName());
				
				LinkedList<String> tll0;
				// Add image links.
				// TODO: Prevent images from the same source to be re-downloaded
				listLinks.addAll(tll0 = parseLinks(receivedFile, new TagNameFilter("img"), "src", null), 1);
				if (debug[3]) {
					if (tll0.size() > 0) {
						numImage += tll0.size();
						System.out.println("Found " + tll0.size() + " image(s).");
						System.out.println(tll0);
					}
					/* else
						System.out.println("Image not found.");
					*/
				}
				
				// Add attachment link.
				listLinks.addAll(tll0 = parseLinks(receivedFile, new TagNameFilter("a"), "href", ATTACHMENT_REGEX), 1);
				if (debug[3]) {
					if (tll0.size() > 0) {
						numAttach += tll0.size();
						System.out.println("Found " + tll0.size() + " attachment(s).");
						System.out.println(tll0);
					}
					/* else
						System.out.println("Attachment not found.");
					*/
				}
			}
			
			//--------------------------------
			// Download images and attachments, if any.
			
			if (debug[3]) {
				System.out.println("There are " + numImage + " images and " + numAttach + " attachments in the list of " + listLinks.size(1) + " links.");
				System.out.println(listLinks.toString(1));
			}
			
			// getNextPollIndex: always >= 1 because nothing is added to level 0 queue.
			if (listLinks.getNextPollIndex() == 1) {
				// Create a new directory for images and attachments
				setCurrentWorkingDir(currentWorkingDir + "extra/", client, false);
				while ((link = listLinks.poll()) != null) {
					// resolve: If ts0 is absolute, return ts0; otherwise return ts0 after resolve against FORUM_URI
					client.setURL(FORUM_URI.resolve(link).toString(), true); 
					receivedFile = client.download(null);
					if (debug[3])
						System.out.println("Downloaded file: " + receivedFile.getName());
				}
			}
		}
	}
	
	/**
	 * Run the login routine and return whether the user successfully login or wish to 
	 * quit the program.
	 * @param sc
	 *        Scanner to get user input.
	 * @param client
	 *        The HTTP client used for connection with server.
	 * @return
	 * @throws Exception
	 */
	private static boolean login(Scanner sc, HttpClient client) throws Exception {
		// Enter user name and password routine to log into IVLE.
		String username, password;
		boolean firstEntry = true;
		do {
			// Notify the user of unsuccessful login attempt.
			if (!firstEntry)
				System.out.println("Login unsuccessful.");
			// User name
			System.out.print("Enter username: ");
			username = sc.next();
			// Allow user to quit
			if (username.toLowerCase().equals("quit"))
				return false;
			sc.nextLine(); // Discard leftovers
			// Password
			System.out.print("Enter password: ");
			password = sc.nextLine(); // Password can contain space.
			// Set the boolean for first entry.
			firstEntry = false;
			if (debug[0])
				// Print out the user name and password
				System.out.println("\"" + username + "\"/" + password + "\"");
			System.out.println("Logging in...");
		} while (!requestLogin(client, username, password));
		// Notify the user of successful login attempt.
		System.out.println("Login successfully");
		
		return true;
	}
	
	/**
	 * Send login request to the server and return the result of the authentication. 
	 * @param client
	 *        The HTTP client used for connection with server.
	 * @param username
	 *        User name to be sent.
	 * @param password
	 *        Password to be sent.
	 * @return {@code true} if the authentication is successful; otherwise, {@code false} is
	 * returned.
	 * @throws Exception
	 */
	private static boolean requestLogin(HttpClient client, String username, String password) throws Exception {
		String upData = 
			// _TODO: Read these entries from default.aspx instead of hard-coding.
			"__LASTFOCUS=&" + 
			"__EVENTTARGET=&" + 
			"__EVENTARGUMENT=&" +
			"__VIEWSTATE=%2FwEPDwULLTE0Njg4NTU2MDhkGAEFHl9fQ29udHJvbHNSZXF1aXJlUG9zdEJhY2tLZXlfXxYCBQ9jdGwwMCRsb2dpbmltZzEFE2N0bDAwJGNoa1JlbWVtYmVyTWU%3D&" + 
			"__SCROLLPOSITIONX=0&" +
			"__SCROLLPOSITIONY=0&" +
			"ctl00%24userid=" + username + "&" +
			"ctl00%24password=" + password + "&" +
			// Assume domain is NUSSTU
			"ctl00%24domain=NUSSTU&" +
			// _TODO: Read the dimension from default.aspx instead of hard-coding.
			"ctl00%24loginimg1.x=" + (int) (Math.random() * 74) + "&" +
			"ctl00%24loginimg1.y=" + (int) (Math.random() * 11);
		client.setURL(IVLE_ADDRESS, true);
		File receivedFile = client.request("POST", upData, null);
		receivedFile.delete();
		if (debug[0])
			System.out.println("Temporary file " + receivedFile.getName() + " deleted.");
		/* 
		 * _TODO: Currently, the success of the login is based on the change in port number.
		 * It can also be decided based on the address returned by the server.
		 * However, the methods above are not definitive way to determined a successful 
		 * login.
		 */
		return !(client.getRemotePort() == HttpClient.DEFAULT_HTTPS_PORT_NUMBER);
	}

	/**
	 * Parse the HTML file.
	 * @param htmlFile
	 *        The HTML file to be parsed.
	 * @param nodeFilter
	 *        The node filter for selecting the useful nodes.
	 * @param attribName
	 *        The name of the attribute in the tag containing the data.
	 * @param regexAttribValue
	 *        Regular expression to match against the value of the specified
	 *        attribute field.
	 * @return a list of String that contains our data.
	 */
	private static LinkedList<String> parseLinks(File htmlFile, NodeFilter nodeFilter, String attribName, String regexAttribValue) throws ParserException {
		Parser parser = new Parser(htmlFile.getPath());
		// Get node list satisfying node filter.
		NodeList nodeList = parser.parse(nodeFilter);
		if (debug[1])
			System.out.println(nodeList.toHtml());
		// List of relative URIs
		LinkedList<String> links = new LinkedList<String>();
		if (debug[1])
			System.out.println("There are " + nodeList.size() + " nodes satisfying the node filter " + nodeFilter.getClass());
		for (int i = 0; i < nodeList.size(); i++) {
			TagNode elem = (TagNode) nodeList.elementAt(i);
			String ts0; // Temporary variable
			if (debug[1])
				System.out.println(elem);
			// Filter nodes with necessary attributes
			if ((ts0 = elem.getAttribute(attribName)) != null) {
				if (debug[1])
					System.out.println("Node " + i + " has " + attribName + " attribute: " + ts0);
				// If there is a regexFilter then we must match the entry against it.
				if (regexAttribValue == null || ts0.matches(regexAttribValue))
					// Decode HTML coding of the URL and add the URL to the list. 
					links.offer(decodeHtml(ts0));
			}
		}
		return links;
	}
	
	private static String getForumId(Scanner sc) {
		// Loop until user quits or enter a correctly formatted forum ID.
		while (true) {
			System.out.print("Enter forum ID: ");
			String forumId = sc.next();
			sc.nextLine(); // Discard leftovers
			
			// Allow user to quit.
			if (forumId.toLowerCase().equals("quit"))
				return null;
			
			if (debug[3])
				System.out.println("Forum ID: " + forumId);
			
			// Check the input forum ID against regular expression
			if (!forumId.matches(FORUM_ID_REGEX)) {
				System.out.println("Invalid forum ID.");
				continue;
			} else 
				return forumId;
		}
	}
	
	private static void setCurrentWorkingDir(String path, HttpClient client, boolean createAllFolder) {
		boolean tb0;
		currentWorkingDir = path;
		// Create folder(s) if not exist. Assume the folders are created successfully.
		File folder = new File(currentWorkingDir);
		if (!folder.exists()) {
			tb0 = createAllFolder ? folder.mkdirs() : folder.mkdir(); 
			if (debug[2])
				System.out.println("Folder " + currentWorkingDir + (tb0 ? "" : " not") + " created.");
		}
		client.setWorkingDir(currentWorkingDir);
	}
	
	/*
	private static String getFileNameFromQuery(String link) {
		// If this is a link to an attachment, path parameter must present.
		link = HttpClient.decodeURL(link.substring(link.lastIndexOf("path=") + "path=".length()));
		// Assume the path does not end with '\'
		return link.substring(link.lastIndexOf("\\") + 1); // lastIndexOf: When not found (-1), start from 0; when found ([0..length-1]), start from [1..length].
	}*/
	
	private static int parseNumPages(File menuFile) throws Exception {
		Parser parser = new Parser(menuFile.getPath());
		NodeList nodeList = parser.parse(new HasAttributeFilter("id", "lblNoPages"));
		if (debug[1])
			System.out.println(nodeList.toHtml());
		/*
		 * We will assume there is always exactly one node satisfying the filter above.
		 * Assume that only node is text node and it satisfies the format: "\\(\\d+\\)".
		 */
		String ts0 = ((TagNode) nodeList.elementAt(0)).getFirstChild().getText();
		int ti0; // Temporary variable
		if (!ts0.matches("\\(\\d+\\)") || (ti0 = Integer.parseInt(ts0.substring(1, ts0.length() - 1))) < 1)
			throw new Exception(menuFile.getPath() + " does not seem to be a valid menu.aspx");
		else
			return ti0;
	}
	
	private static final String REPLACEMENT_PAIRS[][] = {{"(&quot;|&#34;)", "\""}, {"(&apos;|&#39;)", "'"}, {"(&amp;|&#38;)", "&"}, {"(&lt;|&#60;)", "<"}, {"(&gt;|&#62;)", ">"}, {"\\+", "%20"}}; 
	
	private static String decodeHtml(String link) {
		for (String[] pair : REPLACEMENT_PAIRS)
			link = link.replaceAll(pair[0], pair[1]);
		
		return link;
	}
}