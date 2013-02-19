package org.daisy.validation.epubcheck;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

public final class Utils {

	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private Utils() {
		// static utils
	}

	/**
	 * Parses the string argument as a signed decimal integer, if possible.
	 * Otherwise returns -1.
	 * 
	 * @param string
	 *            string to parse
	 * @return the integer value represented by the argument, or -1 if the
	 *         parsing failed
	 */
	public static int toInt(final String string) {
		try {
			return (string != null && string.length() > 0) ? Integer
					.parseInt(string) : -1;
		} catch (NumberFormatException nfe) {
			LOG.warn("Couldn't parse '{}' into an integer", string);
			return -1;
		}
	}

	/**
	 * Lists the entries of the given ZIP file.
	 * 
	 * @param zip
	 *            the ZIP file to get the entries for.
	 * @return the list of entries of the given file, or an empty list if a
	 *         problem occurred
	 */
	public static List<String> getEntries(File zip) {
		List<String> entries = Lists.newLinkedList();
		ZipInputStream zis = null;
		if (zip != null) {
			try {
				zis = new ZipInputStream(new BufferedInputStream(
						new FileInputStream(zip)));
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					entries.add(entry.getName());
				}
			} catch (IOException e) {
				LOG.warn("Couldn't get ZIP entries", e);
			} finally {
				Closeables.closeQuietly(zis);
			}
		}
		return entries;
	}

	/**
	 * Normalizes the given file name by checking whether any of the given
	 * entries is a substring of it and returning that. If no entry is found the
	 * original string is returned.
	 * 
	 * @param entries
	 *            file entries
	 * @param filename
	 *            file name to check
	 * @return normalized filename
	 */
	public static String normalizeFilename(final List<String> entries,
			final String filename) {
		Preconditions.checkNotNull(filename);
		Preconditions.checkNotNull(entries);
		return Iterables.find(entries, new Predicate<String>() {
			@Override
			public boolean apply(String entry) {
				return filename.endsWith(entry);
			}
		}, filename);
	}

}
