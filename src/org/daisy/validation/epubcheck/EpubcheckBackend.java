package org.daisy.validation.epubcheck;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.FutureTask;

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
