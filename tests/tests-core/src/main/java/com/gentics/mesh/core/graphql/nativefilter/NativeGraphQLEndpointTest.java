package com.gentics.mesh.core.graphql.nativefilter;

import static com.gentics.mesh.MeshVersions.CURRENT_API_VERSION;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.graphql.GraphQLEndpointTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.NativeGraphQLFilterTests;

import io.vertx.core.json.JsonObject;

@Category({ NativeGraphQLFilterTests.class })
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, optionChanger = MeshCoreOptionChanger.GRAPHQL_FORCE_NATIVE_FILTER)
public class NativeGraphQLEndpointTest extends GraphQLEndpointTest {

	public NativeGraphQLEndpointTest(String queryName, boolean withMicroschema, boolean withBranchPathPrefix, String version, Consumer<JsonObject> assertion, String apiVersion) {
		super(queryName, withMicroschema, withBranchPathPrefix, version, assertion, apiVersion);
	}

	public static Stream<List<Object>> queries() {
		return Stream.<List<Object>>of(
				Arrays.asList("filtering/nodes-sorted", true, false, "draft"),
				Arrays.asList("filtering/nodes-nodereferences-native", true, false, "draft"),
				Arrays.asList("filtering/nodes-sorted-by-schema", true, false, "draft"),
				Arrays.asList("filtering/children-sorted-by-schema", true, false, "draft")
			);
	}

	@Parameters(name = "query={0},version={3},apiVersion={5}")
	public static Collection<Object[]> paramData() {
		return Stream.of(GraphQLEndpointTest.queries(), NativeGraphQLEndpointTest.queries())
		.flatMap(Function.identity())
		.flatMap(testCase -> IntStream.rangeClosed(1, CURRENT_API_VERSION).mapToObj(version -> {
			// Make sure all testData entries have six parts.
			Object[] array = testCase.toArray(new Object[6]);
			array[5] = "v" + version;
			return array;
		})).collect(Collectors.toList());
	}
}
