package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.monitoring.AbstractHealthTest;
import com.gentics.mesh.test.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class DatabaseHealthTest extends AbstractHealthTest {

	@Test
	public void healthyDatabase() {
		assertTrue(db().isHealthy());
		call(() -> monClient().live(), HttpResponseStatus.OK);
		call(() -> monClient().ready(), HttpResponseStatus.OK);
	}

	@Test
	public void stoppedDatabase() throws Exception {
		db().stop();
		assertFalse(db().isHealthy());
		call(() -> monClient().live(), HttpResponseStatus.OK);
		call(() -> monClient().ready(), HttpResponseStatus.SERVICE_UNAVAILABLE);
		db().init(null);// get all back to close the test gracefully
	}

	@Test
	public void reappearedDatabase() throws Exception {
		stoppedDatabase(); // this starts the db back in the end
		setReady();
		assertTrue(db().isHealthy());
		call(() -> monClient().live(), HttpResponseStatus.OK);
		call(() -> monClient().ready(), HttpResponseStatus.OK);
	}
}
