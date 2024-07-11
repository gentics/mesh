package com.gentics.mesh.hibernate.data.domain.keys;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;

/**
 * Private key for field container version edge.
 * 
 * @author plyhun
 *
 */
public class HibNodeFieldContainerVersionsEdgePK implements Serializable {

	private static final long serialVersionUID = 3346587647484374284L;

	private UUID thisContentUuid;
	private HibSchemaVersionImpl thisVersion;
	private UUID nextContentUuid;
	private HibSchemaVersionImpl nextVersion;

	public HibNodeFieldContainerVersionsEdgePK() {

	}

	public HibNodeFieldContainerVersionsEdgePK(UUID thisContentUuid, HibSchemaVersionImpl thisVersion, UUID nextContentUuid, HibSchemaVersionImpl nextVersion) {
		this.thisContentUuid = thisContentUuid;
		this.thisVersion = thisVersion;
		this.nextContentUuid = nextContentUuid;
		this.nextVersion = nextVersion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HibNodeFieldContainerVersionsEdgePK)) return false;
		HibNodeFieldContainerVersionsEdgePK that = (HibNodeFieldContainerVersionsEdgePK) o;
		return Objects.equals(thisContentUuid, that.thisContentUuid) && Objects.equals(thisVersion, that.thisVersion) && Objects.equals(nextContentUuid, that.nextContentUuid) && Objects.equals(nextVersion, that.nextVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(thisContentUuid, thisVersion, nextContentUuid, nextVersion);
	}
}
