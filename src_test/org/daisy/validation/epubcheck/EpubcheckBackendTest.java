package org.daisy.validation.epubcheck;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.daisy.validation.epubcheck.Issue.Type;
import org.daisy.validation.epubcheck.StdoutStderrSaver.Hook;
import org.junit.Test;

import com.google.common.base.Splitter;

public class EpubcheckBackendTest {

	@Test
	public void testEpubcheckContainerNotOPF() throws IOException {
		EpubcheckBackend.run("resources/ContainerNotOPF.epub");
	}

	private class IssueHook implements Hook {
		public IssueHook(final String theEpubFile) {
			epubFile = theEpubFile;
		}

		public List<Issue> getIssues() {
			return issues;
		}

		@Override
		public void hook() {
			issues = EpubcheckBackend.run(epubFile);
		}

		private List<Issue> issues;
		private final String epubFile;
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void testEpubcheckFileNotFound() throws IOException {
		final String epubFile = "xyxyources/ContainerNotOPF.epub";

		final IssueHook hook = new IssueHook(epubFile);
		final String[] sysOutsysErr = StdoutStderrSaver.process(hook);
		final List<Issue> issues = hook.getIssues();
		assertEquals(1, issues.size());
		final Issue issue = issues.get(0);
		assertEquals(Type.INTERNAL_ERROR, issue.type);
		assertEquals(epubFile, issue.file);
		assertEquals("java.lang.RuntimeException: File " + epubFile
				+ " does not exist!", issue.txt);
		assertEquals(0, sysOutsysErr[0].length());
		assertTrue(sysOutsysErr[1]
				.matches("(?s:an error occurred getting entries of epubfile .*)"));
	}

	@Test
	public void testEpubcheckEmptyDir() throws IOException {
		final String epubFile = "resources/EmptyDir.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(3, issues.size());
		int i = 0;
		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);
		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals("title element is empty", issue.txt);
	}

