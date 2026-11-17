package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.forms.postprocessing.FailingTestAction;
import com.gentics.mesh.plugin.forms.postprocessing.NeverTestAction;
import com.gentics.mesh.plugin.forms.postprocessing.PostProcessingStatus;
import com.gentics.mesh.plugin.forms.postprocessing.RecordingTestAction;
import com.gentics.mesh.plugin.forms.service.PostProcessingService;

import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for post processing of published form data
 */
public class FormPostProcessingTest extends AbstractFormPostDataTest {
	/**
	 * Language of the posted data
	 */
	private static final String LANGUAGE = "it";

	/**
	 * Form with a single input field "input1"
	 */
	private static final String FORM = "inputfield_languages_" + LANGUAGE + ".json";

	/**
	 * Maximum time to wait for asynchronous post processing. The test configuration sets the retry interval to 3 seconds.
	 */
	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	@Before
	public void setup() {
		super.setup();
		RecordingTestAction.INVOCATIONS.clear();
		FailingTestAction.CALLS.set(0);
		NeverTestAction.CALLS.set(0);
	}

	@After
	public void tearDown() throws IOException {
		super.tearDown();
	}

	/**
	 * Test that an applicable action is performed with the configured properties, the form, the posted data and the language,
	 * and that the status is tracked in a child node of the form data node
	 * @throws IOException
	 */
	@Test
	public void testActionPerformed() throws IOException {
		String formUuid = createForm(FORM);
		String value = RecordingTestAction.TRIGGER + " value";
		NodeResponse dataNode = postData(formUuid, value, true);

		NodeResponse statusNode = waitForStatus(dataNode.getUuid(), RecordingTestAction.KEY, PostProcessingStatus.DONE);

		assertThat(getString(statusNode, PostProcessingService.FIELD_ATTEMPTS)).as("Attempts").isEqualTo("1");
		assertThat(getString(statusNode, PostProcessingService.FIELD_LANGUAGE)).as("Language").isEqualTo(LANGUAGE);
		assertThat(getString(statusNode, PostProcessingService.FIELD_ERROR)).as("Error").isEmpty();

		assertThat(RecordingTestAction.INVOCATIONS).as("Invocations").hasSize(1);
		RecordingTestAction.Invocation invocation = RecordingTestAction.INVOCATIONS.get(0);
		assertThat(invocation.properties()).as("Properties").containsOnly(Map.entry("greeting", "hello"), Map.entry("count", 42));
		assertThat(invocation.formUuid()).as("Form UUID").isEqualTo(formUuid);
		assertThat(invocation.value()).as("Posted value").isEqualTo(value);
		assertThat(invocation.language()).as("Language").isEqualTo(LANGUAGE);

		// only the applicable action has a status node
		assertThat(getStatusNodes(dataNode.getUuid())).as("Status nodes").hasSize(1);
		assertThat(NeverTestAction.CALLS.get()).as("Calls of non applicable action").isEqualTo(0);
	}

	/**
	 * Test that no status nodes are created and no action is performed, if no action is applicable
	 * @throws IOException
	 */
	@Test
	public void testNoApplicableAction() throws IOException {
		String formUuid = createForm(FORM);
		NodeResponse dataNode = postData(formUuid, "plain value", true);

		// status nodes are created synchronously before the data is published
		assertThat(getStatusNodes(dataNode.getUuid())).as("Status nodes").isEmpty();
		assertThat(RecordingTestAction.INVOCATIONS).as("Invocations").isEmpty();
		assertThat(NeverTestAction.CALLS.get()).as("Calls of non applicable action").isEqualTo(0);
	}

	/**
	 * Test that saving data without publishing does not trigger post processing
	 * @throws IOException
	 */
	@Test
	public void testSaveWithoutPublish() throws IOException {
		String formUuid = createForm(FORM);
		NodeResponse dataNode = postData(formUuid, RecordingTestAction.TRIGGER + " value", false);

		assertThat(getStatusNodes(dataNode.getUuid())).as("Status nodes").isEmpty();
		assertThat(RecordingTestAction.INVOCATIONS).as("Invocations").isEmpty();
	}

	/**
	 * Test that a failing action is retried until the configured maximum number of attempts is reached and is marked as failed then
	 * @throws IOException
	 */
	@Test
	public void testFailingAction() throws IOException {
		String formUuid = createForm(FORM);
		NodeResponse dataNode = postData(formUuid, FailingTestAction.TRIGGER + " value", true);

		NodeResponse statusNode = waitForStatus(dataNode.getUuid(), FailingTestAction.KEY, PostProcessingStatus.FAILED);

		// test configuration sets maxAttempts to 2
		assertThat(getString(statusNode, PostProcessingService.FIELD_ATTEMPTS)).as("Attempts").isEqualTo("2");
		assertThat(getString(statusNode, PostProcessingService.FIELD_ERROR)).as("Error").isEqualTo(FailingTestAction.ERROR_MESSAGE);
		assertThat(FailingTestAction.CALLS.get()).as("Calls").isEqualTo(2);
	}

