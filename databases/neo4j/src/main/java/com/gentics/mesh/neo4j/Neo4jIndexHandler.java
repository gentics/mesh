package com.gentics.mesh.neo4j;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.index.IndexHandler;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.index.ElementIndexDefinition;
import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.VertexFrame;

@Singleton
public class Neo4jIndexHandler implements IndexHandler {

	@Inject
	public Neo4jIndexHandler() {
	}

	@Override
	public void reindex() {
		// NOOP
	}

	@Override
	public void createIndex(ElementIndexDefinition def) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeVertexIndex(String indexName, Class<? extends VertexFrame> clazz) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Object> edgeLookup(String edgeLabel, String indexPostfix, Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCustomEdgeIndex(String label, String indexPostfix, FieldMap fields, boolean unique) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object createComposedIndexKey(Object... keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ElementFrame> T checkIndexUniqueness(String indexName, T element, Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ElementFrame> T checkIndexUniqueness(String indexName, Class<T> classOfT, Object key) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public <T extends VertexFrame> T findByUuid(Class<? extends T> classOfT, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}
}
