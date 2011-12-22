package org.daisy.validation.epubcheck;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;

public final class OutputParser implements Function<InputStream, List<Issue>> {

	private final File epub;

	public OutputParser(File epub) {
		this.epub = epub;
	}

	@Override
	public List<Issue> apply(InputStream is) {
		LineProcessor<List<Issue>> parser = new StatefulParser(epub);
		new DataPump<List<Issue>>(is, parser).run();
		return parser.getResult();
	}

	private static class StatefulParser implements LineProcessor<List<Issue>> {

		private static enum State {
			PROCESS, IGNORE_STACK_TRACE
		};

		private State state = State.PROCESS;
		private final String[] entries;
		private final List<Issue> issues = Lists.newLinkedList();
		private final List<? extends LineProcessor<Issue>> processors = Lists
				.newArrayList(new IssueProcessor(), new VersionProcessor(),
						new IgnoreProcessor(), new ClassNotFoundProcessor(),
						new FileNotFoundProcessor(), new CatchAllProcessor());

		public StatefulParser(File epub) {
			// TODO lazy init
			this.entries = Utils.getEntriesInEpub(epub);
		}

		@Override
		public List<Issue> getResult() {
			return Collections.unmodifiableList(issues);
		}

		@Override
		public synchronized boolean processLine(String line) {
			switch (state) {
			case PROCESS:
				doProcessLine(line);
				break;
			case IGNORE_STACK_TRACE:
				if (!Patterns.INDENTED.matcher(line).matches()
						&& !Patterns.CAUSED_BY.matcher(line).matches()) {
					state = State.PROCESS;
					doProcessLine(line);
				}
				break;
			}
			return true;
		}

		public void doProcessLine(String line) {
			for (LineProcessor<Issue> processor : processors) {
				try {
					if (processor.processLine(line)) {
						if (processor.getResult() != null) {
							issues.add(processor.getResult());
						}
						return;
					}
				} catch (IOException e) {
					// TODO create issue
					e.printStackTrace();
				}
			}
			throw new RuntimeException("No line processor caught this line:"
					+ line);
		}

		private class ClassNotFoundProcessor extends GenericIssueProcessor {

			public ClassNotFoundProcessor() {
				super(Patterns.CLASS_NOT_FOUND, new Issue("Exception", null,
						"Classpath configuration error"));
			}

			@Override
			public void doProcess(MatchResult match) {
				state = State.IGNORE_STACK_TRACE;
			}
		}

		private class FileNotFoundProcessor extends GenericIssueProcessor {

			public FileNotFoundProcessor() {
				super(Patterns.FILE_NOT_FOUND);
			}

			@Override
			public void doProcess(MatchResult match) {
				issue = new Issue("Exception", match.group(1), match.group());
				state = State.IGNORE_STACK_TRACE;
			}

		}

		private class IssueProcessor extends GenericIssueProcessor {

			public IssueProcessor() {
				super(Patterns.ISSUE);
			}

			@Override
			public void doProcess(MatchResult matcher) {
				issue = new Issue(matcher.group(1), Utils.normalizeFilename(
						entries, matcher.group(2)), Utils.getLineNo(matcher
						.group(3)), Utils.getLineNo(matcher.group(4)),
						matcher.group(5));
			}

		}

		private class VersionProcessor extends GenericIssueProcessor {

			public VersionProcessor() {
				super(Patterns.VERSION);
			}

			@Override
			public void doProcess(MatchResult match) {
				issue = new Issue("Version", null, match.group(1));
			}

		}

		private static class IgnoreProcessor extends GenericIssueProcessor {

			public IgnoreProcessor() {
				super(Patterns.IRRELEVANT);
			}

		}

		private static class CatchAllProcessor extends GenericIssueProcessor {

			public CatchAllProcessor() {
				super(Patterns.ANY);
			}

			@Override
			public void doProcess(MatchResult matcher) {
				System.err.println("unexpected output on stderr:<"
						+ matcher.group() + ">");
			}
		}

		private static class GenericIssueProcessor implements
				LineProcessor<Issue> {

			protected Issue issue;
			private final Pattern pattern;

			public GenericIssueProcessor(Pattern pattern) {
				this(pattern, null);
			}

			public GenericIssueProcessor(Pattern pattern, Issue issue) {
				this.pattern = pattern;
				this.issue = issue;
			}

			@Override
			public Issue getResult() {
				return issue;
			}

			@Override
			public boolean processLine(String line) throws IOException {
				Matcher matcher = pattern.matcher(line);
				if (!matcher.matches()) {
					return false;
				}
				doProcess(matcher.toMatchResult());
				return true;
			}

			public void doProcess(MatchResult match) {
			}
		}
	}

}
