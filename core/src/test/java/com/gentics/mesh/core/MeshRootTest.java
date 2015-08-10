package com.gentics.mesh.core;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.test.AbstractDBTest;

public class MeshRootTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testResolvePath() throws InterruptedException {

		// Valid paths
		assertNotNull(resolve("projects"));
		assertNotNull(resolve("projects/" + project().getUuid()));
		assertNotNull(resolve("projects/" + project().getUuid() + "/schemas"));
		assertNotNull(resolve("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid()));
		assertNotNull(resolve("projects/" + project().getUuid() + "/tagFamilies"));
		assertNotNull(resolve("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid()));
		assertNotNull(resolve("projects/" + project().getUuid() + "/nodes"));
		assertNotNull(resolve("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid()));
		assertNotNull(resolve("projects/" + project().getUuid() + "/tags"));
		assertNotNull(resolve("projects/" + project().getUuid() + "/tags/" + tag("red").getUuid()));

		assertNotNull(resolve("users"));
		assertNotNull(resolve("users/" + user().getUuid()));

		assertNotNull(resolve("roles"));
		assertNotNull(resolve("roles/" + role().getUuid()));

		assertNotNull(resolve("groups"));
		assertNotNull(resolve("groups/" + group().getUuid()));

		assertNotNull(resolve("schemas"));
		assertNotNull(resolve("schemas/" + schemaContainer("folder").getUuid()));
		//assertNotNull(resolve("microschemas"));
		//assertNotNull(resolve("microschemas/" + mircoschemas("gallery").getUuid()));

		// Invalid paths
		assertNull(resolve("bogus"));
		assertNull(resolve("projects/"));
		assertNull(resolve("projects/bogus"));
		assertNull(resolve("projects/bogus/bogus"));
		assertNull(resolve("projects/" + project().getUuid() + "/bogus"));
		assertNull(resolve("projects/" + project().getUuid() + "/tagFamilies/"));
		assertNull(resolve("projects/" + project().getUuid() + "/tagFamilies/bogus"));
		assertNull(resolve("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/"));
		assertNull(resolve("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/bogus"));

		assertNull(resolve("projects/" + project().getUuid() + "/nodes/"));
		assertNull(resolve("projects/" + project().getUuid() + "/nodes/bogus"));
		assertNull(resolve("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid() + "/"));
		assertNull(resolve("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid() + "/bogus"));

		assertNull(resolve("projects/" + project().getUuid() + "/tags/"));
		assertNull(resolve("projects/" + project().getUuid() + "/tags/bogus"));
		assertNull(resolve("projects/" + project().getUuid() + "/tags/" + folder("2015").getUuid() + "/"));
		assertNull(resolve("projects/" + project().getUuid() + "/tags/" + folder("2015").getUuid() + "/bogus"));

		assertNull(resolve("projects/" + project().getUuid() + "/schemas/"));
		assertNull(resolve("projects/" + project().getUuid() + "/schemas/bogus"));
		assertNull(resolve("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid() + "/"));
		assertNull(resolve("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid() + "/bogus"));

		assertNull(resolve("users/"));
		assertNull(resolve("users/bogus"));

		assertNull(resolve("groups/"));
		assertNull(resolve("groups/bogus"));

		assertNull(resolve("roles/"));
		assertNull(resolve("roles/bogus"));

		assertNull(resolve("schemas/"));
		assertNull(resolve("schemas/bogus"));

	}

	private MeshVertex resolve(String pathToElement) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<MeshVertex> vertex = new AtomicReference<>();
		MeshRoot.getInstance().resolvePathToElement(pathToElement, rh -> {
			if(rh.failed()) {
				rh.cause().printStackTrace();
			}
			vertex.set(rh.result());
			latch.countDown();
		});
		latch.await();
		return vertex.get();
	}

}
