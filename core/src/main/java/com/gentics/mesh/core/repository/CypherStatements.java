package com.gentics.mesh.core.repository;

public final class CypherStatements {

	public final static String MATCH_PERMISSION_ON_NODE = "MATCH (requestUser:User)-[:MEMBER_OF]->(:Group)<-[:HAS_ROLE]-(:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	public final static String MATCH_PERMISSION_ON_TAG = "MATCH (requestUser:User)-[:MEMBER_OF]->(:Group)<-[:HAS_ROLE]-(:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	public final static String MATCH_PERMISSION_ON_GROUP = "MATCH (requestUser:User)-[:MEMBER_OF]->(:Group)<-[:HAS_ROLE]-(:Role)-[perm:HAS_PERMISSION]->(group:Group) ";
	public final static String MATCH_PERMISSION_ON_ROLE = "MATCH (requestUser:User)-[:MEMBER_OF]->(:Group)<-[:HAS_ROLE]-(:Role)-[perm:HAS_PERMISSION]->(role:Role) ";
	public final static String MATCH_PERMISSION_ON_OBJECT_SCHEMA = "MATCH (requestUser:User)-[:MEMBER_OF]->(:Group)<-[:HAS_ROLE]-(:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) ";
	public final static String MATCH_PERMISSION_ON_USER = "MATCH (requestUser:User)-[:MEMBER_OF]->(:Group)<-[:HAS_ROLE]-(:Role)-[perm:HAS_PERMISSION]->(user:User) ";

	public final static String FILTER_USER_PERM_AND_PROJECT = " requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} ";
	public final static String FILTER_USER_PERM = " requestUser.uuid = {0} AND perm.`permissions-read` = true ";

	public final static String ORDER_BY_NAME_DESC = " ORDER by p.`properties-name` desc ";

	public final static String MATCH_NODE_OF_PROJECT = " MATCH (node:MeshNode)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
}
