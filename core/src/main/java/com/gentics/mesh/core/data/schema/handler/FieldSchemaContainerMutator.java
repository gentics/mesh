package com.gentics.mesh.core.data.schema.handler;

import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.dagger.MeshComponent;

/**
 * The field container mutator utilizes {@link SchemaChange} objects in order to modify/mutate a given field container implementation (e.g. {@link Schema} or
 * {@link Microschema}. This way {@link SchemaChange} operations can be applied on a given schema.
 */
public class FieldSchemaContainerMutator {

	/**
	 * Applies all changes that are connected to the container to the version of the container and returns the mutated version of the field container that was
	 * initially loaded from the graph field container element.
	 * 
	 * @param containerVersion
	 *            Graph element that provides the chain of changes and the field container that should be mutated
	 * @return
	 */
	public <RM extends FieldSchemaContainer> RM apply(GraphFieldSchemaContainerVersion<?, RM, ?, ?, ?> containerVersion) {
		MeshComponent mesh = containerVersion.getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		RM oldSchema = containerVersion.getSchema();
		mesh.serverSchemaStorage().remove(oldSchema);
		SchemaChange<?> change = containerVersion.getNextChange();
		while (change != null) {
			oldSchema = change.apply(oldSchema);
			change = change.getNextChange();
		}
		return oldSchema;
	}

}
