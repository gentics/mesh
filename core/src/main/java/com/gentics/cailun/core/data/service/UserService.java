package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.Set;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.paging.PagingInfo;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

	/**
	 * Find all users that are readable by the given user. Utilize the paging info when returning paged user data.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return
	 */
	Page<User> findAllVisible(User requestUser, PagingInfo pagingInfo);

	UserResponse transformToRest(User user);

	boolean removeUserFromGroup(User user, Group group);

	Set<GraphPermission> findGraphPermissions(User user, AbstractPersistable node);

	boolean isPermitted(long userNodeId, CaiLunPermission genericPermission) throws Exception;

	String[] getPerms(RoutingContext rc, AbstractPersistable node);

}
