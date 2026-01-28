package com.gentics.mesh.test.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.dagger.MeshComponent;

/**
 * Test rule, which will perform a consistency check after each test, unless either the test class or the test method have the annotation {@link NoConsistencyCheck} set.
 */
public class ConsistencyRule extends TestWatcher {
	/**
	 * Sleep time in between checks for inconsistencies
	 */
	private final static int sleepMs = 100;

	/**
	 * Overall timeout for waiting for inconsistencies to be resolved
	 */
	private final static int timeoutMs = 10_000;

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
		long startWait = System.currentTimeMillis();
		List<InconsistencyInfo> inconsistencies = getInconsistencies();

		// some actions, which resolve (temporary) inconsistencies are performed asynchronously,
		// so we will wait some time for inconsistencies to be resolved.
		while (!inconsistencies.isEmpty() && (System.currentTimeMillis() - startWait) < timeoutMs) {
			try {
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			inconsistencies = getInconsistencies();
		}

		assertThat(inconsistencies).as("Inconsistencies").isEmpty();
	}

	@Override
	protected void succeeded(Description description) {
		Class<?> testClass = description.getTestClass();
		if (testClass != null && testClass.isAnnotationPresent(NoConsistencyCheck.class)) {
			return;
		}

		if (description.getAnnotation(NoConsistencyCheck.class) == null) {
			check();
		}
	}

	/**
	 * Get inconsistencies
	 * @return list of inconsistencies
	 */
	protected List<InconsistencyInfo> getInconsistencies() {
		MeshComponent mesh = testContext.getMeshComponent();
		List<ConsistencyCheck> checks = mesh.consistencyChecks();

		try (Tx tx = mesh.database().tx()) {
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			for (ConsistencyCheck check : checks) {
				ConsistencyCheckResult result = check.invoke(mesh.database(), tx, false);
				response.getInconsistencies().addAll(result.getResults());
			}
			return response.getInconsistencies();
		}
	}
}
