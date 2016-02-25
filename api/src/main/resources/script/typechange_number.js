function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toNumber(node.fields[fieldname]);
	return node;
}
