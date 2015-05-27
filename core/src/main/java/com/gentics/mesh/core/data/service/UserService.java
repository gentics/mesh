package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.Set;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.GraphPermission;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.core.rest.user.response.UserResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

	/**
	 * Find all users that are readable. Utilize the paging info when returning paged user data.
	 * 
	 * @param rc
	 * @param pagingInfo
	 * @return
	 */
	Page<User> findAllVisible(RoutingContext rc, PagingInfo pagingInfo);

	UserResponse transformToRest(User user);

	boolean removeUserFromGroup(User user, Group group);

	Set<GraphPermission> findGraphPermissions(User user, AbstractPersistable node);

	boolean isPermitted(long userNodeId, MeshPermission genericPermission) throws Exception;

	String[] getPerms(RoutingContext rc, AbstractPersistable node);

	User findUser(RoutingContext rc);

	Page<User> findByGroup(RoutingContext rc, Group group, PagingInfo pagingInfo);

}
