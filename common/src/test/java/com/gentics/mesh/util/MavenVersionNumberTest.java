package com.gentics.mesh.util;

import static com.gentics.mesh.util.MavenVersionNumber.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MavenVersionNumberTest {

	@Test
	public void testCompareVersions() {

		MavenVersionNumber a = parse("1.0.0");
		assertEquals("Should be 0 since same version", 0, a.compareTo(parse("1.0.0")));
		assertEquals("Should be 0 since same version", 1, a.compareTo(parse("0.9.9")));
		assertEquals("Should be -1 since newer version", -1, a.compareTo(parse("1.0.1")));
		assertEquals("Should be 1 since older(snapshot version)", 1, a.compareTo(parse("1.0.0-SNAPSHOT")));
		assertEquals("Should be -1 since newer version", -1, a.compareTo(parse("1.0.1-SNAPSHOT")));

		a = parse("1.0.0-SNAPSHOT");
		assertEquals("Should be 0 since same version", 0, a.compareTo(parse("1.0.0-SNAPSHOT")));
		assertEquals("Should be -1 since newer version", -1, a.compareTo(parse("1.0.1")));
		assertEquals("Should be -1 since newer version.", -1, a.compareTo(parse("1.0.0")));

		MavenVersionNumber b = parse("1.0.0-RC1-SNAPSHOT");
		assertEquals("RC1", b.getPostfix());
		assertTrue(b.isSnapshot());
		assertEquals("Should be the same", 0, b.compareTo(parse("1.0.0-RC1-SNAPSHOT")));
		assertEquals("Should be the same", -1, b.compareTo(parse("1.0.0")));
		assertEquals("Should be the same", -1, b.compareTo(parse("1.0.0-RC1")));

	}
}
