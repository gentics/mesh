module CLAdminUi {
	/**
	 * determine the correct input type for a key/val combination
	 */
	function inputType (key, val) {
		if (key === 'id' || key === 'uuid') {
			return 'readonly';
		}
		if (key === 'date') {
			return 'date';
		}
		if (typeof val === 'string') {
			if (val.length > 100) {
				return 'html';
			}
		}
		return 'text';
	}

	angular.module('meshAdminInterface', ['ngRoute'])
		.config(['$routeProvider', function ($routeProvider) {
			$routeProvider.
				when('/list/', {
					templateUrl: 'src/main/html/object-list.html',
					controller: 'ObjectListCtrl'
				}).
				when('/edit/:uuid', {
					templateUrl: 'src/main/html/object-editor.html',
					controller: 'ObjectEditorCtrl'
				});
		}])
		.controller('ObjectListCtrl', function ($scope) {
			$scope.objs = [
				{ id: 27, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'Weve been busy' },
				{ id: 34, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'Test' },
				{ id: 23, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'News Update' },
				{ id: 543, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'Darkest Dungeon' },
				{ id: 89, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'Besiege' },
				{ id: 3, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'GTA V PC' },
				{ id: 24, uuid: 'd4271cccb1d911e4bcf005e1f67ef2c8', name: 'Autumn Leaves' }
			];
		})
		.controller('ObjectEditorCtrl', ['$scope', '$routeParams', function ($scope, $params) {
			$scope.obj = {
				'id' : 27,
				'uuid' : 'd4271cccb1d911e4bcf005e1f67ef2c8',
				'name' : 'We\'ve been busy...',
				'filename' : 'weve-been-busy.html',
				'content' : '<p>Here come our first breaking changes of 2015! Yes, we\'ve been busy.</p><p>				We\'re releasing API improvements that are summed up in the				introduction of <a href="/api/ui.html">aloha.ui</a> as a core				API module. Along with these changes, we\'ve also freshened-up				the design of the website and created <a href="/api/ui.html">new guides</a>				to help you better along. We hope you\'re inspired!			</p><blockquote>				 A good library should not dictate application-level wiring			</blockquote><p>			</p><p>				The goal of these efforts was to provide a better API for wiring				UI with Aloha Editor. Our vision of what an editing library				should be is that a good library should not dictate				application-level wiring. As developers who work in increasingly				complex UI applications, you\'ll most probably have had the pain				of dealing with libraries that assume an architectural				convention or force a design on your system. The result of this				assumption is often as many new problems being introduced by				the library as were solved by it.			</p>',
				'teaser' : 'Introducing aloha.ui',
				'title' : 'We\'ve been busy...',
				'date' : new Date(),
				'author' : {
					'name' : 'Petro Salema'
				}
			};

			$scope.types = {};
			for (var key in $scope.obj) {
				$scope.types[key] = inputType(key, $scope.obj[key]);
			}
		}]);
}
