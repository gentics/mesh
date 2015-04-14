package com.gentics.cailun.test;

import java.io.IOException;

import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.I18NService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.verticle.UserVerticle;
import com.gentics.cailun.demo.DemoDataProvider;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.util.RestAssert;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractDBTest {

	@Autowired
	protected LanguageService languageService;

	@Autowired
	private DemoDataProvider dataProvider;

	@Autowired
	protected RoleService roleService;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Autowired
	protected Neo4jTemplate neo4jTemplate;

	@Autowired
	protected GraphDatabaseService graphDb;

	@Autowired
	protected UserVerticle userVerticle;

	@Autowired
	protected UserService userService;

	@Autowired
	protected GroupService groupService;

	@Autowired
	protected RestAssert test;

	@Autowired
	private I18NService i18n;

	public void setupData() throws JsonParseException, JsonMappingException, IOException {
		purgeDatabase();
		dataProvider.setup(1);
	}

	public DemoDataProvider data() {
		return dataProvider;
	}

	protected void purgeDatabase() {
		try (Transaction tx = graphDb.beginTx()) {
			for (Node node : graphDb.getAllNodes()) {
				for (Relationship rel : node.getRelationships()) {
					rel.delete();
				}
				node.delete();
			}
			tx.success();
		}
	}
}
