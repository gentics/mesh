package com.gentics.mesh.core.data.root;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.util.InvalidArgumentException;

public interface RootVertex<T extends MeshVertex> extends MeshVertex {

	RootVertex findByUuid(String uuid, Handler<AsyncResult<T>> resultHandler);

	List<? extends T> findAll();

	T findByName(String name);

	Page<? extends T> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;


}
