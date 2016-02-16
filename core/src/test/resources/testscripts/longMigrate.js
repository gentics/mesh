function sleep(ms) {
	var start = new Date().getTime(), expire = start + ms;
	while (new Date().getTime() < expire) {
	}
	return;
}

function migrate(node, fieldname, convert) {
	sleep(10000); // Wait 10s
}