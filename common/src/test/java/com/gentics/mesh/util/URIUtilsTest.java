package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class URIUtilsTest {

	@Test
	public void testSegmentEncoder() {
		// Assert that RFC3986 Sub-delims are not escaped
		List<String> mustNotEscape = Arrays.asList("!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "=");
		for (String input : mustNotEscape) {
			assertEquals("The string should not have been escaped", input, URIUtils.encodeSegment(input));
		}
		// Check other chars
		assertEquals("slash", "%2F", URIUtils.encodeSegment("/"));
		assertEquals("questionmark", "%3F", URIUtils.encodeSegment("?"));
		assertEquals("space", "%20", URIUtils.encodeSegment(" "));
		assertEquals("character ä","%C3%A4",  URIUtils.encodeSegment("ä"));
		assertEquals("character ü","%C3%BC", URIUtils.encodeSegment("ü"));
	}
}
