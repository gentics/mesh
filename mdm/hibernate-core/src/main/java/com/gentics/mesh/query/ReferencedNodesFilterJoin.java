package com.gentics.mesh.query;

import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeListFieldEdgeImpl;

import jakarta.persistence.criteria.JoinType;

/**
 * A JOIN for nodes that reference the current node.
 * 
 * @author plyhun
 *
 */
public class ReferencedNodesFilterJoin implements NativeFilterJoin {

	public static final String ALIAS_VALUE = "joined_value_";
	public static final String ALIAS_CONTENTFIELDKEY = "joined_fieldkey_";
	public static final String ALIAS_MICROCONTENTFIELDKEY = "joined_microfieldkey_";
	public static final String ALIAS_NODE_UUID = "joined_nodeUuid_";
	private static final String ALIAS_NODELISTITEM = "joined_nodelistitem_";
	private static final String ALIAS_NODEFIELDREF = "joined_nodefieldref_";
	private static final String ALIAS_NODELISTITEM_CONTAINER = "joined_nodelistitem_container_";
	private static final String ALIAS_NODEFIELDREF_CONTAINER = "joined_nodefieldref_container_";

	private static final String ALIAS_MICRONODELISTITEM_IN_REF = "joined_micronodelistitem_in_ref_";
	private static final String ALIAS_MICRONODEFIELDREF_IN_REF = "joined_micronodefieldref_in_ref_";
	private static final String ALIAS_MICRONODELISTITEM_EDGE_IN_REF = "joined_micronodelistitem_edge_in_ref_";
	private static final String ALIAS_MICRONODEFIELDREF_EDGE_IN_REF = "joined_micronodefieldref_edge_in_ref_";
	private static final String ALIAS_MICRONODELISTITEM_CONTAINER_IN_REF = "joined_micronodelistitem_container_in_ref_";
	private static final String ALIAS_MICRONODEFIELDREF_CONTAINER_IN_REF = "joined_micronodefieldref_container_in_ref_";
	private static final String ALIAS_MICRONODELISTITEM_IN_LIST = "joined_micronodelistitem_in_list_";
	private static final String ALIAS_MICRONODEFIELDREF_IN_LIST = "joined_micronodefieldref_in_list_";
	private static final String ALIAS_MICRONODELISTITEM_EDGE_IN_LIST = "joined_micronodelistitem_edge_in_list_";
	private static final String ALIAS_MICRONODEFIELDREF_EDGE_IN_LIST = "joined_micronodefieldref_edge_in_list";
	private static final String ALIAS_MICRONODELISTITEM_CONTAINER_IN_LIST = "joined_micronodelistitem_container_in_list_";
	private static final String ALIAS_MICRONODEFIELDREF_CONTAINER_IN_LIST = "joined_micronodefieldref_container_in_list_";

	private final String myAlias;
	private final JoinType joinType;
	private final String pkColumn;
	private final boolean lookupInContent;
	private final boolean lookupInMicronode;
	private final boolean lookupInFields;
	private final boolean lookupInLists;

	public ReferencedNodesFilterJoin(String myAlias, String pkColumn, JoinType joinType) {
		this(myAlias, pkColumn, joinType, true, true, true, true);
	}

	public ReferencedNodesFilterJoin(String myAlias, String pkColumn, JoinType joinType, boolean lookupInFields, boolean lookupInLists, boolean lookupInContent, boolean lookupInMicrocontent) {
		this.myAlias = myAlias;
		this.joinType = joinType;
		this.pkColumn = pkColumn;
		this.lookupInFields = lookupInFields;
		this.lookupInLists = lookupInLists;
		this.lookupInContent = lookupInContent;
		this.lookupInMicronode = lookupInMicrocontent;
	}

