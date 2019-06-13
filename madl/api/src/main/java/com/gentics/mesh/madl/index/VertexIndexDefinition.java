package com.gentics.mesh.madl.index;

import com.gentics.mesh.madl.index.impl.VertexIndexDefinitionImpl;
import com.gentics.mesh.madl.index.impl.VertexIndexDefinitionImpl.VertexIndexDefinitionBuilder;

public interface VertexIndexDefinition extends ElementIndexDefinition {

	public static VertexIndexDefinitionBuilder vertexIndex(Class<?> clazz) {
		return new VertexIndexDefinitionImpl.VertexIndexDefinitionBuilder(clazz);
	}

	/**
	 * Return the vertex class which the index should be created.
	 * 
	 * @return
	 */
	Class<?> getClazz();

}
