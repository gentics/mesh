package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;

/**
 * Binary implementation for Gentics Mesh.
 *
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "binary")
@NamedQueries({
	@NamedQuery(
		name = "binary.findBySHA",
		query = "select b from binary b where b.SHA512Sum = :SHA512Sum"),
	@NamedQuery(
		name = "binary.getFieldCount",
		query = "select count(f) from binaryfieldref f where f.valueOrUuid = :uuid"),
	@NamedQuery(
		name = "binary.removedUnreferenced",
		query = "delete from binary b where not exists (select f from binaryfieldref f where f.valueOrUuid = b.dbUuid)"),
	@NamedQuery(
		name = "binary.findUnreferencedBinaryUuids",
		query = "select b.dbUuid from binary b where not exists (select f from binaryfieldref f where f.valueOrUuid = b.dbUuid)"
	),
	@NamedQuery(
		name = "binary.findByUuids",
		query = "select b from binary b where b.dbUuid in :uuids"
	),
	@NamedQuery(
		name = "binary.findByCheckStatus",
		query = "select b from binary b where b.checkStatus = :checkStatus")
})
@Table(
	indexes = {
		@Index(name = "idx_sha", columnList = "SHA512Sum"),
		@Index(name = "idx_check_status", columnList = "checkStatus")
	}
)
public class HibBinaryImpl extends AbstractBinaryImpl implements HibBinary, Serializable {

	private static final long serialVersionUID = 3320676473079382929L;
	private String SHA512Sum;

	@OneToMany(mappedBy = "binary", cascade = CascadeType.REMOVE, targetEntity = HibImageVariantImpl.class)
	protected Set<HibImageVariant> variants;

	@Override
	public String getSHA512Sum() {
		return SHA512Sum;
	}

	@Override
	public HibBinary setSHA512Sum(String sha512sum) {
		SHA512Sum = sha512sum;
		return this;
	}

	public Set<HibImageVariant> getVariants() {
		return variants;
	}

	public void setVariants(Set<HibImageVariant> variants) {
		this.variants = variants;
	}
}
