package com.gentics.mesh.core.data.service.transformation;

import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;

public class TransformationInfo {

	private MeshAuthUser requestUser;
	private List<String> languageTags = new ArrayList<>();
	private RoutingContext rc;
	// Storage for object references
	private Map<String, AbstractRestModel> objectReferences = new HashMap<>();

	public TransformationInfo(MeshAuthUser requestUser, List<String> languageTags, RoutingContext rc) {
		this.requestUser = requestUser;
		this.languageTags = languageTags;
		this.rc = rc;
	}

	public Map<String, AbstractRestModel> getObjectReferences() {
		return objectReferences;
	}

	public MeshAuthUser getRequestUser() {
		return requestUser;
	}

	//	public int getMaxDepth() {
	//		return maxDepth;
	//	}
	//
	public AbstractRestModel getObject(String uuid) {
		return objectReferences.get(uuid);
	}

	//
	public void addObject(String uuid, AbstractRestModel object) {
		objectReferences.put(uuid, object);
	}

	public List<String> getLanguageTags() {
		return languageTags;
	}

	public RoutingContext getRoutingContext() {
		return rc;
	}

}
