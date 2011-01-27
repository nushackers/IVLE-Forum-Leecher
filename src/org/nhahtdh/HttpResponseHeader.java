package org.nhahtdh;

import java.util.*;
import java.io.*;

public class HttpResponseHeader {
	//---------
	// Debug
	//---------
	/*
	 * 0 - Raw input
	 * 1 - Processed data
	 * 2 - HashMap
	 */
	private static final boolean debug[] = {true, false, false};
	
	//--------------
	// Constants
	//--------------
	private static final String HTTP_RESPONSE_STATUS_LINE_REGEX = "HTTP/1\\.(0|1) [1-5]\\d{2} .*";
	
	private static final int DEFAULT_CAPACITY = 23;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	//----------------
	// Data members
	//----------------
	private HashMap<String, List<String>> entries;
	private String version;
	private int statusCode;
	private String reason;
	
	//----------------
	// Constructors
	//----------------
	public HttpResponseHeader() {
		this.entries = new HashMap<String, List<String>>(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
		// this.version = null;
		this.statusCode = -1;
		// this.reason = null;
	}
	
	//----------------
	// Mutators
	//----------------
	
	public void parse(RawStreamReader receive) throws IOException {
		String line; int ti0;
		// Loop until response status line is found.
		while (!(line = new String(receive.readLine(), HttpClient.HTTP_HEADER_CHARSET).trim()).matches(HTTP_RESPONSE_STATUS_LINE_REGEX));
		if (debug[0])
			System.out.println(line);
		// Set HTTP version
		this.version = line.substring(line.indexOf("/") + 1, ti0 = line.indexOf(" "));
		// Set status code
		this.statusCode = Integer.parseInt(line.substring(ti0 + 1, ti0 + 4));
		// Set reason
		this.reason = line.substring(ti0 + 5);
		if (debug[1]) {
			System.out.println("Version: " + this.version) ;
			System.out.println("Status code: " + this.statusCode);
			System.out.println("Reason: " + this.reason);
		}
		
		// Clear all entries for new data.
		this.entries.clear();
		
		// Read the rest of the HTTP header and store the attributes.
		while (!(line = new String(receive.readLine(), HttpClient.HTTP_HEADER_CHARSET).trim()).isEmpty()) {
			if (debug[0])
				System.out.println(line);
			// Extract field name
			String field = line.substring(0, ti0 = line.indexOf(":")).trim().toLowerCase();
			// Extract value
			String value = line.substring(ti0 + 1).trim();
			
			if (debug[1])
				System.out.println("|Field: " + field + "|Value: " + value + "|");
			
			// Store the attributes
			List<String> tl0;
			if ((tl0 = this.entries.get(field)) != null) {
				tl0.add(value);
				if (debug[2])
					System.out.println(value + " added to " + field);
			}
			else {
				(tl0 = new LinkedList<String>()).add(value);
				this.entries.put(field, tl0);
				if (debug[2])
					System.out.println(field + " added and associated with " + value);
			}
		}
	}
	
	//----------------
	// Accessors
	//----------------
	
	public List<String> getValue(String fieldName) {
		fieldName = fieldName.trim().toLowerCase();
		return this.entries.get(fieldName);
	}
	
	public int getStatusCode() {
		return this.statusCode;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public String getReason() {
		return this.reason;
	}
}
