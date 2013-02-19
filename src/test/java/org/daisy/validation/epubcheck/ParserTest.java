package org.daisy.validation.epubcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ParserTest {
	
	private StatefulParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new StatefulParser(null);
	}

	@Test
	public void testIssuesNotNull() {
		assertNotNull(parser.getResult());
	}
	
	@Test
	public void testEpubCheckVersion() {
		parser.processLine("Epubcheck Version 3.0");
		List<Issue> issues = parser.getResult();
		assertEquals(1,issues.size());
		assertEquals(Issue.Type.EPUBCHECK_VERSION,issues.get(0).type);
		assertEquals("3.0",issues.get(0).txt);
	}
	
	@Test
	public void testEpubVersion() {
		parser.processLine("Validating against EPUB version 2.0");
		List<Issue> issues = parser.getResult();
		assertEquals(1,issues.size());
		assertEquals(Issue.Type.EPUB_VERSION,issues.get(0).type);
		assertEquals("2.0",issues.get(0).txt);
	}
	
	@Test
	public void testIgnores() {
		parser.processLine("");
		parser.processLine("Check finished with warnings or errors");
		List<Issue> issues = parser.getResult();
		assertEquals(0,issues.size());
	}
	
	@Test
	public void testValid() {
		parser.processLine("Epubcheck Version 3.0");
		parser.processLine("");
		parser.processLine("Validating against EPUB version 2.0");
		parser.processLine("No errors or warnings detected.");
		List<Issue> issues = parser.getResult();
		assertEquals(2,issues.size());
		assertEquals(Issue.Type.EPUBCHECK_VERSION,issues.get(0).type);
		assertEquals(Issue.Type.EPUB_VERSION,issues.get(1).type);
	}
	
	@Test
	public void testIssue_NoCol() {
		parser.processLine("WARNING: tycpp-sample.epub/OEBPS/Styles/stylesheet.css(1288): Token '<' not allowed here");
		List<Issue> issues = parser.getResult();
		assertEquals(1,issues.size());
		assertEquals(Issue.Type.WARNING,issues.get(0).type);
		assertEquals("Token '<' not allowed here",issues.get(0).txt);
		assertEquals("tycpp-sample.epub/OEBPS/Styles/stylesheet.css",issues.get(0).file);
		assertEquals(1288,issues.get(0).lineNo);
		assertEquals(-1,issues.get(0).colNo);
	}
	
	@Test
	public void testIssue_NoLineNoCol() {
		parser.processLine("WARNING: EmptyDir.epub: zip file contains empty directory emptyDir/");
		List<Issue> issues = parser.getResult();
		assertEquals(1,issues.size());
		assertEquals(Issue.Type.WARNING,issues.get(0).type);
		assertEquals("zip file contains empty directory emptyDir/",issues.get(0).txt);
		assertEquals("EmptyDir.epub",issues.get(0).file);
		assertEquals(-1,issues.get(0).lineNo);
		assertEquals(-1,issues.get(0).colNo);
	}
	
	@Test
	public void testIssue_Error() {
		parser.processLine("ERROR: invalid-ncx.epub/EPUB/lorem.ncx(20,46): 'ch1a': fragment identifier is not defined in 'EPUB/lorem.xhtml'");
		List<Issue> issues = parser.getResult();
		assertEquals(1,issues.size());
		assertEquals(Issue.Type.ERROR,issues.get(0).type);
		assertEquals("'ch1a': fragment identifier is not defined in 'EPUB/lorem.xhtml'",issues.get(0).txt);
		assertEquals("invalid-ncx.epub/EPUB/lorem.ncx",issues.get(0).file);
		assertEquals(20,issues.get(0).lineNo);
		assertEquals(46,issues.get(0).colNo);
	}
	
	@Test
	public void testException() {
		parser.processLine("java.lang.RuntimeException: For files other than epubs, mode must be specified! Default version is 3.0.");
		List<Issue> issues = parser.getResult();
		assertEquals(1,issues.size());
		assertEquals(Issue.Type.INTERNAL_ERROR,issues.get(0).type);
		assertEquals("For files other than epubs, mode must be specified! Default version is 3.0.",issues.get(0).txt);
		
	}
	
	@Test
	public void testException_IngoresStackTrace() {
		parser.processLine("java.lang.NullPointerException: name");
		parser.processLine("	at java.util.zip.ZipFile.getEntry(ZipFile.java:156)");
		parser.processLine("	at com.adobe.epubcheck.ocf.OCFZipPackage.getInputStream(OCFZipPackage.java:42)");
		parser.processLine("Validating against EPUB version 2.0");
		List<Issue> issues = parser.getResult();
		assertEquals(2,issues.size());
		assertEquals(Issue.Type.INTERNAL_ERROR,issues.get(0).type);
		assertEquals(Issue.Type.EPUB_VERSION,issues.get(1).type);
		
	}

}
