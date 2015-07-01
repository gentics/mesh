package com.gentics.mesh.util;

import org.junit.Test;

import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.json.JsonUtil;

public class JsonUtilsTest {

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testToJson() {
		JsonUtil.toJson(new Loop());
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
