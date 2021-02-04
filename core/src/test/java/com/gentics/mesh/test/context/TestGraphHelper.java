package com.gentics.mesh.test.context;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.stream.Stream;

import org.mockito.Mockito;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.OrientDBMicroschemaDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Interface which contains graph specific methods which can be used to quickly interact with the graph.
 */
public interface TestGraphHelper extends TestHelper {

	default HibProject createProject(String name, String schema) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		return Tx.get().projectDao().create(name, null, null, null, user(), schemaContainer(schema).getLatestVersion(), batch);
	}

	default HibBranch createBranch(String name) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		HibProject project = project();
		return Tx.get().branchDao().create(project, name, user(), batch);
	}

	default HibMicroschema createMicroschema(MicroschemaVersionModel schema) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		OrientDBMicroschemaDao microschemaDao = Tx.get().microschemaDao();
		return microschemaDao.create(schema, user(), batch);
	}

	/**
	 * Return the latest branch of the dummy project.
	 * 
	 * @return
	 */
	default HibBranch latestBranch() {
		return project().getLatestBranch();
	}

	/**
	 * Returns the initial branch of the dummy project.
	 * 
	 * @return
	 */
	default HibBranch initialBranch() {
		return project().getInitialBranch();
	}

	/**
	 * Returns all graph field containers in the dummy project.
	 * @return
	 */
	default Stream<NodeGraphFieldContainer> getAllContents() {
		return Tx.get().nodeDao().findAll(project()).stream()
			.flatMap(node -> Stream.of(DRAFT, PUBLISHED)
			.flatMap(type -> boot().contentDao().getGraphFieldContainers(node, type).stream()));
	}
}
