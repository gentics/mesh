function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toBoolean(node.fields[fieldname]);
	return node;
}
