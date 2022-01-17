package com.gentics.mesh.core.schema.change;

import java.io.IOException;

import com.gentics.mesh.core.data.dao.PersistingContainerDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Abstract test class for schema change implementation tests.
 */
public abstract class AbstractChangeTest extends AbstractMeshTest {

	protected FieldSchemaContainerMutator mutator = new FieldSchemaContainerMutator();

	/**
	 * Assert that the basic getter and setter work as expected.
	 * 
	 * @throws IOException
	 */
	abstract public void testFields() throws IOException;

	/**
	 * Assert that the {@link HibSchemaChange#apply(com.gentics.mesh.core.rest.schema.FieldSchemaContainer)} method is working as expected. The supplied schema
	 * container schema must be correctly mutated.
	 */
	abstract public void testApply();

	/**
	 * Assert that the {@link HibSchemaChange#updateFromRest(com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel)} method will correctly populate the graph
	 * object.
	 * @throws IOException 
	 */
	abstract public void testUpdateFromRest() throws IOException;

	/**
	 * Assert that the change will be correctly transformed into the {@link SchemaChangeModel} rest model pojo.
	 * @throws IOException 
	 */
	abstract public void testTransformToRest() throws IOException;

	protected PersistingSchemaDao schemaDao(Tx tx) {
		return tx.<CommonTx>unwrap().schemaDao();
	}

	protected <
				R extends FieldSchemaContainer, 
				RM extends FieldSchemaContainerVersion, 
				RE extends NameUuidReference<RE>, 
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>
			> SCV createVersion(PersistingContainerDao<R, RM, RE, SC, SCV, ?> dao) {
		SC container = dao.createPersisted(null);
		SCV version = dao.createPersistedVersion(container, v -> {});
		return version;
	}

	@SuppressWarnings("unchecked")
	protected <
				R extends FieldSchemaContainer, 
				RM extends FieldSchemaContainerVersion, 
				RE extends NameUuidReference<RE>, 
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>, 
				T extends HibSchemaChange<?>
		> T createChange(PersistingContainerDao<R, RM, RE, SC, SCV, ?> dao, SCV version, SchemaChangeOperation op) {
		return (T) dao.createPersistedChange(version, op);
	}
}
