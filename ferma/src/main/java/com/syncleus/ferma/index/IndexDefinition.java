package com.syncleus.ferma.index;

import com.syncleus.ferma.index.impl.EdgeIndexDefinitionImpl;
import com.syncleus.ferma.index.impl.EdgeIndexDefinitionImpl.EdgeIndexDefinitonBuilder;
import com.syncleus.ferma.index.impl.VertexIndexDefinitionImpl;
import com.syncleus.ferma.index.impl.VertexIndexDefinitionImpl.VertexIndexDefinitonBuilder;

public interface IndexDefinition {

	public static EdgeIndexDefinitonBuilder edge() {
		return new EdgeIndexDefinitionImpl.EdgeIndexDefinitonBuilder();
	}
	
	public static VertexIndexDefinitonBuilder vertex() {
		return new VertexIndexDefinitionImpl.VertexIndexDefinitonBuilder();
	}
}
