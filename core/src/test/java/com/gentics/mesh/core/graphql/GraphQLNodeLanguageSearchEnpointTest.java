package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class GraphQLNodeLanguageSearchEnpointTest extends AbstractGraphQLSearchEndpointTest {
	@Parameterized.Parameters(name = "query={0}")
	public static List<String> paramData() {
		return Collections.singletonList("node-elasticsearch-language-query");
	}

	public GraphQLNodeLanguageSearchEnpointTest(String queryName) {
		super(queryName);
	}

	@Before
	public void createNodes() {
		String parentNodeUuid = tx(() -> folder("2015").getUuid());
		Arrays.asList("de", "en").forEach(lang -> {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
			nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("content"));
			nodeCreateRequest.setLanguage(lang);
			nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("test" + lang));
			nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages(lang)));
		});
	}
}
