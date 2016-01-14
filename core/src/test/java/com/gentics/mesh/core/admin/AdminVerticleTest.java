package com.gentics.mesh.core.admin;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AdminVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AdminVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testAdminStuff() {
		fail("Not yet implemented");
	}

}