	@Override
	public String toSqlJoinString() {
		DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();

		String start = " " + joinType.name() + " JOIN ( ";
		StringBuilder sb = new StringBuilder();
		sb.append(start);

		if (lookupInContent) {
			if (lookupInFields) {
				// Node field reference
				sb.append(" SELECT ").append(ALIAS_NODEFIELDREF_CONTAINER).append(".").append(dc.renderNonContentColumn("node_dbUuid")).append(" AS ").append(ALIAS_NODE_UUID);
				sb.append(", ").append(ALIAS_NODEFIELDREF).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" AS ").append(ALIAS_VALUE);
				sb.append(", ").append(ALIAS_NODEFIELDREF).append(".").append(dc.renderNonContentColumn("fieldKey")).append(" AS ").append(ALIAS_CONTENTFIELDKEY);
				sb.append(" FROM ").append(dc.maybeGetPhysicalTableName(HibNodeFieldEdgeImpl.class).get()).append(" ").append(ALIAS_NODEFIELDREF);
				sb.append(" INNER JOIN ").append(dc.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get()).append(" ").append(ALIAS_NODEFIELDREF_CONTAINER)
					.append(" ON ( ");
				sb.append(ALIAS_NODEFIELDREF_CONTAINER).append(".").append(dc.renderNonContentColumn("contentUuid")).append(" = ").append(ALIAS_NODEFIELDREF).append(".").append(dc.renderNonContentColumn("containerUuid")).append(" ");
			}
			if (lookupInLists) {
				// StringBuilder has no endsWith()
				if (sb.length() > start.length()) {
					sb.append(" ) UNION ALL ");
				}
				// Node list reference
				sb.append(" SELECT DISTINCT ").append(ALIAS_NODELISTITEM_CONTAINER).append(".").append(dc.renderNonContentColumn("node_dbUuid")).append(" AS ").append(ALIAS_NODE_UUID);
				sb.append(", ").append(ALIAS_NODELISTITEM).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" AS ").append(ALIAS_VALUE);
				sb.append(", ").append(ALIAS_NODELISTITEM).append(".").append(dc.renderNonContentColumn("fieldKey")).append(" AS ").append(ALIAS_CONTENTFIELDKEY);
				sb.append(" FROM ").append(dc.maybeGetPhysicalTableName(HibNodeListFieldEdgeImpl.class).get()).append(" ").append(ALIAS_NODELISTITEM);
				sb.append(" LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get()).append(" ").append(ALIAS_NODELISTITEM_CONTAINER)
					.append(" ON ( ");
				sb.append(ALIAS_NODELISTITEM_CONTAINER).append(".").append(dc.renderNonContentColumn("contentUuid")).append(" = ").append(ALIAS_NODELISTITEM).append(".").append(dc.renderNonContentColumn("containerUuid")).append(" ");
			}
		}

