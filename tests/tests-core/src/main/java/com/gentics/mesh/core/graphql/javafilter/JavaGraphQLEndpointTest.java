package com.gentics.mesh.core.graphql.javafilter;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.graphql.GraphQLEndpointTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_JAVA_FILTER, resetBetweenTests = ResetTestDb.ON_HASH_CHANGE)
@NoConsistencyCheck
public class JavaGraphQLEndpointTest extends GraphQLEndpointTest {

	public static Stream<List<Object>> queries() {
		return Stream.<List<Object>>of(
				Arrays.asList("filtering/nodes", true, false, "draft"),
				Arrays.asList("filtering/nodes-en", true, false, "draft"),
				Arrays.asList("filtering/nodes-jp", true, false, "draft"),
				Arrays.asList("filtering/roles-java", true, false, "draft"),
				Arrays.asList("filtering/nodes-string-field-java", true, false, "draft"),
				Arrays.asList("filtering/nodes-number-field-java", true, false, "draft"),
				Arrays.asList("filtering/nodes-nodereferences-java", true, false, "draft")
			);
	}

	@Parameters(name = "query={0},version={3},apiVersion={5}")
	public static Collection<Object[]> paramData() {
		return Stream.of(GraphQLEndpointTest.queries(), JavaGraphQLEndpointTest.queries())
				.flatMap(Function.identity())
				.flatMap(testCase -> IntStream.rangeClosed(1, CURRENT_API_VERSION)
				.mapToObj(version -> {
					// Make sure all testData entries have six parts.
					Object[] array = testCase.toArray(new Object[6]);
					array[5] = "v" + version;
					return array;
				})).collect(Collectors.toList());
	}
	public JavaGraphQLEndpointTest(String queryName, boolean withMicroschema, boolean withBranchPathPrefix, String version, Consumer<JsonObject> assertion, String apiVersion) {
		super(queryName, withMicroschema, withBranchPathPrefix, version, assertion, apiVersion);
	}
}
