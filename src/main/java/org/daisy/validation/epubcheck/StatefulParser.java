package org.daisy.validation.epubcheck;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.daisy.validation.epubcheck.Issue.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;

public class StatefulParser implements LineProcessor<List<Issue>> {

	private static final Logger LOG = LoggerFactory
			.getLogger(StatefulParser.class);

	private static enum State {
		PROCESS, IGNORE_STACK_TRACE
	};

	private StatefulParser.State state = State.PROCESS;
	private final Supplier<List<String>> entries;
	private final List<Issue> issues = Lists.newLinkedList();
	private final List<? extends LineProcessor<Issue>> processors = Lists
			.newArrayList(new IssueProcessor(),
					new EpubcheckVersionProcessor(),
					new EpubVersionProcessor(), new IgnoreProcessor(),
					new ClassNotFoundProcessor(), new FileNotFoundProcessor(),
					new ExceptionProcessor(), new CatchAllProcessor());

	public StatefulParser(final File epub) {
		this.entries = Suppliers.memoize(new Supplier<List<String>>() {
			@Override
			public List<String> get() {
				return Utils.getEntries(epub);
			}
		});
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

	private void doProcessLine(String line) {
		for (LineProcessor<Issue> processor : processors) {
			try {
				if (processor.processLine(line)) {
					if (processor.getResult() != null) {
						issues.add(processor.getResult());
					}
					return;
				}
			} catch (IOException e) {
				LOG.warn("Unexpected IOException: {}", e.getMessage());
				issues.add(new Issue(Type.INTERNAL_ERROR, e.getMessage()));
			}
		}
		throw new RuntimeException("No line processor caught this line:" + line);
	}

	private class ClassNotFoundProcessor extends
			StatefulParser.GenericIssueProcessor {

		public ClassNotFoundProcessor() {
			super(Patterns.CLASS_NOT_FOUND, new Issue(Type.INTERNAL_ERROR,
					"Classpath configuration error"));
		}

		@Override
		public void doProcess(MatchResult match) {
			state = State.IGNORE_STACK_TRACE;
		}
	}

	private class FileNotFoundProcessor extends
			StatefulParser.GenericIssueProcessor {

		public FileNotFoundProcessor() {
			super(Patterns.FILE_NOT_FOUND);
		}

		@Override
		public void doProcess(MatchResult match) {
			issue = new Issue(Type.INTERNAL_ERROR, match.group(1),
					match.group());
			state = State.IGNORE_STACK_TRACE;
		}

	}

	private class ExceptionProcessor extends StatefulParser.GenericIssueProcessor {

		public ExceptionProcessor() {
			super(Patterns.EXCEPTION);
		}

		@Override
		public void doProcess(MatchResult match) {
			issue = new Issue(Type.INTERNAL_ERROR, null,
					match.group(1));
			state = State.IGNORE_STACK_TRACE;
		}

	}

	private class IssueProcessor extends StatefulParser.GenericIssueProcessor {

		public IssueProcessor() {
			super(Patterns.ISSUE);
		}

		@Override
		public void doProcess(MatchResult matcher) {
			issue = new Issue(Type.safeValueOf(matcher.group(1)),
					Utils.normalizeFilename(entries.get(), matcher.group(2)),
					Utils.toInt(matcher.group(3)),
					Utils.toInt(matcher.group(5)), matcher.group(6));
		}

	}

	private class EpubVersionProcessor extends
			StatefulParser.GenericIssueProcessor {

		public EpubVersionProcessor() {
			super(Patterns.EPUB_VERSION);
		}

		@Override
		public void doProcess(MatchResult match) {
			issue = new Issue(Type.EPUB_VERSION, null, match.group(1));
		}

	}

	private static class EpubcheckVersionProcessor extends
			StatefulParser.GenericIssueProcessor {
		public EpubcheckVersionProcessor() {
			super(Patterns.EPUBCHECK_VERSION);
		}

		@Override
		public void doProcess(MatchResult match) {
			issue = new Issue(Type.EPUBCHECK_VERSION, null, match.group(1));
		}
	}

	private static class IgnoreProcessor extends
			StatefulParser.GenericIssueProcessor {

		public IgnoreProcessor() {
			super(Patterns.IRRELEVANT);
		}

	}

	private static class CatchAllProcessor extends
			StatefulParser.GenericIssueProcessor {

		public CatchAllProcessor() {
			super(Patterns.ANY);
		}

		@Override
		public void doProcess(MatchResult matcher) {
			System.err.println("unexpected output on stderr:<"
					+ matcher.group() + ">");
		}
	}

	private static class GenericIssueProcessor implements LineProcessor<Issue> {

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