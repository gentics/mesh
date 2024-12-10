package com.gentics.mesh.enterprise.service.license;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class LicenseManagementEndpointTest extends AbstractMeshTest {

	private static OkHttpClient okHttpClient;

	@BeforeClass
	public static final void beforeClass() {
		okHttpClient = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).writeTimeout(0, TimeUnit.MILLISECONDS).build();
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
		assertThat(jsonBody.getString("status")).isNotEmpty();
		assertThat(jsonBody.getString("message")).isNullOrEmpty();
	}

	@Test
	public void testLicenseFileAdmin() throws IOException {
		grantAdmin();
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license/file").header("Authorization", "Bearer " + client().getAPIKey()).get().build();
		Response response = okHttpClient.newCall(request).execute();
		assertThat(response).isNotNull();
		assertThat(response.body()).isNotNull();
		String key = null;
		String fileLine;
		String bodyLine;
		try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(((HibernateMeshOptions)options()).getLicenseKeyPath())), "UTF-8"))) {
			try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(response.body().bytes()), "UTF-8"))) {
				do {
					fileLine = fileReader.readLine();
					bodyLine = responseReader.readLine();
					if (key == null) {
						key = bodyLine;
					}
					assertEquals(fileLine, bodyLine);
				} while (fileLine != null || bodyLine != null);
			}
		}
		//assertEquals(System.getenv(HibernateMeshOptions.LICENSE_KEY_ENV), key);
		assertThat(key).isNotEmpty();
	}

	@Test
	public void testUploadValidLicenseFile() throws IOException {
		String suffix = Long.toString(System.currentTimeMillis());
		String backupFilename = backupOriginalLicense(suffix);
		File validLicense = new File(backupFilename); 
		try (Writer writer = new BufferedWriter(new FileWriter(new File(((HibernateMeshOptions)options()).getLicenseKeyPath())))) {
			writer.write("bogus");
		}
		grantAdmin();
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", validLicense.getName(),
                        RequestBody.create(MediaType.parse("text/plain"), validLicense))
                .build();
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license/file").header("Authorization", "Bearer " + client().getAPIKey()).post(requestBody).build();
		Response response = okHttpClient.newCall(request).execute();
		JsonObject jsonBody = new JsonObject(response.body().string());
		assertThat(jsonBody.getString("status")).isEqualTo("VALID");
		assertThat(jsonBody.getJsonArray("features").getList().size()).isGreaterThan(0);
		// Not actually needed for the successful result, but better to have around.
		restoreOriginalLicense(suffix);
	}

	@Test
	public void testUploadInvalidLicenseFile() throws IOException {
		String suffix = Long.toString(System.currentTimeMillis());
		String backupFilename = backupOriginalLicense(suffix);
		File bogusLicense = new File(backupFilename); 
		try (Writer writer = new BufferedWriter(new FileWriter(bogusLicense))) {
			writer.write("bogus");
		}
		grantAdmin();
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", bogusLicense.getName(),
                        RequestBody.create(MediaType.parse("text/plain"), bogusLicense))
                .build();
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license/file").header("Authorization", "Bearer " + client().getAPIKey()).post(requestBody).build();
		Response response = okHttpClient.newCall(request).execute();
		JsonObject jsonBody = new JsonObject(response.body().string());
		assertThat(jsonBody.getString("status")).isNull();
		assertThat(jsonBody.getString("message")).isEqualTo("Invalid license!");
		// No change of the license - no restore required
	}

	@Test
	public void testForceUploadInvalidLicenseFile() throws IOException {
		String suffix = Long.toString(System.currentTimeMillis());
		backupOriginalLicense(suffix);
		String backupFilename = backupOriginalLicense("bogus" + Long.toString(System.currentTimeMillis()));
		File bogusLicense = new File(backupFilename); 
		try (Writer writer = new BufferedWriter(new FileWriter(bogusLicense))) {
			writer.write("bogus");
		}
		grantAdmin();
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", bogusLicense.getName(),
                        RequestBody.create(MediaType.parse("text/plain"), bogusLicense))
                .build();
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license/file?force=true").header("Authorization", "Bearer " + client().getAPIKey()).post(requestBody).build();
		Response response = okHttpClient.newCall(request).execute();
		JsonObject jsonBody = new JsonObject(response.body().string());
		assertThat(jsonBody.getString("status")).isNull();
		assertThat(jsonBody.getString("message")).isEqualTo("Invalid license!");
		restoreOriginalLicense(suffix);
	}

	protected JsonObject testLicenseInfo() throws IOException {
		Request request = new Request.Builder().url("http://localhost:" + getTestContext().getHttpPort() + "/api/v1/admin/license").header("Authorization", "Bearer " + client().getAPIKey()).get().build();
		Response response = okHttpClient.newCall(request).execute();
		assertThat(response).isNotNull();
		assertThat(response.body()).isNotNull();
		JsonObject jsonBody = new JsonObject(response.body().string());
		return jsonBody;
	}

	protected String backupOriginalLicense(String suffix) throws FileNotFoundException, IOException {
		String backupFilename = options().getTempDirectory() + File.separator + ((HibernateMeshOptions)options()).getLicenseKeyPath() + "." + suffix + ".test.bak";
		try (OutputStream o = new BufferedOutputStream(new FileOutputStream(new File(backupFilename)))) {
			assertEquals(
					Files.copy(Path.of(((HibernateMeshOptions)options()).getLicenseKeyPath()), o), 
					new File(((HibernateMeshOptions)options()).getLicenseKeyPath()).length()
				);
		}
		return backupFilename;
	}
	protected void restoreOriginalLicense(String suffix) throws FileNotFoundException, IOException {
		String backupFilename = options().getTempDirectory() + File.separator + ((HibernateMeshOptions)options()).getLicenseKeyPath() + "." + suffix + ".test.bak";
		try (OutputStream o = new BufferedOutputStream(new FileOutputStream(new File(((HibernateMeshOptions)options()).getLicenseKeyPath())))) {
			assertEquals(
					Files.copy(Path.of(backupFilename), o), 
					new File(backupFilename).length()
				);
		}
	}
}
