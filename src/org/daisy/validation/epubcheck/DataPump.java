package org.daisy.validation.epubcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DataPump implements Runnable {

	/**
	 * Source of data stream
	 */
	private final InputStream in;

	private final LineProcessor lineProcessor;

	/**
	 * Connect pump from an Input Stream to a String Buffer
	 * 
	 * @param dataSource
	 *            Data Source
	 * @param buffer
	 *            Data Buffer (holding pond)
	 */
	public DataPump(final InputStream dataSource,
			final LineProcessor theLineProcessor) {
		in = dataSource;
		lineProcessor = theLineProcessor;
	}

	/**
	 * Connectors are in place. Begin moving data.
	 */
	public void run() {
		try {
			pump(in);
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * Extracts text line by line from an input stream into a
	 * LineProcessor.
	 * 
	 * @param in
	 *            Data Source
	 * @throws IOException
	 *             if read/write fails
	 */
	private void pump(final InputStream in) throws IOException {
		final BufferedReader bReader = new BufferedReader(
				new InputStreamReader(in));
		try {
			String line;
			while ((line = bReader.readLine()) != null) {
				lineProcessor.process(line);
			}
		} finally {
			if (bReader != null) {
				bReader.close();
			}
		}
	}

}

interface LineProcessor {
	public void process(final String line);
}