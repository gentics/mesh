package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.util.function.Supplier;

import org.junit.Test;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class MetricsLabelTest extends AbstractMeshTest {
	@Test
	public void testGraphQL() {
		String query = "{ me { uuid } }";
		testMetric(pathLabel("graphql"), () -> client().graphqlQuery(PROJECT_NAME, query));
	}

	private String pathLabel(String label) {
		return "path=\"" + label + "\"";
	}

	private void testMetric(String expected, Supplier<MeshRequest<?>> loader) {
		for (int i = 0; i < 10; i++) {
			loader.get().blockingAwait();
		}
		String metrics = call(() -> monClient().metrics());
		assertThat(metrics).as("Metrics result").isNotEmpty().contains(expected);
	}
}
