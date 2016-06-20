describe('mesh demo test', function() {
  it('should be list the demo', function() {
    browser.get('http://' + browser.params.dockerHost + ':' + browser.params.dockerPort + '/demo');

    /*
    element(by.model('vm.userName')).sendKeys('admin');
    element(by.model('vm.password')).sendKeys('admin');
    element(by.model('vm.password')).sendKeys('\n');

    var projectList = element.all(by.repeater('project in vm.projects'));
    expect(projectList.count()).toEqual(1);
    expect(projectList.get(0).getText()).toEqual('demo');
    */
  });
});