		if (lookupInMicronode) {
			if (lookupInFields) {
				// StringBuilder has no endsWith()
				if (sb.length() > start.length()) {
					sb.append(" ) UNION ALL ");
				}
				// Micronode field-in-node reference
				sb.append(" SELECT DISTINCT ").append(ALIAS_MICRONODEFIELDREF_CONTAINER_IN_REF).append(".").append(dc.renderNonContentColumn("node_dbUuid")).append(" AS ").append(ALIAS_NODE_UUID);
				sb.append(", ").append(ALIAS_MICRONODEFIELDREF_IN_REF).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" AS ").append(ALIAS_VALUE);
				sb.append(", ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_REF).append(".").append(dc.renderNonContentColumn("fieldKey")).append(" AS ").append(ALIAS_MICROCONTENTFIELDKEY);
				sb.append(" FROM ").append(dc.maybeGetPhysicalTableName(HibNodeFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODEFIELDREF_IN_REF);
				sb.append(" LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibMicronodeFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_REF)
					.append(" ON ( ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_REF).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" = ").append(ALIAS_MICRONODEFIELDREF_IN_REF).append(".").append(dc.renderNonContentColumn("containerUuid"));
				sb.append(" ) LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODEFIELDREF_CONTAINER_IN_REF)
					.append(" ON ( ");
				sb.append(ALIAS_MICRONODEFIELDREF_CONTAINER_IN_REF).append(".").append(dc.renderNonContentColumn("contentUuid")).append(" = ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_REF).append(".").append(dc.renderNonContentColumn("containerUuid")).append(" ");
			}
			if (lookupInLists) {
				// StringBuilder has no endsWith()
				if (sb.length() > start.length()) {
					sb.append(" ) UNION ALL ");
				}
				// Micronode field-in-list reference
				sb.append(" SELECT DISTINCT ").append(ALIAS_MICRONODEFIELDREF_CONTAINER_IN_LIST).append(".").append(dc.renderNonContentColumn("node_dbUuid")).append(" AS ").append(ALIAS_NODE_UUID);
				sb.append(", ").append(ALIAS_MICRONODEFIELDREF_IN_LIST).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" AS ").append(ALIAS_VALUE);
				sb.append(", ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_LIST).append(".").append(dc.renderNonContentColumn("fieldKey")).append(" AS ").append(ALIAS_MICROCONTENTFIELDKEY);
				sb.append(" FROM ").append(dc.maybeGetPhysicalTableName(HibNodeListFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODEFIELDREF_IN_LIST);
				sb.append(" LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibMicronodeFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_LIST)
					.append(" ON ( ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_LIST).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" = ").append(ALIAS_MICRONODEFIELDREF_IN_LIST).append(".").append(dc.renderNonContentColumn("containerUuid"));
				sb.append(" ) LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODEFIELDREF_CONTAINER_IN_LIST)
					.append(" ON ( ");
				sb.append(ALIAS_MICRONODEFIELDREF_CONTAINER_IN_LIST).append(".").append(dc.renderNonContentColumn("contentUuid")).append(" = ").append(ALIAS_MICRONODEFIELDREF_EDGE_IN_LIST).append(".").append(dc.renderNonContentColumn("containerUuid")).append(" ");
				sb.append(" ) UNION ALL ");

				// Micronode list-in-node reference
				sb.append(" SELECT DISTINCT ").append(ALIAS_MICRONODELISTITEM_CONTAINER_IN_REF).append(".").append(dc.renderNonContentColumn("node_dbUuid")).append(" AS ").append(ALIAS_NODE_UUID);
				sb.append(", ").append(ALIAS_MICRONODELISTITEM_IN_REF).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" AS ").append(ALIAS_VALUE);
				sb.append(", ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_REF).append(".").append(dc.renderNonContentColumn("fieldKey")).append(" AS ").append(ALIAS_MICROCONTENTFIELDKEY);
				sb.append(" FROM ").append(dc.maybeGetPhysicalTableName(HibNodeFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODELISTITEM_IN_REF);
				sb.append(" LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibMicronodeListFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_REF)
					.append(" ON ( ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_REF).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" = ").append(ALIAS_MICRONODELISTITEM_IN_REF).append(".").append(dc.renderNonContentColumn("containerUuid"));
				sb.append(" ) LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODELISTITEM_CONTAINER_IN_REF)
					.append(" ON ( ");
				sb.append(ALIAS_MICRONODELISTITEM_CONTAINER_IN_REF).append(".").append(dc.renderNonContentColumn("contentUuid")).append(" = ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_REF).append(".").append(dc.renderNonContentColumn("containerUuid")).append(" ");		
				sb.append(" ) UNION ALL ");

				// Micronode list-in-list reference
				sb.append(" SELECT DISTINCT ").append(ALIAS_MICRONODELISTITEM_CONTAINER_IN_LIST).append(".").append(dc.renderNonContentColumn("node_dbUuid")).append(" AS ").append(ALIAS_NODE_UUID);
				sb.append(", ").append(ALIAS_MICRONODELISTITEM_IN_LIST).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" AS ").append(ALIAS_VALUE);
				sb.append(", ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_LIST).append(".").append(dc.renderNonContentColumn("fieldKey")).append(" AS ").append(ALIAS_MICROCONTENTFIELDKEY);
				sb.append(" FROM ").append(dc.maybeGetPhysicalTableName(HibNodeListFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODELISTITEM_IN_LIST);
				sb.append(" LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibMicronodeListFieldEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_LIST)
					.append(" ON ( ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_LIST).append(".").append(dc.renderNonContentColumn("valueOrUuid")).append(" = ").append(ALIAS_MICRONODELISTITEM_IN_LIST).append(".").append(dc.renderNonContentColumn("containerUuid"));
				sb.append(" ) LEFT JOIN ").append(dc.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get()).append(" ").append(ALIAS_MICRONODELISTITEM_CONTAINER_IN_LIST)
					.append(" ON ( ");
				sb.append(ALIAS_MICRONODELISTITEM_CONTAINER_IN_LIST).append(".").append(dc.renderNonContentColumn("contentUuid")).append(" = ").append(ALIAS_MICRONODELISTITEM_EDGE_IN_LIST).append(".").append(dc.renderNonContentColumn("containerUuid")).append(" ");		
			}
		}

		sb.append(" ) ) ").append(myAlias)/*.append(makeAlias(ElementType.NODE))*/;
		sb.append(" ON ( ");
		sb.append(myAlias)/*.append(makeAlias(ElementType.NODE))*/.append(".").append(ALIAS_VALUE).append(" = ").append(pkColumn);
		sb.append(" ) ");
		//sb.append(joinType).append(dc.maybeGetPhysicalTableName(HibNodeImpl.class).get()).append(" ").append(myAlias).append(" ON ").append(myAlias).append(makeAlias(ElementType.NODE)).append(".").append(ALIAS_NODE_UUID).append(" = ").append(myAlias).append(".").append(dc.renderNonContentColumn("dbUuid"));
		return sb.toString();
	}

}
