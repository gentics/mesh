package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.EMBEDDED;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;
@MeshTestSetting(elasticsearch = EMBEDDED, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class EmbeddedElasticsearchTest extends AbstractMeshTest {

	private static final Logger log = LoggerFactory.getLogger(EmbeddedElasticsearchTest.class);

	@Before
	public void waitForES() throws Exception {
		Observable.interval(1, TimeUnit.SECONDS)
			.doOnNext(ignore -> log.info("Waiting for ES..."))
			.flatMapSingle(ignore -> client().searchStatus().toSingle())
			.takeWhile(status -> !status.isAvailable())
			.ignoreElements().blockingAwait(20, TimeUnit.SECONDS);
		log.info("Done waiting for ES");
	}

	@Test
	public void testSimpleQuerySearch() throws Exception {
		recreateIndices();
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		waitForSearchIdleEvent();

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals(1, list.getData().size());
		assertEquals("The found element is not the user we were looking for", username, list.getData().get(0).getUsername());

	}

}
