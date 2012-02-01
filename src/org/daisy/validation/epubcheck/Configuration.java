package org.daisy.validation.epubcheck;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.util.concurrent.MoreExecutors;

public class Configuration extends ReloadableConfiguration {

	public static final String PROPERTIES = "epubcheck-backend.properties";

	public static Configuration newConfiguration() {
		Configuration config = new Configuration();
		config.initAutoreload();
		return config;
	}

	public static enum Items implements Default {
		JAR("epubcheck.jar", "epubcheck/epubcheck-3.0b4.jar"), TIMEOUT(
				"epubcheck.timeout", "10"), TIMEOUT_UNIT(
				"epubcheck.timeout.unit", "MINUTES"), POOL_SIZE(
				"epubcheck.poolsize", "10");
		private String name;
		private String defaultValue;

		private Items(String name, String defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return defaultValue;
		}
	}

	protected Configuration() {
		super(EnumSet.allOf(Items.class));
	}

	@Override
	public String getPropertiesName() {
		return PROPERTIES;
	}

	public final Supplier<String> jar = new ConfigItem<String>(Items.JAR) {
		@Override
		protected String newValue(String string, String old) {
			return string;
		}
	};
	public final Supplier<Long> timeout = new ConfigItem<Long>(Items.TIMEOUT) {
		@Override
		protected Long newValue(String string, Long old)
				throws NumberFormatException {
			return Long.parseLong(string);
		}
	};

	public final Supplier<TimeUnit> timeoutUnit = new ConfigItem<TimeUnit>(
			Items.TIMEOUT_UNIT) {
		@Override
		protected TimeUnit newValue(String string, TimeUnit old) {
			return TimeUnit.valueOf(string);
		}
	};

	public final Supplier<ExecutorService> executorService = new ConfigItem<ExecutorService>(
			Items.POOL_SIZE) {

		@Override
		protected ExecutorService newValue(String string, ExecutorService old)
				throws NumberFormatException {
			final int size = Integer.parseInt(string);
			if (old != null) {
				old.shutdown();
			}
			return MoreExecutors
					.getExitingExecutorService((ThreadPoolExecutor) Executors
							.newFixedThreadPool(size));
		}
	};

}
