package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.response.RestUser;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@Component
@Transactional
public class UserServiceImpl extends GenericNodeServiceImpl<User> implements UserService {

	@Autowired
	private CaiLunSpringConfiguration springConfiguration;

	@Autowired
	private UserRepository userRepository;

	@Override
	public void setPassword(User user, String password) {
		user.setPasswordHash(springConfiguration.passwordEncoder().encode(password));
	}

	@Override
	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	@Override
	public RestUser getResponseObject(User user) {

		RestUser restUser = new RestUser();
		restUser.setUsername(user.getUsername());
		restUser.setEmailAddress(user.getEmailAddress());
		restUser.setFirstname(user.getFirstname());
		restUser.setLastname(user.getLastname());
		return restUser;
	}
}
