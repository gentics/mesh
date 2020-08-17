package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeResponse;

public interface NodeDaoWrapper extends NodeDao, DaoWrapper<Node>, DaoTransformable<Node, NodeResponse> {



}
