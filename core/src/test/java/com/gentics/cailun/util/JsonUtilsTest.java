package com.gentics.cailun.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.cailun.error.HttpStatusCodeErrorException;

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
