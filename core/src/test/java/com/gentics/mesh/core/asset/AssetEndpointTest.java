package com.gentics.mesh.core.asset;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class AssetEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	@Override
	public void testCreate() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testUpdateMultithreaded() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
