package com.gentics.mesh.plugin.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.docker.MeshDockerServer;

import io.reactivex.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class FavoritePluginIT {

	public static final File PLUGINS_DIR = new File("target", "it_" + System.currentTimeMillis());

	static {
		PLUGINS_DIR.deleteOnExit();
		PLUGINS_DIR.mkdirs();
	}

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	@ClassRule
	public static final MeshDockerServer meshServer = new MeshDockerServer("gentics/mesh:1.2.1", Vertx.vertx())
		.withFileSystemBind(PLUGINS_DIR.getAbsolutePath(), "/plugins")
		.waitForStartup();

	public static void copyPlugin() throws IOException {
		// 3. Copy pre-build plugin jar
		Path pluginJar = Files.list(Paths.get("target"))
			.filter(Files::isRegularFile)
			.filter(f -> !f.getFileName().toString().endsWith("sources.jar"))
			.filter(f -> !f.getFileName().toString().startsWith("original-"))
			.findFirst().orElse(null);
		if (pluginJar == null) {
			fail("Could not find plugin jar in target folder. Make sure you run mvn package before running integration tests");
		}
		FileUtils.copyFile(pluginJar.toFile(), new File(PLUGINS_DIR, "favorite-plugin.jar"));

		// Prepare config
		File favDir = new File(PLUGINS_DIR, "favorite");
		favDir.mkdirs();
		File favConfSrc = new File("src/test/resources/test.yml");
		File favConfDest = new File(favDir, "config.yml");
		FileUtils.copyFile(favConfSrc, favConfDest);

		// Copy plugin jar
		FileUtils.copyFile(pluginJar.toFile(), new File(PLUGINS_DIR, "favorite-plugin.jar"));

		// Deploy plugin
		PluginResponse response = meshServer.client().deployPlugin(new PluginDeploymentRequest().setPath("favorite-plugin.jar")).blockingGet();
		System.out.println(response.toJson());
	}

	private static FavoritePluginClient client;

	@AfterClass
	public static void cleanup() throws IOException {
		if (PLUGINS_DIR.exists()) {
			FileUtils.forceDelete(PLUGINS_DIR);
		}
	}

	@BeforeClass
	public static void setUp() throws Exception {
		meshServer.client().createProject(new ProjectCreateRequest().setName("test").setSchemaRef("folder")).blockingAwait();
		meshServer.client().createUser(new UserCreateRequest().setUsername("user1").setPassword("user1"));

		copyPlugin();

		client = new FavoritePluginClient(meshServer.client(), "test");
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
		assertThat(client.getFavorites("widgets").blockingGet()).containsExactly("abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8",
			"abc9", "abc10");

	}

	@Test
	public void testInfobox() {
		addFavorites("infobox", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10");
		assertThat(client.getFavorites("infobox").blockingGet()).containsExactly("abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8",
			"abc9", "abc10");

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
		addFavorites("test_no_max", "abc1", "abc2", "abc3", "abc4", "abc5", "abc6", "abc7", "abc8", "abc9", "abc10", "abc11", "abc12", "abc13",
			"abc14", "abc15", "abc16", "abc17", "abc18", "abc19", "abc20");
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
			meshServer.client().logout().blockingGet();
		} else {
			meshServer.client().setLogin(userName, userName).login().blockingGet();
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

	private void addFavorites(String type, String... favorites) {
		for (String favorite : favorites) {
			client.addFavorite(type, favorite).blockingAwait();
		}
	}

}
