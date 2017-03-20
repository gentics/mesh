function migrate(node, fieldname, convert) {
	node.fields[fieldname] = Java.from(convert.toBooleanList(node.fields[fieldname]));
	return node;
}
