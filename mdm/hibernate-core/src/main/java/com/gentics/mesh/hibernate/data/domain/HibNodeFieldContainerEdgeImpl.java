package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.QueryHints;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

/**
 * An implementation of an edge between Node and the field container.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "nodefieldcontainer")
@NamedQueries({
		@NamedQuery(
				name = "contentEdge.findByProjectAndBranch",
				query = "select distinct(edge) from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" where n.project.dbUuid = :projectUuid " +
						" and edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and edge.languageTag in :languageTags " +
						" and perm.role in :roles " +
						" and (perm.readPerm = true or (edge.type = 'PUBLISHED' and perm.readPublishedPerm = true)) "),
		@NamedQuery(
				name = "contentEdge.findByProjectAndBranchForAdmin",
				query = "select edge from node n " +
						" join n.content edge " +
						" where n.project.dbUuid = :projectUuid " +
						" and edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and edge.languageTag in :languageTags "),
		@NamedQuery(
				name = "contentEdge.findByBranchAndVersion",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.branch.dbUuid = :branchUuid " +
						"and edge.version.dbUuid = :versionUuid"),
		@NamedQuery(
				name = "contentEdge.findByBranchVersionAndType",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.branch.dbUuid = :branchUuid " +
						"and edge.version.dbUuid = :versionUuid " +
						"and edge.type = :type "),
		@NamedQuery(
				name = "contentEdge.findByNodeAndType",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.node = :node " +
						"and edge.type = :type"),
		@NamedQuery(
				name = "contentEdge.findByNodeAndTypeAndVersion",
				query = "select distinct (edge.contentUuid) from nodefieldcontainer edge " +
						"where edge.node.dbUuid = :nodeUuid " +
						"and edge.type = :type " +
						"and edge.version.dbUuid = :versionUuid"),
		@NamedQuery(
				name = "contentEdge.findByNodeTypeAndBranch",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.node = :node " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid"),
		@NamedQuery(
				name = "contentEdge.findLanguageTagByNodeTypeAndBranch",
				query = "select edge.languageTag from nodefieldcontainer edge " +
						"where edge.node.dbUuid = :nodeUuid " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid ",
				hints = {@QueryHint(name = QueryHints.HINT_CACHEABLE, value = "true")}),
		@NamedQuery(
				name = "contentEdge.findByNodeTypeBranchAndVersion",
				query = "select distinct (edge.contentUuid) from nodefieldcontainer edge " +
						"where edge.node.dbUuid = :nodeUuid " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid " +
						"and edge.version.dbUuid = :versionUuid",
				hints = {@QueryHint(name = QueryHints.HINT_CACHEABLE, value = "true")}),
		@NamedQuery(
				name = "contentEdge.findByNodesTypeBranchAndVersion",
				query = "select distinct (edge.contentUuid) from nodefieldcontainer edge " +
						"where edge.node.dbUuid in :nodeUuids " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid " +
						"and edge.version.dbUuid = :versionUuid"),
		@NamedQuery(
				name = "contentEdge.findByNodesTypeAndBranch",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.node.dbUuid in :nodeUuids " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid "),
		@NamedQuery(
				name = "contentEdge.findByNodesAndType",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.node.dbUuid in :nodeUuids " +
						"and edge.type = :type "),
		@NamedQuery(
				name = "contentEdge.existsByNodeTypeAnBranch",
				query = "select count(1) as counts from nodefieldcontainer edge " +
						"where edge.node = :node " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid"),
		@NamedQuery(
				name = "contentEdge.findByNodeTypeBranchAndLanguageForAdmin",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.node in :nodes " +
						"and edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid " +
						"and edge.languageTag in :languageTags ",
				hints = {@QueryHint(name = QueryHints.HINT_CACHEABLE, value = "true")}
		),
		@NamedQuery(
				name = "contentEdge.findByTypeBranchLanguageAndVersionForAdmin",
				query = "select edge from nodefieldcontainer edge " +
						"where edge.type = :type " +
						"and edge.branch.dbUuid = :branchUuid " +
						"and edge.languageTag in :languageTags " +
						"and edge.version = :schemaVersion"),
		@NamedQuery(
				name = "contentEdge.findByTypeBranchLanguageAndVersion",
				query = "select distinct(edge) from node n " +
						" join n.content edge " +
						" join permission perm on (perm.element = n.dbUuid) " +
						" where edge.type = :type " +
						" and edge.version = :schemaVersion " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and edge.languageTag in :languageTags " +
						" and perm.role in :roles " +
						" and (perm.readPerm = true or (edge.type = 'PUBLISHED' and perm.readPublishedPerm = true)) "),
		@NamedQuery(
				name = "contentEdge.findByNodeTypeBranchAndLanguage",
				query = "select distinct(edge) from nodefieldcontainer edge " +
						" join permission perm on (perm.element = edge.node.dbUuid) " +
						" where edge.node.dbUuid in :nodes " +
						" and edge.type = :type " +
						" and edge.branch.dbUuid = :branchUuid " +
						" and edge.languageTag in :languageTags " +
						" and perm.role in :roles " +
						" and (perm.readPerm = true or (edge.type = 'PUBLISHED' and perm.readPublishedPerm = true)) "),
		@NamedQuery(
				name = "contentEdge.findByContent",
				query = "select e from nodefieldcontainer e where e.contentUuid = :contentUuid"),
		@NamedQuery(
				name = "contentEdge.findByContentAndTypes",
				query = "select e from nodefieldcontainer e " +
						"where e.contentUuid = :contentUuid " +
						"and e.type IN :types"),
		@NamedQuery(
				name = "contentEdge.findParentNodeOfContentInBranch",
				query = "select n from nodefieldcontainer e join e.node n " +
						"where e.contentUuid = :contentUuid and e.branch.dbUuid = :branchUuid"),
		@NamedQuery(
				name = "contentEdge.findByContentTypeAndBranch",
				query = "select e from nodefieldcontainer e " +
						"where e.contentUuid = :contentUuid " +
						"and e.type = :type " +
						"and e.branch.dbUuid = :branchUuid"),
		@NamedQuery(
				name = "contentEdge.findByBranchAndType",
				query = "select e from nodefieldcontainer e " +
						"where e.branch.dbUuid = :branchUuid " +
						"and e.type = :type"),
		@NamedQuery(
				name = "contentEdge.findByBranchTypeAndWebroot",
				query = "select e from nodefieldcontainer e " +
						"where e.branch.dbUuid = :branchUuid "  +
						"and e.type = :type " +
						"and e.webrootPath = :webrootPath " +
						"and e.element <> :edgeUuid"),
		@NamedQuery(
				name = "contentEdge.findAllByBranchTypeAndWebroot",
				query = "select e from nodefieldcontainer e " +
						"where e.branch.dbUuid = :branchUuid "  +
						"and e.type = :type " +
						"and e.webrootPath = :webrootPath "),
		@NamedQuery(
				name = "contentEdge.findByUrlField",
				query = "select e from nodefieldcontainer e join e.webrootUrlFields f " +
						"where f = :field"),
		@NamedQuery(
				name = "contentEdge.findByBranchTypeAndUrlField",
				query = "select e from nodefieldcontainer e join e.webrootUrlFields f " +
						"where e.branch.dbUuid = :branchUuid "  +
						"and e.type = :type " +
						"and f = :field " +
						"and e.element <> :edgeUuid"),
		@NamedQuery(
				name = "contentEdge.findByBranchTypeAndUrlFieldValues",
				query = "select e from nodefieldcontainer e join e.webrootUrlFields f " +
						"where e.branch.dbUuid = :branchUuid "  +
						"and e.type = :type " +
						"and f in :field " +
						"and e.element <> :edgeUuid"),
		@NamedQuery(
				name = "contentEdge.findAllByBranchTypeAndUrlField",
				query = "select e from nodefieldcontainer e join e.webrootUrlFields f " +
						"where e.branch.dbUuid = :branchUuid "  +
						"and e.type = :type " +
						"and f = :field "),
		@NamedQuery(
				name = "contentEdge.countContentByNode",
				query = "select count(distinct e.contentUuid) from nodefieldcontainer e where e.node = :node"),
		@NamedQuery(
				name = "contentEdge.findUsedLanguages",
				query = "select distinct edge.languageTag from nodefieldcontainer edge " +
						"where edge.element in " +
						"(select edge2.element from nodefieldcontainer edge2 where edge2.node.project = :project) " + 
						"and edge.languageTag in :tags"
		),
		@NamedQuery(
				name = "contentEdge.findUsedKnownLanguages",
				query = "select distinct edge.languageTag from nodefieldcontainer edge " +
						"where edge.element in " +
						"(select distinct edge2.element from nodefieldcontainer edge2 join edge2.node.project.languages projectLang where edge2.node.project = :project and projectLang.languageTag in :tags) " + 
						"and edge.languageTag in :tags"
		),
})
public class HibNodeFieldContainerEdgeImpl implements HibNodeFieldContainerEdge, HibEditorTracking, Serializable {

	private static final long serialVersionUID = 175969258170125593L;

	@Id
	@Column
	private UUID element;

	@ManyToOne(targetEntity = HibNodeImpl.class, fetch = FetchType.LAZY, optional = false)
	private HibNode node;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ContainerType type;

	@ManyToOne(targetEntity = HibBranchImpl.class, fetch = FetchType.LAZY, optional = false)
	private HibBranch branch;

	@ManyToOne(targetEntity = HibSchemaVersionImpl.class, fetch = FetchType.LAZY, optional = false)
	private HibSchemaVersion version;

	@Column(nullable = false)
	private UUID contentUuid;

	@Column(nullable = false)
	private String languageTag;

	@Column
	private String webrootPath;

	@ElementCollection
	@CollectionTable(name = "nodefieldcontainer_webrooturlfields")
	private Set<String> webrootUrlFields = new HashSet<>();

	@Embedded
	protected EditorTracking editorTracking = new EditorTracking();

	public UUID getElement() {
		return element;
	}

	public void setElement(UUID element) {
		this.element = element;
	}

	@Override
	public void setSegmentInfo(String segmentInfo) {
		setWebrootPath(segmentInfo);
		EntityManager em = HibernateTx.get().entityManager();
		em.merge(this);
	}

	@Override
	public String getSegmentInfo() {
		return getWebrootPath();
	}

	@Override
	public void setUrlFieldInfo(Set<String> urlFieldInfo) {
		setWebrootUrlFields(urlFieldInfo);
	}

	@Override
	public HibNodeFieldContainer getNodeContainer() {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		return contentDao.getFieldContainerOfEdge(this);
	}

	public HibNode getNode() {
		return node;
	}

	public void setNode(HibNode node) {
		this.node = node;
	}

	public ContainerType getType() {
		return type;
	}

	public void setType(ContainerType type) {
		this.type = type;
	}

	public HibBranch getBranch() {
		return branch;
	}

	public void setBranch(HibBranch branch) {
		this.branch = branch;
	}

	public HibSchemaVersion getVersion() {
		return version;
	}

	public void setVersion(HibSchemaVersion version) {
		this.version = version;
	}

	public UUID getContentUuid() {
		return contentUuid;
	}

	public void setContentUuid(UUID contentUuid) {
		this.contentUuid = contentUuid;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	public String getWebrootPath() {
		return webrootPath;
	}

	public void setWebrootPath(String webrootPath) {
		this.webrootPath = webrootPath;
	}

	public Set<String> getWebrootUrlFields() {
		return webrootUrlFields;
	}

	public void setWebrootUrlFields(Set<String> webrootUrl) {
		this.webrootUrlFields = webrootUrl;
	}

	@Override
	public String getBranchUuid() {
		return branch != null ? branch.getUuid() : null;
	}

	@Override
	public String toString() {
		return "HibNodeFieldContainerEdgeImpl [element=" + element + ", node=" + node + ", type=" + type + ", branch="
				+ branch + ", version=" + version + ", contentUuid=" + contentUuid + ", languageTag=" + languageTag
				+ ", webrootPath=" + webrootPath + ", webrootUrlFields=" + webrootUrlFields + "]";
	}

	@Override
	public HibUser getEditor() {
		return editorTracking.getEditor();
	}

	@Override
	public void setEditor(HibUser user) {
		editorTracking.setEditor(user);
	}

	@Override
	public Long getLastEditedTimestamp() {
		return editorTracking.getLastEditedTimestamp();
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {
		editorTracking.setLastEditedTimestamp(timestamp);
	}

	@Override
	public void setLastEditedTimestamp() {
		setLastEditedTimestamp(System.currentTimeMillis());
	}
}
