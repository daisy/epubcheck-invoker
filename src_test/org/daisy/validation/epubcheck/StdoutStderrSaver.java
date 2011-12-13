package org.daisy.validation.epubcheck;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StdoutStderrSaver {

	public interface Hook {

		void hook();
	}

	/**
	 * @param hook
	 *            hook to be called and capturing its stdout, stderr
	 * @return Hook's stdout, stderr captured in a string
	 */
	public static String[] process(final Hook hook) {
		return new StdoutStderrSaver().processInternal(hook);
	}

	/**
	 * prevent instantiation
	 */
	private StdoutStderrSaver() {

	}

	private String[] processInternal(final Hook hook) {
		System.setOut(new PrintStream(sysout));
		System.setErr(new PrintStream(syserr));
		hook.hook();
		System.setOut(SYSTEM_OUT);
		System.setErr(SYSTEM_ERR);
		return new String[] { sysout.toString(), syserr.toString() };
	}

	private final ByteArrayOutputStream sysout = new ByteArrayOutputStream();
	private final ByteArrayOutputStream syserr = new ByteArrayOutputStream();
	private static final PrintStream SYSTEM_OUT = System.out;
	private static final PrintStream SYSTEM_ERR = System.err;

}