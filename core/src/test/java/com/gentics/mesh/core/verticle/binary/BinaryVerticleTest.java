package com.gentics.mesh.core.verticle.binary;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.test.core.TestUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.verticle.BinaryVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class BinaryVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private BinaryVerticle verticle;

	@Autowired
	private MeshNodeService nodeService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testUpload() throws Exception {
		MeshNode node = data().getFolder("news");
		roleService.addPermission(info.getRole(), node, PermissionType.UPDATE);
		Buffer buffer = TestUtils.randomBuffer(10000);
		String response = sendFileUploadRequest(buffer, "/api/v1/" + PROJECT_NAME + "/binaries/" + node.getUuid(), 200, "OK");
		System.out.println(response);
	}

	private String sendFileUploadRequest(Buffer fileData, String path, int statusCode, String statusMessage) throws Exception {
		String name = "somename";
		String fileName = "somefile.dat";
		String contentType = "application/octet-stream";

		String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
		Buffer buffer = Buffer.buffer();
		String header = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n"
				+ "Content-Type: " + contentType + "\r\n" + "Content-Transfer-Encoding: binary\r\n" + "\r\n";
		buffer.appendString(header);
		buffer.appendBuffer(fileData);
		String footer = "\r\n--" + boundary + "--\r\n";
		buffer.appendString(footer);
		Map<String, String> extraHeaders = new HashMap<>();
		extraHeaders.put("content-length", String.valueOf(buffer.length()));
		extraHeaders.put("content-type", "multipart/form-data; boundary=" + boundary);

		return request(info, HttpMethod.POST, path, statusCode, statusMessage, buffer, extraHeaders);

	}
}