package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test rendering a form with a path prefix.
 */
public class FormRenderTestPathPrefix extends AbstractFormsPluginIntegrationTest {

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	@BeforeClass
	public static void setupOnce() throws ExecutionException, InterruptedException, TimeoutException {
		ProjectCreateRequest createProject = new ProjectCreateRequest()
			.setName(PROJECT_NAME)
			.setPathPrefix(PROJECT_PATH_PREFIX)
			.setSchemaRef("folder");

		createProject.setHostname(PROJECT_HOSTNAME);

		call(() -> server.client().createProject(createProject));

		server.getMesh().deployPlugin(FormsTestPlugin.class, "forms").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "forms");
	}

	/**
	 * Test rendering a form
	 * @throws IOException
	 */
	@Test
	public void testRender() throws IOException {
		String uuid = createForm("inputfield.json");

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid)).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("inputfield-pathprefix.html"));
	}

	/**
	 * Test rendering a multipage-form with data
	 * @throws IOException
	 */
	@Test
	public void testRenderDataMultipage() throws IOException {
		String uuid = createForm("multipage.json");

		// only fill the first page (without error)
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
			.addFormDataPart("page1", "1").addFormDataPart("input1", "someValue").build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data"))
			.header("Referer", "ref").post(requestBody).build();
		Response postDataResponse = client().newCall(postRequest).execute();
		assertThat(postDataResponse.isRedirect())
			.as(String.format("Response redirect %d (%s)", postDataResponse.code(), postDataResponse.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("multipage-pathprefix_data_page1.html"));

		// now post page 2 (incomplete)
		requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
			.addFormDataPart("page2", "2").build();
		buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid()))
			.header("Referer", "ref").post(requestBody).build();
		postDataResponse = client().newCall(postRequest).execute();
		assertThat(postDataResponse.isRedirect())
			.as(String.format("Response redirect %d (%s)", postDataResponse.code(), postDataResponse.message())).isTrue();

		renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())).header("Accept", "text/html").get().build();
		response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("multipage-pathprefix_data_page1.html"));
	}

}
