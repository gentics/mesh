package com.gentics.mesh.cli;

import org.junit.Test;

import com.gentics.mesh.etc.DaggerMeshDagger;
import com.gentics.mesh.etc.MeshDagger;
import com.gentics.mesh.graphdb.NoTx;

public class DaggerTest {

	@Test
	public void testInMemory() {
		InMemoryMeshDagger meshDagger = DaggerInMemoryMeshDagger.builder().build();
		try (NoTx noTx = meshDagger.database().noTx()) {
			System.out.println("Found Groups: {" + meshDagger.boot().groupRoot().findAll().size() + "}");
		}
		System.out.println(meshDagger.userVerticle());
		System.out.println(meshDagger.config().database());
		System.out.println(meshDagger.userVerticle());
	}

	@Test
	public void test() {
		MeshDagger meshDagger = DaggerMeshDagger.builder().build();
		meshDagger.boot().groupRoot();
	}

}
