package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import com.gentics.mesh.hibernate.data.domain.keys.HibNodeFieldContainerVersionsEdgePK;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * The versioning edge entity, used in the navigation over the contents, connected together
 * in the double linked list version-wise.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "nodefieldcontainer_versions_edge")
@NamedQueries({
	@NamedQuery(
			name = "containerversions.findNextEdgeByVersion",
			query = "select e from nodefieldcontainer_versions_edge e " +
					"where e.thisContentUuid = :contentUuid "  +
					"and e.thisVersion = :version "),
	@NamedQuery(
			name = "containerversions.deleteThisContent",
			query = "delete from nodefieldcontainer_versions_edge e " +
					"where e.thisContentUuid = :contentUuid"),
	@NamedQuery(
			name = "containerversions.deleteNextContent",
			query = "delete from nodefieldcontainer_versions_edge e " +
					"where e.nextContentUuid = :contentUuid"),
	@NamedQuery(
			name = "containerversions.bulkDeleteThisContents",
			query = "delete from nodefieldcontainer_versions_edge e " +
					"where e.thisContentUuid in :contentUuids"),
	@NamedQuery(
			name = "containerversions.bulkDeleteNextContents",
			query = "delete from nodefieldcontainer_versions_edge e " +
					"where e.nextContentUuid in :contentUuids"),
	@NamedQuery(
			name = "containerversions.findPreviousEdgeByVersion",
			query = "select e from nodefieldcontainer_versions_edge e " +
					"where e.nextContentUuid = :contentUuid "  +
					"and e.nextVersion = :version "),
	@NamedQuery(
			name = "containerversions.findPreviousAndNext",
			query = "select previous, next from nodefieldcontainer_versions_edge previous " +
					"left join nodefieldcontainer_versions_edge next on previous.nextContentUuid = next.thisContentUuid " +
					"where previous.nextContentUuid in :contentUuids or next.thisContentUuid in :contentUuids"
	),
	@NamedQuery(
			name = "containerversions.findNextByIds",
			query = "select e from nodefieldcontainer_versions_edge e " +
					"where e.thisContentUuid in :contentUuids"
	),
	@NamedQuery(
			name = "containerversions.findPreviousByIds",
			query = "select e from nodefieldcontainer_versions_edge e " +
					"where e.nextContentUuid in :contentUuids"
	)
})
@IdClass(HibNodeFieldContainerVersionsEdgePK.class)
public class HibNodeFieldContainerVersionsEdgeImpl implements Serializable {
	private static final long serialVersionUID = -3521606858914685076L;

	@Id
	@Column(nullable = false)
	private UUID thisContentUuid;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private HibSchemaVersionImpl thisVersion;

	@Id
	@Column(nullable = false)
	private UUID nextContentUuid;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	private HibSchemaVersionImpl nextVersion;

	public UUID getThisContentUuid() {
		return thisContentUuid;
	}

	public void setThisContentUuid(UUID thisContentUuid) {
		this.thisContentUuid = thisContentUuid;
	}

	public HibSchemaVersionImpl getThisVersion() {
		return thisVersion;
	}

	public void setThisVersion(HibSchemaVersionImpl thisVersion) {
		this.thisVersion = thisVersion;
	}

	public UUID getNextContentUuid() {
		return nextContentUuid;
	}

	public void setNextContentUuid(UUID nextContentUuid) {
		this.nextContentUuid = nextContentUuid;
	}

	public HibSchemaVersionImpl getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(HibSchemaVersionImpl nextVersion) {
		this.nextVersion = nextVersion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HibNodeFieldContainerVersionsEdgeImpl)) return false;
		HibNodeFieldContainerVersionsEdgeImpl that = (HibNodeFieldContainerVersionsEdgeImpl) o;
		return Objects.equals(thisContentUuid, that.thisContentUuid) && Objects.equals(thisVersion, that.thisVersion) && Objects.equals(nextContentUuid, that.nextContentUuid) && Objects.equals(nextVersion, that.nextVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(thisContentUuid, thisVersion, nextContentUuid, nextVersion);
	}
}
