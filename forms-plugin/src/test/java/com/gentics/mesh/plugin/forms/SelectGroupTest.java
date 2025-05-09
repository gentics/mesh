package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;

/**
 * Test cases for posting data to different variations of selectgroup fields
 */
public class SelectGroupTest extends AbstractFormsPluginIntegrationTest {
	private final static Random random = new Random();

	private final static List<String> AVAILABLE_SINGLE_KEYS = Arrays.asList("single_key_1", "single_key_2", "single_key_3");
	private final static List<String> AVAILABLE_MULTI_KEYS = Arrays.asList("multi_key_1", "multi_key_2", "multi_key_3");

	private String formUuid;

	private static String getRandomKey(List<String> keys) {
		return keys.get(random.nextInt(keys.size()));
	}

	private static List<String> getRandomKeys(List<String> keys, int number) {
		List<String> randomKeys = new ArrayList<>();

		while (randomKeys.size() < number) {
			String next = getRandomKey(keys);
			if (!randomKeys.contains(next)) {
				randomKeys.add(next);
			}
		}

		return randomKeys;
	}

	@Before
	public void setup() {
		super.setup();
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		try {
			formUuid = createForm("selectgroup_variants.json");
		} catch (IOException e) {
			fail("Creation of form failed", e);
		}
	}

	@Test
	public void testSingle() throws IOException {
		String key = getRandomKey(AVAILABLE_SINGLE_KEYS);

		ResponseWithCookies response = submit(formUuid, "single", key);
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeResponse dataNode = getSingleFormDataNode(formUuid);
		assertThat(dataNode.getFields().getStringField("single").getString()).as("Field value").isEqualTo(key);
	}

	@Test
	public void testSingleWithAdditional() throws IOException  {
		String additionalKey = "somethingelse";

		ResponseWithCookies response = submit(formUuid, "single_with", "single_withadditional_field", "single_with_additional", additionalKey);
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeResponse dataNode = getSingleFormDataNode(formUuid);
		assertThat(dataNode.getFields().getStringField("single_with").getString()).as("Field value").isEqualTo(additionalKey);
	}

	@Test
	public void testMulti() throws IOException {
		List<String> keys = getRandomKeys(AVAILABLE_MULTI_KEYS, 2);

		ResponseWithCookies response = submit(formUuid, "multi", keys.get(0), "multi", keys.get(1));
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeResponse dataNode = getSingleFormDataNode(formUuid);
		assertThat(dataNode.getFields().getStringFieldList("multi").getItems()).as("Field values").containsOnlyElementsOf(keys);
	}

	@Test
	public void testMultiWithAdditional() throws IOException  {
		List<String> keys = getRandomKeys(AVAILABLE_MULTI_KEYS, 2);
		String additionalKey = "somethingelse";

		ResponseWithCookies response = submit(formUuid, "multi_with", keys.get(0), "multi_with", keys.get(1), "multi_with", additionalKey);
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeResponse dataNode = getSingleFormDataNode(formUuid);
		List<String> allKeys = new ArrayList<>();
		allKeys.addAll(keys);
		allKeys.add(additionalKey);
		assertThat(dataNode.getFields().getStringFieldList("multi_with").getItems()).as("Field values").containsOnlyElementsOf(allKeys);
	}
}
