package com.gentics.diktyo.orientdb3.index;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;

import com.gentics.diktyo.index.AbstractIndexManager;
import com.gentics.diktyo.index.Index;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.index.OIndexManager;

@Singleton
public class IndexManagerImpl extends AbstractIndexManager {

	@Inject
	public IndexManagerImpl() {
	}

	@Override
	public boolean exists(String name) {
		ODatabaseSession db = null;
		db.getMetadata().getSchema().existsClass(name);

		// or
		OrientGraph graph = null;
		OIndexManager idxMgr = graph.getRawDatabase().getMetadata().getIndexManager();
		idxMgr.existsIndex("indexName");

		return true;
	}

	@Override
	public Index get(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Index> list() {
		// TODO Auto-generated method stub
		return null;
	}

}
