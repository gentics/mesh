package com.gentics.mesh.cli;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.SpringTestConfiguration;

public class DaggerTest {

	private InMemoryMeshDagger meshDagger;

	private UserVerticle userVerticle;

	@Before
	public void setup() {
		meshDagger = DaggerInMemoryMeshDagger.builder().build();
		this.userVerticle = meshDagger.userVerticle();
	}
	@Test
	public void testInMemory() {
		
		try (NoTx noTx = meshDagger.database().noTx()) {
			System.out.println("Found Groups: {" + meshDagger.boot().groupRoot().findAll().size() + "}");
		}
		System.out.println(meshDagger.userVerticle());
		//System.out.println(meshDagger.config().database());
		
		System.out.println(userVerticle);
		//System.out.println(userVerticle.db2);
	}

	@Test
	public void test() {
//		MeshDagger meshDagger = DaggerMeshDagger.builder().build();
//		meshDagger.boot().groupRoot();
	}

}
