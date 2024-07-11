package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.MicroschemaDaoImpl;
import com.gentics.mesh.hibernate.data.dao.SchemaDaoImpl;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Schema change entity implementation for Enterprise Mesh. 
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "schemachange")
@Table(name = "schemachange")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class HibSchemaChangeImpl extends AbstractHibPropertyContainerElement implements Serializable {

	private static final long serialVersionUID = 1305126132462144519L;

	@OneToOne
	protected HibSchemaChangeImpl nextChange;
	@OneToOne
	protected HibSchemaChangeImpl previousChange;

	@Column
	protected UUID containerUuid;
	@Column
	protected UUID nextVersionUuid;
	@Column
	protected UUID previousVersionUuid;

	private String operation;

	protected <R> void setRestProperty(String key, R value) {
		property(key, value);
	}

	protected <R> R getRestProperty(String key) {
		return property(key);
	}

	protected SchemaChangeOperation getOperation() {
		return SchemaChangeOperation.valueOf(operation);
	}
	
	protected void setOperation(SchemaChangeOperation operation) {
		this.operation = operation.name();
	}

	protected <R extends FieldSchemaVersionElement<?, ?, ?, ?, ?>> R getPreviousContainerVersion() {
		return findContainerVersion(previousVersionUuid);
	}

	protected void setPreviousContainerVersionInner(FieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		this.previousVersionUuid = (UUID) containerVersion.getId();
		this.containerUuid = (UUID) containerVersion.getSchemaContainer().getId();
	}

	protected <R extends FieldSchemaVersionElement<?, ?, ?, ?, ?>> R getNextContainerVersion() {
		return findContainerVersion(nextVersionUuid);
	}

	protected void setNextSchemaContainerVersionInner(FieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		this.nextVersionUuid = (UUID) containerVersion.getId();
		this.containerUuid = (UUID) containerVersion.getSchemaContainer().getId();
	}

	protected SchemaChange<?> intoSchemaChange() {
		SchemaChangeOperation schemaChangeOperation = SchemaChangeOperation.valueOf(operation);
		SchemaChange<?> schemaChange = null;
		Class<? extends SchemaChange<?>> schemaChangeClass = null;

		switch (schemaChangeOperation) {
		case ADDFIELD:
			schemaChangeClass = HibAddFieldChangeImpl.class;
			break;
		case CHANGEFIELDTYPE:
			schemaChangeClass = HibFieldTypeChangeImpl.class;
			break;
		case REMOVEFIELD:
			schemaChangeClass = HibRemoveFieldChangeImpl.class;
			break;
		case UPDATEFIELD:
			schemaChangeClass = HibUpdateFieldChangeImpl.class;
			break;
		case UPDATEMICROSCHEMA:			
			schemaChangeClass = HibUpdateMicroschemaChangeImpl.class;
			break;
		case UPDATESCHEMA:
			schemaChangeClass = HibUpdateSchemaChangeImpl.class;
			break;
		default:
			throw error(INTERNAL_SERVER_ERROR, "Operation was not expected {" + schemaChangeOperation + "}");
		}
		
		if (schemaChangeClass.isInstance(this)) {
			schemaChange = schemaChangeClass.cast(this);
		} else {
			schemaChange = HibernateTx.get().entityManager().find(schemaChangeClass, UUIDUtil.toJavaUuid(this.getUuid()));
		}
		// TODO mb unneeded		
//		if (schemaChange != this) {
//			final HibSchemaChange<?> hibSchemaChange = schemaChange;
//			hibSchemaChange.setIndexOptions(getIndexOptions());
//			hibSchemaChange.setNextChange(nextChange.intoSchemaChange());
//			hibSchemaChange.setPreviousChange(previousChange.intoSchemaChange());
//			hibSchemaChange.setNextSchemaContainerVersion(getNextContainerVersion());
//			hibSchemaChange.setPreviousContainerVersion(getPreviousContainerVersion());
//			hibSchemaChange.setUuid(getUuid());
//			getRestProperties()
//					.entrySet().stream().forEach(entry -> hibSchemaChange.setRestProperty(entry.getKey(), entry.getValue()));
//		}		
		return schemaChange;
	}

	protected static final HibSchemaChangeImpl intoEntity(SchemaChange<?> schemaChange) {
		if (schemaChange == null) {
			return null;
		}
		HibernateTx tx = HibernateTx.get();
		// TODO make findByUuid a member of TxEntityPersistenceManager ?
		HibSchemaChangeImpl impl = tx.entityManager().find(HibSchemaChangeImpl.class, UUIDUtil.toJavaUuid(schemaChange.getUuid()));
		
		if (impl == null) {
			impl = tx.create(schemaChange.getUuid(), HibSchemaChangeImpl.class);
			impl.fromSchemaChange(schemaChange);
		}		
		return impl;
	}
	
	protected void fromSchemaChange(SchemaChange<?> schemaChange) {
		setNextSchemaContainerVersionInner(schemaChange.getNextContainerVersion());
		setPreviousContainerVersionInner(schemaChange.getPreviousContainerVersion());
		setOperation(schemaChange.getOperation());
		schemaChange.getRestProperties()
			.entrySet().stream()
			.forEach(entry -> setRestProperty(entry.getKey(), (RestModel) entry.getValue()));
	}

	@SuppressWarnings("unchecked")
	protected <R extends FieldSchemaVersionElement<?, ?, ?, ?, ?>> R findContainerVersion(UUID uuid) {
		if (uuid == null) {
			return null;
		}
		SchemaDaoImpl schemaDao = HibernateTx.get().schemaDao();
		MicroschemaDaoImpl microschemaDao = HibernateTx.get().microschemaDao();
		Schema schema = schemaDao.findByUuid(containerUuid);
		if (schema != null) {
			return (R) schemaDao.findVersionByUuid(schema, uuid);
		}
		Microschema microschema = microschemaDao.findByUuid(containerUuid);
		if (microschema != null) {
			return (R) microschemaDao.findVersionByUuid(microschema, uuid);
		}
		return null;
	}
}
