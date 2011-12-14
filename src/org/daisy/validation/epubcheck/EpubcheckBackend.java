package org.daisy.validation.epubcheck;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.FutureTask;

// TODO: possibly add epubcheck-3.0b3.jar and its dependencies to our jar.
//       Then start the external pgm like this:
//       java -cp ourjar com.adobe.epubcheck.tool.Checker
//       But: At least saxon.jar is difficult to distribute in a repackaged jar.
// TODO: possibly add accessors for Error fields and make them private.

public class EpubcheckBackend {
	// NOTE: this jar file's name (excluding the extension .jar) should
	// correspond to the project name of the build.xml file.
	private static final String EPUBCHECK_BACKEND_JAR = "epubcheckbackend.jar";

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

	private static final String[] jarNames = new String[] {
			"epubcheck-3.0b3.jar", "commons-compress-1.2.jar", "flute.jar",
			"jing.jar", "sac.jar", "saxon9he.jar", "guava-10.0.1.jar",
			EPUBCHECK_BACKEND_JAR };

	public static final String JAR_LIST = join(jarNames, ", ");

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

			final String jars = join(jarNames, ":", "lib");

			final String cmdLine = "java -cp " + jars + ":"
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
						System.out.println("line:" + line);
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

	/**
	 * Joins the Strings in array jars using separator sep.
	 * 
	 * NOTE: Not using com.google.common.base.Joiner from guava-10.0.1.jar,
	 * because I add it only to the classpath of the invoked Runtime
	 * 
	 * @param jars
	 *            Strings to join
	 * @param sep
	 *            separator to use joining strings
	 * @return
	 */
	private static String join(final String[] jars, final String sep) {
		return join(jars, sep, null);
	}

	/**
	 * Joins the Strings in array jars, optionally prepending each with path,
	 * using separator sep.
	 * 
	 * @param jars
	 *            Strings to join
	 * @param sep
	 *            separator to use joining strings
	 * @param path
	 *            to prepend to Strings prior to joining. Not used if null.
	 * @return
	 */
	private static String join(final String[] jars, final String sep,
			final String path) {
		final StringBuilder sb = new StringBuilder();
		for (final String jar : jars) {
			if (path != null && path.length() > 0) {
				sb.append(path);
				sb.append("/");
			}
			sb.append(jar);
			sb.append(sep);
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
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
