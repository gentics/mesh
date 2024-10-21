package com.gentics.mesh.hibernate.dialect;

import static org.hibernate.type.SqlTypes.UUID;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.ColumnAliasExtractor;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import com.gentics.mesh.hibernate.TableAwareColumnAliasExtractor;

/**
 * An extension for existing Hibernate dialect for MariaDB 10.3+, considering using UUIDs as binary SQL type of 16 bytes length. 
 * 
 * @author plyhun
 *
 */
public class MariaDBBinaryUuidDialect extends MariaDBDialect {

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "binary(16)", this ) );
	}

	@Override
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return TableAwareColumnAliasExtractor.INSTANCE;
	}
}
