function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toNode(node.fields[fieldname]);
	return node;
}
