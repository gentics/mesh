package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = FULL, startServer = true)
public class DebugInfoTest extends AbstractMeshTest {
	@Test
	public void testAsAnonymous() {
		call(() -> client().debugInfo(), HttpResponseStatus.FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testDefaultContents() throws Exception {
		DebugInfo info = getDebugInfo();
		Stream.of(
			"log.txt",
			"config/mesh.yml",
			"config/hazelcast.xml",
			"config/orientdb-server-config.xml",
			"activeConfig.json",
			"clusterStatus.json",
			"searchStatus.json",
			"binaryDiskUsage.json",
			"threaddump.txt",
			"versions.json",
			"plugins.json",
			"systemInfo.json",
			"entities/projects.json",
			"entities/branches/dummy.json",
			"entities/schemas.json",
			"entities/microschemas.json",
			"entities/jobs.json",
			"migrationStatus/dummy/dummy/schemas.json",
			"migrationStatus/dummy/dummy/microschemas.json"
		).forEach(info::assertExistence);

		ProjectResponse[] projects = info.getPojo("entities/projects.json", ProjectResponse[].class);

		assertThat(projects.length).isEqualTo(1);
		assertThat(projects[0].getName()).isEqualTo("dummy");
	}

	@Test
	public void testInclusions() throws IOException {
		DebugInfo info = getDebugInfo("consistencyCheck", "-log");
		Stream.of(
			"consistencyCheck.json",
			"config/mesh.yml",
			"config/hazelcast.xml",
			"config/orientdb-server-config.xml",
			"activeConfig.json",
			"clusterStatus.json",
			"searchStatus.json",
			"binaryDiskUsage.json",
			"threaddump.txt",
			"versions.json",
			"plugins.json",
			"systemInfo.json",
			"entities/projects.json",
			"entities/branches/dummy.json",
			"entities/schemas.json",
			"entities/microschemas.json",
			"entities/jobs.json",
			"migrationStatus/dummy/dummy/schemas.json",
			"migrationStatus/dummy/dummy/microschemas.json"
		).forEach(info::assertExistence);
	}

	private DebugInfo getDebugInfo(String... includes) throws IOException {
		grantAdminRole();
		MeshBinaryResponse response = client().debugInfo(includes).blockingGet();
		return new DebugInfo(response);
	}

	private class DebugInfo {
		private Map<String, byte[]> files;

		public DebugInfo(MeshBinaryResponse response) throws IOException {
			files = new HashMap<>();
			ZipInputStream stream = new ZipInputStream(response.getStream());
			while (true) {
				ZipEntry entry = stream.getNextEntry();
				if (entry == null) {
					break;
				}
				String name = entry.getName();
				byte[] bytes = IOUtils.toByteArray(stream);
				files.put(name, bytes);
			}
		}

		public <T> T getPojo(String path, Class<T> clazz) {
			return JsonUtil.readValue(new String(files.get(path), StandardCharsets.UTF_8), clazz);
		}

		public void assertExistence(String path) {
			assertThat(files).containsKey(path);
		}
	}
}
