function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toString(node.fields[fieldname]);
	return node;
}
