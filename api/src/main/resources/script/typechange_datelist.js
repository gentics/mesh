function migrate(node, fieldname, convert) {
	node.fields[fieldname] = Java.from(convert.toDateList(node.fields[fieldname]));
	return node;
}
