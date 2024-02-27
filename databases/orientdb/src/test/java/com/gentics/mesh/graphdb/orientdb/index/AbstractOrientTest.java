package com.gentics.mesh.graphdb.orientdb.index;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;

import com.orientechnologies.orient.core.metadata.schema.OClass;

public abstract class AbstractOrientTest {

	public void addEdgeType(Supplier<OrientGraph> txProvider, String label, String superTypeName, Consumer<OClass> typeModifier) {
		System.out.println("Adding edge type for label {" + label + "}");

		OrientGraph noTx = txProvider.get();
		try {
			OClass edgeType = noTx.getRawDatabase().getMetadata().getSchema().getClass(label);
			if (edgeType == null) {
				String superClazz = "E";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				noTx.createClass(label, superClazz);
				edgeType = noTx.getRawDatabase().getMetadata().getSchema().getClass(label);

				if (typeModifier != null) {
					typeModifier.accept(edgeType);
				}
			}
		} finally {
			noTx.close();
		}

	}

	public void addVertexType(Supplier<OrientGraph> txProvider, String typeName, String superTypeName,
		Consumer<OClass> typeModifier) {

		System.out.println("Adding vertex type for class {" + typeName + "}");

		OrientGraph noTx = txProvider.get();
		try {
			OClass vertexType = noTx.getRawDatabase().getMetadata().getSchema().getClass(typeName);
			if (vertexType == null) {
				String superClazz = "V";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				noTx.createClass(typeName, superClazz);
				vertexType = noTx.getRawDatabase().getMetadata().getSchema().getClass(typeName);

				if (typeModifier != null) {
					typeModifier.accept(vertexType);
				}
			}
		} finally {
			noTx.close();
		}
	}
}
