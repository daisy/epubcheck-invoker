package org.daisy.validation.epubcheck;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.MoreExecutors;

public abstract class ReloadableConfiguration {

	private static final Logger LOG = LoggerFactory
			.getLogger(ReloadableConfiguration.class);

	public static interface Default {
		String getName();

		String getValue();
	}

	protected abstract class ConfigItem<T> implements Supplier<T> {
		private final Default def;
		private String currentValue;
		private Supplier<T> value;

		protected ConfigItem(Default def) {
			this.def = def;
			this.currentValue = def.getValue();
			this.value = Suppliers.memoize(new Supplier<T>() {
				@Override
				public T get() {
					return newValue(currentValue, null);
				}
			});
		}

		public final T get() {
			return get(config);
		}

		private T get(Map<String, String> config) {
			synchronized (ReloadableConfiguration.this) {
				String newString = config.get(def.getName());
				if (!currentValue.equals(newString)) {
					T old = value.get();
					try {
						value = Suppliers.ofInstance(newValue(newString, old));
						currentValue = newString;
					} catch (Exception e) {
						LOG.warn(
								"Bad value '{}' for '{}', reverting to default {}",
								new Object[] { newString, def.getName(),
										def.getValue() });
						if (!def.getValue().equals(currentValue)) {
							value = Suppliers.ofInstance(newValue(
									def.getValue(), old));
							currentValue = def.getValue();
						}
					}
				}
			}
			return value.get();
		}

		protected abstract T newValue(String string, T old);
	}

	private final File file;
	private long lastUpdate = 0L;
	private Map<String, String> config;
	private final Map<String, String> defaultConfig;

	protected ReloadableConfiguration(Iterable<? extends Default> defaults) {
		this.file = findProperties();
		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		for (Default def : defaults) {
			builder.put(def.getName(), def.getValue());
		}
		this.defaultConfig = builder.build();
		reload();
	}

	public abstract String getPropertiesName();

	private File findProperties() {
		File file = null;
		URL propsURL = getClass().getClassLoader().getResource(
				getPropertiesName());
		if (propsURL == null) {
			LOG.info("Configuration properties not found.");
		} else {
			try {
				file = new File(propsURL.toURI());
			} catch (URISyntaxException e) {
				LOG.warn("Couldn't load configuration properties: {}",
						e.getMessage());
			}
		}
		return file;
	}

	protected final void initAutoreload() {
		MoreExecutors.getExitingScheduledExecutorService(
				new ScheduledThreadPoolExecutor(1), 0, TimeUnit.SECONDS)
				.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						reload();
					}
				}, 1, 1, TimeUnit.MINUTES);

	}

	protected synchronized void reload() {
		if (file.lastModified() > lastUpdate) {
			InputStream is = null;
			try {
				is = new FileInputStream(file);
				Properties properties = new Properties();
				properties.load(is);
				lastUpdate = System.currentTimeMillis();
				Map<String, String> newConfig = Maps.newHashMap(defaultConfig);
				newConfig.putAll(Maps.fromProperties(properties));
				config = ImmutableMap.copyOf(newConfig);
			} catch (IOException e) {
				LOG.warn("Couldn't load configuration properties: {}",
						e.getMessage());
			} finally {
				Closeables.closeQuietly(is);
			}
		}
	}

}
