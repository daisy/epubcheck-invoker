package org.daisy.validation.epubcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.io.Closeables;
import com.google.common.io.LineProcessor;

public class DataPump<T> implements Runnable {

	private final InputStream is;
	private final LineProcessor<T> lineProcessor;

	/**
	 * Connect pump from an Input Stream to a String Buffer
	 */
	public DataPump(final InputStream is, final LineProcessor<T> lineProcessor) {
		this.is = is;
		this.lineProcessor = lineProcessor;
	}

	/**
	 * Extracts text line by line from an input stream into a LineProcessor.
	 * 
	 */
	public void run() {
		try {
			final BufferedReader bReader = new BufferedReader(
					new InputStreamReader(is));
			try {
				String line;
				while ((line = bReader.readLine()) != null) {
					lineProcessor.processLine(line);
				}
			} finally {
				Closeables.closeQuietly(bReader);
			}
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}