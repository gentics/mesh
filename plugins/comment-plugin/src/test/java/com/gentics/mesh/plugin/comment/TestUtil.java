package com.gentics.mesh.plugin.comment;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.comment.testclient.TestClientUtil;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import static com.gentics.mesh.plugin.comment.util.BasicUtil.propagateError;

public class TestUtil {
	public static NodeResponse loadNodeResponse(String path) {
		return JsonUtil.readValue(loadResource("/node/" + path + ".json"), NodeResponse.class);
	}

	public static UserResponse loadUserResponse(String path) {
		return JsonUtil.readValue(loadResource("/user/" + path + ".json"), UserResponse.class);
	}

	public static String loadResource(String resource) {
		InputStream stream = TestClientUtil.class.getResourceAsStream(resource);

		return propagateError(() -> IOUtils.toString(stream, Charsets.UTF_8));
	}
}
