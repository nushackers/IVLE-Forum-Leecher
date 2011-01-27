package org.nhahtdh;

import java.io.*;

/**
 * A data input stream lets an application read primitive Java data
 * types from an underlying input stream in a machine-independent
 * way. An application uses a data output stream to write data that
 * can later be read by a data input stream.
 * <p>
 * DataInputStream is not necessarily safe for multithreaded access.
 * Thread safety is optional and is the responsibility of users of
 * methods in this class.
 * <p>
 * RawStreamReader is originally DataInputStream. The readLine method
 * is a modified version of readLine method from BufferedReader to
 * return the newline character in the byte array.
 *
 * @author  Arthur van Hoff
 * @version 1.83, 05/05/07
 * @see     java.io.DataOutputStream
 * @since   JDK1.0
 */
public class RawStreamReader extends FilterInputStream {

	/**
	 * Creates a DataInputStream that uses the specified
	 * underlying InputStream.
	 *
	 * @param  in   the specified input stream
	 */
	public RawStreamReader(InputStream in) {
		super (in);
	}

	/**
	 * Reads some number of bytes from the contained input stream and 
	 * stores them into the buffer array <code>b</code>. The number of 
	 * bytes actually read is returned as an integer. This method blocks 
	 * until input data is available, end of file is detected, or an 
	 * exception is thrown. 
	 * 
	 * <p>If <code>b</code> is null, a <code>NullPointerException</code> is 
	 * thrown. If the length of <code>b</code> is zero, then no bytes are 
	 * read and <code>0</code> is returned; otherwise, there is an attempt 
	 * to read at least one byte. If no byte is available because the 
	 * stream is at end of file, the value <code>-1</code> is returned;
	 * otherwise, at least one byte is read and stored into <code>b</code>. 
	 * 
	 * <p>The first byte read is stored into element <code>b[0]</code>, the 
	 * next one into <code>b[1]</code>, and so on. The number of bytes read 
	 * is, at most, equal to the length of <code>b</code>. Let <code>k</code> 
	 * be the number of bytes actually read; these bytes will be stored in 
	 * elements <code>b[0]</code> through <code>b[k-1]</code>, leaving 
	 * elements <code>b[k]</code> through <code>b[b.length-1]</code> 
	 * unaffected. 
	 * 
	 * <p>The <code>read(b)</code> method has the same effect as: 
	 * <blockquote><pre>
	 * read(b, 0, b.length) 
	 * </pre></blockquote>
	 *
	 * @param      b   the buffer into which the data is read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> if there is no more data because the end
	 *             of the stream has been reached.
	 * @exception  IOException if the first byte cannot be read for any reason
	 * other than end of file, the stream has been closed and the underlying
	 * input stream does not support reading after close, or another I/O
	 * error occurs.
	 * @see        java.io.FilterInputStream#in
	 * @see        java.io.InputStream#read(byte[], int, int)
	 */
	public final int read(byte b[]) throws IOException {
		return in.read(b, 0, b.length);
	}

	/**
	 * Reads up to <code>len</code> bytes of data from the contained 
	 * input stream into an array of bytes.  An attempt is made to read 
	 * as many as <code>len</code> bytes, but a smaller number may be read, 
	 * possibly zero. The number of bytes actually read is returned as an 
	 * integer.
	 *
	 * <p> This method blocks until input data is available, end of file is
	 * detected, or an exception is thrown.
	 *
	 * <p> If <code>len</code> is zero, then no bytes are read and
	 * <code>0</code> is returned; otherwise, there is an attempt to read at
	 * least one byte. If no byte is available because the stream is at end of
	 * file, the value <code>-1</code> is returned; otherwise, at least one
	 * byte is read and stored into <code>b</code>.
	 *
	 * <p> The first byte read is stored into element <code>b[off]</code>, the
	 * next one into <code>b[off+1]</code>, and so on. The number of bytes read
	 * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
	 * bytes actually read; these bytes will be stored in elements
	 * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
	 * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
	 * <code>b[off+len-1]</code> unaffected.
	 *
	 * <p> In every case, elements <code>b[0]</code> through
	 * <code>b[off]</code> and elements <code>b[off+len]</code> through
	 * <code>b[b.length-1]</code> are unaffected.
	 *
	 * @param      b     the buffer into which the data is read.
	 * @param off the start offset in the destination array <code>b</code>
	 * @param      len   the maximum number of bytes read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> if there is no more data because the end
	 *             of the stream has been reached.
	 * @exception  NullPointerException If <code>b</code> is <code>null</code>.
	 * @exception  IndexOutOfBoundsException If <code>off</code> is negative, 
	 * <code>len</code> is negative, or <code>len</code> is greater than 
	 * <code>b.length - off</code>
	 * @exception  IOException if the first byte cannot be read for any reason
	 * other than end of file, the stream has been closed and the underlying
	 * input stream does not support reading after close, or another I/O
	 * error occurs.
	 * @see        java.io.FilterInputStream#in
	 * @see        java.io.InputStream#read(byte[], int, int)
	 */
	public final int read(byte b[], int off, int len)
	throws IOException {
		return in.read(b, off, len);
	}

	/**
	 * See the general contract of the <code>skipBytes</code>
	 * method of <code>DataInput</code>.
	 * <p>
	 * Bytes for this operation are read from the contained
	 * input stream.
	 *
	 * @param      n   the number of bytes to be skipped.
	 * @return     the actual number of bytes skipped.
	 * @exception  IOException  if the contained input stream does not support
	 *             seek, or the stream has been closed and
	 *             the contained input stream does not support 
	 *	           reading after close, or another I/O error occurs.
	 */
	public final int skipBytes(int n) throws IOException {
		int total = 0;
		int cur = 0;

		while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
			total += cur;
		}

		return total;
	}

	private byte lineBuffer[];

	/**
	 * See the general contract of the <code>readLine</code>
	 * method of <code>DataInput</code>.
	 * <p>
	 * Bytes
	 * for this operation are read from the contained
	 * input stream.
	 *
	 * @return     the next line of text from this input stream.
	 * @exception  IOException  if an I/O error occurs.
	 * @see        java.io.BufferedReader#readLine()
	 * @see        java.io.FilterInputStream#in
	 */
	public final byte[] readLine() throws IOException {
		byte buf[] = lineBuffer;

		if (buf == null) {
			buf = lineBuffer = new byte[128];
		}

		int room = buf.length;
		int offset = 0;
		int c;

		loop: while (true) {
			c = in.read();
			if (c == -1) {
				int n;
				byte[] tempbuf = new byte[2];
				do {
					n = in.read(tempbuf, 0, tempbuf.length);
				} while (n == 0); // Force read 1 character or return end of stream.

				if (n > 0) { // Some character read.
					// Push the array back to stream.
					if (!(in instanceof PushbackInputStream)) {
						this.in = new PushbackInputStream(in);
					}
					((PushbackInputStream) in).unread(tempbuf);
				} else // n < 0 which is EOF
					break loop;
			}
			else {
				if (--room < 0) {
					buf = new byte[offset + 128];
					room = buf.length - offset - 1;
					System.arraycopy(lineBuffer, 0, buf, 0, offset);
					lineBuffer = buf;
				}
				buf[offset++] = (byte) c;
				if (c == '\n')
					break loop;
			}
		}
		if ((c == -1) && (offset == 0)) {
			return null;
		}
		return java.util.Arrays.copyOfRange(buf, 0, offset);
	}
}

