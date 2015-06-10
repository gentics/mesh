package com.gentics.mesh.core.data.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.paging.PagingInfo;

public interface RoutingContextService {

	public List<String> getSelectedLanguageTags(RoutingContext rc);

	public Future<Boolean> getTagsIncludeParameter(RoutingContext rc);

	public Future<Integer> getDepthParameter(RoutingContext rc);

	public PagingInfo getPagingInfo(RoutingContext rc);

	public Future<Boolean> getChildTagIncludeParameter(RoutingContext rc);

	public Future<Boolean> getContentsIncludeParameter(RoutingContext rc);

	/**
	 * Extracts the project name from the routing context
	 * 
	 * @param rc
	 * @return extracted project name
	 */
	public String getProjectName(RoutingContext rc);

	public <T extends MeshVertex> void loadObjectByUuid(RoutingContext rc, String uuid, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler);

	public <T extends MeshVertex> void loadObjectByUuid(RoutingContext rc, String uuid, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompletedHandler);

	public <T extends MeshVertex> void loadObjectByUuid(RoutingContext rc, String uuid, String projectName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompletedHandler);

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, String projectName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompleteHandler);

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler);

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, String projectName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler);

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompleteHandler);

	void hasPermission(RoutingContext rc, MeshVertex node, PermissionType type, Handler<AsyncResult<Boolean>> resultHandler,
			Handler<AsyncResult<Boolean>> transactionCompletedHandler) throws InvalidPermissionException;

	void hasPermission(RoutingContext rc, MeshVertex node, PermissionType type, Handler<AsyncResult<Boolean>> resultHandler);

}
