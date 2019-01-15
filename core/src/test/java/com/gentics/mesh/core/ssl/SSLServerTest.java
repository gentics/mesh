package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.SSLUtil;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true, ssl = true)
public class SSLServerTest extends AbstractMeshTest {

	static {
		// Add the snakeoil cert to the keystore
		SSLUtil.updateKeyStore("/ssl/cert.pem");
	}

	@Test
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		call(() -> client().findUserByUuid(uuid));
	}

}
