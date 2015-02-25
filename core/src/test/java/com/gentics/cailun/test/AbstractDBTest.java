package com.gentics.cailun.test;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class AbstractDBTest {

	@Autowired
	protected LanguageService languageService;

	@Autowired
	private DummyDataProvider dataProvider;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Before
	public void setup() {
		purgeDatabase();
		dataProvider.setup();
	}

	public DummyDataProvider getDataProvider() {
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
