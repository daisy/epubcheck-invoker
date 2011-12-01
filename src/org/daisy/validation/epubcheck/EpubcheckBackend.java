package org.daisy.validation.epubcheck;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO: add build.xml that builds jar, tests.
// TODO: possibly add epubcheck-jar to our jar.
//       Then start the external pgm like this:
//       java -cp ourjar com.adobe.epubcheck.tool.Checker
//       But: At least saxon.jar is difficult to distribute in a repackaged jar.
// TODO: get the path to the epubcheck.jar relative to "play" fwk
// TODO: possibly add accessors for Error fields and make them private.

public class EpubcheckBackend {
	private static final String EPUBCHECK_JAR_PRODUCTION = "epubcheckbackend.jar";
	private static final String EPUBCHECK_JAR_IDE = "lib/epubcheck-3.0b3.jar";

	public static class Issue {
		public final String type;
		public final String file;
		public final int lineNo;
		public final int colNo;
		public final String txt;

		public Issue(final String theType, final String theFile,
				final int theLineNo, final int theColNo, final String theTxt) {
			type = theType;
			file = theFile;
			lineNo = theLineNo;
			colNo = theColNo;
			txt = theTxt;
		}

		public Issue(final String theType, final String theFile,
				final String theTxt) {
			this(theType, theFile, -1, -1, theTxt);
		}

		public String toString() {
			return "type:'"
					+ type
					+ "' file:'"
					+ file
					+ (lineNo != -1 ? "' (" + lineNo + "," + colNo + ") " : " ")
					+ "txt: '" + txt + "'";
		}
	}

	private static class ErrorParser {
		private State currentState;

		public ErrorParser(final String theEpubfile) {
			normalState = new NormalState(theEpubfile);
			currentState = normalState;
		}

		public List<Issue> getIssues() {
			return normalState.getIssues();
		}

		public void processLine(final String line) {
			currentState.processLine(line);
		}

		public void setCurrentState(State theCurrentState) {
			currentState = theCurrentState;
		}

		private final NormalState normalState;

		private final State fnfState = new FileNotFoundState();

		private final State confErrorState = new ConfError();

		private interface State {
			/**
			 * Processes a line
			 * 
			 * @param line
			 */
			public void processLine(final String line);
		}

		private class NormalState implements State {

			public List<Issue> getIssues() {
				return issues;
			}

			private final String epubFile;
			private final String[] entries;
			private final List<Issue> issues = new ArrayList<Issue>();

			public NormalState(final String theEpubFile) {
				epubFile = theEpubFile;
				entries = EpubcheckBackend.getEntriesInEpub(theEpubFile);
			}

			@Override
			public void processLine(final String line) {
				final Issue issue;
				if (line.matches(".*File .* does not exist!.*")) {
					final String file = line.replaceAll(
							".*File (.*) does not exist!.*", "$1");
					issues.add(new Issue("Exception", file, line));
					setCurrentState(fnfState);
				}
				else if (line.matches(".*java.lang.NoClassDefFoundError: .*")) {
					setCurrentState(confErrorState);
				}
				else if (line.length() == 0
						|| line.matches("Check finished with warnings or errors!")) {
					// skipping
				}
				else if ((issue = generateIssue(line, epubFile, entries)) != null) {
					issues.add(issue);
				}
				else {
					System.err.println("unexpected output on stderr:<" + line
							+ ">");
				}
			}
		}

		private class FileNotFoundState implements State {

			@Override
			public void processLine(final String line) {
				if (!line.matches("^\\s+.*")) {
					setCurrentState(normalState);
				}
			}
		}

		private class ConfError implements State {

			@Override
			public void processLine(final String line) {
				if (!line.matches("^\\s+.*") && !line.matches("^Caused by:.*")) {
					setCurrentState(normalState);
				}
			}
		}
	}

	/**
	 * Retrieves entries in the given epub.
	 * TODO: Is only used by NormalState and I would like to move it there. But
	 * then I don't know how to test it, since NormalState isn't needed
	 * elsewhere than within the private ErrorParser thus should remain private
	 * itself.
	 * 
	 * @param epubFilename
	 *            the name of the epub to get the entries for.
	 * @return the array of entries in the given epub
	 * @throws IOException
	 */
	public static String[] getEntriesInEpub(final String epubFilename) {
		ZipFile zf = null;
		try {
			zf = new ZipFile(epubFilename);
		} catch (IOException e) {
			System.err
					.println("an error occurred getting entries of epubfile '"
							+ epubFilename + "', but we'll continue anyway ...");
			e.printStackTrace(System.err);
			return null;
		}
		final Enumeration<? extends ZipEntry> k = zf.entries();
		final String[] entries = new String[zf.size()];
		int i = 0;
		while (k.hasMoreElements()) {
			final ZipEntry ze = k.nextElement();
			entries[i++] = ze.getName();
		}
		return entries;
	}

