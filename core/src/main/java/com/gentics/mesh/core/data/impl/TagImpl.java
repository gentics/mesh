package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.TagGraphFieldContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see Tag
 */
public class TagImpl extends AbstractGenericFieldContainerVertex<TagResponse, Tag> implements Tag {

	private static final Logger log = LoggerFactory.getLogger(TagImpl.class);

	public static final String DEFAULT_TAG_LANGUAGE_TAG = "en";

	public static void checkIndices(Database database) {
		database.addVertexType(TagImpl.class);
	}

	@Override
	public String getType() {
		return Tag.TYPE;
	}

	@Override
	public List<? extends Node> getNodes() {
		return in(HAS_TAG).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public List<? extends TagGraphFieldContainer> getFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(TagGraphFieldContainerImpl.class).toListExplicit(TagGraphFieldContainerImpl.class);
	}

	@Override
	public TagGraphFieldContainer getFieldContainer(Language language) {
		return getGraphFieldContainer(language, TagGraphFieldContainerImpl.class);
	}

	@Override
	public TagGraphFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateGraphFieldContainer(language, TagGraphFieldContainerImpl.class);
	}

	@Override
	public String getName() {
		return getFieldContainer(BootstrapInitializer.getBoot().languageRoot().getTagDefaultLanguage()).getName();
	}

	@Override
	public void setName(String name) {
		getOrCreateFieldContainer(BootstrapInitializer.getBoot().languageRoot().getTagDefaultLanguage()).setName(name);
	}

	@Override
	public void removeNode(Node node) {
		unlinkIn(node.getImpl(), HAS_TAG);
	}

	@Override
	public TagReference transformToReference(InternalActionContext ac) {
		TagReference tagReference = new TagReference();
		tagReference.setName(getName());
		tagReference.setUuid(getUuid());
		return tagReference;
	}

	@Override
	public Observable<TagResponse> transformToRest(InternalActionContext ac) {

		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrxExperimental(() -> {
			Set<Observable<TagResponse>> obs = new HashSet<>();

			TagResponse restTag = new TagResponse();

			TagFamily tagFamily = getTagFamily();
			if (tagFamily != null) {
				TagFamilyReference tagFamilyReference = new TagFamilyReference();
				tagFamilyReference.setName(tagFamily.getName());
				tagFamilyReference.setUuid(tagFamily.getUuid());
				restTag.setTagFamily(tagFamilyReference);
			}
			restTag.getFields().setName(getName());

			// Add common fields
			obs.add(fillCommonRestFields(ac, restTag));

			// Role permissions
			obs.add(setRolePermissions(ac, restTag));

			// Merge and complete
			return Observable.merge(obs).last();
		});
	}

	@Override
	public void setTagFamily(TagFamily tagFamily) {
		setLinkOutTo(tagFamily.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	@Override
	public TagFamily getTagFamily() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
	}

	@Override
	public void setProject(Project project) {
		setLinkOutTo(project.getImpl(), ASSIGNED_TO_PROJECT);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void delete() {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag {" + getName() + "}");
		}
		addIndexBatch(DELETE_ACTION);
		getVertex().remove();
	}

	@Override
	public PageImpl<? extends Node> findTaggedNodes(MeshAuthUser requestUser, List<String> languageTags, PagingParameter pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_TAG).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_TAG).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		PageImpl<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	@Override
	public Observable<Tag> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		//TagFamilyReference reference = requestModel.getTagFamily();

		return db.trx(() -> {
			boolean updateTagFamily = false;
//			if (reference != null) {
//				// Check whether a uuid was specified and whether the tag family changed
//				if (!isEmpty(reference.getUuid())) {
//					if (!getTagFamily().getUuid().equals(reference.getUuid())) {
//						updateTagFamily = true;
//					}
//				}
//			}

			String newTagName = requestModel.getFields().getName();
			if (isEmpty(newTagName)) {
				throw error(BAD_REQUEST, "tag_name_not_set");
			} else {
				TagFamily tagFamily = getTagFamily();
				Tag foundTagWithSameName = tagFamily.getTagRoot().findByName(newTagName).toBlocking().single();
				if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(getUuid())) {
					throw conflict(foundTagWithSameName.getUuid(), newTagName, "tag_create_tag_with_same_name_already_exists", newTagName,
							tagFamily.getName());
				}
				setEditor(ac.getUser());
				setLastEditedTimestamp(System.currentTimeMillis());
				setName(requestModel.getFields().getName());
				if (updateTagFamily) {
					// TODO update the tagfamily
				}
			}
			return addIndexBatch(UPDATE_ACTION);
		}).process().map(i -> this);

	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		for (Node node : getNodes()) {
			batch.addEntry(node, UPDATE_ACTION);
		}
		batch.addEntry(getTagFamily(), UPDATE_ACTION);
	}

	@Override
	public TagReference createEmptyReferenceModel() {
		return new TagReference();
	}
}
