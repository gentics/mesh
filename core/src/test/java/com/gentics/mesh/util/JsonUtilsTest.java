package com.gentics.mesh.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.util.JsonUtils;

public class JsonUtilsTest {

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testToJson() {
		JsonUtils.toJson(new Loop());
	}

	@Test
	public void testMapper() {
		assertNotNull(JsonUtils.getMapper());
	}
}

class Loop {
	Loop loop;

	public Loop() {
		loop = this;
	}

	public Loop getLoop() {
		return loop;
	}
}
