package com.gentics.mesh.changelog;

import org.junit.Test;

import com.gentics.mesh.util.UUIDUtil;

/**
 * Generator for uuid models.
 */
public class UUIDGenerator {

	@Test
	public void test() {
		System.out.println(UUIDUtil.randomUUID().toUpperCase());
	}

}
