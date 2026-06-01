package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.util.UUIDUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameter;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Abstract base class for validation tests
 */
public abstract class AbstractEndpointValidationTest extends AbstractFormsPluginIntegrationTest {

	protected static String formUuid;

	protected static String formDataUuid;
	/**
	 * Dummy request body
	 */
	protected final static RequestBody DUMMY_REQUEST_BODY = new MultipartBody.Builder()
		.setType(MultipartBody.FORM)
		.addFormDataPart("formdata", AbstractFormsPluginIntegrationTest.makeFormdataMultipart(formUuid, formDataUuid, Map.of("input1", "bla")))
		.build();

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
			updateTranslations();
		} catch (IOException e) {
			fail("Could not set translations: %s".formatted(e.getMessage()), e);
		}

		try {
			formUuid = createForm("inputfield.json");

			Request postRequest = new Request.Builder()
					.url(HttpUrl.get(projectUrl("/form/%s/save?lang=en".formatted(formUuid))).newBuilder().build())
					.header("Referer", "ref").post(DUMMY_REQUEST_BODY).build();

			Response response = client().newCall(postRequest).execute();
			assertResponse(response, 200);

			formDataUuid = getSingleFormDataNode(formUuid).getUuid();

		} catch (IOException e) {
			fail("Error while creating initial data", e);
		}
	}

	@Parameter(0)
	public String description;

	/**
	 * Post the translations from translations.json to the forms plugin.
	 */
	protected void updateTranslations() throws IOException {
		Request postRequest = new Request.Builder()
			.url(HttpUrl.get(projectUrl("/translations")).newBuilder().build())
			.headers(auth(true))
			.header("Referer", "ref")
			.post(requestBodyFromFile("translations.json"))
			.build();

		Response response = client().newCall(postRequest).execute();

		assertResponse(response, HttpResponseStatus.OK.code());
	}
}
