package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A {@link GraphFieldSchemaContainerVersion} stores the versioned data for a {@link GraphFieldSchemaContainer} element.
 * 
 * @param <R>
 *            Rest model response type
 * @param <RM><
 *            Rest model type
 * @param <RE>
 *            Reference model type
 * @param <SCV>
 *            Schema container version type
 * @param <SC>
 *            Schema container type
 * 
 */
public interface GraphFieldSchemaContainerVersion<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, RE extends NameUuidReference<RE>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>, SC extends HibFieldSchemaElement<R, RM, SC, SCV>>
	extends MeshCoreVertex<R>, ReferenceableElement<RE>, Comparable<SCV>, HibFieldSchemaVersionElement<R, RM, SC, SCV> {

	public static final String VERSION_PROPERTY_KEY = "version";

	/**
	 * Apply changes which will be extracted from the action context.
	 *
	 * @param ac
	 *            Action context that provides the migration request data
	 * @param batch
	 * @return The created schema container version
	 */
	SCV applyChanges(InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Apply the given list of changes to the schema container. This method will invoke the schema migration process.
	 *
	 * @param ac
	 * @param listOfChanges
	 * @param batch
	 * @return The created schema container version
	 */
	SCV applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges, EventQueueBatch batch);

	/**
	 * Generate a schema change list by comparing the schema with the specified schema update model which is extracted from the action context.
	 *
	 * @param ac
	 *            Action context that provides the schema update request
	 * @param comparator
	 *            Comparator to be used to compare the schemas
	 * @param restModel
	 *            Rest model of the container that should be compared
	 * @return Rest model which contains the changes list
	 */
	SchemaChangesListModel diff(InternalActionContext ac, FieldSchemaContainerComparator<?> comparator, FieldSchemaContainer restModel);	
}
