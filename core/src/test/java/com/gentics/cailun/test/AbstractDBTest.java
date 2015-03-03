package com.gentics.cailun.test;

import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractDBTest {

	@Autowired
	protected LanguageService languageService;

	@Autowired
	private TestDataProvider dataProvider;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	public void setupData() {
		purgeDatabase();
		dataProvider.setup();
	}

	public TestDataProvider data() {
		return dataProvider;
	}

	protected void purgeDatabase() {
		try (Transaction tx = springConfig.graphDatabase().beginTx()) {
			for (Node node : springConfig.getGraphDatabaseService().getAllNodes()) {
				for (Relationship rel : node.getRelationships()) {
					rel.delete();
				}
				node.delete();
			}
			tx.success();
		}
	}
}
