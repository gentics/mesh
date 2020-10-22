package com.gentics.mesh.graphdb.orientdb.index;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public abstract class AbstractOrientTest {

	public void addEdgeType(Supplier<OrientGraphNoTx> txProvider, String label, String superTypeName, Consumer<OrientEdgeType> typeModifier) {
		System.out.println("Adding edge type for label {" + label + "}");

		OrientGraphNoTx noTx = txProvider.get();
		try {
			OrientEdgeType edgeType = noTx.getEdgeType(label);
			if (edgeType == null) {
				String superClazz = "E";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				edgeType = noTx.createEdgeType(label, superClazz);

				if (typeModifier != null) {
					typeModifier.accept(edgeType);
				}
			}
		} finally {
			noTx.shutdown();
		}

	}

	public void addVertexType(Supplier<OrientGraphNoTx> txProvider, String typeName, String superTypeName,
		Consumer<OrientVertexType> typeModifier) {

		System.out.println("Adding vertex type for class {" + typeName + "}");

		OrientGraphNoTx noTx = txProvider.get();
		try {
			OrientVertexType vertexType = noTx.getVertexType(typeName);
			if (vertexType == null) {
				String superClazz = "V";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				vertexType = noTx.createVertexType(typeName, superClazz);

				if (typeModifier != null) {
					typeModifier.accept(vertexType);
				}
			}
		} finally {
			noTx.shutdown();
		}
	}
}
