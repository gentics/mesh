package com.gentics.cailun.core.data.service;

import java.util.List;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.response.RestUserResponse;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

	List<User> findAll();

	RestUserResponse transformToRest(User user);

}
