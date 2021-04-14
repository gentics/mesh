function sleep(ms) {
	var start = new Date().getTime(), expire = start + ms;
	while (new Date().getTime() < expire) {
	}
	return;
}

function migrate(node, fieldname, convert) {
	if (node.fields['content']== "triggerWait") {
		sleep(10000); // Wait 1s
	}
	node.fields['content'] = false;
	return node;
}