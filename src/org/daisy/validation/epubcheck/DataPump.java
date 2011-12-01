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
	 * Extracts text line by line from an input stream into a
	 * LineProcessor.
	 * 
	 */
	public void run() {
		try {
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
		} catch (final IOException ex) {
			ex.printStackTrace(System.err);
		}
	}

}

interface LineProcessor {
	public void process(final String line);
}