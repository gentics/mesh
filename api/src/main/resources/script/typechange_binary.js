function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toBinary(node.fields[fieldname]);
	return node;
}