	@Test
	public void testInvalidUUID() throws IOException {
		final String epubFile = "resources/InvalidUUID.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;
		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);
		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals("title element is empty", issue.txt);
	}

	@Test
	public void testIssue21() throws IOException {
		final String epubFile = "resources/Issue21.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(4, issues.size());
		int i = 0;
		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);
		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals("title element is empty", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(
				"date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string",
				issue.txt);
	}

	@Test
	public void testIssue25() throws IOException {
		final String epubFile = "resources/Issue25.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		int i = 0;
		assertEquals(9, issues.size());

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("mimetype", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"Mimetype file should contain only the string \"application/epub+zip\".",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(35, issue.lineNo);
		assertEquals(9, issue.colNo);
		assertEquals("element \"spine\" missing required attribute \"toc\"",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/title_page.html", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"Only UTF-8 and UTF-16 encodings are allowed for XML, detected ISO-8859-1",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"item (OEBPS/toc.html) exists in the zip file, but is not declared in the OPF file",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"item (OEBPS/stylesheet.css) exists in the zip file, but is not declared in the OPF file",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"item (OEBPS/page-template.xpgt) exists in the zip file, but is not declared in the OPF file",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/title_page.html", issue.file);
		assertEquals(7, issue.lineNo);
		assertEquals(64, issue.colNo);
		assertEquals(
				"'OEBPS/stylesheet.css': referenced resource exists, but not declared in the OPF file",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/chapter01.html", issue.file);
		assertEquals(9, issue.lineNo);
		assertEquals(100, issue.colNo);
		assertEquals(
				"'OEBPS/page-template.xpgt': referenced resource exists, but not declared in the OPF file",
				issue.txt);
	}

	@Test
	public void testIssue95() throws IOException {
		final String epubFile = "resources/Issue95.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		final Issue issue = issues.get(0);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("META-INF/container.xml", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"No rootfiles with media type 'application/oebps-package+xml'",
				issue.txt);
	}

	@Test
	public void testIssueJoyce() throws IOException {
		final String epubFile = "resources/joyce-a-portrait-of-the-artist-as-a-young-man.epub";
		List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		final Issue issue = issues.get(0);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

	}

	@Test
	public void testIssueMetaInfoNotOPF() throws IOException {
		final String epubFile = "resources/MetaInfNotOPF.epub";
		List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;
		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals("title element is empty", issue.txt);
	}

	@Test
	public void testNon8601Date() throws IOException {
		final String epubFile = "resources/Non8601Date.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(3, issues.size());
		int i = 0;
		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(5, issue.lineNo);
		assertEquals(15, issue.colNo);
		assertEquals("title element is empty", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(10, issue.lineNo);
		assertEquals(35, issue.colNo);
		assertEquals(
				"date value 'May 21, 2011' is not valid as per http://www.w3.org/TR/NOTE-datetime:[For input string: \"May 21, 2011\"] is not an integer",
				issue.txt);
	}

	@Test
	public void testNullDate() throws IOException {
		final String epubFile = "resources/NullDate.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(3, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(5, issue.lineNo);
		assertEquals(15, issue.colNo);
		assertEquals("title element is empty", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(10, issue.lineNo);
		assertEquals(23, issue.colNo);
		assertEquals(
				"date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string",
				issue.txt);
	}

	@Test
	public void testOPFIllegalElement_UniqueID() throws IOException {
		final String epubFile = "resources/OPFIllegalElement_UniqueID.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(4, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(2, issue.lineNo);
		assertEquals(9, issue.colNo);
		assertEquals(
				"element \"hello\" not allowed anywhere; expected element \"metadata\"",
				issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(5, issue.lineNo);
		assertEquals(13, issue.colNo);
		assertEquals("title element is empty", issue.txt);
	}

	@Test
	public void testOPFIllegalElement() throws IOException {
		final String epubFile = "resources/OPFIllegalElement.epub";
		List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(3, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(2, issue.lineNo);
		assertEquals(8, issue.colNo);
		assertEquals(
				"element \"hello\" not allowed anywhere; expected element \"metadata\"",
				issue.txt);

	}

	@Test
	public void testPageMap() throws IOException {
		final String epubFile = "resources/PageMap.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(26, issue.lineNo);
		assertEquals(33, issue.colNo);
		assertEquals(
				"attribute \"page-map\" not allowed here; expected attribute \"id\"",
				issue.txt);

	}

	@Test
	public void testTest() throws IOException {
		final String epubFile = "resources/Test.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
	}

	@Test
	public void testUniqueIDNotUsed() throws IOException {
		final String epubFile = "resources/UniqueIDNotUsed.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(4, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(4, issue.lineNo);
		assertEquals(13, issue.colNo);
		assertEquals("title element is empty", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(9, issue.lineNo);
		assertEquals(12, issue.colNo);
		assertEquals(
				"date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string",
				issue.txt);

	}

	@Test
	public void testUnmanifested() throws IOException {
		final String epubFile = "resources/Unmanifested.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(4, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(5, issue.lineNo);
		assertEquals(15, issue.colNo);
		assertEquals("title element is empty", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals(
				"item (Unmanifested.txt) exists in the zip file, but is not declared in the OPF file",
				issue.txt);

	}

	@Test
	public void testUnmanifestedGuideItems() throws IOException {
		final String epubFile = "resources/UnmanifestedGuideItems.epub";
		final List<Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(4, issues.size());
		int i = 0;

		Issue issue = issues.get(i++);
		assertEquals(Type.VERSION, issue.type);
		assertNull(issue.file);
		assertEquals("2.0", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.WARNING, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(5, issue.lineNo);
		assertEquals(15, issue.colNo);
		assertEquals("title element is empty", issue.txt);

		issue = issues.get(i++);
		assertEquals(Type.ERROR, issue.type);
		assertEquals("OEBPS/content.opf", issue.file);
		assertEquals(29, issue.lineNo);
		assertEquals(67, issue.colNo);
		assertEquals(
				"File listed in reference element in guide was not declared in OPF manifest: OEBPS/toc.html",
				issue.txt);

	}

	@Test
	public void testGetPathToEpubRoot() throws IOException, SecurityException,
			NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final String[] strs = Utils.getEntriesInEpub(new File(
				"resources/Issue25.epub"));
		final String expString = "[mimetype, META-INF/container.xml, OEBPS/chapter01.html, OEBPS/chapter02.html, OEBPS/chapter03.html, OEBPS/chapter04.html, OEBPS/chapter05.html, OEBPS/chapter06.html, OEBPS/chapter07.html, OEBPS/chapter08.html, OEBPS/chapter09.html, OEBPS/chapter10.html, OEBPS/chapter11.html, OEBPS/chapter12.html, OEBPS/content.opf, OEBPS/images/holmes.jpg, OEBPS/legal_preface.html, OEBPS/page-template.xpgt, OEBPS/stylesheet.css, OEBPS/title_page.html, OEBPS/toc.html, OEBPS/toc.ncx, OEBPS/trailing_legalese.html]";

		final List<String> expected = new ArrayList<String>();
		final Iterable<String> exp = Splitter.on(", ").split(
				expString.substring(1, expString.length() - 1));
		for (final String str : exp) {
			expected.add(str);
		}
		assertThat(Arrays.asList(strs), is(expected));
	}

	@Test
	public void testnormalizeFilename() throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final String entriesStr = "[mimetype, META-INF/container.xml, OEBPS/chapter01.html, OEBPS/chapter02.html, OEBPS/chapter03.html, OEBPS/chapter04.html, OEBPS/chapter05.html, OEBPS/chapter06.html, OEBPS/chapter07.html, OEBPS/chapter08.html, OEBPS/chapter09.html, OEBPS/chapter10.html, OEBPS/chapter11.html, OEBPS/chapter12.html, OEBPS/content.opf, OEBPS/images/holmes.jpg, OEBPS/legal_preface.html, OEBPS/page-template.xpgt, OEBPS/stylesheet.css, OEBPS/title_page.html, OEBPS/toc.html, OEBPS/toc.ncx, OEBPS/trailing_legalese.html]";
		final List<String> entries = new ArrayList<String>();
		final Iterable<String> exp = Splitter.on(", ").split(
				entriesStr.substring(1, entriesStr.length() - 1));
		for (final String str : exp) {
			entries.add(str);
		}
		assertEquals("mimetype", Utils.normalizeFilename(
				entries.toArray(new String[0]), "blabla/bloblo/mimetype"));
	}

	@Test
	public void testnormalizeFilenameKaputt() throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final String entriesStr = "[mimetype, META-INF/container.xml, OEBPS/chapter01.html, OEBPS/chapter02.html, OEBPS/chapter03.html, OEBPS/chapter04.html, OEBPS/chapter05.html, OEBPS/chapter06.html, OEBPS/chapter07.html, OEBPS/chapter08.html, OEBPS/chapter09.html, OEBPS/chapter10.html, OEBPS/chapter11.html, OEBPS/chapter12.html, OEBPS/content.opf, OEBPS/images/holmes.jpg, OEBPS/legal_preface.html, OEBPS/page-template.xpgt, OEBPS/stylesheet.css, OEBPS/title_page.html, OEBPS/toc.html, OEBPS/toc.ncx, OEBPS/trailing_legalese.html]";
		final List<String> entries = new ArrayList<String>();
		final Iterable<String> exp = Splitter.on(", ").split(
				entriesStr.substring(1, entriesStr.length() - 1));
		for (final String str : exp) {
			entries.add(str);
		}
		assertEquals("", Utils.normalizeFilename(
				entries.toArray(new String[0]), "blabla/bloblo/mimetypx"));
	}

	@Test
	public void testLineNo() throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final int NUM = 3453;
		assertEquals(NUM, Utils.getLineNo(Integer.toString(NUM)));
	}

	@Test
	public void testLineNoKaputt() throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		assertEquals(-1, Utils.getLineNo("hoho"));
	}

}
