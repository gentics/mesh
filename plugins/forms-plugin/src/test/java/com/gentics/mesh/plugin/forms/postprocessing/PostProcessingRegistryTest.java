package com.gentics.mesh.plugin.forms.postprocessing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Test cases for scanning post processing actions with the {@link PostProcessingRegistry}
 */
public class PostProcessingRegistryTest {
	/**
	 * Test that valid annotated actions are found with their keys and invalid ones are skipped
	 */
	@Test
	public void testScan() {
		PostProcessingRegistry registry = new PostProcessingRegistry();

		assertThat(registry.getActions()).as("Found actions").containsOnlyKeys(RecordingTestAction.KEY, FailingTestAction.KEY, NeverTestAction.KEY);
		assertThat(registry.get(RecordingTestAction.KEY)).as("Recording action").containsInstanceOf(RecordingTestAction.class);
		assertThat(registry.get(FailingTestAction.KEY)).as("Failing action").containsInstanceOf(FailingTestAction.class);
		assertThat(registry.get(NeverTestAction.KEY)).as("Never action").containsInstanceOf(NeverTestAction.class);
		assertThat(registry.get("not-an-action")).as("Class not implementing the interface").isEmpty();
		assertThat(registry.get("no-default-constructor")).as("Class without default constructor").isEmpty();
	}
}
