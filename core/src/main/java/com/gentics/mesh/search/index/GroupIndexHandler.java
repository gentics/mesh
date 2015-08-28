package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@Component
public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	@Override
	protected String getIndex() {
		return "group";
	}

	@Override
	protected String getType() {
		return "group";
	}

	@Override
	protected RootVertex<Group> getRootVertex() {
		return boot.meshRoot().getGroupRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Group group) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", group.getName());
		addBasicReferences(map, group);
		return map;
	}
}
