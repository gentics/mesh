package com.gentics.mesh.dagger;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.graphdb.NoTx;

public class DaggerTest {

	private MeshComponent meshDagger;

	private UserVerticle userVerticle;

	@Before
	public void setup() {
		meshDagger = MeshCore.create();
		System.out.println(meshDagger.searchProvider().getClass().getName());
		System.out.println(meshDagger);
		System.out.println(meshDagger.hashCode());
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
