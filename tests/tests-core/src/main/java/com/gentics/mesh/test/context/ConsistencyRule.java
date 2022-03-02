package com.gentics.mesh.test.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.dagger.MeshComponent;

/**
 * Test rule, which will perform a consistency check after each test, unless either the test class or the test method have the annotation {@link NoConsistencyCheck} set.
 */
public class ConsistencyRule extends TestWatcher {
	private MeshTestContext testContext;

	/**
	 * Create instance for the given test context
	 * @param testContext
	 */
	public ConsistencyRule(MeshTestContext testContext) {
		this.testContext = testContext;
	}

	/**
	 * Perform a consistency check
	 */
	public void check() {
		MeshComponent mesh = testContext.getMeshComponent();
		List<ConsistencyCheck> checks = mesh.consistencyChecks();
		try (Tx tx = mesh.database().tx()) {
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			for (ConsistencyCheck check : checks) {
				ConsistencyCheckResult result = check.invoke(mesh.database(), tx, false);
				response.getInconsistencies().addAll(result.getResults());
			}

			assertThat(response.getInconsistencies()).as("Inconsistencies").isEmpty();
		}
	}

	@Override
	protected void finished(Description description) {
		Class<?> testClass = description.getTestClass();
		if (testClass != null && testClass.isAnnotationPresent(NoConsistencyCheck.class)) {
			return;
		}

		if (description.getAnnotation(NoConsistencyCheck.class) == null) {
			check();
		}
	}
}
