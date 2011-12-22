package org.daisy.validation.epubcheck;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.google.common.base.Function;

public final class OutputParser implements Function<InputStream, List<Issue>> {

	private final ErrorParser parser;

	public OutputParser(File epub) {
		parser = new ErrorParser(epub.getAbsolutePath());
	}

	@Override
	public List<Issue> apply(InputStream is) {
		new DataPump(is, new LineProcessor() {
			@Override
			public void process(final String line) {
				// System.out.println("line:" + line);
				parser.processLine(line);
			}
		}).run();
		return parser.getIssues();
	}
}
