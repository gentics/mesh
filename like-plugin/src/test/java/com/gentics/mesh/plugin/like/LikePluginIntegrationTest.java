package com.gentics.mesh.plugin.like;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.like.model.LikeConfig;
import com.gentics.mesh.plugin.like.model.LikeResponse;
import com.gentics.mesh.plugin.like.model.PlusMinus;
import com.gentics.mesh.plugin.like.model.PlusMinusType;
import com.gentics.mesh.plugin.like.model.PostedLike;
import com.gentics.mesh.plugin.like.model.Rating;
import com.gentics.mesh.plugin.like.model.StatisticsResponse;
import com.gentics.mesh.plugin.like.model.Type;
import com.gentics.mesh.plugin.like.service.SchemaService;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;

import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Integration tests for the LikePlugin
 */
public class LikePluginIntegrationTest extends AbstractPluginLocalIntegrationTest {

	private final static String PROJECT_NAME = "testproject";

	@ClassRule
	public static final MeshLocalServer server = new HibernateMeshLocalServer()
		.waitForStartup();

	private static ProjectResponse project;

	private static SchemaResponse schema;

	private static Map<String, String> userTokens = new HashMap<>();

	/**
	 * Get the project specific url for the plugin with the given path appended
	 * 
	 * @param path
	 *            path
	 * @return complete url
	 */
	private static String projectUrl(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return String.format("http://%s:%d/api/v1/%s/plugins/%s%s", server.getHostname(), server.getPort(), PROJECT_NAME, "like", path);
	}

	@BeforeClass
	public static void setupOnce() throws InterruptedException, ExecutionException, TimeoutException, GenericRestException, IOException {
		ProjectCreateRequest createProject = new ProjectCreateRequest().setName(PROJECT_NAME).setSchemaRef("folder");
		project = call(() -> server.client().createProject(createProject));

		SchemaCreateRequest request = JsonUtil.readValue(
			IOUtils.toString(LikePluginIntegrationTest.class.getResource("testcontent_schema.json"), "UTF-8"),
			SchemaCreateRequest.class);
		schema = call(() -> server.client().createSchema(request));
		call(() -> server.client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		server.getMesh().deployPlugin(LikeTestPlugin.class, "like").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "like");

		for (int i = 1; i <= 100; i++) {
			UserCreateRequest createUser = new UserCreateRequest();
			createUser.setFirstname(String.format("User%dFirst", i));
			createUser.setLastname(String.format("User%dLast", i));
			createUser.setUsername(String.format("user%d", i));
			createUser.setPassword(String.format("user%d", i));
			UserResponse user = call(() -> server.client().createUser(createUser));
			UserAPITokenResponse token = call(() -> server.client().issueAPIToken(user.getUuid()));
			userTokens.put(user.getUuid(), token.getToken());
		}
	}

