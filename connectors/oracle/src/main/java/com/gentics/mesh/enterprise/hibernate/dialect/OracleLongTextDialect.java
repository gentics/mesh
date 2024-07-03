package com.gentics.mesh.enterprise.hibernate.dialect;

import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.LONGNVARCHAR;
import static org.hibernate.type.SqlTypes.LONGVARCHAR;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * The overridden Oracle Hibernate dialect, forcefully replacing the long character sequence types with NCLOB.
 * 
 * @author plyhun
 *
 */
public class OracleLongTextDialect extends OracleDialect {

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);

		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONGVARCHAR, "nclob", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONGNVARCHAR, "nclob", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( DOUBLE, "binary_double", this ) );
	}
}
