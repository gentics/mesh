$.fn.exists = function () {
	return this.length !== 0;
}

var GCMHTML;
$(document).ready(function () {

	var that = this;

	GCMHTML = {

		bannerCookieName: 'gcmdata_',

		isBannerCookieGCMCookie: true,

		getXPath: function (element) {
			$element = $(element);
			var selector = $element
				.parents()
				.map(function () {
					var returnValue = this.tagName.toLowerCase();
					var id = $(this).attr('id');

					if (typeof id != 'undefined' && id.trim().length > 0) {
						returnValue = '#' + id;
					}

					return returnValue;
				})
				.get()
				.reverse()
				.concat([this.nodeName])
				.join(">");

			var id = $element.attr("id");
			if (id) {
				selector += "#" + id;
			}

			var classNames = $(element).attr("class");

			if (classNames && classNames.length > 0) {
				selector += "." + $.trim(classNames).replace(/\s/gi, ".");
			} else {
				return '';
			}

			return selector;
		},

		setCookie: function (cookieName, cookieValue, expDays) {
			var d = new Date();
			d.setTime(d.getTime() + (expDays * 24 * 60 * 60 * 1000));
			var expires = "expires=" + d.toUTCString();
			var path = "path=/";
			document.cookie = cookieName + "=" + cookieValue + "; " + expires + "; " + path;
		},
		deleteCookie: function (cookieName) {
			GCMHTML.setCookie(cookieName, ' ', 0);
		},

		isCookiesAllowed: function () {
			var cookies = document.cookie;

			if (typeof cookies === 'undefined' || cookies === null) {
				return false;
			}
			return (cookies.indexOf(this.bannerCookieName) >= 0);
		},

		applications: [],

		groups: [],

		bannerText: 'Wir ersuchen um Ihre Zustimmung für Cookies. Wir setzen technisch notwendige, funktionelle und ' +
			'Marketing-Cookies ein. Marketing-Cookies werden erst mit Ihrer Zustimmung und ausschließlich für ' +
			'statistische Zwecke verwendet, unsere Websites sind werbefrei. Bis auf die technisch notwendigen sind ' +
			'alle Cookies zu Beginn deaktiviert. Nähere Informationen finden Sie in unserer ' +
			'<a class="apa-consent-reset" href="https://www.apa.at/Site/datenschutz/datenschutzerklaerung.html" target="_blank">Datenschutzerklärung</a>.' +
			' Eine detaillierte Übersicht der Cookies finden Sie in den ' +
			'<a class="apa-consent-reset apa-consent-linkDetails" href="javascript:void(0)">Cookie-Präferenzen</a>.',

		bannerTextEN: 'We request your consent for cookies. We use technically necessary, functional and marketing ' +
			'cookies. Marketing cookies are only used with your consent and exclusively for statistical purposes; ' +
			'our websites are free of advertisement. Except for the technically necessary cookies, all cookies are ' +
			'disabled from the outset. Further information can be found in our privacy statement to be found at' +
			' <a class="apa-consent-reset"  href="https://www.apa.at/privacypolicy" target="_blank">privacy policy</a>. ' +
			'A detailed overview of the cookies can be found in the cookie preferences.',

		hideClass: 'testOFF',
		bannerWrapper: $('.apa-consent-wrapper')[0],
		banner: $('.apa-consent-banner')[0],
		bannerButtonAccept: $('.apa-consent-buttonWrapperAccept')[0],
		bannerButtonOpen: $('.apa-consent-bannerButtonON')[0],

		/**
		 * prefDialogRoundButtonOpen is inside apa-consent-wrapper, which is the banner wrapper
		 */
		prefDialogRoundButtonOpen: $('.apa-consent-bannerButtonOFF')[0],

		hidePrefDialogRoundButtonOpen: function () {
			$(this.prefDialogRoundButtonOpen).addClass(this.hideClass);
		},
		prefDialog: $('.apa-consent-dialogWrapper')[0],

		prefDialogButtonSaveSettings: $('.apa-consent-dialogSubmit')[0],

		/**
		 * prefDialogButtonOpen & prefDialogButtonDetails have the same function...
		 */
		prefDialogButtonOpen: $('.apa-consent-dialogButtonON')[0],

		// two buttons!
		prefDialogButtonDetails: $('.apa-consent-buttonWrapperDetails')[0],

		prefDialogButtonClose: $('.apa-consent-dialogButtonOFF')[0],

		hidePrefDialogButtonClose: function () {
			$(this.prefDialogButtonClose).addClass(this.hideClass);
		},

		showPrefDialogButtonClose: function () {
			$(this.prefDialogButtonClose).removeClass(this.hideClass);
		},

		prefDialogButtonMobileClose: $('.apa-consent-mobileClose')[0],

		showOrHideBanner: function () {

			if (this.isCookiesAllowed()) {
				this.hideBanner(true);
				this.showPrefDialogButtonOpen();
				return false;
			} else {
				this.showBanner();
				return true;
			}
		},
		hideBanner: function (prefDialogMode) {

			if (typeof prefDialogMode === 'undefined' || prefDialogMode === null) {
				prefDialogMode = false;
			}

			$(this.banner).addClass(GCMHTML.hideClass);

			// bannerWrapper needs to be off, cause it would overlap a button to open prefDialog
			$(this.bannerWrapper).addClass(GCMHTML.hideClass);

			if (prefDialogMode) {
				$(this.bannerButtonOpen).addClass(GCMHTML.hideClass);
				$(this.prefDialogButtonOpen).removeClass(GCMHTML.hideClass);

			} else {
				$(this.bannerButtonOpen).removeClass(GCMHTML.hideClass);
				$(this.prefDialogButtonOpen).addClass(GCMHTML.hideClass);
			}
		},
		showBanner: function () {
			$(this.bannerWrapper).removeClass(GCMHTML.hideClass);
			$(this.banner).removeClass(GCMHTML.hideClass);
			$(this.bannerButtonOpen).addClass(GCMHTML.hideClass)
			$(this.bannerButtonOpen).addClass(GCMHTML.hideClass)
		},

		hideBannerButtonOpen: function () {
			$(this.bannerButtonOpen).removeClass(GCMHTML.hideClass);
			$(this.prefDialogButtonOpen).addClass(GCMHTML.hideClass);
		},

		showPrefDialogButtonOpen: function () {
			$(this.bannerButtonOpen).addClass(GCMHTML.hideClass);
			$(this.prefDialogButtonOpen).removeClass(GCMHTML.hideClass);
		},

		showPrefDialog: function () {
			// this.hideBanner(true);

			// we need to display bannerWrapper, cause it gets turned of by previouse method hideBanner...
			$(this.bannerWrapper).removeClass(GCMHTML.hideClass);

			$(this.prefDialog).removeClass(GCMHTML.hideClass);
			$(this.prefDialogButtonOpen).addClass(GCMHTML.hideClass);

			GCMHTML.showPrefDialogButtonClose();

			//GCMHTML.showPrefDialogButtonOpen()
			// GCMHTML.hidePrefDialogRoundButtonOpen();
		},
		hidePrefDialog: function () {
			$(this.bannerWrapper).addClass(GCMHTML.hideClass);
			$(this.prefDialog).addClass(GCMHTML.hideClass);

			GCMHTML.hidePrefDialogButtonClose();

			if (GCMHTML.isCookiesAllowed()) {
				$(this.bannerButtonOpen).addClass(GCMHTML.hideClass);
				$(this.prefDialogButtonOpen).removeClass(GCMHTML.hideClass);
			} else {
				$(this.prefDialogButtonOpen).addClass(GCMHTML.hideClass);
				this.showBanner();
			}

		},
		eventButtonAccept: function () {
			$(document).on('click', this.getXPath(this.bannerButtonAccept), function (event) {

				if (!GCMHTML.isBannerCookieGCMCookie) {
					GCMHTML.setCookie(GCMHTML.bannerCookieName, 'true', 365);
				}


				var groupKeys = Object.keys(window.GCM.groups.getList());

				groupKeys.forEach(function (group, index) {
					window.GCM.groups.set(group, true);

				});

				window.GCM.settings.save();

				GCMHTML.hideBanner(true);
			});
		},
		eventButtonSaveSettings: function () {
			$(this.prefDialogButtonSaveSettings).click(function (event) {

				if (!GCMHTML.isBannerCookieGCMCookie) {
					GCMHTML.setCookie(GCMHTML.bannerCookieName, 'true', 365);
				}

				GCMHTML.hideBanner();
				GCMHTML.hideBannerButtonOpen();
				GCMHTML.showPrefDialogButtonOpen();

				/* TODO CHECK FOLLOWING LINES
				GCMHTML.groups.forEach(function (group) {

					// todo auf group.protected prüfen
					if (!group.protected && GCMHTML.isGroupActive(group)) {

						var keys = Object.keys(group.gcmGroup.apps);

						keys.forEach(function (app) {

							var application = group.gcmGroup.apps[app];

							console.log(application);
							console.log(app + '_' + application.value);

							window.GCM.apps.setAndSave(app, application.value);

						});

						window.GCM.groups.setAndSave(group.groupid, GCMHTML.isGroupActive(group));
					} else if (!group.protected) {
						// BUG setAndSave delete cookies from all groups...
						// window.GCM.groups.setAndSave(group.groupid, GCMHTML.isGroupActive(group));
						// window.GCM.groups.set(group.groupid, false);

						var appKeys = Object.keys(group.gcmGroup.apps);

						appKeys.forEach(function (app, index) {

							var appObject = group.gcmGroup.apps[app];

							appObject.value = false;

							window.GCM.apps.setAndSave(appObject.appid, false);
						});
					}


					if (document.cookie.indexOf('cookiesAllowed=true') == -1) {
						GCMHTML.setCookie("cookiesAllowed", 'true', 365);
					}

				});
				*/

				window.GCM.settings.save();

				GCMHTML.hidePrefDialog();
			});
		},
		addGroup: function (groupid, gcmGroup, htmlGroup) {
			var group = {
				groupid: groupid,
				gcmGroup: gcmGroup,
				htmlGroup: htmlGroup
			}

			this.groups.push(group);
			return group;
		},
		isGroupActive: function (group) {
			var result = false;

			$.each(group.gcmGroup.apps, function (index, app) {
				if (app.value) {
					result = true;
				}
			});

			return result;
		},

		activateGroup: function (group) {
			// activate all application cookies...
			GCMHTML.activateCookiesByGroup(group);

			//updateApplication(application);
			//window.GCM.apps.set(application.appid, true);
			group.gcmGroup.value = true;
			window.GCM.groups.set(group.groupid, true);
		},
		deactivateAllGroups: function () {
			GCMHTML.groups.forEach(function (group) {
				if (!group.gcmGroup.protected) {
					GCMHTML.deactivateGroup(group);
				}
			});
		},

		deactivateGroup: function (group) {
			// deactivate all application cookies...
			GCMHTML.deactivateCookiesByGroup(group);

			// deactivate all cookies ?
			// window.GCM.apps.set(application.appid, false);

			window.GCM.groups.set(group.groupid, false);
		},
		loadGroup: function (group) {
			GCMHTML.renderGroupHTMLFields(group);
			GCMHTML.initGroupToggleSwitch(group);
		},
		activateGroupSwitchOnly: function (group) {

			var consentSwitch = $(document).find('#' + group.gcmGroup.htmlid + ' .apa-consent-itemSwitch')[0];

			$consentSwitch = $(consentSwitch);

			$consentSwitch.addClass('apa-consent-itemSwitchON');
			$consentSwitch.removeClass('apa-consent-itemSwitchOFF');
		},
		activateGroupSwitch: function (group) {

			var consentSwitch = $(document).find('#' + group.gcmGroup.htmlid + ' .apa-consent-itemSwitch')[0];

			$consentSwitch = $(consentSwitch);

			$consentSwitch.addClass('apa-consent-itemSwitchON');
			$consentSwitch.removeClass('apa-consent-itemSwitchOFF');


			GCMHTML.activateGroup(group);
		},
		deactivateGroupSwitchOnly: function (group) {

			var consentSwitch = $('#' + group.gcmGroup.htmlid).find('.apa-consent-itemSwitch')[0];

			$consentSwitch = $(consentSwitch);

			$consentSwitch.removeClass('apa-consent-itemSwitchON');
			$consentSwitch.addClass('apa-consent-itemSwitchOFF');
		},
		deactivateGroupSwitch: function (group) {

			$consentSwitch = $('#' + group.gcmGroup.htmlid).find('.apa-consent-itemSwitch');

			$consentSwitch.removeClass('apa-consent-itemSwitchON');
			$consentSwitch.addClass('apa-consent-itemSwitchOFF');

			GCMHTML.deactivateGroup(group);
		},
		toggleGroupSwitch: function (group) {
			if (group.gcmGroup.protected) {
				return;
			} else if (!GCMHTML.isGroupActive(group)) {
				GCMHTML.activateGroupSwitch(group);
			} else {
				GCMHTML.deactivateGroupSwitch(group);
			}
		},
		initGroupToggleSwitch: function (group) {

			if (group.protected) {
				return;
			}

			/*
			if (typeof group.gcmGroup.value === 'undefined' || group.gcmGroup.value === null
				|| group.gcmGroup.value || this.isGroupActive(group)) {
				GCMHTML.activateGroupSwitch(group);
			} else {
				GCMHTML.deactivateGroupSwitch(group);
			}
			*/

			$(document).on('click', '#' + group.gcmGroup.htmlid + ' .apa-consent-itemSwitch',
				function (event) {

					/*
						const applications = group.gcmGroup.apps;

						$.each(applications, function (index, application) {

							if(application.value) {

								// at least an application is active, so deactivate all cookies.

								return false;
							}
						});
					*/

					GCMHTML.toggleGroupSwitch(group);

					var appList = window.GCM.apps.getList();
					// TODO remove hardcoded access...
					if (!appList['gcm'] && group.gcmGroup.value) {

						var groups = GCMHTML.groups;

						$.each(groups, function (index, funcGroup) {
							if (funcGroup.groupid === 'functional') {
								// activate gcm
								var appKeys = Object.keys(appList);
								$.each(appKeys, function (index, app) {

									var appObject = funcGroup.gcmGroup.apps['gcm'];

									if (app === 'gcm') {

										console.log(appObject);
										GCMHTML.activateCookiesByApplication(funcGroup, appObject);
									}
								});
							}
						});
					}
				});
		},
		initGroupCookieToggleSwitch: function (group, application, gcmCookieName, gcmCookie, htmlCookie) {

			if (group.protected) {
				// no click event intendend.
				return;
			}

			var gcmGroup = group.gcmGroup;
			var htmlGroup = group.htmlGroup;


			$htmlGroup = $(htmlGroup);
			$htmlCookie = $(htmlCookie);

			var cookieSelector = '#' + $htmlGroup.attr('id') + ' #' + $htmlCookie.attr('id') +
				' .apa-consent-subItemTitle';


			if (!this.isGroupActive(group)) {
				// deactivate cookie
				this.deactivateCookiesByGroup(group);

			}

			if (!application.value) {
				this.deactivateCookiesByApplication(group, application);
			}

			$('body').on('click', cookieSelector, function (event) {

				if (group.gcmGroup.protected) {
					return;
				} else if ((gcmCookieName === GCMHTML.bannerCookieName || (GCMHTML.isBannerCookieGCMCookie && gcmCookieName.startsWith(GCMHTML.bannerCookieName))) && !GCMHTML.isCookieDeactivate(gcmGroup.htmlid,
						gcmCookie.htmlid)) {

					GCMHTML.deactivateCookiesAll();
					GCMHTML.deactivateAllGroups();
					GCMHTML.deactivateGroupSwitchOnly(group);


				} else if ((gcmCookieName === GCMHTML.bannerCookieName || (GCMHTML.isBannerCookieGCMCookie && gcmCookieName.startsWith(GCMHTML.bannerCookieName)))) {

					GCMHTML.activateCookie(gcmGroup.htmlid, gcmCookie.htmlid);

					if (!GCMHTML.isGroupActive(group)) {
						// this method is a patch ..., merge this with toggleGroupSwitch
						GCMHTML.activateGroupSwitchOnly(group);
					}

					window.GCM.apps.setAndSave(application.appName, true);

				} else if (gcmCookieName.startsWith('gcmdata_') && !GCMHTML.isCookieDeactivate(gcmGroup.htmlid,
						gcmCookie.htmlid)) {


					GCMHTML.deactivateCookiesAll([GCMHTML.bannerCookieName]);

					GCMHTML.groups.forEach(function (group) {
						if (!group.protected) {
							window.GCM.groups.set(group.groupid, false);
						}
					});


				} else if (gcmCookieName.startsWith('gcmdata_')) {
					GCMHTML.activateCookie(gcmGroup.htmlid, gcmCookie.htmlid);


				} else if (GCMHTML.isCookieDeactivate(gcmGroup.htmlid, gcmCookie.htmlid)) {

					GCMHTML.activateCookiesByApplication(group, application);

					var appList = window.GCM.apps.getList();
					// TODO remove hardcoded access...
					if (!appList['gcm']) {

						var groups = GCMHTML.groups;

						$.each(groups, function (index, funcGroup) {
							if (funcGroup.groupid === 'functional') {
								// activate gcm
								var appKeys = Object.keys(appList);
								$.each(appKeys, function (index, app) {

									var appObject = funcGroup.gcmGroup.apps['gcm'];

									if (app === 'gcm') {

										console.log(appObject);
										GCMHTML.activateCookiesByApplication(funcGroup, appObject);
									}
								});
							}
						});
					}

					window.GCM.apps.set(application.appName, true);

					GCMHTML.activateGroupSwitchOnly(group);


				} else if (!GCMHTML.isCookieDeactivate(gcmGroup.htmlid, gcmCookie.htmlid)) {

					GCMHTML.deactivateCookiesByApplication(group, application);
					window.GCM.apps.set(application.appName, false);
					GCMHTML.deactivateGroupSwitchOnly(group);
				}
			});
		},

		activateCookie: function (htmlApplicationId, htmlCookieId) {
			var htmlCookie = $('body #' + htmlApplicationId + ' #' + htmlCookieId);

			htmlCookie.removeClass('apa-consent-itemSubItemOFF');
		},
		activateCookiesByPrefix: function (application, prefix) {
			$.each(application.gcmApplication.cookies, function (index, cookie) {
				if (cookie.label.toLowerCase().startsWith(prefix.toLowerCase())) {
					GCMHTML.activateCookie(application.gcmApplication.htmlid, cookie.htmlid);
				}
			});
		},

		activateCookiesByGroup: function (group) {
			$.each(group.gcmGroup.apps, function (index, app) {
				app.value = true;
				$.each(app.cookies, function (index, cookie) {
					GCMHTML.activateCookie(group.gcmGroup.htmlid, cookie.htmlid);
				});
			});
		},

		deactivateCookiesByGroup: function (group) {

			$.each(group.gcmGroup.apps, function (index, app) {
				app.value = false;
				$.each(app.cookies, function (index, cookie) {
					GCMHTML.deactivateCookie(group.gcmGroup.htmlid, cookie.htmlid);
				});
			});
		},

		activateCookiesByApplication: function (group, application) {

			window.GCM.apps.set(application.appName, true);

			$.each(application.cookies, function (index, cookie) {
				GCMHTML.activateCookie(group.gcmGroup.htmlid, cookie.htmlid, group.htmlGroup);
			});

			/*
			$.each(group.gcmGroup.apps, function (index, app) {
				if (application == app) {
					$.each(app.cookies, function (index, cookie) {
						GCMHTML..activateCookie(group.gcmGroup.htmlid, cookie.htmlid);
					});
				}
			});
			*/
		},
		isActivateCookie: function (htmlApplicationId, htmlCookieId) {
			var htmlCookie = $('body #' + htmlApplicationId + ' #' + htmlCookieId);

			// opposite cond, jquery bug?
			if (htmlCookie.hasClass('apa-consent-itemSubItemOFF')) {
				return true;
			}

			return false;
		},
		isCookieDeactivate: function (htmlApplicationId, htmlCookieId) {
			var htmlCookie = $('body #' + htmlApplicationId + ' #' + htmlCookieId);

			return htmlCookie.hasClass('apa-consent-itemSubItemOFF');
		},

		deactivateCookie: function (htmlApplicationId, htmlCookieId, htmlApplication) {

			if (typeof htmlApplication !== 'undefined') {

				var htmlCookie = $('body #' + htmlApplicationId + ' #' + htmlCookieId);

				htmlCookie.addClass('apa-consent-itemSubItemOFF');

			} else {
				var htmlCookie = $('body #' + htmlApplicationId + ' #' + htmlCookieId);

				htmlCookie.addClass('apa-consent-itemSubItemOFF');
			}
		},
		deactivateCookiesByPrefix: function (application, prefix) {
			$.each(application.gcmApplication.cookies, function (index, cookie) {
				if (cookie.label.toLowerCase().startsWith(prefix.toLowerCase())) {
					GCMHTML.deactivateCookie(application.gcmApplication.htmlid, cookie.htmlid);
				}
			});
		},
		deactivateCookiesByApplication: function (group, application) {

			application.value = false;
			window.GCM.apps.set(application.appName, false)

			$.each(application.cookies, function (index, cookie) {
				GCMHTML.deactivateCookie(group.gcmGroup.htmlid, cookie.htmlid, group.htmlGroup);
			});


			var appKeys = Object.keys(group.gcmGroup.apps);

			if (appKeys.length == 1) {
				this.deactivateGroupSwitch(group);
			} else {

				var result = false;

				appKeys.forEach(function (app) {
					var application = group.gcmGroup.apps[app];

					if (application.value) {
						result = true;
					}
				});

				if (!result) {
					this.deactivateGroupSwitch(group);
				}
			}
		},
		deactivateCookiesAll: function (except) {

			if (typeof except === 'undefined' || except === null) {
				excepts = [];
			}

			$.each(this.groups, function (index, group) {

				if (!group.gcmGroup.protected) {

					// deactivate group
					if (group.gcmGroup.value) {

						this.deactivateGroupSwitch(group);

						if (excepts.length > 0) {

							excepts.forEach(function (except) {

								var appKeys = Object.keys(group.gcmGroup.apps);

								appKeys.forEach(function (app) {
									var appObject = group.gcmGroup.apps[app];
									var cookieKeys = Object.keys(appObject.cookies);

									cookieKeys.forEach(function (cookie) {
										// TODO
										if (except == cookie) {
											GCMHTML.activateCookie(group.gcmGroup.htmlid, appObject.cookies[cookie].htmlid);
										}
									});
								});
							});
						}
					}
				}
			});

			/*
			$.each(this.applications, function (index, app) {

				if (!app.gcmApplication.protected) {

					var htmlApp = app.htmlApplication;
					$htmlApp = $(htmlApp);

					// deactivate application
					if (app.gcmApplication.value) {
						app.gcmApplication.value = false;
						this.deactivateApplication(app);
					}

					$.each(app.gcmApplication.cookies, function (index, cookie) {
						this.deactivateCookie(app.gcmApplication.htmlid, cookie.htmlid);
					});
				}
			});
			*/
		},
		renderGroupHTMLFields: function (group) {

			var gcmGroup = group.gcmGroup;
			var htmlGroup = group.htmlGroup;

			$htmlGroup = $(htmlGroup);

			$htmlGroup.find('.apa-consent-itemTitle').text(gcmGroup.label);
			$htmlGroup.find('.apa-consent-itemText').text(gcmGroup.description);

			// TODO BUG... check applications
			if (gcmGroup.protected) {
				// nothing to do...
				$groupEntry.find('.apa-consent-itemSwitch').removeClass('apa-consent-itemSwitchON');
				$groupEntry.find('.apa-consent-itemSwitch').removeClass('apa-consent-itemSwitchOFF');
			} else if (gcmGroup.value) {
				$groupEntry.find('.apa-consent-itemSwitch').addClass('apa-consent-itemSwitchON');
				// GCMHTML.activateGroupSwitchOnly(group);
			} else {
				$groupEntry.find('.apa-consent-itemSwitch').addClass('apa-consent-itemSwitchOFF');
				// GCMHTML.deactivateGroupSwitchOnly(group);
			}

			/*
			// TODO ... check the following line...
			$('body').on('click', $groupEntry.find('.apa-consent-itemSwitch')[0], function (event) {
				window.GCM.apps.set(application.appid, !gcmApplication.value);
			});
			*/

			var appKeys = Object.keys(gcmGroup.apps);

			appKeys.forEach(function (application, i) {


				var appObject = gcmGroup.apps[application];
				appObject.appName = application;
				var keys = Object.keys(appObject.cookies);

				keys.forEach(function (cookie, i) {
					//$.each(keys, function (i, cookie) {

					var gcmCookie = appObject.cookies[cookie];

					var cookieItemNewId = null;
					var htmlCookie = null;

					if (gcmCookie.appid === 'essential') {
						htmlCookie = $('#prototypeAppEntryProtectedCookieItem').clone();
						cookieItemNewId = 'prototypeAppEntryProtectedCookieItem' + '_' + application + '-' + i;
					} else {
						htmlCookie = $('#prototypeAppEntryCookieItem').clone();
						cookieItemNewId = 'prototypeAppEntryCookieItem' + '_' + application + '-' + i;
					}

					gcmCookie['htmlid'] = cookieItemNewId;

					htmlCookie.attr('id', cookieItemNewId);


					$cookieItem = $(htmlCookie);

					/*
					if (!gcmApplication.value) {
						this.cookieToggleOff(htmlCookie[0]);
					}*/

					$cookieItem.find('.apa-consent-subItemTitle > span').text(gcmCookie.label);

					if (gcmCookie.description != null && gcmCookie.description.length > 0) {
						$cookieItem.find('.apa-consent-P').text(gcmCookie.description);
					}

					$cookieItem.css('display', 'block');

					$htmlGroup.find('.apa-consent-cookie-items').append($('<div>').append($cookieItem).html());

					setTimeout(function () {
						GCMHTML.initGroupCookieToggleSwitch(group, appObject, cookie, gcmCookie, htmlCookie[0])
					}, 500);
				});
			});

			$('.apa-consent-items').append($('<div>').append($htmlGroup).html());
		},
		ini: function () {

			/**
			 * prepare HTML
			 */

			$('.apa-consent-bannerText').html(this.bannerTextEN);

			if (this.showOrHideBanner()) {

			} else {
				// cookie banner is hidden.
			}

			this.eventButtonAccept();
			this.eventButtonSaveSettings();

			var groups = window.GCM.groups.getList();

			var groupKeys = Object.keys(groups);
			var that = this;

			$.each(groupKeys, function (index, group) {

				// group is a fieldname
				var groupObject = groups[group];

				var htmlGroupEntry = null;
				var newId;
				if (!groupObject.protected) {
					newId = 'prototypeAppEntry' + '-' + index;

					htmlGroupEntry = $('#prototypeAppEntry').clone();

				} else {
					newId = 'prototypeAppEntryProtected' + '-' + index;

					htmlGroupEntry = $('#prototypeAppEntryProtected').clone();
				}

				$groupEntry = $(htmlGroupEntry);
				$groupEntry.attr('id', newId);

				$groupEntry.attr('data-appid', group);

				groupObject.htmlid = newId;

				that.loadGroup(that.addGroup(group, groupObject, htmlGroupEntry[0]));

			});
			$('body').on('click', '#bannerButtonOpen', function() {
				GCMHTML.showPrefDialog();
			});

			/*
			if(!this.isCookiesAllowed()) {
				this.hidePrefDialogRoundButtonOpen();
			}
			*/
			$(this.prefDialogRoundButtonOpen).click(function () {
				GCMHTML.showPrefDialog();
			});

			$(this.prefDialogButtonOpen).click(function (event) {
				GCMHTML.showPrefDialog();
			});

			$(this.prefDialogButtonDetails).click(function (event) {
				GCMHTML.showPrefDialog();
			});


			// '.apa-consent-linkDetails' isnt available before yet, it gets dynamically loaded, so we need to set a timeout
			setTimeout(function () {
				var prefDialogHyperlinkDetails = $('.apa-consent-linkDetails')[0];
				$(prefDialogHyperlinkDetails).click(function (event) {
					GCMHTML.showPrefDialog();
				});
			}, 500);


			$(this.prefDialogButtonMobileClose).click(function () {
				GCMHTML.hidePrefDialog();
			});

			$(this.prefDialogButtonClose).click(function () {
				GCMHTML.hidePrefDialog();
			});

			$('[id*=prototypeAppEntry-] .apa-consent-itemTitle, [id*=prototypeAppEntryProtected-] .apa-consent-itemTitle').each(function (index, item) {
				var selector = GCMHTML.getXPath(item);

				if (selector.trim().length > 0) {

					$(document).on('click', selector, function (event) {
						var parent = event.target.parentNode;
						$parent = $(parent);

						if ($parent.hasClass('apa-consent-itemSubItemsON')) {
							$parent.removeClass('apa-consent-itemSubItemsON');
						} else {
							$parent.addClass('apa-consent-itemSubItemsON');
						}
					});
				}
			});
		}
	}

	GCMHTML.ini();
});