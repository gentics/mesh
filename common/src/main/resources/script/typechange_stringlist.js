function migrate(node, fieldname, convert) {
	node.fields[fieldname] = Java.from(convert.toStringList(node.fields[fieldname]));
	return node;
}
