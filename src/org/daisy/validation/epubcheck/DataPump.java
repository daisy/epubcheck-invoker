package org.daisy.validation.epubcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class DataPump implements Runnable {

	/**
	 * Source of data stream
	 */
	private final InputStream in;

	private String output;

	/**
	 * Connect pump from an Input Stream to a String Buffer
	 * 
	 * @param dataSource
	 *            Data Source
	 * @param buffer
	 *            Data Buffer (holding pond)
	 */
	public DataPump(final InputStream dataSource) {
		this.in = dataSource;
	}

	/**
	 * Connectors are in place. Begin moving data.
	 */
	public void run() {
		try {
			output = DataPump.pump(in);
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * This pump extracts text from an input stream into a String.
	 * 
	 * @param in
	 *            Data Source
	 * @param buffer
	 *            Holding Buffer
	 * 
	 * @return the string containing output of InputStream in
	 * 
	 * @throws IOException
	 *             if read/write fails
	 */
	/**
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static String pump(final InputStream in) throws IOException {
		final StringBuilder sb = new StringBuilder();
		final BufferedReader bReader = new BufferedReader(
				new InputStreamReader(in));
		try {
			final int bucketSize = 1024;
			final char[] bucket = new char[bucketSize];
			int drawn;
			while ((drawn = bReader.read(bucket)) >= 0) {
				sb.append(bucket, 0, drawn);
			}
		} finally {
			if (bReader != null) {
				bReader.close();
			}
		}
		return sb.toString();
	}

	/**
	 * @return the output
	 */
	public String getOutput() {
		return output;
	}

}