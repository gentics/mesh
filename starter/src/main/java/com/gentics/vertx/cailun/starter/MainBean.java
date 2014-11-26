package com.gentics.vertx.cailun.starter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.vertx.cailun.repository.Page;
import com.gentics.vertx.cailun.repository.PageRepository;

public class MainBean {

	@Autowired
	private PageRepository pageRepository;

	private static Logger log = LoggerFactory.getLogger(MainBean.class);

	public void start() {

		// // Spring Data JPA CRUD operations are transactionnal by default !
		// // http://static.springsource.org/spring-data/data-jpa/docs/current/reference/html/#transactions
		// User newUser = new User();
		// newUser.setName("inserted");
		// userRepository.save(newUser);
		//
		// List<User> all = userRepository.findAll();
		// log.info("users=" + all);

		Page page = new Page();
		page.setName("Some name");
		page.setContent("Some total nice content");
		pageRepository.save(page);

		System.out.println("COUNT:  " + pageRepository.count());
	}
}