	/**
	 * Clean data before test run
	 */
	@Before
	public void setup() {
		// delete all nodes of the specified schemas
		List<String> schemasToDelete = Arrays.asList(SchemaService.SECTION_SCHEMA_NAME, schema.getName());
		NodeListResponse response = call(() -> server.client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		for (NodeResponse node : response.getData()) {
			if (schemasToDelete.contains(node.getSchema().getName())) {
				call(() -> server.client().deleteNode(PROJECT_NAME, node.getUuid(), new DeleteParametersImpl().setRecursive(true)));
			}
		}
	}

	/**
	 * Test posting a single like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostLike() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postLike(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertNodeLikes(node.getUuid(), "en", userUuid);
	}

	/**
	 * Test posting like twice with the same user
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostTwice() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postLike(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		response = postLike(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertNodeLikes(node.getUuid(), "en", userUuid);
	}

	/**
	 * Test multiple users posting like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMultipleLikes() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		Random rand = new Random();
		Set<String> usersWithLikes = new HashSet<>();
		for (String userUuid : userTokens.keySet()) {
			if (rand.nextBoolean()) {
				postLike(node.getUuid(), "en", userUuid);
				usersWithLikes.add(userUuid);
			}
		}

		assertNodeLikes(node.getUuid(), "en", usersWithLikes.toArray(new String[usersWithLikes.size()]));
	}

	/**
	 * Test posting a like to content having likes inactive
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostInactive() throws IOException {
		NodeResponse node = createContent("With inactive likes", "en", false, null, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postLike(node.getUuid(), "en", userUuid);

		assertThat(response.code()).as("Post response").isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
	}

	/**
	 * Test posting a like to content having like configuration
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostNoConfig() throws IOException {
		NodeResponse node = createContent("With no likes config", "en", null, null, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postLike(node.getUuid(), "en", userUuid);

		assertThat(response.code()).as("Post response").isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
	}

	/**
	 * Test posting a like anonymously
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostAnonymous() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, null, 0);
		Response response = postLike(node.getUuid(), "en", null);

		assertThat(response.code()).as("Post response").isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
	}

	/**
	 * Test post a rating
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostRating() throws IOException {
		NodeResponse node = createContent("With rating", "en", true, Type.rating, 5);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postRating(node.getUuid(), "en", userUuid, 3);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertNodeLikes(node.getUuid(), "en", userUuid);
	}

	/**
	 * Test post an invalid rating
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostInvalidRating() throws IOException {
		NodeResponse node = createContent("With rating", "en", true, Type.rating, 5);
		String userUuid = userTokens.keySet().iterator().next();

		for (int rating : Arrays.asList(-3, 0, 7, 4711)) {
			Response response = postRating(node.getUuid(), "en", userUuid, rating);
			assertThat(response.code()).as(String.format("Post response for rating %d", rating)).isEqualTo(HttpResponseStatus.BAD_REQUEST.code());
		}

		Response response = postLike(node.getUuid(), "en", userUuid);
		assertThat(response.code()).as("Post response to posting like").isEqualTo(HttpResponseStatus.BAD_REQUEST.code());

		response = postPlusMinus(node.getUuid(), "en", userUuid, PlusMinusType.plus);
		assertThat(response.code()).as("Post response to posting plusminus").isEqualTo(HttpResponseStatus.BAD_REQUEST.code());
	}

	/**
	 * Post plusminus like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostPlusminus() throws IOException {
		NodeResponse node = createContent("With plusminus", "en", true, Type.plusminus, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postPlusMinus(node.getUuid(), "en", userUuid, PlusMinusType.plus);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertNodeLikes(node.getUuid(), "en", userUuid);
	}

	/**
	 * Test posting an invalid plusminus like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostInvalidPlusminus() throws IOException {
		NodeResponse node = createContent("With plusminus", "en", true, Type.plusminus, 0);
		String userUuid = userTokens.keySet().iterator().next();

		Response response = postLike(node.getUuid(), "en", userUuid);
		assertThat(response.code()).as("Post response to posting like").isEqualTo(HttpResponseStatus.BAD_REQUEST.code());

		response = postRating(node.getUuid(), "en", userUuid, 3);
		assertThat(response.code()).as("Post response to posting rating").isEqualTo(HttpResponseStatus.BAD_REQUEST.code());
	}

	/**
	 * Test getting likes for the anonymous user
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetLikeAnonymous() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		Response response = get(node.getUuid(), "en", null);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		LikeResponse like = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(like, new LikeResponse().setConfiguration(new LikeConfig().setActive(false)));
	}

	/**
	 * Test getting likes for a user who did not post
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetLikeNotLiked() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = get(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		LikeResponse like = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(like,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.like))
				.setStatistics(new StatisticsResponse()));
	}

	/**
	 * Test getting likes for a user who already liked
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetLiked() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();
		postLike(node.getUuid(), "en", userUuid);
		Response response = get(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertEquals("The content type did not match", APPLICATION_JSON, response.header(CONTENT_TYPE.toString()));

		LikeResponse like = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(like,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.like))
				.setPosted(new PostedLike().setLike(true))
				.setStatistics(new StatisticsResponse().setLikes(1)));
	}

	/**
	 * Test getting likes for a user who posted plusminus
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetPlusminus() throws IOException {
		NodeResponse node = createContent("With plusminus", "en", true, Type.plusminus, 0);
		String userUuid = userTokens.keySet().iterator().next();
		postPlusMinus(node.getUuid(), "en", userUuid, PlusMinusType.minus);
		Response response = get(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertEquals("The content type did not match", APPLICATION_JSON, response.header(CONTENT_TYPE.toString()));

		LikeResponse like = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(like,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.plusminus))
				.setPosted(new PostedLike().setPlusMinus(PlusMinusType.minus))
				.setStatistics(new StatisticsResponse().setLikes(1).setPlusMinus(createPlusMinusMap(PlusMinusType.minus))));
	}

	/**
	 * Test getting likes for a user who posted rating
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetRating() throws IOException {
		NodeResponse node = createContent("With rating", "en", true, Type.rating, 10);
		String userUuid = userTokens.keySet().iterator().next();
		postRating(node.getUuid(), "en", userUuid, 7);
		Response response = get(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertEquals("The content type did not match", APPLICATION_JSON, response.header(CONTENT_TYPE.toString()));

		LikeResponse like = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(like,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.rating).setMaxRating(10))
				.setPosted(new PostedLike().setRating(7))
				.setStatistics(new StatisticsResponse().setLikes(1).setRatings(createRatingsMap(7, 10))));
	}

	/**
	 * Test getting statistics without permission
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore("Statistic do not require special permissions")
	public void testStatisticsWithoutPermission() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		Response response = getStatistics(node.getUuid(), "en", false);
		assertThat(response.code()).as("Statistics response").isEqualTo(HttpResponseStatus.FORBIDDEN.code());
	}

	/**
	 * Test getting like statistics
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetLikeStatistics() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		Random rand = new Random();
		Set<String> usersWithLikes = new HashSet<>();
		for (String userUuid : userTokens.keySet()) {
			if (rand.nextBoolean()) {
				postLike(node.getUuid(), "en", userUuid);
				usersWithLikes.add(userUuid);
			}
		}

		Response response = getStatistics(node.getUuid(), "en", true);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		StatisticsResponse statistics = JsonUtil.readValue(response.body().string(), StatisticsResponse.class);
		assertStatistics(statistics, new StatisticsResponse().setLikes(usersWithLikes.size()));
	}

	/**
	 * Test getting plusminus statistics
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetPlusminusStatistics() throws IOException {
		NodeResponse node = createContent("With plusminus", "en", true, Type.plusminus, 0);
		Random rand = new Random();
		Set<String> usersWithLikes = new HashSet<>();
		Map<PlusMinusType, Integer> map = new TreeMap<>();
		map.put(PlusMinusType.plus, 0);
		map.put(PlusMinusType.minus, 0);
		for (String userUuid : userTokens.keySet()) {
			if (rand.nextBoolean()) {
				PlusMinusType value = rand.nextBoolean() ? PlusMinusType.plus : PlusMinusType.minus;
				postPlusMinus(node.getUuid(), "en", userUuid, value);
				usersWithLikes.add(userUuid);
				map.put(value, map.get(value) + 1);
			}
		}

		Response response = getStatistics(node.getUuid(), "en", true);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		StatisticsResponse statistics = JsonUtil.readValue(response.body().string(), StatisticsResponse.class);
		assertStatistics(statistics, new StatisticsResponse().setLikes(usersWithLikes.size()).setPlusMinus(map));
	}

	/**
	 * Test getting ratings statistics
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetRatingsStatistics() throws IOException {
		int maxRating = 10;
		NodeResponse node = createContent("With rating", "en", true, Type.rating, maxRating);
		Random rand = new Random();
		Set<String> usersWithLikes = new HashSet<>();
		Map<Integer, Integer> map = new TreeMap<>();
		for (int i = 1; i <= maxRating; i++) {
			map.put(i, 0);
		}
		for (String userUuid : userTokens.keySet()) {
			if (rand.nextBoolean()) {
				int value = rand.nextInt(maxRating) + 1;
				postRating(node.getUuid(), "en", userUuid, value);
				usersWithLikes.add(userUuid);
				map.put(value, map.get(value) + 1);
			}
		}

		Response response = getStatistics(node.getUuid(), "en", true);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		StatisticsResponse statistics = JsonUtil.readValue(response.body().string(), StatisticsResponse.class);
		assertStatistics(statistics, new StatisticsResponse().setLikes(usersWithLikes.size()).setRatings(map));
	}

	/**
	 * Test language specific settings
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLanguages() throws IOException {
		NodeResponse en = createContent("With likes in english", "en", true, Type.like, 0);
		NodeResponse de = translate(en, "Mit rating auf deutsch", "de", true, Type.rating, 10);

		assertThat(en.getUuid()).isEqualTo(de.getUuid());

		String userUuid = userTokens.keySet().iterator().next();
		Response response = postLike(en.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		response = get(en.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		LikeResponse enLike = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(enLike,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.like))
				.setPosted(new PostedLike().setLike(true))
				.setStatistics(new StatisticsResponse().setLikes(1)));

		response = get(de.getUuid(), "de", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		LikeResponse deLike = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(deLike,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.rating).setMaxRating(10))
				.setStatistics(new StatisticsResponse().setRatings(createRatingsMap(0, 10))));

		response = postRating(de.getUuid(), "de", userUuid, 3);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		response = get(de.getUuid(), "de", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		deLike = JsonUtil.readValue(response.body().string(), LikeResponse.class);
		assertLikeResponse(deLike,
			new LikeResponse()
				.setConfiguration(new LikeConfig().setActive(true).setType(Type.rating).setMaxRating(10))
				.setPosted(new PostedLike().setRating(3))
				.setStatistics(new StatisticsResponse().setLikes(1).setRatings(createRatingsMap(3, 10))));
	}

	/**
	 * Test deleting a like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteLike() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postLike(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertNodeLikes(node.getUuid(), "en", userUuid);

		response = delete(node.getUuid(), "en", userUuid);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertNodeLikes(node.getUuid(), "en");
	}

	/**
	 * Test deleting a like before liking
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteNonExistentLike() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();

		Response response = delete(node.getUuid(), "en", userUuid);
		assertThat(response.code()).as("Post response").isEqualTo(HttpResponseStatus.NOT_FOUND.code());
	}

	/**
	 * Test deleting a like, when likes where inactivated
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteInactive() throws IOException {
		NodeResponse node = createContent("With active likes", "en", true, Type.like, 0);
		String userUuid = userTokens.keySet().iterator().next();
		postLike(node.getUuid(), "en", userUuid);
		assertNodeLikes(node.getUuid(), "en", userUuid);

		LikeConfig config = JsonUtil.readValue(node.getFields().getStringField("likes").getString(), LikeConfig.class);
		config.setActive(false);
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setVersion(node.getVersion());
		update.setLanguage(node.getLanguage());
		update.getFields().put("likes", new StringFieldImpl().setString(JsonUtil.toJson(config)));
		call(() -> server.client().updateNode(PROJECT_NAME, node.getUuid(), update));

		Response response = delete(node.getUuid(), "en", userUuid);
		assertThat(response.code()).as("Post response").isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
		assertNodeLikes(node.getUuid(), "en", userUuid);
	}

	/**
	 * Test updating a plusminus like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUpdatePlusminus() throws IOException {
		NodeResponse node = createContent("With plusminus", "en", true, Type.plusminus, 0);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postPlusMinus(node.getUuid(), "en", userUuid, PlusMinusType.plus);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeResponse like = loadLike(node.getUuid(), "en", userUuid);
		assertThat(like.getFields().getBooleanField("plusminus").getValue()).as("plusminus value").isTrue();

		response = postPlusMinus(node.getUuid(), "en", userUuid, PlusMinusType.minus);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		like = loadLike(node.getUuid(), "en", userUuid);
		assertThat(like.getFields().getBooleanField("plusminus").getValue()).as("plusminus value").isFalse();
	}

	/**
	 * Test updating a rating like
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUpdateRating() throws IOException {
		NodeResponse node = createContent("With rating", "en", true, Type.rating, 10);
		String userUuid = userTokens.keySet().iterator().next();
		Response response = postRating(node.getUuid(), "en", userUuid, 5);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeResponse like = loadLike(node.getUuid(), "en", userUuid);
		assertThat(like.getFields().getNumberField("rating").getNumber()).as("ratung value").isEqualTo(5);

		response = postRating(node.getUuid(), "en", userUuid, 10);
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		like = loadLike(node.getUuid(), "en", userUuid);
		assertThat(like.getFields().getNumberField("rating").getNumber()).as("ratung value").isEqualTo(10);
	}

	@Test
	public void testTemplates() throws IOException {
		Request request = new Request.Builder().url(projectUrl("/templates")).get().build();
		Response response = client().newCall(request).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		System.out.println(response.body().string());
	}

	private OkHttpClient client() {
		Builder builder = new OkHttpClient.Builder();
		builder.connectTimeout(1, TimeUnit.HOURS).readTimeout(1, TimeUnit.HOURS).writeTimeout(1, TimeUnit.HOURS);
		return builder.build();
	}

	/**
	 * Create a content
	 * 
	 * @param name
	 *            content name
	 * @param lang
	 *            language
	 * @param active
	 *            true for active likes, false for config with likes inactive, null for no config
	 * @param type
	 *            like type
	 * @param maxRating
	 *            maximum rating
	 * @return node response
	 */
	private NodeResponse createContent(String name, String lang, Boolean active, Type type, int maxRating) {
		NodeCreateRequest create = new NodeCreateRequest();
		create.setSchemaName(schema.getName());
		create.setParentNodeUuid(project.getRootNode().getUuid());
		create.setLanguage(lang);
		create.getFields().put("name", new StringFieldImpl().setString(name));
		if (active != null) {
			LikeConfig config = new LikeConfig();
			config.setActive(active);
			if (type != null) {
				config.setType(type);
				config.setMaxRating(maxRating);
			}
			create.getFields().put("likes", new StringFieldImpl().setString(JsonUtil.toJson(config)));
		}
		return call(() -> server.client().createNode(PROJECT_NAME, create));
	}

	/**
	 * Translate the node into another language
	 * 
	 * @param node
	 *            node to translate
	 * @param name
	 *            name
	 * @param lang
	 *            language
	 * @param active
	 *            true for active likes, false for config with likes inactive, null for no config
	 * @param type
	 *            like type
	 * @param maxRating
	 *            maximum rating
	 * @return node response
	 */
	private NodeResponse translate(NodeResponse node, String name, String lang, Boolean active, Type type, int maxRating) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(lang);
		update.getFields().put("name", new StringFieldImpl().setString(name));
		if (active != null) {
			LikeConfig config = new LikeConfig();
			config.setActive(active);
			if (type != null) {
				config.setType(type);
				config.setMaxRating(maxRating);
			}
			update.getFields().put("likes", new StringFieldImpl().setString(JsonUtil.toJson(config)));
		}
		return call(() -> server.client().updateNode(PROJECT_NAME, node.getUuid(), update));
	}

