package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaMigrationResponse;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * Common graph model interface for schema field containers.
 * 
 * @param <R>
 *            Response model class of the container (e.g.: {@link SchemaResponse})
 * @param <V>
 *            Vertex implementation class
 * @param <RE>
 *            Response reference model class of the container (e.g.: {@link SchemaReference})
 */
public interface GraphFieldSchemaContainer<R extends RestModel, V extends GraphFieldSchemaContainer<R, V, RE>, RE extends NameUuidReference<RE>>
		extends MeshCoreVertex<R, V>, ReferenceableElement<RE> {

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	int getVersion();

	/**
	 * Return the next version of this schema.
	 * 
	 * @return
	 */
	V getNextVersion();

	/**
	 * Set the next version of the schema container.
	 * 
	 * @param container
	 */
	void setNextVersion(V container);

	/**
	 * Return the previous version of this schema.
	 * 
	 * @return
	 */
	V getPreviousVersion();

	/**
	 * Set the previous version of the container.
	 * 
	 * @param container
	 */
	void setPreviousVersion(V container);

	/**
	 * Return the change for the previous version of the schema. Normally the previous change was used to build the schema.
	 * 
	 * @return
	 */
	SchemaChange<?> getPreviousChange();

	/**
	 * Return the change for the next version.
	 * 
	 * @return Can be null if no further changes exist
	 */
	SchemaChange<?> getNextChange();

	/**
	 * Set the next change for the schema. The next change is the first change in the chain of changes that lead to the new schema version.
	 * 
	 * @param change
	 */
	void setNextChange(SchemaChange<?> change);

	/**
	 * Set the previous change for the schema. The previous change is the last change in the chain of changes that was used to create the schema container.
	 * 
	 * @param change
	 */
	void setPreviousChange(SchemaChange<?> change);

	/**
	 * Generate a schema change list by comparing the schema with the specified schema update model which is extracted from the action context.
	 * 
	 * @param ac
	 *            Action context that provides the schema update request
	 * @param comparator
	 *            Comparator to be used to compare the schemas
	 * @param restModel
	 *            Rest model of the container that should be compared
	 * @return
	 */
	Observable<SchemaChangesListModel> diff(InternalActionContext ac, AbstractFieldSchemaContainerComparator<?> comparator,
			FieldSchemaContainer restModel);

	/**
	 * Return the latest container version.
	 * 
	 * @return Latest version
	 */
	V getLatestVersion();

	/**
	 * Apply changes which will be extracted from the action context.
	 * 
	 * @param ac
	 *            Action context that provides the migration request data
	 * @return
	 */
	Observable<SchemaMigrationResponse> applyChanges(InternalActionContext ac);

}
