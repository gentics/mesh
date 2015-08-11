package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG_FAMILY;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily>implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	@Override
	protected Class<? extends TagFamily> getPersistanceClass() {
		return TagFamilyImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_TAG_FAMILY;
	}

	@Override
	public TagFamily create(String name, User creator) {

		TagFamilyImpl tagFamily = getGraph().addFramedVertex(TagFamilyImpl.class);
		tagFamily.setName(name);
		addTagFamily(tagFamily);
		tagFamily.setCreator(creator);
		tagFamily.setEditor(creator);
		// TODO set creation and editing timestamps
		TagFamilyRoot root = BootstrapInitializer.getBoot().tagFamilyRoot();
		if (root != null && !root.equals(this)) {
			root.addTagFamily(tagFamily);
		}
		return tagFamily;
	}

	@Override
	public void removeTagFamily(TagFamily tagFamily) {
		removeItem(tagFamily);
	}

	@Override
	public void addTagFamily(TagFamily tagFamily) {
		addItem(tagFamily);
	}

	@Override
	public void delete() {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamilyRoot {" + getUuid() + "}");
		}
		for (TagFamily tagFamily : findAll()) {
			tagFamily.delete();
		}
		getElement().remove();
	}

}
