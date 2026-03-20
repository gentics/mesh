package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
public class SchemaVersionPurgeJobTest extends AbstractMeshTest {

}
