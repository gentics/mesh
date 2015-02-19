package com.gentics.cailun.core.rest.service;

import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.service.generic.GenericNodeService;

public interface UserService extends GenericNodeService<User> {

	void setPassword(User user, String password);

	User findByUsername(String username);

}
