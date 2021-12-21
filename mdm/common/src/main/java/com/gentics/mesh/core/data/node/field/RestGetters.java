package com.gentics.mesh.core.data.node.field;

public class RestGetters {

	public static FieldGetter STRING_GETTER = (container, fieldSchema) -> container.getString(fieldSchema.getName());

	public static FieldGetter STRING_LIST_GETTER = (container, fieldSchema) -> container.getStringList(fieldSchema.getName());

	public static FieldGetter NUMBER_GETTER = (container, fieldSchema) -> container.getNumber(fieldSchema.getName());

	public static FieldGetter NUMBER_LIST_GETTER = (container, fieldSchema) -> container.getNumberList(fieldSchema.getName());

	public static FieldGetter DATE_GETTER = (container, fieldSchema) -> container.getDate(fieldSchema.getName());

	public static FieldGetter DATE_LIST_GETTER = (container, fieldSchema) -> container.getDateList(fieldSchema.getName());

	public static FieldGetter BOOLEAN_GETTER = (container, fieldSchema) -> container.getBoolean(fieldSchema.getName());

	public static FieldGetter BOOLEAN_LIST_GETTER = (container, fieldSchema) -> container.getBooleanList(fieldSchema.getName());

	public static FieldGetter HTML_GETTER = (container, fieldSchema) -> container.getHtml(fieldSchema.getName());

	public static FieldGetter HTML_LIST_GETTER = (container, fieldSchema) -> container.getHTMLList(fieldSchema.getName());

	public static FieldGetter MICRONODE_GETTER = (container, fieldSchema) -> container.getMicronode(fieldSchema.getName());

	public static FieldGetter MICRONODE_LIST_GETTER = (container, fieldSchema) -> container.getMicronodeList(fieldSchema.getName());

	public static FieldGetter NODE_GETTER = (container, fieldSchema) -> container.getNode(fieldSchema.getName());

	public static FieldGetter NODE_LIST_GETTER = (container, fieldSchema) -> container.getNodeList(fieldSchema.getName());

	public static FieldGetter BINARY_GETTER = (container, fieldSchema) -> container.getBinary(fieldSchema.getName());

	public static FieldGetter S3_BINARY_GETTER = (container, fieldSchema) -> container.getS3Binary(fieldSchema.getName());
}
