package com.gentics.mesh.core.monitoring;

import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.MeshRestHttpClientImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true, clusterMode = true)
public class WritableServerEndpointTest extends AbstractMeshTest {

    // this test make sense only in an orient db context
    @Test
    public void testReturns503WhenQuorumNotReached() {
        MeshRestClient client = client();
        if (client instanceof MeshRestHttpClientImpl) {
            MeshRestHttpClientImpl clientImpl = (MeshRestHttpClientImpl) client;
            clientImpl.setLogin("admin", "admin");
            clientImpl.login().blockingGet();
            // updating cluster config requires admin permission
            ClusterConfigResponse response = call(() -> clientImpl.updateClusterConfig(buildClusterConfigRequest(2)));
            Assertions.assertThat(response.getWriteQuorum()).isEqualTo("2");
            call(() -> monClient().writable(), SERVICE_UNAVAILABLE, "error_internal");
        }

    }

    @After
    public void setWriteQuorumToOne() {
        MeshRestClient client = client();
        if (client instanceof MeshRestHttpClientImpl) {
            // needed because other tests which write to the db might fail because of quorum not reached
            MeshRestHttpClientImpl clientImpl = (MeshRestHttpClientImpl) client;
            call(() -> clientImpl.updateClusterConfig(buildClusterConfigRequest(1)));
        }
    }

    private ClusterConfigRequest buildClusterConfigRequest(int writeQuorum) {
        ClusterConfigRequest request = new ClusterConfigRequest();
        request.setWriteQuorum(String.valueOf(writeQuorum));

        return request;
    }
}