	/**
	 * Runs epubcheck on the given epub-File
	 * 
	 * @param epubFile
	 *            epub-File to run the tests on.
	 * @throws IOException
	 */
	public static List<Issue> run(final String epubFile) {

		final Runtime rt = Runtime.getRuntime();
		Process process;
		try {

			// It's really ugly, but we're lumping together the class path
			// for both development environment (IDE) and production.
			// They're different, because we simplify the deployment of
			// the jars by keeping them in one directory, but in the IDE we keep
			// them in separate directories (to keep track of what's epubcheck
			// and what's their third party libs.
			final String[] jarNames = new String[] {
					"commons-compress-1.2.jar", "flute.jar", "jing.jar",
					"sac.jar", "saxon9he.jar", EPUBCHECK_JAR_PRODUCTION };

			final String jarsInProduction = join(jarNames, "lib");
			final String jarsInIDE = join(jarNames, "lib/lib");

			final String cmdLine = "java -cp " + jarsInProduction + ":"
					+ jarsInIDE + ":" + EPUBCHECK_JAR_IDE
					+ " com.adobe.epubcheck.tool.Checker " + epubFile;
			// System.err.println("cmdLine:<" + cmdLine + ">");
			process = rt.exec(cmdLine);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final ErrorParser errorParser = new ErrorParser(epubFile);

		processStderr(process, errorParser);

		processStdout(process);

		// sync threads
		try {
			process.waitFor();
		} catch (final InterruptedException e) {
			// ignore
		}

		return errorParser.getIssues();
	}

	private static void processStderr(final Process process,
			final ErrorParser errorParser) {
		final DataPump stderrProcessor = new DataPump(process.getErrorStream(),
				new LineProcessor() {
					@Override
					public void process(final String line) {
						errorParser.processLine(line);
					}
				});
		new FutureTask<Object>(stderrProcessor, null).run();
	}

	private static void processStdout(final Process theProcess) {
		final DataPump stdoutProcessor = new DataPump(
				theProcess.getInputStream(), new LineProcessor() {
					private final String[] expected = new String[] {
							"Epubcheck Version ", "",
							"No errors or warnings detected." };
					private int i = 0;

					@Override
					public void process(final String line) {
						if (!line.startsWith(expected[i])) {
							System.err.println("Unexpected stdtout:<" + line
									+ "> on line " + (i + 1));
						}
						++i;
					}
				});
		new FutureTask<Object>(stdoutProcessor, null).run();
	}

	private static String join(final String[] jars, final String path) {
		final StringBuilder sb = new StringBuilder();
		for (final String jar : jars) {
			if (path != null & path.length() > 0) {
				sb.append(path);
				sb.append("/");
			}
			sb.append(jar);
			sb.append(":");
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Generates an issue from the given epubcheck output line, epubfilename,
	 * and entries of the epub.
	 * Returns null if the given line appears not to be an epubcheck issue.
	 * TODO: Is only used by NormalState and I would like to move it there. But
	 * then I don't know how to test it, since NormalState isn't needed
	 * elsewhere than within the private ErrorParser thus should remain private
	 * itself.
	 * 
	 * @param line
	 *            epubcheck output line
	 * @param theEpubFilename
	 *            epub filename
	 * @param entries
	 *            entries of the epub
	 * @return Issue represented by the given input.
	 */
	public static Issue generateIssue(final String line,
			final String theEpubFilename, final String[] entries) {

		final String regex = "^(ERROR|WARNING): (.*?)(?:\\((\\d+),(\\d+)\\))?: (.*)";
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		int i = 1;
		final String type = matcher.group(i++);

		String file = matcher.group(i++);
		if (entries != null && entries.length > 0) {
			file = normalizeFilename(entries, file);
			file = new File(theEpubFilename).getName()
					+ (file != null && file.length() > 0 ? "/" + file : "");
		}

		final String lineNoStr = matcher.group(i++);
		final String colNoStr = matcher.group(i++);
		int lineNo = -1;
		int colNo = -1;
		lineNo = getLineNo(lineNoStr);
		colNo = getLineNo(colNoStr);
		final String txt = matcher.group(i++);

		final Issue issue = new Issue(type, file, lineNo, colNo, txt);
		return issue;
	}

	/**
	 * Normalizes the given longFilename by checking whether any of the given
	 * epub-entries is a substring
	 * of the given longFilename and returning that.
	 * If no entry is found the empty string is returned.
	 * 
	 * @param entries
	 *            epub-entries
	 * @param longFilename
	 *            filename to check
	 * @return normalized filename
	 */
	private static String normalizeFilename(final String[] entries,
			final String longFilename) {
		for (final String filename : entries) {
			if (longFilename.endsWith(filename)) {
				return filename;
			}
		}
		return "";
	}

	/**
	 * Retrieves a (line or column) number from the given string, if possible.
	 * Otherwise returns -1.
	 * 
	 * @param lineNoStr
	 *            string to parse
	 * @return (line or column) number from the given string, if possible.
	 *         Otherwise returns -1
	 */
	private static int getLineNo(final String lineNoStr) {
		int lineNo = -1;
		if (lineNoStr != null && lineNoStr.length() > 0) {
			try {
				lineNo = Integer.parseInt(lineNoStr);
			} catch (NumberFormatException nfe) {

			}
		}
		return lineNo;
	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("\nPlease provide one epub filename\n");
			System.exit(1);
		}
		final List<Issue> issues = EpubcheckBackend.run(args[0]);
		System.out.println("found " + issues.size() + " issues.");
		for (final Issue issue : issues) {
			System.out.println(issue);
		}
	}
}
