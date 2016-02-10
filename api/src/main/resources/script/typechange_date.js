function migrate(node, fieldname, convert) {
	node.fields[fieldname] = convert.toDate(node.fields[fieldname]);
	return node;
}
