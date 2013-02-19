package org.daisy.validation.epubcheck;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.daisy.validation.epubcheck.Issue.Type;
import org.junit.Before;
import org.junit.Test;

public class EpubCheckIT {
	

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testValid() {
		List<Issue> issues = EpubCheckInvoker.run("src/test/resources/epub/valid.epub");
		assertEquals(2,issues.size());
		assertEquals(Issue.Type.EPUBCHECK_VERSION,issues.get(0).type);
		assertEquals(Issue.Type.EPUB_VERSION,issues.get(1).type);
	}
	
	@Test
	public void testInvalid() {
		List<Issue> issues = EpubCheckInvoker.run("src/test/resources/epub/invalid-ncx.epub");
		assertEquals(4,issues.size());
		assertEquals(Issue.Type.ERROR,issues.get(2).type);
		assertEquals(Issue.Type.ERROR,issues.get(3).type);
		assertEquals("'ch1a': fragment identifier is not defined in 'EPUB/lorem.xhtml'",issues.get(2).txt);
		assertEquals("EPUB/lorem.ncx",issues.get(2).file);
		assertEquals(20,issues.get(2).lineNo);
		assertEquals(46,issues.get(2).colNo);
	}
	

	@Test
	public void test_FileNotFound() throws IOException {
		List<Issue> issues = EpubCheckInvoker.run("foobar.epub");
		assertEquals(2, issues.size());
		assertEquals(Type.INTERNAL_ERROR, issues.get(1).type);
		assertEquals("File foobar.epub does not exist", issues.get(1).txt);
	}

}
