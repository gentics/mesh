package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class TagRootImpl extends AbstractRootVertex<Tag> implements TagRoot {

	public static void checkIndices(Database database) {
		database.addVertexType(TagRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_TAG);
	}

	private static final Logger log = LoggerFactory.getLogger(TagRootImpl.class);

	@Override
	public Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_TAG;
	}

	@Override
	public void addTag(Tag tag) {
		addItem(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		removeItem(tag);
	}

	@Override
	public Observable<Tag> findByName(String name) {
		return Observable.just(out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back()
				.nextOrDefaultExplicit(TagImpl.class, null));
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO add check to prevent deletion of MeshRoot.tagRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag root {" + getUuid() + "}");
		}
		for (Tag tag : findAll()) {
			tag.delete(batch);
		}
		getElement().remove();
	}

	@Override
	public Tag create(String name, Project project, TagFamily tagFamily, User creator) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);
		addTag(tag);

		// Add to global list of tags
		TagRoot globalTagRoot = BootstrapInitializer.getBoot().tagRoot();
		if (this != globalTagRoot) {
			globalTagRoot.addTag(tag);
		}

		// Add tag to project list of tags
		TagRoot projectTagRoot = project.getTagRoot();
		if (this != projectTagRoot) {
			projectTagRoot.addTag(tag);
		}

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);

		return tag;
	}

	@Override
	public Observable<Tag> create(InternalActionContext ac) {
		throw new NotImplementedException("The tag family is the root element thus should be used for creation of tags.");
	}


}
