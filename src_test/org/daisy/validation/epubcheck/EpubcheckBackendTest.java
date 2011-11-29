package org.daisy.validation.epubcheck;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class EpubcheckBackendTest {
	
	@Test
	public void testEpubcheckContainerNotOPF() throws IOException{
		EpubcheckBackend.run("resources/ContainerNotOPF.epub");
	}
	
	/**
	 * @throws IOException
	 */
	@Test
	public void testEpubcheckFileNotFound() throws IOException{
		final String epubFile = "xyxyources/ContainerNotOPF.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		final EpubcheckBackend.Issue issue = issues.get(0);
		assertEquals("Exception", issue.type);
		assertEquals(epubFile, issue.file);
		assertEquals("java.lang.RuntimeException: File "+epubFile+" does not exist!", issue.txt);
	}
	
	@Test
	public void testEpubcheckEmptyDir() throws IOException{
		final String epubFile = "resources/EmptyDir.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		final EpubcheckBackend.Issue issue = issues.get(0);
		assertEquals("WARNING", issue.type);
		assertEquals("EmptyDir.epub", issue.file);
		assertEquals("zip file contains empty directory emptyDir/", issue.txt);
	}
	
	@Test
	public void testInvalidUUID() throws IOException{
		final String epubFile = "resources/InvalidUUID.epub";
		assertEquals(0, EpubcheckBackend.run(epubFile).size());
	}
	
	@Test
	public void testIssue21() throws IOException{
		final String epubFile = "resources/Issue21.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;
		EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue21.epub/OEBPS/content.opf", issue.file);
		assertEquals("date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue21.epub/OEBPS/content.opf", issue.file);
		assertEquals("toc attribute references resource with non-NCX mime type; \"application/x-dtbncx+xml\" is expected", issue.txt);
	}
	
	@Test
	public void testError(){
		final String msg = "ERROR: resources/Issue21.epub/OEBPS/content.opf(9,12): date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string";
		final EpubcheckBackend.Issue issue = EpubcheckBackend.generateIssue(msg, "resources/Issue21.epub", null);
		assertEquals("ERROR", issue.type);
		assertEquals("resources/Issue21.epub/OEBPS/content.opf", issue.file);
		assertEquals("date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string", issue.txt);
	}
	
	@Test
	public void testIssue25() throws IOException{
		final String epubFile = "resources/Issue25.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		int i = 0;
		assertEquals(8, issues.size());

		EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue25.epub/mimetype", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("Mimetype file should contain only the string \"application/epub+zip\".", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue25.epub/OEBPS/content.opf", issue.file);
		assertEquals(35, issue.lineNo);
		assertEquals(9, issue.colNo);
		assertEquals("element \"spine\" missing required attribute \"toc\"", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue25.epub/OEBPS/title_page.html", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("Only UTF-8 and UTF-16 encodings are allowed for XML, detected ISO-8859-1", issue.txt);

		issue = issues.get(i++);
		assertEquals("WARNING", issue.type);
		assertEquals("Issue25.epub", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("item (OEBPS/toc.html) exists in the zip file, but is not declared in the OPF file", issue.txt);

		issue = issues.get(i++);
		assertEquals("WARNING", issue.type);
		assertEquals("Issue25.epub", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("item (OEBPS/stylesheet.css) exists in the zip file, but is not declared in the OPF file", issue.txt);

		issue = issues.get(i++);
		assertEquals("WARNING", issue.type);
		assertEquals("Issue25.epub", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("item (OEBPS/page-template.xpgt) exists in the zip file, but is not declared in the OPF file", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue25.epub/OEBPS/title_page.html", issue.file);
		assertEquals(7, issue.lineNo);
		assertEquals(64, issue.colNo);
		assertEquals("'OEBPS/stylesheet.css': referenced resource exists, but not declared in the OPF file", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue25.epub/OEBPS/chapter01.html", issue.file);
		assertEquals(9, issue.lineNo);
		assertEquals(100, issue.colNo);
		assertEquals("'OEBPS/page-template.xpgt': referenced resource exists, but not declared in the OPF file", issue.txt);
	}
	
	@Test
	public void testIssue95() throws IOException{
		final String epubFile = "resources/Issue95.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		final EpubcheckBackend.Issue issue = issues.get(0);
		assertEquals("ERROR", issue.type);
		assertEquals("Issue95.epub/META-INF/container.xml", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("No rootfiles with media type 'application/oebps-package+xml'", issue.txt);
	}
	
	@Test
	public void testIssueJoyce() throws IOException{
		final String epubFile = "resources/joyce-a-portrait-of-the-artist-as-a-young-man.epub";
		List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(0, issues.size());
	}
	
	@Test
	public void testIssueMetaInfoNotOPF() throws IOException{
		final String epubFile = "resources/MetaInfNotOPF.epub";
		List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(0, issues.size());
	}
	
	@Test
	public void testNon8601Date() throws IOException{
		final String epubFile = "resources/Non8601Date.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		final EpubcheckBackend.Issue issue = issues.get(0);
		assertEquals(1, issues.size());
		assertEquals("ERROR", issue.type);
		assertEquals("Non8601Date.epub/OEBPS/content.opf", issue.file);
		assertEquals(10, issue.lineNo);
		assertEquals(35, issue.colNo);
		assertEquals("date value 'May 21, 2011' is not valid as per http://www.w3.org/TR/NOTE-datetime:[For input string: \"May 21, 2011\"] is not an integer", issue.txt);
	}
	
	@Test
	public void testNullDate() throws IOException{
		final String epubFile = "resources/NullDate.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		final EpubcheckBackend.Issue issue = issues.get(0);
		assertEquals(1, issues.size());
		assertEquals("ERROR", issue.type);
		assertEquals("NullDate.epub/OEBPS/content.opf", issue.file);
		assertEquals(10, issue.lineNo);
		assertEquals(23, issue.colNo);
		assertEquals("date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string", issue.txt);
	}
	
	@Test
	public void testOPFIllegalElement_UniqueID() throws IOException{
		final String epubFile = "resources/OPFIllegalElement_UniqueID.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;

		EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("OPFIllegalElement_UniqueID.epub/OEBPS/content.opf", issue.file);
		assertEquals(2, issue.lineNo);
		assertEquals(9, issue.colNo);
		assertEquals("element \"hello\" not allowed anywhere; expected element \"metadata\"", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("OPFIllegalElement_UniqueID.epub/OEBPS/content.opf", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("unique-identifier attribute in package element must reference an existing identifier element id", issue.txt);
	}
	
	@Test
	public void testOPFIllegalElement() throws IOException{
		final String epubFile = "resources/OPFIllegalElement.epub";
		List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		int i = 0;

		final EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("OPFIllegalElement.epub/OEBPS/content.opf", issue.file);
		assertEquals(2, issue.lineNo);
		assertEquals(8, issue.colNo);
		assertEquals("element \"hello\" not allowed anywhere; expected element \"metadata\"", issue.txt);

	}
	
	@Test
	public void testPageMap() throws IOException{
		final String epubFile = "resources/PageMap.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(1, issues.size());
		int i = 0;

		final EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("PageMap.epub/OEBPS/content.opf", issue.file);
		assertEquals(26, issue.lineNo);
		assertEquals(33, issue.colNo);
		assertEquals("attribute \"page-map\" not allowed here; expected attribute \"id\"", issue.txt);

	}
	
	@Test
	public void testTest() throws IOException{
		final String epubFile = "resources/Test.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(0, issues.size());
	}
	
	@Test
	public void testUniqueIDNotUsed() throws IOException{
		final String epubFile = "resources/UniqueIDNotUsed.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;

		EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("UniqueIDNotUsed.epub/OEBPS/content.opf", issue.file);
		assertEquals(9, issue.lineNo);
		assertEquals(12, issue.colNo);
		assertEquals("date value '' is not valid as per http://www.w3.org/TR/NOTE-datetime:zero-length string", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("UniqueIDNotUsed.epub/OEBPS/content.opf", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("unique-identifier attribute in package element must reference an existing identifier element id", issue.txt);

	}
	
	@Test
	public void testUnmanifested() throws IOException{
		final String epubFile = "resources/Unmanifested.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;

		EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("WARNING", issue.type);
		assertEquals("Unmanifested.epub", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("item (Unmanifested.txt) exists in the zip file, but is not declared in the OPF file", issue.txt);

		issue = issues.get(i++);
		assertEquals("WARNING", issue.type);
		assertEquals("Unmanifested.epub", issue.file);
		assertEquals(-1, issue.lineNo);
		assertEquals(-1, issue.colNo);
		assertEquals("item (OEBPS/Unmanifested2.txt) exists in the zip file, but is not declared in the OPF file", issue.txt);

	}
	
	@Test
	public void testUnmanifestedGuideItems() throws IOException{
		final String epubFile = "resources/UnmanifestedGuideItems.epub";
		final List<EpubcheckBackend.Issue> issues = EpubcheckBackend.run(epubFile);
		assertEquals(2, issues.size());
		int i = 0;

		EpubcheckBackend.Issue issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("UnmanifestedGuideItems.epub/OEBPS/content.opf", issue.file);
		assertEquals(29, issue.lineNo);
		assertEquals(67, issue.colNo);
		assertEquals("File listed in reference element in guide was not declared in OPF manifest: OEBPS/toc.html", issue.txt);

		issue = issues.get(i++);
		assertEquals("ERROR", issue.type);
		assertEquals("UnmanifestedGuideItems.epub/OEBPS/content.opf", issue.file);
		assertEquals(29, issue.lineNo);
		assertEquals(67, issue.colNo);
		assertEquals("'OEBPS/toc.html': referenced resource missing in the package", issue.txt);

	}
	
	@Test
	public void testGetPathToEpubRoot() throws IOException{
		EpubcheckBackend.getEntriesInEpub("resources/Issue25.epub");
	}
}
