package org.daisy.validation.epubcheck;

import java.util.regex.Pattern;

final class Patterns {

	static final Pattern ANY = Pattern.compile(".*");
	static final Pattern CAUSED_BY = Pattern.compile("^Caused by:");
	static final Pattern CLASS_NOT_FOUND = Pattern
			.compile(".*java.lang.NoClassDefFoundError: .*");
	static final Pattern FILE_NOT_FOUND = Pattern
			.compile(".*File (.*) does not exist!.*");
	static final Pattern INDENTED = Pattern.compile("^\\s+");
	static final Pattern IRRELEVANT = Pattern
			.compile("^\\s*$|Check finished with warnings or errors!|Epubcheck Version \\S+|No errors or warnings detected.");
	static final Pattern ISSUE = Pattern
			.compile("^(ERROR|WARNING): (.*?)(?:\\((\\d+)(,(\\d+))?\\))?: (.*)");
	static final Pattern EPUB_VERSION = Pattern
			.compile("Validating against EPUB version (\\S+)");
	static final Pattern EPUBCHECK_VERSION = Pattern.compile("Epubcheck Version (.*)");

}