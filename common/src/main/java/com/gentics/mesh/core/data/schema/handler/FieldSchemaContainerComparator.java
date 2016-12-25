package com.gentics.mesh.core.data.schema.handler;

import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public interface FieldSchemaContainerComparator<FC extends FieldSchemaContainer> {

	/**
	 * Compare the two field containers. The implementor should invoke {@link #diff(FieldSchemaContainer, FieldSchemaContainer, Class)} and specify the actual
	 * field container class.
	 * 
	 * @param containerA
	 * @param containerB
	 * @return
	 */
	List<SchemaChangeModel> diff(FC containerA, FC containerB);

}
