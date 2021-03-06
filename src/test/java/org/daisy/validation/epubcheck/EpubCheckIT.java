package org.daisy.validation.epubcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    public void testVersion() {
        String version = EpubCheckInvoker.version();
        assertFalse("unknown".equals(version));
    }

    @Test
    public void testValid() {
        List<Issue> issues = EpubCheckInvoker.run("src/test/resources/epub/valid.epub");
        assertEquals(1, issues.size());
        assertEquals(Issue.Type.EPUB_VERSION, issues.get(0).type);
    }

    @Test
    public void testValidWithInfo() {
        List<Issue> issues = EpubCheckInvoker.run("src/test/resources/epub/valid-info.epub");
        assertEquals(2, issues.size());
        assertEquals(Issue.Type.EPUB_VERSION, issues.get(0).type);
        assertEquals(Issue.Type.INFO, issues.get(1).type);
        assertEquals("OPF declares type 'edupub', validating using profile 'EDUPUB'.", issues.get(1).txt);
    }

    @Test
    public void testInvalid() {
        List<Issue> issues = EpubCheckInvoker.run("src/test/resources/epub/invalid-ncx.epub");
        assertEquals(3, issues.size());
        assertEquals(Issue.Type.ERROR, issues.get(1).type);
        assertEquals(Issue.Type.ERROR, issues.get(2).type);
        assertEquals("Fragment identifier is not defined.", issues.get(2).txt);
        assertEquals("EPUB/lorem.ncx", issues.get(2).file);
        assertEquals(20, issues.get(1).lineNo);
        assertEquals(46, issues.get(1).colNo);
    }

    @Test
    public void test_FileNotFound() throws IOException {
        List<Issue> issues = EpubCheckInvoker.run("foobar.epub");
        assertEquals(1, issues.size());
        assertEquals(Type.INTERNAL_ERROR, issues.get(0).type);
        assertEquals("File not found: 'foobar.epub'", issues.get(0).txt);
    }

}
