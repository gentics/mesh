package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Node entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "node")
@ElementTypeKey(ElementType.NODE)
@NamedEntityGraphs({
	@NamedEntityGraph(name = "node.content",
			attributeNodes = @NamedAttributeNode("content")),
	@NamedEntityGraph(
			name = "node.contentAndTags",
			attributeNodes = {
					@NamedAttributeNode("content"),
					@NamedAttributeNode(value = "tags", subgraph = "subgraph.tags")
			},
			subgraphs = {
					@NamedSubgraph(
							name = "subgraph.tags",
							attributeNodes = @NamedAttributeNode(value = "tag")
							)
			}
			)
})
@NamedQueries({
		@NamedQuery(
				name = "node.findByProjectBranchAndContainerType.read",
				query = "select distinct n from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" where n.project = :project and edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles " + HibNodeImpl.DEFAULT_PERM_CHECK),
		@NamedQuery(
				name = "node.findByProjectBranchAndContainerType.read_published",
				query = "select distinct n from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" where n.project = :project and edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles " +
						" and (perm.readPerm = true or perm.readPublishedPerm = true)"),
		@NamedQuery(
				name = "node.findByProjectBranchAndContainerType.admin",
				query = "select distinct n from node n " +
						" join n.content edge " +
						" where n.project = :project and edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid "),
		@NamedQuery(
				name = "node.findByTagsLanguageTagsAndContainerType",
				query = "select distinct n from node n " + 
						" join n.tags tag " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" where edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles " +
						" and tag.id.tagUUID = :tagUuid and tag.id.branchUUID = :branchUuid " + HibNodeImpl.DEFAULT_PERM_CHECK),
		@NamedQuery(
				name = "node.findByTagsLanguageTagsAndContainerTypeForAdmin",
				query = "select distinct n from node n " + 
						" join n.tags tag " +
						" join n.content edge " +
						" where edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and tag.id.tagUUID = :tagUuid and tag.id.branchUUID = :branchUuid"),
		@NamedQuery(
				name = "node.findByTagsLanguageTags",
				query = HibNodeImpl.QUERY_FIND_BY_TAGS_LANG_TAGS + HibNodeImpl.DEFAULT_PERM_CHECK),
		@NamedQuery(
				name = "node.findByTagsLanguageTags.admin",
				query = "select distinct n from node n " + 
						" join n.tags tag " +
						" join n.content edge " +
						" where edge.branch.dbUuid = :branchUuid " +
						" and tag.id.tagUUID = :tagUuid and tag.id.branchUUID = :branchUuid"),
		@NamedQuery(
				name = "node.findByProject",
				query = "select n from node n where n.project = :project"),
		@NamedQuery(
				name = "node.findUuidsByProject",
				query = "select n.dbUuid from node n where n.project = :project"),
		@NamedQuery(
				name = "node.findBySchema",
				query = "select n from node n where n.schemaContainer = :schema"),
		@NamedQuery(
				name = "node.findBySchemaBranchType",
				query = "select distinct n from node n "+
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" where edge.branch.dbUuid = :branchUuid " +
						" and edge.type = :type " +
						" and n.schemaContainer.dbUuid = :schemaUuid" +
						" and perm.role in :roles " + HibNodeImpl.DEFAULT_PERM_CHECK),
		@NamedQuery(
				name = "node.findBySchemaBranchTypeForAdmin",
				query = "select distinct n from node n "+
						" join n.content edge " +
						" where edge.branch.dbUuid = :branchUuid " +
						" and edge.type = :type " +
						" and n.schemaContainer.dbUuid = :schemaUuid"),
		@NamedQuery(
				name = "node.findNodesByUuids",
				query = "select distinct n from node n " +
						"where n.dbUuid in :nodeUuids"),
		@NamedQuery(
				name = "node.countChildrenBySchema.read",
				query = "select n.schemaContainer.dbUuid, count(distinct n) from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" join node_branch_parent nbp on (nbp.id.childUuid = n.dbUuid) " +
						" where nbp.distance = 1 and nbp.nodeParent.dbUuid = :parentUuid " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles and perm.readPerm = true " +
						" group by n.schemaContainer.dbUuid"),
		@NamedQuery(
				name = "node.countChildrenBySchema.read_published",
				query = "select n.schemaContainer.dbUuid, count(distinct n) from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" join node_branch_parent nbp on (nbp.id.childUuid = n.dbUuid) " +
						" where nbp.distance = 1 and nbp.nodeParent.dbUuid = :parentUuid " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles and (perm.readPerm = true or perm.readPublishedPerm = true)" +
						" group by n.schemaContainer.dbUuid"),
		@NamedQuery(
				name = "node.countChildrenBySchema.admin",
				query = "select n.schemaContainer.dbUuid, count(distinct n) from node n " +
						" join n.content edge " +
						" join node_branch_parent nbp on (nbp.id.childUuid = n.dbUuid) " +
						" where nbp.distance = 1 and nbp.nodeParent.dbUuid = :parentUuid " +
						" and edge.branch.dbUuid = :branchUuid " +
						" group by n.schemaContainer.dbUuid"),
		@NamedQuery(
				name = "node.countMultipleChildrenBySchema.read",
				query = "select n.schemaContainer.dbUuid, nbp.nodeParent.dbUuid, count(distinct n) from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" join node_branch_parent nbp on (nbp.id.childUuid = n.dbUuid) " +
						" where nbp.distance = 1 and nbp.nodeParent.dbUuid in :parentUuids " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles and perm.readPerm = true " +
						" group by n.schemaContainer.dbUuid, nbp.nodeParent.dbUuid"),
		@NamedQuery(
				name = "node.countMultipleChildrenBySchema.read_published",
				query = "select n.schemaContainer.dbUuid, nbp.nodeParent.dbUuid, count(distinct n) from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" join node_branch_parent nbp on (nbp.id.childUuid = n.dbUuid) " +
						" where nbp.distance = 1 and nbp.nodeParent.dbUuid in :parentUuids " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and perm.role in :roles and (perm.readPerm = true or perm.readPublishedPerm = true)" +
						" group by n.schemaContainer.dbUuid, nbp.nodeParent.dbUuid"),
		@NamedQuery(
				name = "node.countMultipleChildrenBySchema.admin",
				query = "select n.schemaContainer.dbUuid, nbp.nodeParent.dbUuid, count(distinct n) from node n " +
						" join n.content edge " +
						" join node_branch_parent nbp on (nbp.id.childUuid = n.dbUuid) " +
						" where nbp.distance = 1 and nbp.nodeParent.dbUuid in :parentUuids " +
						" and edge.branch.dbUuid = :branchUuid " +
						" group by n.schemaContainer.dbUuid, nbp.nodeParent.dbUuid")

})
public class HibNodeImpl extends AbstractHibBucketableElement implements HibNode, Serializable {

	private static final long serialVersionUID = -7505762081933808577L;

	@ManyToOne(targetEntity = HibUserImpl.class, fetch = FetchType.LAZY)
	public static final String QUERY_FIND_BY_TAGS_LANG_TAGS = "select distinct n from node n " +
			" join n.tags tag " +
			" join n.content edge " +
			" join permission perm on (perm.element = n.dbUuid) " +
			" where edge.branch.dbUuid = :branchUuid " +
			" and perm.role in :roles " +
			" and tag.id.tagUUID = :tagUuid and tag.id.branchUUID = :branchUuid ";

	public static final String DEFAULT_PERM_CHECK = " and ((perm.readPerm = true) or (edge.type = 'PUBLISHED'  and perm.readPublishedPerm = true))";

	@ManyToOne(targetEntity = HibUserImpl.class, fetch = FetchType.LAZY)
	private HibUser creator;

	private Instant created;

	@ManyToOne(targetEntity = HibProjectImpl.class, fetch = FetchType.LAZY)
	private HibProject project;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@OneToMany(mappedBy = "node", targetEntity = HibNodeTag.class, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<HibNodeTag> tags = new HashSet<>();

	@ManyToOne(targetEntity = HibSchemaImpl.class, fetch = FetchType.LAZY)
	private HibSchema schemaContainer;

	@OneToMany(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Set<HibNodeFieldContainerEdgeImpl> content = new HashSet<>();

	public Set<HibNodeFieldContainerEdgeImpl> getContentEdges() {
		return content;
	}

	public void addEdge(HibNodeFieldContainerEdgeImpl edge) {
		content.add(edge);
	}

	public void removeEdge(HibNodeFieldContainerEdgeImpl edge) {
		content.remove(edge);
	}

	@Override
	public HibUser getCreator() {
		return creator;
	}

	@Override
	public void setCreator(HibUser user) {
		this.creator = user;
	}

	@Override
	public void setCreated(HibUser creator) {
		Instant now = Instant.now();
		setCreator(creator);
		created = now;
	}

	@Override
	public void setCreationTimestamp(long timestamp) {
		this.created = Instant.ofEpochMilli(timestamp);
	}

	@Override
	public void setCreationTimestamp() {
		setCreationTimestamp(System.currentTimeMillis());
	}

	@Override
	public Long getCreationTimestamp() {
		return created.toEpochMilli();
	}

	@Override
	public HibProject getProject() {
		return project;
	}

	public void setProject(HibProject project) {
		this.project = project;
	}

	@Override
	public HibSchema getSchemaContainer() {
		return schemaContainer;
	}

	@Override
	public void setSchemaContainer(HibSchema container) {
		schemaContainer = container;
	}

	@Override
	public void addTag(HibTag tag, HibBranch branch) {
		HibNodeTag newTag = new HibNodeTag(this, (HibTagImpl) tag, (HibBranchImpl) branch);
		tags.add(newTag);
	}

	@Override
	public void removeTag(HibTag tag, HibBranch branch) {
		Optional<HibNodeTag> tagToRemove = tags.stream()
				.filter(t -> t.getId().getTagUUID().equals(UUIDUtil.toJavaUuid(tag.getUuid())) && t.getId().getBranchUUID().equals(UUIDUtil.toJavaUuid(branch.getUuid())))
				.findFirst();

		tagToRemove.ifPresent(tags::remove);
	}

	@Override
	public void removeAllTags(HibBranch branch) {
		tags.removeIf(tag -> tag.getBranch().getUuid().equals(branch.getUuid()));
	}

	@Override
	public Result<HibTag> getTags(HibBranch branch) {
		return new TraversalResult<>(tags.stream()
				.filter(t -> t.getId().getBranchUUID().equals(((HibBranchImpl)branch).getDbUuid()))
				.map(HibNodeTag::getTag));
	}

	public Set<HibNodeTag> getTags() {
		return tags;
	}

	@Override
	public HibNode getParentNode(String branchUuid) {
		return Tx.get().nodeDao().getParentNode(this, branchUuid);
	}

	@Override
	public boolean isBaseNode() {
		return project.getBaseNode().getId().equals(getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().isInstance(obj)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return getClass().cast(obj).getId().equals(getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}
}
