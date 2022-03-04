package com.gentics.mesh.distributed;

import static com.gentics.mesh.util.TokenUtil.randomToken;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.testcontainers.containers.BindMode;

import com.gentics.mesh.test.docker.MeshContainer;

/**
 * Make sure the node created an internal backup before performing full sync.
 * 
 * @author plyhun
 *
 */
public class ClusterTortureSecondaryCreatedBackupOnSync extends AbstractClusterTortureTest {
	@Test
	public void testSecondaryBackupCreated() throws Exception {
		torture((a, b, c) -> {
			MeshContainer serverB2 = prepareSlave("dockerCluster" + clusterPostFix, "nodeB2", randomToken(), true, true, 1);
			File backupFolder = new File("target/backup-" + serverB2.getDataPathPostfix());
			backupFolder.mkdirs();
			serverB2.overrideODBClusterBackupFolder(backupFolder.getAbsolutePath(), BindMode.READ_WRITE);
			serverB2.start();
			
			File plannedBackup = new File(backupFolder.getAbsolutePath() + "/databases/storage");
			assertTrue("Backup does not exist", plannedBackup.exists());
			assertTrue("Backup is not a directory", plannedBackup.isDirectory());
			assertTrue("Backup is empty", plannedBackup.list().length > 0);
		});
	}	
}
