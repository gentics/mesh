package com.gentics.mesh.core.data.schema.handler;

import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * Comparator for field schema containers are used to detect changes in between multiple schema versions.
 * 
 * @param <FC>
 */
public interface FieldSchemaContainerComparator<FC extends FieldSchemaContainer> {

	/**
	 * Compare the two field containers. The implementor should invoke {@link #diff(FieldSchemaContainer, FieldSchemaContainer, Class)} and specify the actual field container
	 * class.
	 * 
	 * @param containerA
	 *            First container
	 * @param containerB
	 *            Second container
	 * @return List of detected changed
	 */
	List<SchemaChangeModel> diff(FC containerA, FC containerB);

}
