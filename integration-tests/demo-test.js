describe('mesh demo test', function() {
	it('should be list the demo', function() {
		browser.get('http://' + browser.params.meshHost + ':' + browser.params.meshPort + '/demo');

	});
});
