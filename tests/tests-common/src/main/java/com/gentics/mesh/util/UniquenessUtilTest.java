package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UniquenessUtilTest {

	@Test
	public void testNaming() {
		assertEquals("test_1", UniquenessUtil.suggestNewName("test"));
		assertEquals("test_1", UniquenessUtil.suggestNewName("test_0"));
		assertEquals("test_2", UniquenessUtil.suggestNewName("test_1"));
		assertEquals("test.txt_1", UniquenessUtil.suggestNewName("test.txt"));
		assertEquals("test_0.txt_1", UniquenessUtil.suggestNewName("test_0.txt"));
		assertEquals("test_1.txt_1", UniquenessUtil.suggestNewName("test_1.txt"));
		assertEquals("test._1", UniquenessUtil.suggestNewName("test."));
		assertEquals("test_1._1", UniquenessUtil.suggestNewName("test_1."));
		assertEquals("._1", UniquenessUtil.suggestNewName("."));
	}

	@Test
	public void testFilename() {
		assertEquals("test_1.txt", UniquenessUtil.suggestNewFilename("test.txt"));
		assertEquals("test_1.txt", UniquenessUtil.suggestNewFilename("test_0.txt"));
		assertEquals("test_2.txt", UniquenessUtil.suggestNewFilename("test_1.txt"));
	}

}
