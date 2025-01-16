package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ClientHandler;

@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = false)
@RunWith(Parameterized.class)
public class GraphQLBreadcrumbQueryCountingTest extends AbstractMeshTest {

	private static final String CHILDREN_Q 
			= "children(nativeFilter: ONLY, filter: {and: [{schema: {is: folder}}, {not: {fields: {folder: {slug: {isNull: true}}}}}]}) {\n"
			+ "   elements {\n"
			+ "       uuid\n"
			+ "       ...navigation\n"
			+ "       %s\n"
			+ "   }\n"
			+ "}\n";
	private static final String ROOT_Q 
			= "{"
			+ " rootNode {\n"
			+ "  uuid\n"
			+ "  %s\n"
			+ " }\n"
			+ "}"
			+ "fragment navigation on Node {\n"
			+ "  path\n"
			+ "  ... on folder {\n"
			+ "    fields {\n"
			+ "      slug\n"
			+ "      reference {"
			+ "         languages { language path }"
			+ "      }"
			+ "    }\n"
			+ "  }\n"
			+ "}";

	/**
	 * Number of levels
	 */
	@Parameter(0)
	public int number = 1;
	/**
	 * Number of nodes per level
	 */
	public final static int NUM_NODES = 3;

	/**
	 * Max inlay levels
	 */
	public final static int MAX_LEVELS = 6;

	/**
	 * Set this flag to true for debugging. The test will then output changes in the number of executed queries
	 */
	public static boolean DEBUG = false;

	@Parameters(name = "{index}: number = {0}")
	public static Collection<Object[]> parameters() throws Exception {
		return IntStream.range(1, MAX_LEVELS).mapToObj(i -> new Object[] { i }).collect(Collectors.toList());
	}

	@Before
	public void setup() {
		if (LoggerFactory.getLogger(AbstractMeshTest.class).isDebugEnabled()) {
			DEBUG = true;
		}
		if (getTestContext().needsSetup()) {
			SchemaUpdateRequest schemaRequest = call(() -> client().findSchemaByUuid(tx(() -> schemaContainer("folder").getUuid()))).toUpdateRequest();
			schemaRequest.addField(new NodeFieldSchemaImpl().setAllowedSchemas("folder").setName("reference"));
			call(() -> client().updateSchema(tx(() -> schemaContainer("folder").getUuid()), schemaRequest));

			String parentNodeUuid = tx(() -> folder("2015").getUuid());

			createNodeLevel(parentNodeUuid, MAX_LEVELS);
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	protected void createNodeLevel(String parentNodeUuid, int numLevels) {
		if (DEBUG) {
			System.out.println("Creating " + numLevels + " level of " + parentNodeUuid);
		}
		String projectName = tx(() -> projectName());
		String prevNodeUuid = null;
		for (int i = 0; i < NUM_NODES; i++) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
			nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.randomAlphabetic(5)));
			if (prevNodeUuid != null) {
				nodeCreateRequest.getFields().put("reference", new NodeFieldImpl().setUuid(prevNodeUuid));
			}
			NodeResponse node = call(() -> client().createNode(projectName, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
			prevNodeUuid = node.getUuid();
			if (numLevels > 0) {
				createNodeLevel(node.getUuid(), numLevels-1);
			}
		}
	}

	@Test
	public void testBreadcrumbQuery() {
		makeAndRunQuery();
	}

	@Test
	public void testBreadcrumbQueryCache() {
		String root = makeAndRunQuery();
		for (int i = -1; i < number; i++) {
			doTest(() -> client().graphqlQuery(tx(() -> projectName()), root), 15 * NUM_NODES * number / 2);
		}
	}

	protected String makeAndRunQuery() {
		String content = "";
		for (int i = 0; i < number+1; i++) {
			content = String.format(CHILDREN_Q, content);
		}
		String root = String.format(ROOT_Q, content);
		if (DEBUG) {
			System.out.println(root);
		}
		GraphQLResponse result = doTest(() -> client().graphqlQuery(tx(() -> projectName()), root), 9 * NUM_NODES * number);
		if (DEBUG) {
			System.out.println(result.toJson(false));
		}
		if (result.getErrors() != null) {
			fail(result.getErrors().stream().map(GraphQLError::getMessage).collect(Collectors.joining("\r\n", "\r", "")));
		}
		return root;
	}

	/**
	 * Execute the given handler and assert that no more than numQueries were executed by Hibernate.
	 * @param <T> type of response of handler
	 * @param handler test handler
	 */
	protected <T> T doTest(ClientHandler<T> handler, int numQueries) {
		long periodicId = 0;
		long millis = 0;
		try (QueryCounter queryCounter = QueryCounter.Builder.get().clear()
				.assertNotMoreThan(numQueries).build()) {
			if (DEBUG) {
				AtomicLong currentCount = new AtomicLong();
				periodicId = vertx().setPeriodic(1000, id -> {
					long newCount = queryCounter.getCountSinceStart();
					if (currentCount.get() != newCount) {
						currentCount.set(newCount);
						System.out.println(String.format(" +++ Current diff: %d", currentCount.get()));
						System.out.println(queryCounter.getQueries());
						System.out.println(String.format(" +++ Current diff: %d", currentCount.get()));
					}
				});
				millis = System.currentTimeMillis();
			}
			T result = nonAdminCall(handler);
			if (DEBUG) {
				System.out.println(queryCounter.getQueries());
				System.out.println(" --- " + number + " = " + (System.currentTimeMillis() - millis) + "ms");
				vertx().cancelTimer(periodicId);
			}
			return result;
		}
	}
}
