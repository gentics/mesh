package com.gentics.mesh.core.action;

import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * Node DAO action
 * 
 * TODO MDM move this to {@link NodeDao} instead and remove this interface
 */
public interface NodeDAOActions extends DAOActions<HibNode, NodeResponse> {

}
