package com.gentics.mesh.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;

public class JsonUtilTest {

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testToJson() {
		JsonUtil.toJson(new Loop());
	}

	@Test
	public void testJsonList() {
		UserListResponse list = new UserListResponse();
		UserResponse user = new UserResponse();
		list.getData().add(user);
		String json = JsonUtil.toJson(list);
		assertNotNull(json);
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
