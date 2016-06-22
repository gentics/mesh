describe('mesh login test', function() {
	it('should be able to login', function() {
		browser.get('http://' + browser.params.meshHost + ':'	+ browser.params.meshPort + '/mesh-ui');

		element(by.model('vm.userName')).sendKeys('admin');
		element(by.model('vm.password')).sendKeys('admin');
		element(by.model('vm.password')).sendKeys('\n');

		var projectList = element.all(by.repeater('project in vm.projects'));
		expect(projectList.count()).toEqual(1);
		expect(projectList.get(0).getText()).toEqual('demo');
	});
});
