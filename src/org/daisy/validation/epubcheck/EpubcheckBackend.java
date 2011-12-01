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

	/**
	 * Retrieves entries in the given epub.
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
		final List<Issue> issues = new ArrayList<Issue>();

		final Runtime rt = Runtime.getRuntime();
		Process process;
		try {

			// It's really ugly, but we're lumping together the class path
			// for both development environment (IDE) and production.
			// They're different, because we simplify the deployment of
			// the jars in one directory, but in the IDE we keep them in
			// separate directories (to keep track of what's epubcheck and
			// what's their third party libs.
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

		final DataPump errCapture = new DataPump(process.getErrorStream());
		new FutureTask<Object>(errCapture, null).run();
		final DataPump outCapture = new DataPump(process.getInputStream());
		new FutureTask<Object>(outCapture, null).run();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			// ignore
		}

		// TODO:
		// pass a callback to the task and let it do the parsing
		// instead of collecting all the output, split the lines and then parse
		// them!
		final String stdout = outCapture.getOutput();
		final String[] stdouts = stdout.split("\n");
		checkStdout(stdouts);
		final String stderr = errCapture.getOutput();
		final String[] stderrs = stderr.split("\n");

		final String[] entries = getEntriesInEpub(epubFile);

		Issue issue;
		int i = 0;
		while (i < stderrs.length) {
			// System.out.println("line:<"+line+">");
			if (stderrs[i].matches(".*File .* does not exist!.*")) {
				String file = stderrs[i].replaceAll(
						".*File (.*) does not exist!.*", "$1");
				issues.add(new Issue("Exception", file, stderrs[i]));
				++i;
				while ((i < stderrs.length) && stderrs[i++].matches("^\\s+.*")) {
					// read on while output is indented
					// because it's still part of the same error.
				}
			}
			else if (stderrs[i].matches(".*java.lang.NoClassDefFoundError: .*")) {
				final String ERROR_MSG = "\"lib\" directory required in cwd containing: 1) "
						+ new File(EPUBCHECK_JAR_PRODUCTION).getName()
						+ " and 2) \"lib\" directory, containing: commons-compress-1.2.jar, flute.jar, jing.jar, sac.jar, saxon9he.jar.";
				System.err.println();
				System.err.println(ERROR_MSG);
				System.err.println();
				issues.add(new Issue("EpubcheckBackend Configuration Error",
						"", ERROR_MSG));
				System.out.println("original error:" + stderrs[i]);
				++i;
				String line;
				while ((line = stderrs[i++]) != null
						&& (line.matches("^\\s+.*") || line
								.matches("^Caused by:.*"))) {
					// read on while output is indented
					// because it's still part of the same error.
				}
			}
			else if ((issue = generateIssue(stderrs[i], epubFile, entries)) != null) {
				issues.add(issue);
			}
			else if (stderrs[i]
					.matches("Check finished with warnings or errors!")
					|| stderrs[i].length() == 0) {
				// skipping
			}
			else {
				System.err.println("unexpected output on stderr:<" + stderrs[i]
						+ ">");
			}
			++i;
		}
		return issues;
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

	/**
	 * Checks whether the output on stdout given by epubcheck complies with our
	 * expectation.
	 * 
	 * @param process
	 *            The process running epubcheck.
	 * @throws IOException
	 */
	private static void checkStdout(final String[] stdouts) {

		final String[] expected = new String[] { "Epubcheck Version ", "",
				"No errors or warnings detected." };
		int i = 0;
		while (i < stdouts.length && i < expected.length) {
			if (!stdouts[i].startsWith(expected[i])) {
				System.err.println("Unexpected stdtout:<" + stdouts[i]
						+ "> on line " + (i + 1));
			}
			i++;
		}
	}

	public static void main(String[] args) throws IOException {
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
