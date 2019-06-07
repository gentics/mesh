package com.syncleus.ferma.type;

import com.syncleus.ferma.type.impl.EdgeTypeDefinitionImpl;
import com.syncleus.ferma.type.impl.EdgeTypeDefinitionImpl.EdgeTypeDefinitionBuilder;

public interface EdgeTypeDefinition extends ElementTypeDefinition {

	/**
	 * Create a new edge type definition builder for the given label.
	 * 
	 * @param label
	 * @param stringPropertyKeys
	 */
	public static EdgeTypeDefinitionBuilder edgeType(String label) {
		return new EdgeTypeDefinitionImpl.EdgeTypeDefinitionBuilder(label);
	}

	/**
	 * Label of the edge.
	 * 
	 * @return
	 */
	String getLabel();
}
