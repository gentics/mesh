package com.syncleus.ferma.index;

import com.syncleus.ferma.index.impl.VertexIndexDefinitionImpl;
import com.syncleus.ferma.index.impl.VertexIndexDefinitionImpl.VertexIndexDefinitionBuilder;

public interface VertexIndexDefinition extends ElementIndexDefinition {

	public static VertexIndexDefinitionBuilder builder() {
		return new VertexIndexDefinitionImpl.VertexIndexDefinitionBuilder();
	}
}
