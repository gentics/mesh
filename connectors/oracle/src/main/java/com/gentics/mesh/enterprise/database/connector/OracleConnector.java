package com.gentics.mesh.enterprise.database.connector;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.OracleLongTextDialect;
import com.gentics.mesh.enterprise.server.license.LicenseService;

import oracle.jdbc.driver.OracleDriver;

/**
 * Oracle database connector for Gentics Mesh
 */
public class OracleConnector extends AbstractDatabaseConnector<OracleDriver, OracleLongTextDialect> {

	public OracleConnector(EnterpriseMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("Oracle database connector requires a valid license!");
		}
	}

	@Override
	public Class<OracleDriver> getJdbcDriverClass() {
		return OracleDriver.class;
	}

	@Override
	public Class<OracleLongTextDialect> getHibernateDialectClass() {
		return OracleLongTextDialect.class;
	}

	@Override
	public String installCount(String sqlQuery) {
		return "SELECT COUNT(1) FROM ( " + sqlQuery + " ) ";
	}

	@Override
	public String installListContentColumn(String listItemType, String listAlias, String operator, String paramName) {
		if ("string".equalsIgnoreCase(listItemType) || "html".equalsIgnoreCase(listItemType)) {
			return unblobStringContentColumn(listAlias + "." + renderNonContentColumn("valueOrUuid"), false, false) + " " + operator + " " + ":" + paramName;
		} else {
			return super.installListContentColumn(listItemType, listAlias, operator, paramName);
		}
	}

	@Override
	public String installStringContentColumn(String columnName, boolean makeAlias, boolean customAlias) {
		if (columnName.endsWith("-string\"") || columnName.endsWith("-html\"")) {
			return unblobStringContentColumn(columnName, makeAlias, customAlias);
		} else {
			return super.installStringContentColumn(columnName, makeAlias, customAlias);
		}
	}

	@Override
	public String findSortAlias(String columnDef) {
		return columnDef.indexOf(" AS ") > -1 ? columnDef.substring(columnDef.indexOf(" AS ") + 4) : super.findSortAlias(columnDef);
	}

	@Override
	public String makeSortDefinition(String column, Optional<String> customSortAlias) {
		return customSortAlias.orElseGet(() -> {
				String[] parts = column.split(" AS ");
				if (parts.length == 2) {
					return " MAX(" + parts[0] + ") AS " + makeSortAlias(parts[1]);
				} else {
					return " MAX(" + column + ") AS " + makeSortAlias(column);
				}
		});
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 1000;
	}

	@Override
	public String renderNonContentColumn(String column) {
		return column;
	}

	protected String unblobStringContentColumn(String columnName, boolean makeAlias, boolean customAlias) {
		return " TO_CHAR(" + columnName + ") " + (makeAlias ? ("AS " + columnName.substring(columnName.indexOf(".") + 1, columnName.length() - 1) + (customAlias ? Integer.toHexString(columnName.hashCode()) : StringUtils.EMPTY) + "\"") : StringUtils.EMPTY);
	}
}
