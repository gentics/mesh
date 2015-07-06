package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.util.InvalidArgumentException;

public interface RootVertex<T extends MeshVertex> extends MeshVertex {

	T findByUUID(String uuid);

	List<? extends T> findAll();

	T findByName(String name);

	Page<? extends T> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;


}
