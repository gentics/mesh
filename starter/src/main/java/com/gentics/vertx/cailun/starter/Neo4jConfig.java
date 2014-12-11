package com.gentics.vertx.cailun.starter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import javax.annotation.PostConstruct;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.vertx.cailun.model.perm.User;
import com.gentics.vertx.cailun.repository.UserRepository;
import static java.util.Arrays.asList;

@Configuration
@EnableNeo4jRepositories("com.gentics.vertx.cailun")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.vertx.cailun" })
public class Neo4jConfig extends Neo4jConfiguration {

	@Autowired
	private UserRepository userRepository;

	private final static Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);

	public Neo4jConfig() {
		setBasePackage("com.gentics.vertx.cailun");
	}

	@PostConstruct
	private void addUsers() {
		User john = new User();
		john.setFirstName("John");
		User mary = new User();
		mary.setFirstName("Mary");
		userRepository.save(asList(john, mary));
	}

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		try {
			return Neo4jGraphVerticle.getDatabase();
		} catch (Exception e) {
			logger.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

	@Bean
	public MainBean mainBean() {
		return new MainBean();
	}
}
