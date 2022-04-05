package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.util.UUIDUtil;

import okhttp3.Request;

/**
 * Test cases for the online status of forms
 */
@RunWith(value = Parameterized.class)
public class FormOnlineStatusTest extends AbstractFormsPluginIntegrationTest {
	@Parameters(name = "{index}: permission {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Boolean permission : Arrays.asList(true, false)) {
			data.add(new Object[] { permission });
		}
		return data;
	}

	@Parameter(0)
	public boolean permission;

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	/**
	 * Test that a form is online after creation
	 * @throws IOException
	 */
	@Test
	public void testCreate() throws IOException {
		String uuid = createForm("create.json", true, true);

		FormOnlineResponse formOnlineResponse = checkFormOnline(uuid, "en", permission, true);
		if (permission) {
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isTrue();
		}
	}

	/**
	 * Test that a form in another language is online after creation
	 * @throws IOException
	 */
	@Test
	public void testCreateDe() throws IOException {
		String uuid = createForm("create_de.json", true, true);

		FormOnlineResponse formOnlineResponse = checkFormOnline(uuid, "de", permission, true);
		if (permission) {
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isTrue();
		}
	}

	/**
	 * Test that a form is online after updating
	 * @throws IOException
	 */
	@Test
	public void testUpdate() throws IOException {
		String uuid = createForm("create.json", true, true);

		// set form offline
		FormOnlineResponse formOnlineResponse = setFormOffline(uuid, "en", true, true);
		assertThat(formOnlineResponse).as("Response").isNotNull();
		assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();

		// update the form
		doRequest(new Request.Builder()
			.url(projectUrl("/forms/" + uuid))
			.headers(auth(permission))
			.post(requestBodyFromFile("update.json"))
			.build(), permission, true, NodeResponse.class);

		if (permission) {
			// when the user had permission to update the form, the form should be online now
			formOnlineResponse = checkFormOnline(uuid, "en", true, true);
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isTrue();
		} else {
			// user did not have permission to update the form, it should still be offline
			formOnlineResponse = checkFormOnline(uuid, "en", true, true);
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();
		}
	}

	/**
	 * Test checking status for a form, that does not exist
	 * @throws IOException
	 */
	@Test
	public void testCheckNotExisting() throws IOException {
		checkFormOnline(UUIDUtil.randomUUID(), "fr", true, false);
	}

	/**
	 * Test checking status for a form, that does not have the language variant
	 * @throws IOException
	 */
	@Test
	public void testCheckLanguageNotExisting() throws IOException {
		String uuid = createForm("create_de.json", true, true);

		checkFormOnline(uuid, "it", true, false);
	}

	/**
	 * Test setting a form offline
	 * @throws IOException
	 */
	@Test
	public void testSetOffline() throws IOException {
		String uuid = createForm("create.json", true, true);

		// try to set form offline
		FormOnlineResponse formOnlineResponse = setFormOffline(uuid, "en", permission, true);
		if (permission) {
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();

			// get the status again
			formOnlineResponse = checkFormOnline(uuid, "en", true, true);
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();
		}
	}

	/**
	 * Test setting a form online
	 * @throws IOException
	 */
	@Test
	public void testSetOnline() throws IOException {
		String uuid = createForm("create.json", true, true);

		// set form offline
		FormOnlineResponse formOnlineResponse = setFormOffline(uuid, "en", true, true);
		assertThat(formOnlineResponse).as("Response").isNotNull();
		assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();

		// try to set form online
		formOnlineResponse = setFormOnline(uuid, "en", permission, true);
		if (permission) {
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isTrue();

			// get the status again
			formOnlineResponse = checkFormOnline(uuid, "en", true, true);
			assertThat(formOnlineResponse).as("Response").isNotNull();
			assertThat(formOnlineResponse.isOnline()).as("Form is online").isTrue();
		}
	}
}