	/**
	 * Post a like for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID (null for anonymous)
	 * @return response
	 * @throws IOException
	 */
	private Response postLike(String nodeUuid, String lang, String userUuid) throws IOException {
		return post(nodeUuid, lang, userUuid, "");
	}

	/**
	 * Post a rating for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID (null for anonymous)
	 * @param rating
	 *            rating
	 * @return response
	 * @throws IOException
	 */
	private Response postRating(String nodeUuid, String lang, String userUuid, int rating) throws IOException {
		Rating body = new Rating();
		body.setRating(rating);
		return post(nodeUuid, lang, userUuid, JsonUtil.toJson(body));
	}

	/**
	 * Post a plusminus for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID (null for anonymous)
	 * @param rating
	 *            rating
	 * @return response
	 * @throws IOException
	 */
	private Response postPlusMinus(String nodeUuid, String lang, String userUuid, PlusMinusType rating) throws IOException {
		PlusMinus body = new PlusMinus();
		body.setRating(rating);
		return post(nodeUuid, lang, userUuid, JsonUtil.toJson(body));
	}

	/**
	 * Post a like for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID (null for anonymous)
	 * @param body
	 *            post body
	 * @return response
	 * @throws IOException
	 */
	private Response post(String nodeUuid, String lang, String userUuid, String body) throws IOException {
		Request.Builder builder = new Request.Builder().url(projectUrl("/likes/" + nodeUuid + "/" + lang));
		if (userUuid != null) {
			builder.header("Authorization", String.format("Bearer %s", userTokens.get(userUuid)));
		}
		Request createRequest = builder
			.post(RequestBody.create(MediaType.parse("application/json"), body))
			.build();
		return client().newCall(createRequest).execute();
	}