	/**
	 * Test that a pending action (e.g. left over from a restart) is performed by the periodic job
	 * @throws IOException
	 */
	@Test
	public void testResumePendingAction() throws IOException {
		String formUuid = createForm(FORM);
		String value = "resumed value";
		NodeResponse dataNode = postData(formUuid, value, true);
		assertThat(getStatusNodes(dataNode.getUuid())).as("Status nodes").isEmpty();

		// create a pending status node, as if the instance had been stopped before performing the action
		NodeCreateRequest request = new NodeCreateRequest()
				.setSchemaName("form_postprocessing")
				.setParentNodeUuid(dataNode.getUuid())
				.setLanguage("en");
		request.getFields().put(PostProcessingService.FIELD_KEY, new StringFieldImpl().setString(RecordingTestAction.KEY));
		request.getFields().put(PostProcessingService.FIELD_STATUS, new StringFieldImpl().setString(PostProcessingStatus.PENDING.name()));
		request.getFields().put(PostProcessingService.FIELD_ATTEMPTS, new NumberFieldImpl().setNumber(0));
		request.getFields().put(PostProcessingService.FIELD_LANGUAGE, new StringFieldImpl().setString(LANGUAGE));
		request.getFields().put(PostProcessingService.FIELD_LAST_UPDATE, new DateFieldImpl().setDate(Instant.now().minus(Duration.ofHours(1)).toString()));
		call(() -> server.client().createNode(PROJECT_NAME, request));

		waitForStatus(dataNode.getUuid(), RecordingTestAction.KEY, PostProcessingStatus.DONE);

		assertThat(RecordingTestAction.INVOCATIONS).as("Invocations").hasSize(1);
		RecordingTestAction.Invocation invocation = RecordingTestAction.INVOCATIONS.get(0);
		assertThat(invocation.formUuid()).as("Form UUID").isEqualTo(formUuid);
		assertThat(invocation.value()).as("Posted value").isEqualTo(value);
		assertThat(invocation.language()).as("Language").isEqualTo(LANGUAGE);
	}

	/**
	 * Test that deleting form data also deletes the status nodes
	 * @throws IOException
	 */
	@Test
	public void testDeleteFormData() throws IOException {
		String formUuid = createForm(FORM);
		NodeResponse dataNode = postData(formUuid, RecordingTestAction.TRIGGER + " value", true);
		NodeResponse statusNode = waitForStatus(dataNode.getUuid(), RecordingTestAction.KEY, PostProcessingStatus.DONE);

		Response response = client().newCall(new Request.Builder()
				.url(projectUrl("/forms/%s/data/%s".formatted(formUuid, dataNode.getUuid())))
				.headers(auth(true))
				.delete()
				.build()).execute();
		assertOk(response);

		call(() -> server.client().findNodeByUuid(PROJECT_NAME, statusNode.getUuid(), new VersioningParametersImpl().draft()), HttpResponseStatus.NOT_FOUND);
	}

	/**
	 * Post a value for "input1" to the form and return the single data node
	 * @param formUuid form uuid
	 * @param value value
	 * @param publish true to submit (and publish), false to only save
	 * @return data node
	 * @throws IOException
	 */
	protected NodeResponse postData(String formUuid, String value, boolean publish) throws IOException {
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", AbstractFormsPluginIntegrationTest.makeFormdataMultipart(formUuid, null, Map.of("input1", value)))
				.build();

		Request postRequest = new Request.Builder()
				.url(projectUrl(String.format("/form/%s/%s?lang=%s", formUuid, (publish ? "submit" : "save"), LANGUAGE)))
				.header("Referer", "ref")
				.post(requestBody).build();
		assertOk(client().newCall(postRequest).execute());

		return assertSingleDataNode(formUuid, dataNode -> {
			assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo(value);
			assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo(publish ? "1.0" : "0.1");
		});
	}

	/**
	 * Get the post processing status nodes of the form data node
	 * @param formDataUuid form data uuid
	 * @return list of status nodes
	 */
	protected List<NodeResponse> getStatusNodes(String formDataUuid) {
		return call(() -> server.client().findNodeChildren(PROJECT_NAME, formDataUuid, new VersioningParametersImpl().draft())).getData()
				.stream().filter(node -> "form_postprocessing".equals(node.getSchema().getName()))
				.collect(Collectors.toList());
	}

	/**
	 * Wait until the status node for the action with given key has the expected status
	 * @param formDataUuid form data uuid
	 * @param key action key
	 * @param expected expected status
	 * @return status node
	 */
	protected NodeResponse waitForStatus(String formDataUuid, String key, PostProcessingStatus expected) {
		waitFor(() -> getStatusNode(formDataUuid, key).map(node -> expected.name().equals(getString(node, PostProcessingService.FIELD_STATUS))).orElse(false),
				"Status %s of action %s for %s".formatted(expected, key, formDataUuid));
		return getStatusNode(formDataUuid, key).get();
	}

	private Optional<NodeResponse> getStatusNode(String formDataUuid, String key) {
		return getStatusNodes(formDataUuid).stream().filter(node -> key.equals(getString(node, PostProcessingService.FIELD_KEY))).findFirst();
	}

	private static String getString(NodeResponse node, String fieldName) {
		// node listings may (transiently) return nodes without fields, while the status node is updated concurrently
		if (node.getFields() == null) {
			return null;
		}
		if (PostProcessingService.FIELD_ATTEMPTS.equals(fieldName)) {
			return String.valueOf(node.getFields().getNumberField(fieldName).getNumber().intValue());
		}
		return Optional.ofNullable(node.getFields().getStringField(fieldName)).map(f -> f.getString()).orElse(null);
	}

	private static void waitFor(BooleanSupplier condition, String description) {
		Instant end = Instant.now().plus(TIMEOUT);
		while (!condition.getAsBoolean()) {
			if (Instant.now().isAfter(end)) {
				fail("Timeout while waiting for: " + description);
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("Interrupted while waiting for: " + description);
			}
		}
	}
}
