package com.gentics.cailun.cli;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.CaiLunRoot;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class CaiLunInitializer {

	private static Logger LOG = LoggerFactory.getLogger(CaiLunInitializer.class);

	@Autowired
	CaiLunRootRepository rootRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	GroupRepository groupRepository;

	@Autowired
	CaiLunSpringConfiguration configuration;

	public CaiLunInitializer() {
	}

	public void init() {
		// Verify that the root node is existing
		CaiLunRoot rootNode = rootRepository.findRoot();
		if (rootNode == null) {
			rootNode = new CaiLunRoot();
			rootRepository.save(rootNode);
			LOG.info("Stored root node");
		}
		// Reload the node to get one with a valid uuid
		rootNode = rootRepository.findRoot();

		// Verify that an admin user exists
		User adminUser = userRepository.findByUsername("admin");
		if (adminUser == null) {
			adminUser = new User("admin");
			System.out.println("Enter admin password:");
			Scanner scanIn = new Scanner(System.in);
			String pw = scanIn.nextLine();
			scanIn.close();
			adminUser.setPasswordHash(configuration.passwordEncoder().encode(pw));
			userRepository.save(adminUser);
			LOG.info("Stored admin user");
		}
		rootNode.getMembers().add(adminUser);
		rootRepository.save(rootNode);

		Group adminGroup = groupRepository.findByName("admin");
		if (adminGroup == null) {
			adminGroup = new Group("admin");
			adminGroup.getMembers().add(adminUser);
			groupRepository.save(adminGroup);
			LOG.info("Stored admin group");
		}
		rootNode.setRootGroup(adminGroup);
		rootRepository.save(rootNode);

	}
}
