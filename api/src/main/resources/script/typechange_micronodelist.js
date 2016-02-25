function migrate(node, fieldname, convert) {
	node.fields[fieldname] = Java.from(convert.toMicronodeList(node.fields[fieldname]));
	return node;
}
