package com.gentics.mesh.hibernate.data.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import com.gentics.mesh.core.data.s3binary.S3Binary;

/**
 * Amazon S3 Binary entity implementation for Enterprise Mesh.
 *
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "s3binary")
@NamedQueries({
	@NamedQuery(
		name = "s3Binary.findByS3ObjectKey",
		query = "select s from s3binary s where s.s3ObjectKey = :s3ObjectKey"),
	@NamedQuery(
		name = "s3Binary.getFieldCount",
		query = "select count(f) from s3binaryfieldref f where f.valueOrUuid = :uuid"),
	@NamedQuery(
		name = "s3Binary.deleteUnreferenced",
		query = "delete from s3binary s where not exists (select f from s3binaryfieldref f where f.valueOrUuid = s.dbUuid)"),
	@NamedQuery(
		name = "s3binary.findByCheckStatus",
		query = "select s from s3binary s where s.checkStatus = :checkStatus")
})
@Table(
	indexes = {
		@Index(name = "idx_s3_object_key", columnList = "s3ObjectKey"),
		@Index(name = "idx_s3_check_status", columnList = "checkStatus")
	}
)
public class HibS3BinaryImpl extends AbstractBinaryImpl implements S3Binary, Serializable {

	private static final long serialVersionUID = -6878480194464684318L;

	private String mimeType;
	private String s3ObjectKey;
	private String fileName;

	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public S3Binary setMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	@Override
	public String getS3ObjectKey() {
		return s3ObjectKey;
	}

	@Override
	public S3Binary setS3ObjectKey(String s3ObjectKey) {
		this.s3ObjectKey = s3ObjectKey;
		return this;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public S3Binary setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}
}
