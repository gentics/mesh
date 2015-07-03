package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.test.AbstractDBTest;

public class AtomicTagTest extends AbstractDBTest {

	@Test
	public void testTagCreation() {
		MeshRoot meshRoot = boot.createMeshRoot();
		LanguageRoot languageRoot = meshRoot.createLanguageRoot();
		assertNotNull(languageRoot);
		Language language = languageRoot.create("Deutsch", "de");
		Language english = languageRoot.create("English", "en");

		ProjectRoot projectRoot = meshRoot.createProjectRoot();
		Project project = projectRoot.create("dummy");
		TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
		TagFamily tagFamily = tagFamilyRoot.create("basic");

		Tag tag = tagFamily.create("dummyName");
		String uuid = tag.getUuid();
		assertNotNull(tag);
		assertEquals("dummyName", tag.getName());
		tag.setName("renamed tag");
		assertEquals("renamed tag", tag.getName());

		Tag reloadedTag = boot.tagRoot().findByUUID(uuid);
		assertNotNull(reloadedTag);
		assertNotNull(reloadedTag.getFieldContainers());
		assertEquals(1, reloadedTag.getFieldContainers().size());
		assertEquals("renamed tag", reloadedTag.getName());
	}
}
