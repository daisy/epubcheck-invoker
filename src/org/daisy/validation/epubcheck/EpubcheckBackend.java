package org.daisy.validation.epubcheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO: find out why checkStdout hangs on Moby Dick
// TODO: add build.xml that builds jar, tests.
// TODO: possibly add epubcheck-jar to our jar.
//       Then start the external pgm like this:
//       java -cp ourjar com.adobe.epubcheck.tool.Checker
//       But: At least saxon.jar is difficult to distribute in a repackaged jar.
//       It generates security errors.
// TODO: get the path to the epubcheck.jar relative to "play" fwk
// TODO: possibly add accessors for Error fields and make them private.

public class EpubcheckBackend {
	private static final String EPUBCHECK_JAR = "lib/epubcheck-3.0b3.jar";

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
			final String cmdLine = "java -cp " + EPUBCHECK_JAR
					+ " com.adobe.epubcheck.tool.Checker " + epubFile;
			// System.err.println("cmdLine:<"+cmdLine+">");
			process = rt.exec(cmdLine);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// hangs on Moby Dick. Why? Only on my mac! On Ubuntu it works...
		// checkStdout(process);

		final String[] entries = getEntriesInEpub(epubFile);

		final BufferedReader bre = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		try {
			String line;
			Issue issue;
			while ((line = bre.readLine()) != null) {
				// System.out.println("line:<"+line+">");
				if (line.matches(".*File .* does not exist!.*")) {
					String file = line.replaceAll(
							".*File (.*) does not exist!.*", "$1");
					issues.add(new Issue("Exception", file, line));
					while ((line = bre.readLine()) != null
							&& line.matches("^\\s+.*")) {
						// read on while output is indented
						// because it's still part of the same error.
					}
				}
				else if (line.matches(".*java.lang.NoClassDefFoundError: .*")) {
					final String ERROR_MSG = "\"lib\" directory required in cwd containing: 1) "
							+ new File(EPUBCHECK_JAR).getName()
							+ " and 2) \"lib\" directory, containing: commons-compress-1.2.jar, flute.jar, jing.jar, sac.jar, saxon9he.jar.";
					System.err.println();
					System.err.println(ERROR_MSG);
					System.err.println();
					issues.add(new Issue(
							"EpubcheckBackend Configuration Error", "",
							ERROR_MSG));
					System.out.println("original error:" + line);
					while ((line = bre.readLine()) != null
							&& (line.matches("^\\s+.*") || line
									.matches("^Caused by:.*"))) {
						// read on while output is indented
						// because it's still part of the same error.
					}
				}
				else if ((issue = generateIssue(line, epubFile, entries)) != null) {
					issues.add(issue);
				}
				else if (line
						.matches("Check finished with warnings or errors!")
						|| line.length() == 0) {
					// skipping
				}
				else {
					System.out.println("unexpected output on stderr:<" + line
							+ ">");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return issues;
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
	private static void checkStdout(final Process process) {

		final BufferedReader bri = new BufferedReader(new InputStreamReader(
				process.getInputStream()));

		String line;
		final String[] expected = new String[] { "Epubcheck Version ", "",
				"No errors or warnings detected." };
		int i = 0;
		try {
			while ((line = bri.readLine()) != null) {
				if (!line.startsWith(expected[i])) {
					System.err.println("Unexpected stdtout:<" + line
							+ "> on line " + (i + 1));
				}
				i++;
			}
		} catch (IOException e) {
			System.err
					.println("an error occurred checking stdout, but we'll continue anyway ...");
			e.printStackTrace(System.err);
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
