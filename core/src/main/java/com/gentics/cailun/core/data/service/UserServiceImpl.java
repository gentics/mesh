package com.gentics.cailun.core.data.service;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.user.response.UserResponse;
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
	public List<User> findAll() {

		// TODO i assume this could create memory problems for big data
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			List<User> list = new ArrayList<>();
			for (User user : userRepository.findAll()) {
				list.add(user);
			}
			tx.success();
			return list;
		}

	}

	@Override
	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	@Override
	public UserResponse transformToRest(User user) {
		if (user == null) {
			return null;
		}
		UserResponse restUser = new UserResponse();
		restUser.setUuid(user.getUuid());
		restUser.setUsername(user.getUsername());
		restUser.setEmailAddress(user.getEmailAddress());
		restUser.setFirstname(user.getFirstname());
		restUser.setLastname(user.getLastname());

		for (Group group : user.getGroups()) {
			restUser.addGroup(group.getName());
		}
		return restUser;
	}
}
