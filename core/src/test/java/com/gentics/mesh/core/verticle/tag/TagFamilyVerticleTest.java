package com.gentics.mesh.core.verticle.tag;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.verticle.TagFamilyVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class TagFamilyVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagFamilyVerticle;
	}

	@Test
	public void testTagFamilyReadWithPerm() {

	}

	@Test
	public void testTagFamilyReadWithoutPerm() {

	}

	@Test
	public void testTagFamilyListing() {

	}

	@Test
	public void testTagFamilyCreateWithPerm() {

	}

	@Test
	public void testTagFamilyCreateWithoutPerm() {

	}

	@Test
	public void testTagFamilyCreateWithNoName() {

	}

	@Test
	public void testTagFamilyDeletionWithPerm() {

	}

	@Test
	public void testTagFamilyDeletionWithNoPerm() {

	}

	@Test
	public void testTagFamilyUpdateWithPerm() {

	}

	@Test
	public void testTagFamilyUpdateWithNoPerm() {

	}

}