	/**
	 * Get the like for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID (null for anonymous)
	 * @return response
	 * @throws IOException
	 */
	private Response get(String nodeUuid, String lang, String userUuid) throws IOException {
		Request.Builder builder = new Request.Builder().url(projectUrl("/likes/" + nodeUuid + "/" + lang));
		if (userUuid != null) {
			builder.header("Authorization", String.format("Bearer %s", userTokens.get(userUuid)));
		}
		Request getRequest = builder.get().build();
		return client().newCall(getRequest).execute();
	}

	/**
	 * Delete the like for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID (null for anonymous)
	 * @return response
	 * @throws IOException
	 */
	private Response delete(String nodeUuid, String lang, String userUuid) throws IOException {
		Request.Builder builder = new Request.Builder().url(projectUrl("/likes/" + nodeUuid + "/" + lang));
		if (userUuid != null) {
			builder.header("Authorization", String.format("Bearer %s", userTokens.get(userUuid)));
		}
		Request deleteRequest = builder.delete().build();
		return client().newCall(deleteRequest).execute();
	}

	/**
	 * Get statistics
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param admin
	 *            true to make the request as admin
	 * @return response
	 * @throws IOException
	 */
	private Response getStatistics(String nodeUuid, String lang, boolean admin) throws IOException {
		Request.Builder builder = new Request.Builder().url(projectUrl("/likes/" + nodeUuid + "/" + lang + "/statistics"));
		if (admin) {
			builder.header("Authorization", String.format("Bearer %s", server.client().getAuthentication().getToken()));
		}
		Request getRequest = builder.get().build();
		return client().newCall(getRequest).execute();
	}

