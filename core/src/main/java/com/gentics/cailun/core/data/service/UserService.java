package com.gentics.cailun.core.data.service;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.path.PagingInfo;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

	/**
	 * Find all users that are readable by the given user. Utilize the paging info when returning paged user data.
	 * 
	 * @param user
	 * @param pagingInfo
	 * @return
	 */
	Page<User> findAllVisible(User user, PagingInfo pagingInfo);

	UserResponse transformToRest(User user);

	boolean removeUserFromGroup(User user, Group group);

}
