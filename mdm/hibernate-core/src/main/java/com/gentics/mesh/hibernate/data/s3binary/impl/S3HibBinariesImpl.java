package com.gentics.mesh.hibernate.data.s3binary.impl;

import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryImpl;
import com.gentics.mesh.util.UUIDUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

/**
 * Amazon S3 binary manager component implementation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class S3HibBinariesImpl implements S3Binaries {

	private final HibernateDatabase database;

	@Inject
	public S3HibBinariesImpl(HibernateDatabase database) {
		this.database = database;
	}

	@Override
	public Transactional<S3HibBinary> findByS3ObjectKey(String s3ObjectKey) {
		return database.transactional(tx -> {
			HibernateTx hibTx = (HibernateTx) tx;

			return hibTx.entityManager().createNamedQuery("s3Binary.findByS3ObjectKey", HibS3BinaryImpl.class)
					.setParameter("s3ObjectKey", s3ObjectKey)
					.getResultStream()
					.findFirst()
					.orElse(null);
		});
	}

	@Override
	public Transactional<S3HibBinary> create(String uuid, String objectKey, String fileName, BinaryCheckStatus checkStatus) {
		return database.transactional(tx -> {
			HibernateTx hibTx = (HibernateTx) tx;
			HibS3BinaryImpl binary = hibTx.create(uuid, HibS3BinaryImpl.class);
			binary.setS3ObjectKey(objectKey);
			binary.setFileName(fileName);
			binary.setCheckStatus(checkStatus);

			if (checkStatus == BinaryCheckStatus.POSTPONED) {
				binary.setCheckSecret(UUIDUtil.randomUUID());
			}
			return binary;
		});
	}

	@Override
	public Transactional<Stream<S3HibBinary>> findAll() {
		return database.transactional(tx -> {
			HibernateTx hibTx = (HibernateTx) tx;

			return hibTx.entityManager().createQuery("select s from s3binary s", HibS3BinaryImpl.class)
					.getResultStream()
					.map(S3HibBinary.class::cast);
		});
	}
}