	/**
	 * Load the section node for the node
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @return section node
	 */
	private NodeResponse loadSection(String nodeUuid, String lang) {
		return call(() -> server.client().webroot(PROJECT_NAME, new String[] { LikePlugin.NAME, nodeUuid + "_" + lang })).getNodeResponse();
	}

	/**
	 * Load the like node for the node and user
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuid
	 *            user UUID
	 * @return like node
	 */
	private NodeResponse loadLike(String nodeUuid, String lang, String userUuid) {
		return call(() -> server.client().webroot(PROJECT_NAME, new String[] { LikePlugin.NAME, nodeUuid + "_" + lang, userUuid })).getNodeResponse();
	}

	/**
	 * Assert that exactly the given users liked the content
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param lang
	 *            language
	 * @param userUuids
	 *            array of user UUIDs
	 */
	private void assertNodeLikes(String nodeUuid, String lang, String... userUuids) {
		NodeResponse sectionNode = loadSection(nodeUuid, lang);
		List<NodeResponse> likeNodes = Arrays.asList(userUuids).stream().map(uuid -> loadLike(nodeUuid, lang, uuid)).collect(Collectors.toList());

		NodeListResponse sectionChildren = call(
			() -> server.client().findNodeChildren(PROJECT_NAME, sectionNode.getUuid(), new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(sectionChildren.getData()).as("Children of section node").usingElementComparatorOnFields("uuid").containsOnlyElementsOf(likeNodes);
	}

	/**
	 * Assert that the response is equal to the expected response
	 * 
	 * @param response
	 *            response
	 * @param expected
	 *            expected response
	 */
	private void assertLikeResponse(LikeResponse response, LikeResponse expected) {
		assertThat(JsonUtil.toJson(response)).as("Like response").isEqualTo(JsonUtil.toJson(expected));
	}

	/**
	 * Assert that the response is equal to the expected response
	 * 
	 * @param response
	 *            response
	 * @param expected
	 *            expected response
	 */
	private void assertStatistics(StatisticsResponse response, StatisticsResponse expected) {
		assertThat(JsonUtil.toJson(response)).as("Statistics response").isEqualTo(JsonUtil.toJson(expected));
	}

	/**
	 * Create a ratings map with one counted rating
	 * 
	 * @param rating
	 *            counted rating
	 * @param maxRating
	 *            max rating
	 * @return ratings map
	 */
	private Map<Integer, Integer> createRatingsMap(int rating, int maxRating) {
		Map<Integer, Integer> map = new TreeMap<>();
		for (int i = 1; i <= maxRating; i++) {
			map.put(i, i == rating ? 1 : 0);
		}
		return map;
	}

	/**
	 * Create a plusMinus map with one count
	 * 
	 * @param rating
	 *            rating
	 * @return plusMinus map
	 */
	private Map<PlusMinusType, Integer> createPlusMinusMap(PlusMinusType rating) {
		Map<PlusMinusType, Integer> map = new TreeMap<>();
		for (PlusMinusType key : PlusMinusType.values()) {
			map.put(key, key.equals(rating) ? 1 : 0);
		}
		return map;
	}
}
