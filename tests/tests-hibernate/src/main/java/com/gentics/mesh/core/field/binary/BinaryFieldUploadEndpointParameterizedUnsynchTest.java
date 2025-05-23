package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.field.binary.AbstractBinaryFieldUploadEndpointParameterizedTest;
import com.gentics.mesh.test.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class BinaryFieldUploadEndpointParameterizedUnsynchTest extends AbstractBinaryFieldUploadEndpointParameterizedTest {

	@Override
	public boolean isSyncWrites() {
		return false;
	}
}
