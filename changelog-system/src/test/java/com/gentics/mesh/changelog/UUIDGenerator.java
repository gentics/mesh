package com.gentics.mesh.changelog;

import org.junit.Test;

import com.gentics.mesh.util.UUIDUtil;

public class UUIDGenerator {

	@Test
	public void test() {
		System.out.println(UUIDUtil.randomUUID().toUpperCase());
	}

}
