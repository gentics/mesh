package com.gentics.mesh.test.context;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.stream.Stream;

import com.gentics.mesh.core.data.schema.Schema;
import org.mockito.Mockito;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Interface which contains graph specific methods which can be used to quickly interact with the graph.
 */
public interface TestGraphHelper extends TestHelper {

	default Project createProject(String name, String schema) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		Schema schemaEntity = Tx.get().schemaDao().findByName(schema);
		return Tx.get().projectDao().create(name, null, null, null, user(), schemaEntity.getLatestVersion(), batch);
	}

	default Branch createBranch(String name) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		Project project = project();
		return Tx.get().branchDao().create(project, name, user(), batch);
	}

	default Microschema createMicroschema(MicroschemaVersionModel schema) {
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		MicroschemaDao microschemaDao = Tx.get().microschemaDao();
		return microschemaDao.create(schema, user(), batch);
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

	/**
	 * Returns all graph field containers in the dummy project.
	 * @return
	 */
	default Stream<NodeFieldContainer> getAllContents() {
		return Tx.get().nodeDao().findAll(project()).stream()
			.flatMap(node -> Stream.of(DRAFT, PUBLISHED)
			.flatMap(type -> Tx.get().contentDao().getFieldContainers(node, type).stream()));
	}
}
