package org.daisy.validation.epubcheck;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class UtilsTest {

	@Test
	public void testGetEntries() {
		List<String> entries = Utils.getEntries(new File(
				"src/test/resources/epub/valid.epub"));
		List<String> expected = Lists.newArrayList("mimetype","EPUB/lorem.xhtml","EPUB/lorem.css","EPUB/lorem.opf","META-INF/container.xml");
		assertThat(entries, is(expected));
	}

	@Test
	public void testNormalizeFilename() {
		List<String> entries = Lists.newArrayList("mimetype","EPUB/lorem.xhtml","EPUB/lorem.css","EPUB/lorem.opf","META-INF/container.xml");
		assertEquals("mimetype",
				Utils.normalizeFilename(entries, "foo/bar/mimetype"));
		assertEquals("foo/bar/notfound",
				Utils.normalizeFilename(entries, "foo/bar/notfound"));
	}

	@Test
	public void testToInt() {
		assertEquals(3453, Utils.toInt("3453"));
		assertEquals(-1, Utils.toInt("foobar"));
	}

}
