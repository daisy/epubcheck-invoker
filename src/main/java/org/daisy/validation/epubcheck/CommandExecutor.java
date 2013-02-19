package org.daisy.validation.epubcheck;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.google.common.io.LineProcessor;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

public final class CommandExecutor<T> {

	private final List<String> args;

	public CommandExecutor(List<String> args) {
		Preconditions.checkNotNull(args);
		for (String arg : args) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(arg));
		}
		this.args = ImmutableList.copyOf(args);
	}

	public CommandExecutor(String... args) {
		this(Arrays.asList(Preconditions.checkNotNull(args)));
	}

	public T run(final LineProcessor<T> lineProcessor, Long timeout,
			TimeUnit timeoutUnit) throws InterruptedException,
			UncheckedTimeoutException, Exception {
		Preconditions.checkNotNull(lineProcessor);

		return new SimpleTimeLimiter().callWithTimeout(new Callable<T>() {

			@Override
			public T call() throws Exception {
				final ProcessBuilder processBuilder = new ProcessBuilder(args);
				processBuilder.redirectErrorStream(true);
				Process process;
				process = processBuilder.start();
				try {
					new DataPump<T>(process.getInputStream(), lineProcessor)
							.run();
					process.waitFor();
				} finally {
					process.destroy();
					Closeables.closeQuietly(process.getInputStream());
					Closeables.closeQuietly(process.getOutputStream());
				}
				return lineProcessor.getResult();
			}
		}, timeout, timeoutUnit, true);
	}
}
