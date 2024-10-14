package com.gentics.mesh.enterprise.service.license;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class LicenseManagementEndpointTest extends AbstractMeshTest {

	private static OkHttpClient okHttpClient;

	@BeforeClass
	public static final void beforeClass() {
		okHttpClient = new OkHttpClient.Builder().build();
	}

	@Test
	public void testLicenseInfoNoAdmin() throws IOException {
		revokeAdmin();
		JsonObject jsonBody = testLicenseInfo();
		assertThat(jsonBody.getString("status")).isNullOrEmpty();
		assertThat(jsonBody.getString("message")).isEqualTo("Administration permissions are required.");
	}
	@Test
	public void testLicenseInfoAdmin() throws IOException {
		grantAdmin();
		JsonObject jsonBody = testLicenseInfo();
		assertThat(jsonBody.getString("status")).isEqualTo("VALID");
		assertThat(jsonBody.getJsonArray("features").getList().size()).isGreaterThan(0);
	}

	@Test
	public void testLicenseFileAdmin() throws IOException {
		grantAdmin();
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license/file").header("Authorization", "Bearer " + client().getAPIKey()).get().build();
		Response response = okHttpClient.newCall(request).execute();
		assertThat(response).isNotNull();
		assertThat(response.body()).isNotNull();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(response.body().bytes()), "UTF-8"));
		assertEquals(reader.readLine(), System.getenv(HibernateMeshOptions.LICENSE_KEY_ENV));
	}

	protected JsonObject testLicenseInfo() throws IOException {
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license").header("Authorization", "Bearer " + client().getAPIKey()).get().build();
		Response response = okHttpClient.newCall(request).execute();
		assertThat(response).isNotNull();
		assertThat(response.body()).isNotNull();
		JsonObject jsonBody = new JsonObject(response.body().string());
		return jsonBody;
	}
}
