package com.gentics.mesh.cli;

import org.junit.Test;

import com.gentics.mesh.etc.DaggerMeshDagger;
import com.gentics.mesh.etc.MeshDagger;

public class DaggerTest {

	@Test
	public void test() {
		MeshDagger meshDagger = DaggerMeshDagger.builder().build();
		meshDagger.boot().groupRoot();
	}

}
