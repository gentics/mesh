package com.gentics.mesh.core.monitoring;

import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true, clusterMode = true)
public class WritableServerEndpointTest extends AbstractMeshTest {

    @Test
    public void testReturns503WhenQuorumNotReached() {
        client().setLogin("admin", "admin");
        client().login().blockingGet();
        // updating cluster config requires admin permission
        ClusterConfigResponse clusterConfigResponse = call(() -> client().loadClusterConfig());
        ClusterConfigResponse response = call(() -> client().updateClusterConfig(buildClusterConfigRequest(2)));
        Assertions.assertThat(response.getWriteQuorum()).isEqualTo("2");
        call(() -> monClient().writable(), SERVICE_UNAVAILABLE, "error_internal");
        // clean up
        call(() -> client().updateClusterConfig(buildClusterConfigRequest(1)));
    }

    private ClusterConfigRequest buildClusterConfigRequest(int writeQuorum) {
        ClusterConfigRequest request = new ClusterConfigRequest();
        request.setWriteQuorum(String.valueOf(writeQuorum));

        return request;
    }
}
