package com.gentics.cailun.core.data.service;

import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

	Result<User> findAll();

	UserResponse transformToRest(User user);

	boolean removeUserFromGroup(User user, Group group);

}
