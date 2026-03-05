package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.function.Supplier;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.util.UUIDUtil;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Abstract base class for validation tests
 */
public abstract class AbstractEndpointValidationTest extends AbstractFormsPluginIntegrationTest {
	/**
	 * Dummy request body
	 */
	protected final static RequestBody DUMMY_REQUEST_BODY = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("input1", "bla")
			.build();

	protected static String formUuid;

	protected static String formDataUuid;

	/**
	 * Supplier for a real form uuid
	 */
	protected final static Supplier<String> FORM_UUID = () -> formUuid;

	/**
	 * Supplier for a real form data uuid
	 */
	protected final static Supplier<String> FORM_DATA_UUID = () -> formDataUuid;

	/**
	 * Supplier for a bogus uuid
	 */
	protected final static Supplier<String> BOGUS_UUID = () -> RandomStringUtils.insecure().nextAlphabetic(10);

	/**
	 * Supplier for a random uuid
	 */
	protected final static Supplier<String> RANDOM_UUID = () -> UUIDUtil.randomUUID();

	/**
	 * Supplier for the uuid of a foreign node (not a form or a form data node)
	 */
	protected final static Supplier<String> FOREIGN_UUID = () -> {
		ProjectResponse project = server.client().findProjectByName(PROJECT_NAME).blockingGet();
		return project.getRootNode().getUuid();
	};

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		try {
			formUuid = createForm("inputfield.json");

			Request postRequest = new Request.Builder()
					.url(HttpUrl.get(projectUrl("/forms/%s/data".formatted(formUuid))).newBuilder().build())
					.header("Referer", "ref").post(DUMMY_REQUEST_BODY).build();

			Response response = client().newCall(postRequest).execute();
			assertResponse(response, 205);

			formDataUuid = getSingleFormDataNode(formUuid).getUuid();

		} catch (IOException e) {
			fail("Error while creating initial data", e);
		}
	}

	@Parameter(0)
	public String description;

	/**
	 * Assert that the response has the given code
	 * @param response response to check
	 * @param expectedResponseCode expected response code
	 */
	protected void assertResponse(Response response, int expectedResponseCode) {
		boolean expectSuccess = expectedResponseCode < 400;
		assertThat(response.isSuccessful())
				.as(String.format("Response status %d (%s)", response.code(), response.message())).isEqualTo(expectSuccess);
		assertThat(response.code()).as("Response code").isEqualTo(expectedResponseCode);
	}
}
