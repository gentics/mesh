function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toMicronode(node.fields[fieldname]);
	return node;
}
