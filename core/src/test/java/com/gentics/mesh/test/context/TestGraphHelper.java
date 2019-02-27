package com.gentics.mesh.test.context;

import org.mockito.Mockito;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Interface which contains graph specific methods which can be used to quickly interact with the graph.
 */
public interface TestGraphHelper extends TestHelper {

	default Project createProject(String name, String schema) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		return boot().projectRoot().create(name, null, null, null, user(), schemaContainer(schema).getLatestVersion(), batch);
	}

	default Branch createBranch(String name) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		Project project = project();
		BranchRoot branchRoot = project.getBranchRoot();
		return branchRoot.create(name, user(), batch);
	}

	/**
	 * Return the latest branch of the dummy project.
	 * 
	 * @return
	 */
	default Branch latestBranch() {
		return project().getLatestBranch();
	}

	/**
	 * Returns the initial branch of the dummy project.
	 * 
	 * @return
	 */
	default Branch initialBranch() {
		return project().getInitialBranch();
	}

}
