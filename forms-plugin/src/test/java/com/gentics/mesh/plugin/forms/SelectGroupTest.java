package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;

/**
 * Test cases for posting data to different variations of selectgroup fields
 */
public class SelectGroupTest extends AbstractFormsPluginIntegrationTest {
	private final static Random random = new Random();

	private final static List<String> AVAILABLE_KEYS = Arrays.asList("single_key_1", "single_key_2", "single_key_3");

	private String formUuid;

	private final static String getRandomKey() {
		return AVAILABLE_KEYS.get(random.nextInt(AVAILABLE_KEYS.size()));
	}

	private final static List<String> getRandomKeys(int number) {
		List<String> randomKeys = new ArrayList<>();

		while (randomKeys.size() < number) {
			String next = getRandomKey();
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
		String key = getRandomKey();

		ResponseWithCookies response = submit(formUuid, "single", key);
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		assertThat(dataNodes.getData().stream().filter(n -> !n.getSchema().getName().equals("form_" + formUuid))).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("single").getString()).as("Field value").isEqualTo(key);
	}

	@Test
	public void testSingleWithAdditional() throws IOException  {
		String additionalKey = "somethingelse";

		ResponseWithCookies response = submit(formUuid, "single_with", "single_withadditional_field", "single_with_additional", additionalKey);
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		assertThat(dataNodes.getData().stream().filter(n -> !n.getSchema().getName().equals("form_" + formUuid))).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("single_with").getString()).as("Field value").isEqualTo(additionalKey);
	}

	@Test
	public void testMulti() throws IOException {
		List<String> keys = getRandomKeys(2);

		ResponseWithCookies response = submit(formUuid, "multi", keys.get(0), "multi", keys.get(1));
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		assertThat(dataNodes.getData().stream().filter(n -> !n.getSchema().getName().equals("form_" + formUuid))).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringFieldList("multi").getItems()).as("Field values").containsOnlyElementsOf(keys);
	}

	@Test
	public void testMultiWithAdditional() throws IOException  {
		List<String> keys = getRandomKeys(2);
		String additionalKey = "somethingelse";

		ResponseWithCookies response = submit(formUuid, "multi_with", keys.get(0), "multi_with", keys.get(1), "multi_with", additionalKey);
		assertThat(response.response.isSuccessful()).as("Response is success").isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		assertThat(dataNodes.getData().stream().filter(n -> !n.getSchema().getName().equals("form_" + formUuid))).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		List<String> allKeys = new ArrayList<>();
		allKeys.addAll(keys);
		allKeys.add(additionalKey);
		assertThat(dataNode.getFields().getStringFieldList("multi_with").getItems()).as("Field values").containsOnlyElementsOf(allKeys);
	}
}
