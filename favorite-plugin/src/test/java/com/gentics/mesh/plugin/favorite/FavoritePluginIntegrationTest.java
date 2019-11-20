package com.gentics.mesh.plugin.favorite;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.plugin.favorite.error.InvalidTypeError;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.Response;
import org.junit.*;

import com.gentics.mesh.test.local.MeshLocalServer;

/**
 * Integration tests for the FavoritePlugin
 */
public class FavoritePluginIntegrationTest {

	private static FavoritePluginClient client;

	@ClassRule
	public final static MeshLocalServer server = new MeshLocalServer()
			.withInMemoryMode()
			.waitForStartup();

	@BeforeClass
	public static void setUp() throws Exception {
		server.client().createProject(new ProjectCreateRequest().setName("test").setSchemaRef("folder")).blockingAwait();
		server.client().createUser(new UserCreateRequest().setUsername("user1").setPassword("user1"));

		server.getMesh().deployPlugin(FavoriteTestPlugin.class, "favorite").blockingAwait();
		client = new FavoritePluginClient(server.client(), "test");
	}


	@After
	public void cleanUp() {
		switchUser("admin");
		client.deleteAllFavorites().blockingAwait();
	}

	@Test
	public void testAddFavorite() {
		client.addFavorite("widgets", "abc1").blockingAwait();
		client.addFavorite("widgets", "abc2").blockingAwait();
		client.addFavorite("widgets", "abc3").blockingAwait();
		assertThat(client.getFavorites("widgets").blockingGet()).containsExactly("abc3", "abc2", "abc1");
	}

