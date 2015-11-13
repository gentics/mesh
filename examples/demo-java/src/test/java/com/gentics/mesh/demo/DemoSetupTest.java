package com.gentics.mesh.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.SpringTestConfiguration;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class DemoSetupTest {

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	private DemoDataProvider dataProvider;

	@Autowired
	protected DatabaseService databaseService;

	@Autowired
	protected Database db;

	protected NoTrx tx;

	@Before
	public void setup() throws Exception {
		dataProvider.setup();
		tx = db.noTrx();
	}

	@After
	public void cleanup() {
		tx.close();
		BootstrapInitializer.clearReferences();
		Database db = databaseService.getDatabase();
		db.clear();
		DatabaseHelper helper = new DatabaseHelper(db);
		helper.init();
	}

	@Test
	public void testSetup() throws Exception {
		assertTrue(boot.meshRoot().getProjectRoot().findByName("demo").getNodeRoot().findAll().size() > 0);
		User user = boot.meshRoot().getUserRoot().findByUsername("webclient");
		assertNotNull(user);
		Group group = user.getGroups().get(0);
		assertNotNull(group);
		Role role = group.getRoles().get(0);

		assertTrue(role.hasPermission(GraphPermission.READ_PERM, user));
		assertTrue(user.hasPermission(user, GraphPermission.READ_PERM));
		assertTrue(user.hasPermission(boot.meshRoot().getUserRoot(), GraphPermission.READ_PERM));
	}
}
