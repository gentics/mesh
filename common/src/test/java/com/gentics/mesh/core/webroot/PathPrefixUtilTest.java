package com.gentics.mesh.core.webroot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.data.Branch;

public class PathPrefixUtilTest {

	@Test
	public void testSanitize() {
		assertEquals("", PathPrefixUtil.sanitize(""));
		assertEquals("/bla", PathPrefixUtil.sanitize("bla"));
		assertEquals("/bla", PathPrefixUtil.sanitize("bla/"));
		assertEquals("", PathPrefixUtil.sanitize("/"));
	}

	@Test
	public void testStrip() {
		Branch branch = Mockito.mock(Branch.class);
		when(branch.getPathPrefix()).thenReturn("");
		assertEquals("", PathPrefixUtil.strip(branch, ""));

		when(branch.getPathPrefix()).thenReturn("abc");
		assertEquals("", PathPrefixUtil.strip(branch, ""));
		assertEquals("", PathPrefixUtil.strip(branch, "/abc"));
	}

	@Test
	public void testStartsWithPrefix() {
		Branch branch = Mockito.mock(Branch.class);
		when(branch.getPathPrefix()).thenReturn("cba");
		assertFalse(PathPrefixUtil.startsWithPrefix(branch, "/abc"));

		when(branch.getPathPrefix()).thenReturn("abc");
		assertTrue(PathPrefixUtil.startsWithPrefix(branch, "/abc"));
	}
}
