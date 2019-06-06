package com.gentics.mesh.core.schema.change;

import java.io.IOException;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
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
	 * Assert that the {@link SchemaChange#apply(com.gentics.mesh.core.rest.schema.FieldSchemaContainer)} method is working as expected. The supplied schema
	 * container schema must be correctly mutated.
	 */
	abstract public void testApply();

	/**
	 * Assert that the {@link SchemaChange#updateFromRest(com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel)} method will correctly populate the graph
	 * object.
	 * @throws IOException 
	 */
	abstract public void testUpdateFromRest() throws IOException;

	/**
	 * Assert that the change will be correctly transformed into the {@link SchemaChangeModel} rest model pojo.
	 * @throws IOException 
	 */
	abstract public void testTransformToRest() throws IOException;
}
