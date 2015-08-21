package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class TagRootImpl extends AbstractRootVertex<Tag>implements TagRoot {

	private static final Logger log = LoggerFactory.getLogger(TagRootImpl.class);

	@Override
	protected Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	protected String getRootLabel() {
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
		return out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back()
				.nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public void delete() {
		//TODO add check to prevent deletion of MeshRoot.tagRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag root {" + getUuid() + "}");
		}
		for (Tag tag : findAll()) {
			tag.delete();
		}
		getElement().remove();
	}
	
	@Override
	public void create(RoutingContext rc, Handler<AsyncResult<Tag>> handler) {
		// TODO Auto-generated method stub
		
	}

}
