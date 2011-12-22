package org.daisy.validation.epubcheck;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;

public final class CommandExecutor<T> {

	private static final ScheduledExecutorService timedExecutor = Executors
			.newScheduledThreadPool(1);
	private static final ExecutorService cachedThreadPool = Executors
			.newCachedThreadPool();

	private final List<String> args;
	// TODO make timeout configurable
	private final long timeout = 10;
	private final TimeUnit timeoutUnit = TimeUnit.MINUTES;

	public CommandExecutor(List<String> args) {
		// TODO check args not null
		// TODO check args not empty
		// TODO set unmodifiable list
		this.args = args;
	}

	public T run(final Function<InputStream, T> streamProcessor) {

		ProcessBuilder processBuilder = new ProcessBuilder(args);
		processBuilder.redirectErrorStream(true);
		final Process process;
		try {
			process = processBuilder.start();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		Future<T> result = cachedThreadPool.submit(new Callable<T>() {
			@Override
			public T call() throws Exception {
				return streamProcessor.apply(process.getInputStream());
			}
		});
		

		// sync threads
		ScheduledFuture<?> interrupter = timedExecutor.schedule(
				new ThreadInterrupter(Thread.currentThread()), timeout,
				timeoutUnit);
		try {
			process.waitFor();
		} catch (final InterruptedException e) {
			process.destroy();
		} finally {
			interrupter.cancel(false);
			Thread.interrupted();
		}

		try {
			return result.get(timeout, timeoutUnit);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}

	}

	private final class ThreadInterrupter implements Runnable {
		private final Thread thread;

		public ThreadInterrupter(Thread thread) {
			this.thread = thread;
		}

		@Override
		public void run() {
			this.thread.interrupt();
		}

	}
}