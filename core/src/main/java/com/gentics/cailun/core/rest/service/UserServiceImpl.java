package com.gentics.cailun.core.rest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.service.generic.GenericNodeServiceImpl;
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
}