	@Test
	public void testAnonymous() {
		switchUser("anonymous");
		assertStatusCode(401, client.addFavorite("infobox.2", "abc1"));
	}
	@Test
	public void testMaxEntriesWithDuplicatesAppend() {
		client.addFavorite("infobox", "abc1").blockingAwait();
		client.addFavorite("infobox", "abc2").blockingAwait();
		client.addFavorite("infobox", "abc3").blockingAwait();
		client.addFavorite("infobox", "abc4").blockingAwait();
		client.addFavorite("infobox", "abc5").blockingAwait();
		client.addFavorite("infobox", "abc6").blockingAwait();
		client.addFavorite("infobox", "abc4").blockingAwait();
		assertThat(client.getFavorites("infobox").blockingGet()).containsExactly("abc3", "abc4", "abc5", "abc6", "abc4");

	}
	@Test
	public void testMaxEntriesWithoutDuplicatesPrepend() {
		client.addFavorite("widgets", "abc1").blockingAwait();
		client.addFavorite("widgets", "abc2").blockingAwait();
		client.addFavorite("widgets", "abc3").blockingAwait();
		client.addFavorite("widgets", "abc4").blockingAwait();
		client.addFavorite("widgets", "abc5").blockingAwait();
		client.addFavorite("widgets", "abc6").blockingAwait();
		client.addFavorite("widgets", "abc4").blockingAwait();
		assertThat(client.getFavorites("widgets").blockingGet()).containsExactly("abc6", "abc5", "abc4", "abc3");

	}
	@Test
	public void testMaxEntriesWithDuplicatesPrepend() {
		client.addFavorite("ongoing_procedures", "abc1").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc2").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc3").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc4").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc5").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc6").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc4").blockingAwait();
		assertThat(client.getFavorites("ongoing_procedures").blockingGet()).containsExactly("abc4", "abc5", "abc6", "abc4");

	}
	@Test
	public void testMaxEntriesWithOutDuplicatesAllowReorderPrepend() {
		client.addFavorite("prepend_duplicates", "abc1").blockingAwait();
		client.addFavorite("prepend_duplicates", "abc2").blockingAwait();
		client.addFavorite("prepend_duplicates", "abc3").blockingAwait();
		client.addFavorite("prepend_duplicates", "abc4").blockingAwait();
		client.addFavorite("prepend_duplicates", "abc5").blockingAwait();
		client.addFavorite("prepend_duplicates", "abc6").blockingAwait();
		client.addFavorite("prepend_duplicates", "abc4").blockingAwait();
		assertThat(client.getFavorites("prepend_duplicates").blockingGet()).containsExactly("abc4", "abc6", "abc5", "abc3", "abc2");

	}
	@Test
	public void testMaxEntriesWithOutDuplicatesDontAllowReorderPrepend() {
		client.addFavorite("widgets", "abc1").blockingAwait();
		client.addFavorite("widgets", "abc2").blockingAwait();
		client.addFavorite("widgets", "abc3").blockingAwait();
		client.addFavorite("widgets", "abc4").blockingAwait();
		client.addFavorite("widgets", "abc5").blockingAwait();
		client.addFavorite("widgets", "abc6").blockingAwait();
		client.addFavorite("widgets", "abc4").blockingAwait();
		assertThat(client.getFavorites("widgets").blockingGet()).containsExactly("abc6", "abc5", "abc4", "abc3");

	}
	@Test
	public void testMaxEntriesWithOutDuplicatesAllowReorderAppend() {
		client.addFavorite("ongoing_procedures_append_reorder", "abc1").blockingAwait();
		client.addFavorite("ongoing_procedures_append_reorder", "abc2").blockingAwait();
		client.addFavorite("ongoing_procedures_append_reorder", "abc3").blockingAwait();
		client.addFavorite("ongoing_procedures_append_reorder", "abc4").blockingAwait();
		client.addFavorite("ongoing_procedures_append_reorder", "abc5").blockingAwait();
		client.addFavorite("ongoing_procedures_append_reorder", "abc6").blockingAwait();
		client.addFavorite("ongoing_procedures_append_reorder", "abc4").blockingAwait();
		assertThat(client.getFavorites("ongoing_procedures_append_reorder").blockingGet()).containsExactly("abc3", "abc5", "abc6", "abc4");

	}
	@Test
	public void testMaxEntriesWithOutDuplicatesDontAllowReorderAppend() {
		client.addFavorite("ongoing_procedures_append", "abc1").blockingAwait();
		client.addFavorite("ongoing_procedures_append", "abc2").blockingAwait();
		client.addFavorite("ongoing_procedures_append", "abc3").blockingAwait();
		client.addFavorite("ongoing_procedures_append", "abc4").blockingAwait();
		client.addFavorite("ongoing_procedures_append", "abc5").blockingAwait();
		client.addFavorite("ongoing_procedures_append", "abc6").blockingAwait();
		client.addFavorite("ongoing_procedures_append", "abc4").blockingAwait();
		assertThat(client.getFavorites("ongoing_procedures_append").blockingGet()).containsExactly("abc3", "abc4", "abc5", "abc6");

	}
	@Test
	public void testUnlimitedEntries() {
		client.addFavorite("test_no_max", "abc1").blockingAwait();
		client.addFavorite("test_no_max", "abc2").blockingAwait();
		client.addFavorite("test_no_max", "abc3").blockingAwait();
		client.addFavorite("test_no_max", "abc4").blockingAwait();
		client.addFavorite("test_no_max", "abc5").blockingAwait();
		client.addFavorite("test_no_max", "abc6").blockingAwait();
		client.addFavorite("test_no_max", "abc7").blockingAwait();
		client.addFavorite("test_no_max", "abc8").blockingAwait();
		client.addFavorite("test_no_max", "abc9").blockingAwait();
		client.addFavorite("test_no_max", "abc10").blockingAwait();
		client.addFavorite("test_no_max", "abc11").blockingAwait();
		client.addFavorite("test_no_max", "abc12").blockingAwait();
		client.addFavorite("test_no_max", "abc13").blockingAwait();
		client.addFavorite("test_no_max", "abc14").blockingAwait();
		client.addFavorite("test_no_max", "abc15").blockingAwait();
		client.addFavorite("test_no_max", "abc16").blockingAwait();
		client.addFavorite("test_no_max", "abc17").blockingAwait();
		client.addFavorite("test_no_max", "abc18").blockingAwait();
		client.addFavorite("test_no_max", "abc19").blockingAwait();
		client.addFavorite("test_no_max", "abc20").blockingAwait();
		assertThat(client.getFavorites("test_no_max").blockingGet()).hasSize(20);

	}

	@Test
	public void testOverride() {
		client.addFavorite("ongoing_procedures", "abc1").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc2").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc3").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc4").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc5").blockingAwait();
		client.addFavorite("ongoing_procedures", "abc6").blockingAwait();
		client.overrideFavorite("ongoing_procedures", "widget1", "widget2").blockingAwait();
		assertThat(client.getFavorites("ongoing_procedures").blockingGet()).containsExactly("widget1", "widget2");

	}
	@Test
	public void testDelete() {
		client.addFavorite("infobox", "abc1").blockingAwait();
		client.addFavorite("infobox", "abc2").blockingAwait();
		client.addFavorite("infobox", "abc1").blockingAwait();
		client.addFavorite("infobox", "abc1").blockingAwait();
		client.addFavorite("infobox", "abc3").blockingAwait();
		client.addFavorite("infobox", "abc1").blockingAwait();
		client.addFavorite("infobox", "abc1").blockingAwait();
		client.deleteFavorite("infobox", "abc1").blockingAwait();
		assertThat(client.getFavorites("infobox").blockingGet()).containsExactly("abc3");

	}
	@Test
	public void testWrongRegexType() {
		assertStatusCode(401, client.addFavorite("infobox.2", "abc1"));


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
			if(e.getCause() instanceof MeshRestClientMessageException){
				assertEquals(code, ((MeshRestClientMessageException) e.getCause()).getStatusCode());
			} else {
				throw e;
			}
		}

	}

}
