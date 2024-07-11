package com.gentics.mesh.hibernate.data.dao;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.dao.PersistingS3BinaryDao;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibS3BinaryFieldImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.data.s3binary.impl.S3HibBinariesImpl;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Amazon S3 Binary DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class S3BinaryDaoImpl extends AbstractImageDataHibDao<S3HibBinary> implements PersistingS3BinaryDao {

    private final S3HibBinariesImpl s3HibBinaries;

    @Inject
    public S3BinaryDaoImpl(HibPermissionRoots permissionRoots, CommonDaoHelper daoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory, Lazy<Vertx> vertx, S3HibBinariesImpl binaries) {
        super(permissionRoots, daoHelper, currentTransaction, eventFactory, vertx);
        this.s3HibBinaries = binaries;
    }

    @Override
    public S3Binaries getS3HibBinaries() {
        return s3HibBinaries;
    }

    public S3HibBinaryField getField(UUID contentUuid, String fieldKey) {
        return em().createNamedQuery("s3binaryfieldref.getByContentAndFieldKey", HibS3BinaryFieldImpl.class)
                .setParameter("contentUuid", contentUuid)
                .setParameter("fieldKey", fieldKey)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void removeField(BulkActionContext bac, HibField field) {
        S3HibBinaryField s3BinaryRef = (S3HibBinaryField) field;
        HibS3BinaryImpl s3Binary = (HibS3BinaryImpl) s3BinaryRef.getBinary();

        em().remove(s3BinaryRef);
        long fieldCount = ((Number) em().createNamedQuery("s3Binary.getFieldCount").setParameter("uuid", s3Binary.getId()).getSingleResult()).longValue();
        if (fieldCount == 0) {
            bac.add(currentTransaction.getTx().data().s3BinaryStorage().delete(s3Binary.getUuid()));
            em().remove(s3Binary);
        }
    }

    public void removeField(List<UUID> containers) {
        SplittingUtils.splitAndConsume(containers, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em().createNamedQuery("s3binaryfieldref.removeByContainerUuids")
                .setParameter("containerUuids", slice)
                .executeUpdate());

        em().createNamedQuery("s3Binary.deleteUnreferenced")
            .executeUpdate();
    }

	@Override
	public String[] getHibernateEntityName(Object... unused) {
		return new String[] {currentTransaction.getTx().data().getDatabaseConnector().maybeGetDatabaseEntityName(HibS3BinaryImpl.class).get()};
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "key": return "s3ObjectKey";
		case "filename": return "fileName";
		case "mime": return "mimeType";
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}
}
