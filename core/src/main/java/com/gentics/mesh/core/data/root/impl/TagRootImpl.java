package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see TagRoot
 */
public class TagRootImpl extends AbstractRootVertex<Tag> implements TagRoot {

	/**
	 * Initialise the indices and type.
	 * 
	 * @param database
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagRootImpl.class, MeshVertexImpl.class);
		//TODO why was the branch key omitted? TagEdgeImpl.BRANCH_UUID_KEY
		index.createIndex(edgeIndex(HAS_TAG));
		index.createIndex(edgeIndex(HAS_TAG).withInOut().withOut());
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
	public Tag findByName(String name) {
		return out(getRootLabel()).mark().has(TagImpl.TAG_VALUE_KEY, name).back().nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public void delete(BulkActionContext bac) {
		// TODO add check to prevent deletion of MeshRoot.tagRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag root {" + getUuid() + "}");
		}
		// Delete all the tags of the tag root
		for (Tag tag : findAll()) {
			tag.delete(bac);
		}
		// Now delete the tag root element
		getElement().remove();
		bac.process();
	}

	@Override
	public Tag create(String name, Project project, TagFamily tagFamily, User creator) {
		Tag tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);
		addTag(tag);

		// Add to global list of tags
		TagRoot globalTagRoot = MeshInternal.get().boot().tagRoot();
		if (this != globalTagRoot) {
			globalTagRoot.addTag(tag);
		}

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);
		return tag;
	}

	@Override
	public Tag create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new NotImplementedException("The tag family is the root element thus should be used for creation of tags.");
	}

}
