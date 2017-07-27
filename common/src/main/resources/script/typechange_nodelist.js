function migrate(node, fieldname, convert) {
	node.fields[fieldname] = Java.from(convert.toNodeList(node.fields[fieldname]));
	return node;
}
