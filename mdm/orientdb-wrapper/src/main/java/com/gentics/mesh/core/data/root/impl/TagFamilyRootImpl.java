package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see TagFamilyRoot
 */
public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily> implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagFamilyRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_TAG_FAMILY).withInOut().withOut());
	}

	@Override
	public Class<? extends TagFamily> getPersistanceClass() {
		return TagFamilyImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_TAG_FAMILY;
	}

	@Override
	public Project getProject() {
		return in(HAS_TAGFAMILY_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void addTagFamily(TagFamily tagFamily) {
		addItem(tagFamily);
		// small hack: set the name of the tagfamily again, so the uniqueName (which contains the root uuid) will get updated
		tagFamily.setName(tagFamily.getName());
	}

	@Override
	public long globalCount() {
		return db().count(TagFamilyImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamilyRoot {" + getUuid() + "}");
		}
		for (TagFamily tagFamily : findAll()) {
			tagFamily.delete(bac);
			bac.process();
		}
		getElement().remove();
		bac.inc();
		bac.process();
	}

	@Override
	public HibBaseElement resolveToElement(HibBaseElement permissionRoot, HibBaseElement root, Stack<String> stack) {
		if (stack.isEmpty()) {
			return this;
		} else {
			String uuidSegment = stack.pop();
			TagFamily tagFamily = findByUuid(uuidSegment);
			if (stack.isEmpty()) {
				return tagFamily;
			} else {
				String nestedRootNode = stack.pop();
				if (PermissionRoots.TAGS.contentEquals(nestedRootNode)) {
					return tagFamily.resolveToElement(tagFamily, tagFamily, stack);
				} else {
					// TODO i18n
					throw error(NOT_FOUND, "Unknown tagFamily element {" + nestedRootNode + "}");
				}
			}
		}
	}

	@Override
	public TagFamily findByName(String name) {
		return (TagFamily) mesh().tagFamilyNameCache().get(name, familyName -> {
			return super.findByName(familyName);
		});
	}
}
