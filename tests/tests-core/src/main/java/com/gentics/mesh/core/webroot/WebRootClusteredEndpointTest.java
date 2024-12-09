package com.gentics.mesh.core.webroot;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.ClusterTests;
import com.gentics.mesh.test.context.AbstractMeshTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

@Category(ClusterTests.class)
@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = true, clusterInstances = 1, clusterName = "WebRootClusteredEndpointTest")
public class WebRootClusteredEndpointTest extends AbstractMeshTest {

    @Test
    public void testNodeCreateUpdate() {
        NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
        nodeCreateRequest.setLanguage("en");
        nodeCreateRequest.setSchemaName("content");
        nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
        nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
        nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

        NodeResponse response = call(() -> client().webrootCreate(PROJECT_NAME, "/new-page.html", nodeCreateRequest));
        assertEquals("0.1", response.getVersion());

        NodeCreateRequest nodeUpdateRequest = new NodeCreateRequest();
        nodeUpdateRequest.setLanguage("en");
        nodeUpdateRequest.setSchemaName("content");
        nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
        nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
        nodeUpdateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again 2!"));

        call(() -> client().webrootCreate(PROJECT_NAME, "/new-page.html", nodeUpdateRequest));
    }
}
