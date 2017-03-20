function migrate(node, fieldname, convert) {
	delete node.fields[fieldname];
	return node;
}
