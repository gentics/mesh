function migrate(node, fieldname, convert) {
	node.fields[fieldname] = Java.from(convert.toNumberList(node.fields[fieldname]));
	return node;
}
