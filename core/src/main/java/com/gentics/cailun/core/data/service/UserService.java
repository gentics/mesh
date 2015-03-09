package com.gentics.cailun.core.data.service;

import java.util.List;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

	List<User> findAll();

	UserResponse transformToRest(User user);

	User transformFromRest(UserCreateRequest requestModel);

}
