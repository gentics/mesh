package com.gentics.mesh.plugin.forms;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Integration tests for validating form definitions.
 */
@RunWith(Parameterized.class)
public class FormValidationIntegrationTest extends AbstractFormsPluginIntegrationTest {
	@Parameters(name = "{index}: form: {0}, expect success: {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			new Object[] { "invalid_form_no_list_elements.json", false },
			new Object[] { "invalid_form_missing_value.json", false },
			new Object[] { "invalid_form_missing_key.json", false },
			new Object[] { "invalid_form_not_validated.json", true },
			new Object[] { "selectgroup_legacy_listelements.json", true }
		);
	}

	@Parameter(0)
	public String fileName;

	@Parameter(1)
	public boolean expectSuccess;

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	@Test
	public void testCreateInvalidForm() throws IOException {
		// The method will already check if the create response was successful, so there is nothing more to test here.
		createForm(fileName, true, expectSuccess);
	}

	/**
	 * Test updating a form
	 * @throws IOException
	 */
	@Test
	public void testUpdateInvalidForm() throws IOException {
		// The method will already check if the create response was successful, so there is nothing more to test here.
		updateForm(fileName, true, expectSuccess);
	}
}
