package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.util.InvalidArgumentException;

public interface ContainerNode<T extends AbstractResponse> extends GenericVertex<T> {

	List<? extends Node> getChildren();

	void setParentNode(ContainerNode<T> parentNode);

	Node create(User creator, SchemaContainer schemaContainer, Project project);

	Page<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) throws InvalidArgumentException;

}
