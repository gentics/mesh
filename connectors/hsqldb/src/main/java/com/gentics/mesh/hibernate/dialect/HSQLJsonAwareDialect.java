package com.gentics.mesh.hibernate.dialect;

import static org.hibernate.type.SqlTypes.JSON;

import java.sql.Types;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import com.gentics.mesh.database.connector.QueryUtils;

public class HSQLJsonAwareDialect extends HSQLDialect {

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "varchar(" + QueryUtils.DEFAULT_STRING_LENGTH + ")", this ) );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptorIfAbsent( SqlTypes.JSON, JsonJdbcType.INSTANCE );
		super.contributeTypes( typeContributions, serviceRegistry );
	}

	@Override
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return typeCode1 == Types.LONGVARCHAR && typeCode2 == SqlTypes.JSON
			|| typeCode1 == SqlTypes.JSON && typeCode2 == Types.LONGVARCHAR
			|| super.equivalentTypes( typeCode1, typeCode2 );
	}
}
