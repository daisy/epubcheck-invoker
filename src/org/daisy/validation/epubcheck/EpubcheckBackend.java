package org.daisy.validation.epubcheck;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import org.daisy.validation.epubcheck.Issue.Type;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;

public final class EpubcheckBackend {

	private static final EpubcheckBackend INSTANCE = new EpubcheckBackend();

	public static List<Issue> run(String file) {
		return INSTANCE.validate(file);
	}

	// TODO externalize jar names (to not have to rebuild after changes to
	// epubcheck)
	private static final String[] jars = new String[] { "epubcheck-3.0b4.jar",
			"commons-compress-1.2.jar", "cssparser-0.9.6.jar", "jing.jar",
			"sac-1.3.jar", "saxon9he.jar" };

	// TODO get number of threads from config
	private final ExecutorService executor = MoreExecutors
			.getExitingExecutorService((ThreadPoolExecutor) Executors
					.newFixedThreadPool(10));

	public List<Issue> validate(String epubPath) {
		return validate(new File(epubPath));
	}

	public List<Issue> validate(final File epubFile) {
		Future<List<Issue>> result = executor
				.submit(new Callable<List<Issue>>() {

					@Override
					public List<Issue> call() {
						return doValidate(epubFile);
					}
				});
		try {
			return result.get();
		} catch (InterruptedException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					"InterruptedException - " + e.getMessage()));
		} catch (ExecutionException e) {
			// Shouldn't happen
			throw new RuntimeException(e);
		}
	}

	private List<Issue> doValidate(File epub) {
		CommandExecutor<List<Issue>> cmdExec = new CommandExecutor<List<Issue>>(
				Lists.newArrayList(
						"java",
						"-cp",
						Joiner.on(File.pathSeparatorChar).join(
								Lists.transform(Lists.newArrayList(jars),
										new Function<String, String>() {
											@Override
											public String apply(String jar) {
												return "lib/" + jar;
											}
										})),
						"com.adobe.epubcheck.tool.Checker", epub.getPath()));
		try {
			return cmdExec.run(new OutputParser(epub));
		} catch (InterruptedException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					"InterruptedException - " + e.getMessage()));
		} catch (ExecutionException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,

			e.getCause().getClass().getSimpleName() + " - " + e.getMessage()));
		} catch (TimeoutException e) {
			return Lists.newArrayList(new Issue(Type.INTERNAL_ERROR,
					"Process timed out"));
		}
	}
}
