package com.gentics.mesh.plugin.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.local.MeshLocalServer;

import io.reactivex.Completable;

/**
 * Integration tests for the FavoritePlugin
 */
public class FavoritePluginIntegrationTest extends AbstractPluginLocalIntegrationTest {

	private static FavoritePluginClient client;

	@ClassRule
	public final static MeshLocalServer server = new HibernateMeshLocalServer()
			.waitForStartup();

	@BeforeClass
	public static void setUp() throws Exception {
		server.client().createProject(new ProjectCreateRequest().setName("test").setSchemaRef("folder")).blockingAwait();
		server.client().createUser(new UserCreateRequest().setUsername("user1").setPassword("user1"));

		server.getMesh().deployPlugin(FavoriteTestPlugin.class, "favorite").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "favorite");
		client = new FavoritePluginClient(server.client(), "test");
	}

	@After
	public void cleanUp() {
		switchUser("admin");
		client.deleteAllFavorites().blockingAwait();
	}

	@Test
	public void testAddFavorite() {
		addFavorites("widgets_max_nr_prepend", "abc1", "abc2", "abc3");
		assertThat(client.getFavorites("widgets_max_nr_prepend").blockingGet()).containsExactly("abc3", "abc2", "abc1");
	}
	@Test
	public void testAddFavoriteNoConfig() {
		assertStatusCode(400, client.addFavorite("infoboxxx", "111"));
	}

	@Test
	public void testDeleteFavoriteNoConfig() {
		assertStatusCode(400, client.deleteFavorite("infoboxxx", "abc1"));
	}

	@Test
	public void testOverrideFavoriteNoConfig() {
		assertStatusCode(400, client.overrideFavorite("infoboxxx", "abc1"));
	}

	@Test
	public void testGetFavoriteNoConfig() {
		assertStatusCode(400, client.getFavorites("widgetsss").ignoreElement());
	}

	@Test
	public void testAnonymous() {
		switchUser("anonymous");
		assertStatusCode(401, client.addFavorite("infobox_max_nr_duplicates", "abc1"));
	}

	@Test
	public void testWidgets() {
		addFavorites("widgets", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc4", "abc9", "abc10");
		assertThat(client.getFavorites("widgets").blockingGet()).containsExactly("abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10");

	}

	@Test
	public void testInfobox() {
		addFavorites("infobox", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10");
		assertThat(client.getFavorites("infobox").blockingGet()).containsExactly("abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10");

	}

	@Test
	public void testOngoingProcedures() {
		addFavorites("ongoing_procedures", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("ongoing_procedures").blockingGet()).containsExactly("abc5", "abc6", "abc4");

	}

	@Test
	public void testMaxEntriesWithDuplicatesAppend() {
		addFavorites("infobox_max_nr_duplicates", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("infobox_max_nr_duplicates").blockingGet()).containsExactly("abc3", "abc4", "abc5", "abc6", "abc4");

	}

	@Test
	public void testMaxEntriesWithoutDuplicatesPrepend() {
		addFavorites("widgets_max_nr_prepend", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("widgets_max_nr_prepend").blockingGet()).containsExactly("abc6", "abc5", "abc4", "abc3");

	}

	@Test
	public void testMaxEntriesWithDuplicatesPrepend() {
		addFavorites("ongoing_procedures_duplicates", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("ongoing_procedures_duplicates").blockingGet()).containsExactly("abc4", "abc5", "abc6", "abc4");

	}

	@Test
	public void testMaxEntriesWithOutDuplicatesAllowReorderPrepend() {
		addFavorites("prepend_duplicates", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("prepend_duplicates").blockingGet()).containsExactly("abc4", "abc6", "abc5", "abc3", "abc2");

	}

	@Test
	public void testMaxEntriesWithOutDuplicatesDontAllowReorderPrepend() {
		addFavorites("widgets_max_nr_prepend", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("widgets_max_nr_prepend").blockingGet()).containsExactly("abc6", "abc5", "abc4", "abc3");

	}

	@Test
	public void testMaxEntriesWithOutDuplicatesAllowReorderAppend() {
		addFavorites("ongoing_procedures_append_reorder", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("ongoing_procedures_append_reorder").blockingGet()).containsExactly("abc3", "abc5", "abc6", "abc4");

	}

	@Test
	public void testMaxEntriesWithOutDuplicatesDontAllowReorderAppend() {
		addFavorites("ongoing_procedures_append", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc4");
		assertThat(client.getFavorites("ongoing_procedures_append").blockingGet()).containsExactly("abc3", "abc4", "abc5", "abc6");

	}

	@Test
	public void testUnlimitedEntries() {
		addFavorites("test_no_max", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10", "abc11", "abc12", "abc13", "abc14", "abc15", "abc16", "abc17", "abc18", "abc19", "abc20");
		assertThat(client.getFavorites("test_no_max").blockingGet()).hasSize(20);

	}

	@Test
	public void testOverride() {
		addFavorites("ongoing_procedures_duplicates", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6");
		client.overrideFavorite("ongoing_procedures_duplicates", "widget1", "widget2").blockingAwait();
		assertThat(client.getFavorites("ongoing_procedures_duplicates").blockingGet()).containsExactly("widget1", "widget2");

	}

	@Test
	public void testDelete() {
		addFavorites("infobox_max_nr_duplicates", "abc1", "abc2", "abc1", "abc1", "abc3", "abc1", "abc1");
		client.deleteFavorite("infobox_max_nr_duplicates", "abc1").blockingAwait();
		assertThat(client.getFavorites("infobox_max_nr_duplicates").blockingGet()).containsExactly("abc3");

	}

	@Test
	public void testWrongRegexType() {
		assertStatusCode(400, client.addFavorite("infobox.2", "abc1"));


	}

	private void switchUser(String userName) {
		if (userName.equals("anonymous")) {
			server.client().logout().blockingGet();
		} else {
			server.client().setLogin(userName, userName).login().blockingGet();
		}

	}

	protected void assertStatusCode(int code, Completable completable) {
		try {
			completable.blockingAwait();
			fail();
		} catch (Throwable e) {
			e.getCause();
			if (e.getCause() instanceof MeshRestClientMessageException) {
				assertEquals(code, ((MeshRestClientMessageException) e.getCause()).getStatusCode());
			} else {
				throw e;
			}
		}

	}
	private void addFavorites(String type, String... favorites){
		for (String favorite : favorites) {
			client.addFavorite(type, favorite).blockingAwait();
		}
	}
}
