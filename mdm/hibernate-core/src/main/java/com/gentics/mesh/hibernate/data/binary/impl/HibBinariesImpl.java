package com.gentics.mesh.hibernate.data.binary.impl;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Binary storage implementation for Gentics Mesh.
 *
 * @author plyhun
 *
 */
@Singleton
public class HibBinariesImpl implements Binaries {

	private final HibernateDatabase database;

	@Inject
	public HibBinariesImpl(HibernateDatabase database) {
		this.database = database;
	}

	@Override
	public Transactional<HibBinary> findByHash(String SHA512Sum) {
		return database.transactional((tx) -> {
			HibernateTx hibTx = (HibernateTx) tx;

			return hibTx.entityManager().createNamedQuery("binary.findBySHA", HibBinaryImpl.class)
					.setParameter("SHA512Sum", SHA512Sum)
					.getResultStream()
					.findFirst()
					.orElse(null);
		});
	}

	@Override
	public Transactional<Stream<? extends HibBinary>> findByCheckStatus(BinaryCheckStatus checkStatus) {
		return database.transactional(tx -> {
			HibernateTx hibTx = (HibernateTx) tx;

			return hibTx.entityManager().createNamedQuery("binary.findByCheckStatus", HibBinaryImpl.class)
				.setParameter("checkStatus", checkStatus)
				.getResultStream();
		});
	}

	@Override
	public Transactional<HibBinary> create(String uuid, String SHA512Sum, Long size, BinaryCheckStatus checkStatus) {
		return database.transactional((tx) -> {
			HibernateTx hibTx = (HibernateTx) tx;
			HibBinaryImpl binary = hibTx.create(uuid, HibBinaryImpl.class);
			binary.setSHA512Sum(SHA512Sum);
			binary.setSize(size);
			binary.setCheckStatus(checkStatus);

			if (checkStatus == BinaryCheckStatus.POSTPONED) {
				binary.setCheckSecret(UUIDUtil.randomUUID());
			}

			return binary;
		});
	}

	@Override
	public Transactional<Stream<HibBinary>> findAll() {
		return database.transactional((tx) -> {
			HibernateTx hibTx = (HibernateTx) tx;

			return hibTx.entityManager().createQuery("select b from binary b", HibBinaryImpl.class)
					.getResultStream()
					.map(HibBinary.class::cast);
		});
	}
}
