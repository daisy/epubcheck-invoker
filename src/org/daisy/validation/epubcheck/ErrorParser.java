package org.daisy.validation.epubcheck;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.daisy.validation.epubcheck.EpubcheckBackend.Issue;

class ErrorParser {
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

	private void setCurrentState(State theCurrentState) {
		currentState = theCurrentState;
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
	 * Generates an issue from the given epubcheck output line, epubfilename,
	 * and entries of the epub.
	 * Returns null if the given line appears not to be an epubcheck issue.
	 * TODO: Is only used by NormalState and I would like to move it there. But
	 * then I don't know how to test it, since NormalState isn't needed
	 * elsewhere than within ErrorParser thus should remain private.
	 * 
	 * @param line
	 *            epubcheck output line
	 * @param theEpubFilename
	 *            epub filename
	 * @param entries
	 *            entries of the epub
	 * @return Issue represented by the given input.
	 */
	public static EpubcheckBackend.Issue generateIssue(final String line,
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
			file = ErrorParser.normalizeFilename(entries, file);
			file = new File(theEpubFilename).getName()
					+ (file != null && file.length() > 0 ? "/" + file : "");
		}

		final String lineNoStr = matcher.group(i++);
		final String colNoStr = matcher.group(i++);
		int lineNo = -1;
		int colNo = -1;
		lineNo = ErrorParser.getLineNo(lineNoStr);
		colNo = ErrorParser.getLineNo(colNoStr);
		final String txt = matcher.group(i++);

		final EpubcheckBackend.Issue issue = new EpubcheckBackend.Issue(type,
				file, lineNo, colNo, txt);
		return issue;
	}

	/**
	 * Retrieves entries in the given epub.
	 * TODO: Is only used by NormalState and I would like to move it there. But
	 * then I don't know how to test it, since NormalState isn't needed
	 * elsewhere than within ErrorParser thus should remain private.
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

	/**
	 * Would like to move into NormalState, but:
	 * http://www.xyzws.com/Javafaq/why-an-interface-cant-be-defined-in-an-inner
	 * -class/56
	 * 
	 */
	interface LineHandler {
		boolean catches(final String line);

		void handle(final String line);
	}

	class NormalState implements State {

		public List<Issue> getIssues() {
			return issues;
		}

		private final String epubFile;
		private final String[] entries;
		private final List<Issue> issues = new ArrayList<Issue>();

		class FileNotFoundLineHandler implements LineHandler {

			@Override
			public boolean catches(final String line) {
				return line.matches(".*File .* does not exist!.*");
			}

			@Override
			public void handle(final String line) {
				final String file = line.replaceAll(
						".*File (.*) does not exist!.*", "$1");
				issues.add(new Issue("Exception", file, line));
				setCurrentState(fnfState);
			}
		}

		class ClassNotFoundLineHandler implements LineHandler {

			@Override
			public boolean catches(final String line) {
				return line.matches(".*java.lang.NoClassDefFoundError: .*");
			}

			@Override
			public void handle(final String line) {
				setCurrentState(confErrorState);
			}
		}

		class IrrelevantLineHandler implements LineHandler {

			@Override
			public boolean catches(final String line) {
				return line.length() == 0
						|| line.matches("Check finished with warnings or errors!");
			}

			@Override
			public void handle(final String line) {
				// irrelevant
				// we skip and remain in current normalState
			}
		}

		class IssueLineHandler implements LineHandler {

			private Issue issue;

			@Override
			public boolean catches(final String line) {
				issue = ErrorParser.generateIssue(line, epubFile, entries);
				return issue != null;
			}

			@Override
			public void handle(final String line) {
				issues.add(issue);
				// we remain in current normalState
			}
		}

		class CatchAllLineHandler implements LineHandler {

			@Override
			public boolean catches(final String line) {
				return true; // catch-all
			}

			@Override
			public void handle(final String line) {
				System.err
						.println("unexpected output on stderr:<" + line + ">");
				// we skip and remain in current normalState
			}
		}

		final List<LineHandler> lineHandlers = new ArrayList<LineHandler>();

		public NormalState(final String theEpubFile) {
			epubFile = theEpubFile;
			entries = ErrorParser.getEntriesInEpub(theEpubFile);
			lineHandlers.addAll(Arrays.asList(new LineHandler[] {
					new FileNotFoundLineHandler(),
					new ClassNotFoundLineHandler(),
					new IrrelevantLineHandler(), new IssueLineHandler(), }));
			// Attention:
			// The priority of LineHandlers is determined by their order in
			// the lineHandlers list.
			// Make sure CatchAllLineHandler comes last.
			lineHandlers.add(new CatchAllLineHandler());
		}

		@Override
		public void processLine(final String line) {

			int i = 0;
			while (i < lineHandlers.size()
					&& !lineHandlers.get(i).catches(line)) {
				++i;
			}
			if (i < lineHandlers.size()) {
				lineHandlers.get(i).handle(line);
			}
			else {
				throw new RuntimeException(
						"Programming Error: No LineHandler (" + lineHandlers
								+ ") caught this line:" + line);
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