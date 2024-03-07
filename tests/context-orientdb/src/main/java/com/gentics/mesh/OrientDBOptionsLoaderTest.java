package com.gentics.mesh;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;

import com.gentics.mesh.etc.config.DiskQuotaOptions;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;

public class OrientDBOptionsLoaderTest extends OptionsLoaderTest<GraphDBMeshOptions> {

	@Override
	public GraphDBMeshOptions getOptions() {
		return new GraphDBMeshOptions();
	}

	@Test(expected = NullPointerException.class)
	public void testInvalidOptions4() {
		GraphDBMeshOptions options = getOptions();
		options.getStorageOptions().setDirectory(null);
		options.getStorageOptions().setStartServer(true);
		options.validate();
	}

	@Test
	public void testInvalidOptions5() {
		GraphDBMeshOptions options = getOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		options.getStorageOptions().setDirectory(null);
		options.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDiskQuotaOptions1() {
		GraphDBMeshOptions options = getOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		DiskQuotaOptions diskQuotaOptions = options.getStorageOptions().getDiskQuotaOptions();
		diskQuotaOptions.setReadOnlyThreshold("Not a number");
		options.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDiskQuotaOptions2() {
		GraphDBMeshOptions options = getOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		DiskQuotaOptions diskQuotaOptions = options.getStorageOptions().getDiskQuotaOptions();
		diskQuotaOptions.setWarnThreshold("Not a number");
		options.validate();
	}

	@Test
	public void testLegalDiskQuotaOptions() {
		GraphDBMeshOptions options = getOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		DiskQuotaOptions diskQuotaOptions = options.getStorageOptions().getDiskQuotaOptions();
		diskQuotaOptions.setReadOnlyThreshold("10M");
		diskQuotaOptions.setWarnThreshold("10%");

		// mock storage dir with 200M total space
		File storageDir = mock(File.class);
		when(storageDir.getTotalSpace()).thenReturn(200L * 1024 * 1024);

		// absolute read-only threshold must be 10M (configured)
		assertEquals(10 * 1024 * 1024, diskQuotaOptions.getAbsoluteReadOnlyThreshold(storageDir));
		// absolute warn threshold must be 20M (10% of 200M)
		assertEquals(20 * 1024 * 1024, diskQuotaOptions.getAbsoluteWarnThreshold(storageDir));
		options.validate();
	}
}
