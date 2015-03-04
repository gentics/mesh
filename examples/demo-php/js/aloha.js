(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else {
        root.aloha = factory();
    }
}(this, function () {
/**
 * @license almond 0.3.0 Copyright (c) 2011-2014, The Dojo Foundation All Rights Reserved.
 * Available via the MIT or new BSD license.
 * see: http://github.com/jrburke/almond for details
 */
//Going sloppy to avoid 'use strict' string cost, but strict practices should
//be followed.
/*jslint sloppy: true */
/*global setTimeout: false */

var requirejs, require, define;
(function (undef) {
    var main, req, makeMap, handlers,
        defined = {},
        waiting = {},
        config = {},
        defining = {},
        hasOwn = Object.prototype.hasOwnProperty,
        aps = [].slice,
        jsSuffixRegExp = /\.js$/;

    function hasProp(obj, prop) {
        return hasOwn.call(obj, prop);
    }

    /**
     * Given a relative module name, like ./something, normalize it to
     * a real name that can be mapped to a path.
     * @param {String} name the relative name
     * @param {String} baseName a real name that the name arg is relative
     * to.
     * @returns {String} normalized name
     */
    function normalize(name, baseName) {
        var nameParts, nameSegment, mapValue, foundMap, lastIndex,
            foundI, foundStarMap, starI, i, j, part,
            baseParts = baseName && baseName.split("/"),
            map = config.map,
            starMap = (map && map['*']) || {};

        //Adjust any relative paths.
        if (name && name.charAt(0) === ".") {
            //If have a base name, try to normalize against it,
            //otherwise, assume it is a top-level require that will
            //be relative to baseUrl in the end.
            if (baseName) {
                //Convert baseName to array, and lop off the last part,
                //so that . matches that "directory" and not name of the baseName's
                //module. For instance, baseName of "one/two/three", maps to
                //"one/two/three.js", but we want the directory, "one/two" for
                //this normalization.
                baseParts = baseParts.slice(0, baseParts.length - 1);
                name = name.split('/');
                lastIndex = name.length - 1;

                // Node .js allowance:
                if (config.nodeIdCompat && jsSuffixRegExp.test(name[lastIndex])) {
                    name[lastIndex] = name[lastIndex].replace(jsSuffixRegExp, '');
                }

                name = baseParts.concat(name);

                //start trimDots
                for (i = 0; i < name.length; i += 1) {
                    part = name[i];
                    if (part === ".") {
                        name.splice(i, 1);
                        i -= 1;
                    } else if (part === "..") {
                        if (i === 1 && (name[2] === '..' || name[0] === '..')) {
                            //End of the line. Keep at least one non-dot
                            //path segment at the front so it can be mapped
                            //correctly to disk. Otherwise, there is likely
                            //no path mapping for a path starting with '..'.
                            //This can still fail, but catches the most reasonable
                            //uses of ..
                            break;
                        } else if (i > 0) {
                            name.splice(i - 1, 2);
                            i -= 2;
                        }
                    }
                }
                //end trimDots

                name = name.join("/");
            } else if (name.indexOf('./') === 0) {
                // No baseName, so this is ID is resolved relative
                // to baseUrl, pull off the leading dot.
                name = name.substring(2);
            }
        }

        //Apply map config if available.
        if ((baseParts || starMap) && map) {
            nameParts = name.split('/');

            for (i = nameParts.length; i > 0; i -= 1) {
                nameSegment = nameParts.slice(0, i).join("/");

                if (baseParts) {
                    //Find the longest baseName segment match in the config.
                    //So, do joins on the biggest to smallest lengths of baseParts.
                    for (j = baseParts.length; j > 0; j -= 1) {
                        mapValue = map[baseParts.slice(0, j).join('/')];

                        //baseName segment has  config, find if it has one for
                        //this name.
                        if (mapValue) {
                            mapValue = mapValue[nameSegment];
                            if (mapValue) {
                                //Match, update name to the new value.
                                foundMap = mapValue;
                                foundI = i;
                                break;
                            }
                        }
                    }
                }

                if (foundMap) {
                    break;
                }

                //Check for a star map match, but just hold on to it,
                //if there is a shorter segment match later in a matching
                //config, then favor over this star map.
                if (!foundStarMap && starMap && starMap[nameSegment]) {
                    foundStarMap = starMap[nameSegment];
                    starI = i;
                }
            }

            if (!foundMap && foundStarMap) {
                foundMap = foundStarMap;
                foundI = starI;
            }

            if (foundMap) {
                nameParts.splice(0, foundI, foundMap);
                name = nameParts.join('/');
            }
        }

        return name;
    }

    function makeRequire(relName, forceSync) {
        return function () {
            //A version of a require function that passes a moduleName
            //value for items that may need to
            //look up paths relative to the moduleName
            var args = aps.call(arguments, 0);

            //If first arg is not require('string'), and there is only
            //one arg, it is the array form without a callback. Insert
            //a null so that the following concat is correct.
            if (typeof args[0] !== 'string' && args.length === 1) {
                args.push(null);
            }
            return req.apply(undef, args.concat([relName, forceSync]));
        };
    }

    function makeNormalize(relName) {
        return function (name) {
            return normalize(name, relName);
        };
    }

    function makeLoad(depName) {
        return function (value) {
            defined[depName] = value;
        };
    }

    function callDep(name) {
        if (hasProp(waiting, name)) {
            var args = waiting[name];
            delete waiting[name];
            defining[name] = true;
            main.apply(undef, args);
        }

        if (!hasProp(defined, name) && !hasProp(defining, name)) {
            throw new Error('No ' + name);
        }
        return defined[name];
    }

    //Turns a plugin!resource to [plugin, resource]
    //with the plugin being undefined if the name
    //did not have a plugin prefix.
    function splitPrefix(name) {
        var prefix,
            index = name ? name.indexOf('!') : -1;
        if (index > -1) {
            prefix = name.substring(0, index);
            name = name.substring(index + 1, name.length);
        }
        return [prefix, name];
    }

    /**
     * Makes a name map, normalizing the name, and using a plugin
     * for normalization if necessary. Grabs a ref to plugin
     * too, as an optimization.
     */
    makeMap = function (name, relName) {
        var plugin,
            parts = splitPrefix(name),
            prefix = parts[0];

        name = parts[1];

        if (prefix) {
            prefix = normalize(prefix, relName);
            plugin = callDep(prefix);
        }

        //Normalize according
        if (prefix) {
            if (plugin && plugin.normalize) {
                name = plugin.normalize(name, makeNormalize(relName));
            } else {
                name = normalize(name, relName);
            }
        } else {
            name = normalize(name, relName);
            parts = splitPrefix(name);
            prefix = parts[0];
            name = parts[1];
            if (prefix) {
                plugin = callDep(prefix);
            }
        }

        //Using ridiculous property names for space reasons
        return {
            f: prefix ? prefix + '!' + name : name, //fullName
            n: name,
            pr: prefix,
            p: plugin
        };
    };

    function makeConfig(name) {
        return function () {
            return (config && config.config && config.config[name]) || {};
        };
    }

    handlers = {
        require: function (name) {
            return makeRequire(name);
        },
        exports: function (name) {
            var e = defined[name];
            if (typeof e !== 'undefined') {
                return e;
            } else {
                return (defined[name] = {});
            }
        },
        module: function (name) {
            return {
                id: name,
                uri: '',
                exports: defined[name],
                config: makeConfig(name)
            };
        }
    };

    main = function (name, deps, callback, relName) {
        var cjsModule, depName, ret, map, i,
            args = [],
            callbackType = typeof callback,
            usingExports;

        //Use name if no relName
        relName = relName || name;

        //Call the callback to define the module, if necessary.
        if (callbackType === 'undefined' || callbackType === 'function') {
            //Pull out the defined dependencies and pass the ordered
            //values to the callback.
            //Default to [require, exports, module] if no deps
            deps = !deps.length && callback.length ? ['require', 'exports', 'module'] : deps;
            for (i = 0; i < deps.length; i += 1) {
                map = makeMap(deps[i], relName);
                depName = map.f;

                //Fast path CommonJS standard dependencies.
                if (depName === "require") {
                    args[i] = handlers.require(name);
                } else if (depName === "exports") {
                    //CommonJS module spec 1.1
                    args[i] = handlers.exports(name);
                    usingExports = true;
                } else if (depName === "module") {
                    //CommonJS module spec 1.1
                    cjsModule = args[i] = handlers.module(name);
                } else if (hasProp(defined, depName) ||
                           hasProp(waiting, depName) ||
                           hasProp(defining, depName)) {
                    args[i] = callDep(depName);
                } else if (map.p) {
                    map.p.load(map.n, makeRequire(relName, true), makeLoad(depName), {});
                    args[i] = defined[depName];
                } else {
                    throw new Error(name + ' missing ' + depName);
                }
            }

            ret = callback ? callback.apply(defined[name], args) : undefined;

            if (name) {
                //If setting exports via "module" is in play,
                //favor that over return value and exports. After that,
                //favor a non-undefined return value over exports use.
                if (cjsModule && cjsModule.exports !== undef &&
                        cjsModule.exports !== defined[name]) {
                    defined[name] = cjsModule.exports;
                } else if (ret !== undef || !usingExports) {
                    //Use the return value from the function.
                    defined[name] = ret;
                }
            }
        } else if (name) {
            //May just be an object definition for the module. Only
            //worry about defining if have a module name.
            defined[name] = callback;
        }
    };

    requirejs = require = req = function (deps, callback, relName, forceSync, alt) {
        if (typeof deps === "string") {
            if (handlers[deps]) {
                //callback in this case is really relName
                return handlers[deps](callback);
            }
            //Just return the module wanted. In this scenario, the
            //deps arg is the module name, and second arg (if passed)
            //is just the relName.
            //Normalize module name, if it contains . or ..
            return callDep(makeMap(deps, callback).f);
        } else if (!deps.splice) {
            //deps is a config object, not an array.
            config = deps;
            if (config.deps) {
                req(config.deps, config.callback);
            }
            if (!callback) {
                return;
            }

            if (callback.splice) {
                //callback is an array, which means it is a dependency list.
                //Adjust args if there are dependencies
                deps = callback;
                callback = relName;
                relName = null;
            } else {
                deps = undef;
            }
        }

        //Support require(['a'])
        callback = callback || function () {};

        //If relName is a function, it is an errback handler,
        //so remove it.
        if (typeof relName === 'function') {
            relName = forceSync;
            forceSync = alt;
        }

        //Simulate async callback;
        if (forceSync) {
            main(undef, deps, callback, relName);
        } else {
            //Using a non-zero value because of concern for what old browsers
            //do, and latest browsers "upgrade" to 4 if lower value is used:
            //http://www.whatwg.org/specs/web-apps/current-work/multipage/timers.html#dom-windowtimers-settimeout:
            //If want a value immediately, use require('id') instead -- something
            //that works in almond on the global level, but not guaranteed and
            //unlikely to work in other AMD implementations.
            setTimeout(function () {
                main(undef, deps, callback, relName);
            }, 4);
        }

        return req;
    };

    /**
     * Just drops the config on the floor, but returns req in case
     * the config return value is used.
     */
    req.config = function (cfg) {
        return req(cfg);
    };

    /**
     * Expose module registry for debugging and tooling
     */
    requirejs._defined = defined;

    define = function (name, deps, callback) {

        //This module may not have dependencies
        if (!deps.splice) {
            //deps is not an array, so probably means
            //an object literal or factory function for
            //the value. Adjust args.
            callback = deps;
            deps = [];
        }

        if (!hasProp(defined, name) && !hasProp(waiting, name)) {
            waiting[name] = [name, deps, callback];
        }
    };

    define.amd = {
        jQuery: true
    };
}());

define("../build/almond", function(){});

/**
 * functions.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace fn
 */
define('functions',[], function () {
	

	/**
	 * Returns its single argument.
	 * Useful for composition when some default behaviour is needed.
	 *
	 * @param  {*} arg
	 * @return {*} The given argument `arg`.
	 * @memberOf fn
	 */
	function identity(arg) {
		return arg;
	}

	/**
	 * Does nothing.
	 * @memberOf fn
	 */
	function noop() {
	}

	/**
	 * Always returns `true`.
	 *
	 * @return {boolean}
	 * @memberOf fn
	 */
	function returnTrue() {
		return true;
	}

	/**
	 * Always returns `false`.
	 *
	 * @return {boolean}
	 * @memberOf fn
	 */
	function returnFalse() {
		return false;
	}

	/**
	 * Is null or undefined.
	 * @memberOf fn
	 */
	function isNou(obj) {
		return null == obj;
	}

	/**
	 * Generates the complement function for `fn`.
	 * The complement function will return the opposite boolean result when
	 * called with the same arguments as the given `fn` function.
	 *
	 * @param  {function():boolean} fn
	 * @return {function():boolean}
	 * @memberOf fn
	 */
	function complement(fn) {
		return function () {
			return !fn.apply(this, arguments);
		};
	}

	/**
	 * Like function.prototype.bind except without the `this` argument.
	 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/function/bind
	 *
	 * Returns a function that concatenates the given arguments, and the
	 * arguments given to the returned function and calls the given
	 * function with it.
	 *
	 * The returned function will have a length of 0, not a length of
	 * fn.length - number of partially applied arguments. This is
	 * important consideration for any code that introspects the arity
	 * of the returned function. Function.prototype.bind() returns a
	 * function with the correct number of arguments but is not
	 * available across all browsers.
	 *
	 * @param  {function} fn
	 * @param  {Object} thisArg
	 * @return {function}
	 * @memberOf fn
	 */
	function partial(fn) {
		var args = Array.prototype.slice.call(arguments, 1);
		return function () {
			return fn.apply(null, args.concat(
				Array.prototype.slice.call(arguments, 0)
			));
		};
	}

	/**
	 * Compare the given arguments using the strict equals operator.
	 *
	 * Useful to pass as an arguments to other functions.
	 *
	 * @param a {*}
	 * @param b {*}
	 * @return  {boolean}
	 * @memberOf fn
	 */
	function strictEquals(a, b) {
		return a === b;
	}

	/**
	 * Composes the functions given as arguments.
	 *
	 * comp(a, b, c)(value) === a(b(c(value)))
	 *
	 * @param  {function(...number):...number}
	 * @return {*}
	 * @memberOf fn
	 */
	function comp() {
		var fns = arguments;
		var len = fns.length;
		return function () {
			var result;
			var i = len;
			if (i-- > 0) {
				result = fns[i].apply(this, arguments);
			}
			while (i-- > 0) {
				result = fns[i].call(this, result);
			}
			return result;
		};
	}

	/**
	 * Composes a predicate function made up of a chain of the given predicate
	 * arguments.
	 *
	 * @param  {function():...boolean}
	 * @return {function():boolean}
	 * @memberOf fn
	 */
	function and() {
		var predicates = arguments;
		var len = predicates.length;
		return function () {
			for (var i = 0; i < len; i++) {
				if (!predicates[i].apply(this, arguments)) {
					return false;
				}
			}
			return true;
		};
	}

	/**
	 * Like and() but for boolean OR.
	 *
	 * @param  {function(): ...boolean}
	 * @return {function(): boolean}
	 * @memberOf fn
	 */
	function or() {
		var predicates = arguments;
		var len = predicates.length;
		return function () {
			for (var i = 0; i < len; i++) {
				if (predicates[i].apply(this, arguments)) {
					return true;
				}
			}
			return false;
		};
	}

	/**
	 * Returns a function that constantly returns the given value.
	 * @memberOf fn
	 */
	function constantly(value) {
		return function () {
			return value;
		};
	}

	/**
	 * Returns true if the given value is a function.
	 * @memberOf fn
	 */
	function is(obj) {
		return 'function' === typeof obj;
	}

	/**
	 * Wraps a function and passes `this` as the first argument.
	 *
	 * The function that is wrapped is available on the returned method
	 * as the fn property, which allows one to easily switch between
	 * method and function invokation form.
	 *
	 * The Function.length property of the given function is examined
	 * and may be either 0, no matter how many arguments the function
	 * expects, or if not 0, must be the actual number of arguments the
	 * function expects.
	 * @memberOf fn
	 */
	function asMethod(fn) {
		var len = fn.length;
		// Optimize the common case of len <= 4
		var method = (1 === len) ? function () {
			return fn(this);
		} : (2 === len) ? function (arg1) {
			return fn(this, arg1);
		} : (3 === len) ? function (arg1, arg2) {
			return fn(this, arg1, arg2);
		} : (4 === len) ? function (arg1, arg2, arg3) {
			return fn(this, arg1, arg2, arg3);
		} : function () {
			var args = Array.prototype.slice.call(arguments, 0);
			args.unshift(this);
			return fn.apply(null, args);
		};
		return method;
	}

	/**
	 * Adds functions to the given type's prototype.
	 *
	 * The functions will be converted to methods using Fn.asMethod().
	 *
	 * @param Type {!*}
	 * @param fnByName {Object.<string,function>}
	 * @memberOf fn
	 */
	function extendType(Type, fnByName) {
		for (var name in fnByName) {
			if (fnByName.hasOwnProperty(name)) {
				Type.prototype[name] = asMethod(fnByName[name]);
			}
		}
	}

	return {
		identity     : identity,
		noop         : noop,
		returnTrue   : returnTrue,
		returnFalse  : returnFalse,
		complement   : complement,
		partial      : partial,
		strictEquals : strictEquals,
		comp         : comp,
		and          : and,
		or           : or,
		constantly   : constantly,
		is           : is,
		isNou        : isNou,
		asMethod     : asMethod,
		extendType   : extendType
	};
});

/**
 * arrays.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace arrays
 */
define('arrays',['functions'], function (Fn) {
	

	/**
	 * Does a shallow compare of two arrays.
	 *
	 * @param {Array} a
	 *        An array to compare.
	 * @param {Array} b
	 *        A second array to compare with `a`.
	 * @param {function(*, *):number} equalFn
	 *        A custom comparison function that accepts two values a and b from
	 *        the given arrays and returns true or false for equal and not equal
	 *        respectively.
	 *
	 *        If no equalFn is provided, the algorithm will use the strict
	 *        equals operator.
	 * @return {boolean}
	 *         True if all items in a and b are equal, false if not.
	 * @memberOf arrays
	 */
	function equal(a, b, equalFn) {
		var i,
			len = a.length;
		if (len !== b.length) {
			return false;
		}
		equalFn = equalFn || Fn.strictEquals;
		for (i = 0; i < len; i++) {
			if (!equalFn(a[i], b[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if the given Array `xs` contains the value `x`.
	 *
	 * @param {Array} xs
	 * @param {*} x
	 *        A value to search for in the given array.
	 * @return {boolean}
	 *         True of argument `x` is an element of the set `xs`.
	 * @memberOf arrays
	 */
	function contains(xs, x) {
		return -1 !== xs.indexOf(x);
	}

	/**
	 * Returns all items in the array `xs` that are also contained in array
	 * `zs`.
	 *
	 * @param {Array} xs
	 * @param {Array} zs
	 * @return {Array}
	 *         The intersection of the sets `xs` and `zs`.
	 * @memberOf arrays
	 */
	function intersect(xs, zs) {
		return xs.filter(function (x) {
			return contains(zs, x);
		});
	}

	/**
	 * Returns the relative difference of array `zs` in `xs`:
	 * All items in the array `xs` that are not contained in array `zs`.
	 *
	 * @param {Array} xs
	 * @param {Array} zs
	 * @return {Array}
	 *         The difference of the sets `xs` and `zs`.
	 * @memberOf arrays
	 */
	function difference(xs, zs) {
		return xs.filter(function (x) {
			return !contains(zs, x);
		});
	}

	/**
	 * Returns the last item in the given Array.
	 *
	 * @param {Array} xs
	 * @return {*}
	 *         Last item in xs, or null if the given array is empty.
	 * @memberOf arrays
	 */
	function last(xs) {
		return xs.length ? xs[xs.length - 1] : null;
	}

	/**
	 * Coerces the given object (NodeList, arguments) to an array.
	 *
	 * This implementation works on modern browsers and IE >= 9. For IE
	 * < 9 a shim can be used, available here:
	 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
	 *
	 * @param arrayLikeObject {*}
	 * @return {Array.<*>}
	 * @memberOf arrays
	 */
	function coerce(arrayLikeObject) {
		return Array.prototype.slice.call(arrayLikeObject);
	}

	/**
	 * Like Array.prototype.map() except expects the given function to return
	 * arrays which will be concatenated together into the resulting array.
	 *
	 * Related to partition() in the sense that
	 * mapcat(partition(xs, n), identity) == xs.
	 *
	 * Don't use Array.prototype.concat.apply():
	 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/apply
	 * "The consequences of applying a function with too many arguments
	 * (think more than tens of thousands of arguments) vary across
	 * engines (JavaScriptCore has hard-coded argument limit of 65536),
	 * because the limit (indeed even the nature of any
	 * excessively-large-stack behavior) is unspecified. "
	 *
	 * @param xs {Array.<*>}
	 * @param fn {function(*):Array.<*>}
	 * @return {Array.<*>}
	 * @memberOf arrays
	 */
	function mapcat(xs, fn) {
		return xs.reduce(function(result, x) {
			return result.concat(fn(x));
		}, []);
	}

	/**
	 * Partitions the given array xs into an array of arrays where each
	 * nested array is a subsequence of xs of length n.
	 *
	 * See mapcat().
	 *
	 * @param xs {Array.<*>}
	 * @param n {number}
	 * @return {Array.<Array.<*>>}
	 * @memberOf arrays
	 */
	function partition(xs, n) {
		return xs.reduce(function (result, x) {
			var l = last(result);
			if (l && l.length < n) {
				l.push(x);
			} else {
				result.push([x]);
			}
			return result;
		}, []);
	}

	/**
	 * Similar to some(), except that it returns an index into the given array
	 * for the first element for which `pred` returns true.
	 *
	 * If none return true, -1 is returned.
	 *
	 * @param {Array.<*>}           xs
	 * @param {function(*):boolean} pred
	 * @return {*}
	 * @memberOf arrays
	 */
	function someIndex(xs, pred) {
		var result = -1;
		xs.some(function (x, i) {
			if (pred(x)) {
				result = i;
				return true;
			}
		});
		return result;
	}

	/**
	 * Similar to 
	 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/some
	 * Except, instead of returning true, returns the first value in the array
	 * for which the `pred` returns true.
	 *
	 * @param  {Array.<*>}           xs
	 * @param  {function(*):boolean} pred
	 * @return {*} One of xs
	 * @memberOf arrays
	 */
	function some(xs, pred) {
		var index = someIndex(xs, pred);
		return -1 === index ? null : xs[index];
	}

	/**
	 * Splits the list into two parts using the given predicate.
	 *
	 * The first element will be the "prefix," containing all elements of `list` before
	 * the element that returns true for the predicate.
	 *
	 * The second element is equal to dropWhile(list).
	 *
	 * @param  {Array<*>}            list
	 * @param  {function(*):boolean} predicate
	 * @return {Array<Array<*>>}     The prefix and suffix of `list`
	 * @memberOf arrays
	 */
	function split(xs, predicate) {
		var end = someIndex(xs, predicate);
		end = -1 === end ? xs.length : end;
		return [xs.slice(0, end), xs.slice(end)];
	}

	/**
	 * Creates a new array that contains a unique list of entries
	 * created from the old array. Example:
	 * [1, 2, 2, 3, 2, 4] => [1, 2, 3, 4]
	 *
	 * @param  {Array.<*>} arr
	 * @return {Array.<*>}
	 * @memberOf arrays
	 */
	function unique(arr) {
		var set = [];
		arr.forEach(function (entry) {
			if (set.indexOf(entry) === -1) {
				set.push(entry);
			}
		});
		return set;
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf arrays
	 */
	function refill(arrays) {
		var receivers = coerce(arguments).slice(1);
		for (var i = 0; i < arrays.length; i++) {
			if (!arrays[i] || !receivers[i]) {
				return;
			}
			receivers[i].length = 0;
			Array.prototype.splice.apply(receivers[i], [0, 0].concat(arrays[i]));
		}
	}

	/**
	 * Returns true if the given value is an array.
	 * @memberOf arrays
	 */
	function is(obj) {
		return (Object.prototype.toString.call(obj) === '[object Array]');
	}

	return {
		contains   : contains,
		difference : difference,
		equal      : equal,
		intersect  : intersect,
		is         : is,
		last       : last,
		coerce     : coerce,
		mapcat     : mapcat,
		partition  : partition,
		some       : some,
		someIndex  : someIndex,
		split      : split,
		unique     : unique,
		refill     : refill
	};
});

/**
 * assert.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('assert',[], function () {
	

	/**
	 * Generates an error message with a link to corresponding helpful resource
	 * on the Aloha Editor website.
	 *
	 * @param  {string} type
	 * @return {string}
	 */
	function errorLink(type) {
		return '✘ Error (' + type + '). See http://www.alohaeditor.org/docs/errors/' + type;
	}

	var NOT_IMPLEMENTED = 0;
	var NOU             = 1;
	var NOT_NOU         = 2;
	var ASSERT_TYPE_NOU = 15;

	function error(type) {
		throw Error(type ? errorLink(type) : 'assertion failed');
	}

	function assert(cond, type) {
		// TODO all asserts must pass a type, which must be not-null,
		// otherwise it's too easy to have a typo when referencing the
		// assert type as in "Assert.NOu [sic]".
		//if (null == type) {
		//	error(ASSERT_TYPE_NOU);
		//}
		if (!cond) {
			error(type);
		}
	}

	function notImplemented() {
		error(NOT_IMPLEMENTED);
	}

	function assertNotNou(obj) {
		assert(null != obj, NOU);
	}

	function assertNou(obj) {
		assert(null == obj, NOT_NOU);
	}

	return {
		assert         : assert,
		error          : error,
		notImplemented : notImplemented,
		assertNou      : assertNou,
		assertNotNou   : assertNotNou,

		// Don't renumber to maintain well-known values for error
		// conditions.
		NOT_IMPLEMENTED              : NOT_IMPLEMENTED,
		NOU                          : NOU,
		NOT_NOU                      : NOT_NOU,

		// assert.js
		ASSERT_TYPE_NOU              : ASSERT_TYPE_NOU,

		// record.js
		READ_FROM_DISCARDED_TRANSIENT: 3,
		PERSISTENT_WRITE_TO_TRANSIENT: 4,
		TRANSIENT_WRITE_TO_PERSISTENT: 5,
		RECORD_WRONG_TYPE            : 16,

		// boromir.js
		STYLE_NOT_AS_ATTR            : 8,
		EXPECT_ELEMENT               : 9,
		EXPECT_TEXT_NODE             : 10,
		ELEMENT_NOT_ATTACHED         : 11,
		MISSING_SYMBOL               : 12,

		// accessors.js
		GETTER_AT_LEAST_1_ARG        : 13,
		SETTER_1_MORE_THAN_GETTER    : 14
	};
});

/**
 * maps.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace maps
 */
define('maps',['arrays', 'assert'], function (Arrays, Assert) {
	

	/**
	 * Checks whether the given object has no own or inherited properties.
	 *
	 * @param {!Object} obj
	 *        Object to check.
	 * @return {boolean}
	 *         True if the object is empty. eg: isEmpty({}) == true
	 * @memberOf maps
	 */
	function isEmpty(obj) {
		var name;
		for (name in obj) {
			if (obj.hasOwnProperty(name)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Fills the given map with the given keys mapped to the given value.
	 *
	 * @param {Object} map
	 *        The given map will have one entry added for each given key.
	 * @param {Array.<string>} keys
	 *        An array of string keys. JavaScript maps can only contain string
	 *        keys, so these must be strings or they will be cast to string.
	 * @param {string} value A single value that each given key will map to.
	 * @return {Object}
	 *         The given map.
	 * @memberOf maps
	 */
	function fillKeys(map, keys, value) {
		var i = keys.length;
		while (i--) {
			map[keys[i]] = value;
		}
		return map;
	}

	/**
	 * For each mapping, calls `cb(value, key, m)`.
	 *
	 * Like ECMAScript edition 5 Array.forEach but for Maps.
	 *
	 * Contrary to "for (key in m)" iterates only over the "hasOwnProperty"
	 * properties of the m, which is usually what you want.
	 * @memberOf maps
	 */
	function forEach(m, cb) {
		var key;
		for (key in m) {
			if (m.hasOwnProperty(key)) {
				cb(m[key], key, m);
			}
		}
	}

	/**
	 * Selects the values for the given keys in the given map.
	 *
	 * @param {!Object} m
	 * @param {!Array} ks
	 * @param {*} _default used in place of non-existing properties
	 * @return {!Array}
	 * @memberOf maps
	 */
	function selectVals(m, ks, _default) {
		return ks.map(function (k) {
			return m.hasOwnProperty(k) ? m[k] : _default;
		});
	}

	/**
	 * Same as Array.filter except for maps.
	 *
	 * The given predicate is applied to each entry in the given map,
	 * and only if the predicate returns true, will the entry appear in
	 * the result.
	 * @memberOf maps
	 */
	function filter(m, pred) {
		var result = {};
		forEach(m, function (val, key) {
			if (pred(val, key, m)) {
				result[key] = val;
			}
		});
		return result;
	}

	/**
	 * Returns an array of the map's keys.
	 *
	 * @param {!Object} m
	 * @return {!Array} The set of keys in `m`.
	 * @memberOf maps
	 */
	function keys(m) {
		var ks = [];
		forEach(m, function (value, key) {
			ks.push(key);
		});
		return ks;
	}

	/**
	 * Returns an array of the map's values.
	 *
	 * @param {!Object} m
	 * @return {!Array} The values in `m`.
	 * @memberOf maps
	 */
	function vals(m) {
		return selectVals(m, keys(m));
	}
	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf maps
	 */
	function extend(dest) {
		var i;
		for (i = 1; i < arguments.length; i++) {
			var src = arguments[i];
			if (src) {
				forEach(src, function (value, key) {
					dest[key] = value;
				});
			}
		}
		return dest;
	}

	/**
	 * Merges one or more maps.
	 * Merging happens from left to right, which is useful for example
	 * when merging a number of given options with default options:
	 * var effectiveOptions = Maps.merge(defaults, options);
	 *
	 * @param {...Object}
	 *        A variable number of map objects.
	 * @return {Object}
	 *         A merge of all given maps in a single object.
	 * @memberOf maps
	 */
	function merge() {
		return extend.apply(null, [{}].concat(Arrays.coerce(arguments)));
	}

	/**
	 * Clones a map.
	 *
	 * @param map {!Object}
	 * @return {!Object}
	 * @memberOf maps
	 */
	function clone(map) {
		Assert.assertNotNou(map);
		return extend({}, map);
	}

	/**
	 * Sets a value on a clone of the given map and returns the clone.
	 *
	 * @param map {!Object}
	 * @param key {string}
	 * @param value {*}
	 * @return {!Object}
	 * @memberOf maps
	 */
	function cloneSet(map, key, value) {
		map = clone(map);
		map[key] = value;
		return map;
	}

	/**
	 * Deletes a key from a clone of the given map and returns the clone.
	 *
	 * @param map {!Object}
	 * @param key {string}
	 * @return {!Object}
	 * @memberOf maps
	 */
	function cloneDelete(map, key) {
		map = clone(map);
		delete map[key];
		return map;
	}

	/**
	 * Whether the given object is a map that can be operated on by
	 * other functions in this module.
	 *
	 * We exclude things like new String("..."), new Number(...),
	 * document.createElement(...), but include new MyType("...").
	 * @memberOf maps
	 */
	function isMap(obj) {
		return !!(obj
		          // On IE7 DOM Nodes are [object Object] but don't have a constructor
		          && obj.constructor
		          && Object.prototype.toString.call(obj) === '[object Object]');
	}

	/**
	 * Creates a map without inheriting from Object.
	 *
	 * Use this instead of an object literal to avoid having unwanted, inherited
	 * properties on the map.
	 *
	 * A map constructed like this allows for the
	 * ```for (var key in map) { }```
	 * pattern to be used without a hasOwnProperty check.
	 *
	 * @return {!Object}
	 * @memberOf maps
	 */
	function create() {
		return Object.create(null);
	}

	/**
	 * Converts a list of tuples into a hash map key-value pair.
	 *
	 * @param  {Array.<Array.<string, *>>} tuples
	 * @return {Object.<string, *>}
	 * @memberOf maps
	 */
	function mapTuples(tuples) {
		var map = {};
		tuples.forEach(function (tuple) {
			map[tuple[0]] = tuple[1];
		});
		return map;
	}

	return {
		isEmpty     : isEmpty,
		fillKeys    : fillKeys,
		keys        : keys,
		vals        : vals,
		selectVals  : selectVals,
		filter      : filter,
		forEach     : forEach,
		extend      : extend,
		merge       : merge,
		isMap       : isMap,
		clone       : clone,
		cloneSet    : cloneSet,
		cloneDelete : cloneDelete,
		create      : create,
		mapTuples   : mapTuples
	};
});

/**
 * strings.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * Reference:
 * http://www.w3.org/TR/html401/struct/text.html
 * @namespace strings
 */
define('strings',['arrays'], function (Arrays) {
	

	/**
	 * Unicode zero width space characters:
	 * http://www.unicode.org/Public/UNIDATA/Scripts.txt
	 *
	 * @type {Array.<string>}
	 * @memberOf strings
	 */
	var ZERO_WIDTH_CHARACTERS = [
		'\\u200B', // ZWSP
		'\\u200C',
		'\\u200D',
		'\\uFEFF'  // ZERO WIDTH NO-BREAK SPACE
	];

	/**
	 * Unicode non-breaking space characters as defined in the W3 HTML5
	 * specification:
	 * http://www.w3.org/TR/html5/infrastructure.html#common-parser-idioms
	 *
	 * @type {Array.<string>}
	 * @memberOf strings
	 */
	var NON_BREAKING_SPACE_CHARACTERS = [
		'\\u00A0', // NON BREAKING SPACE ("&nbsp;")
		'\\u202F'  // NARROW NON BREAKING SPACE
	];

	/**
	 * Unicode White_Space characters are those that have the Unicode property
	 * "White_Space" in the Unicode PropList.txt data file.
	 *
	 * http://www.unicode.org/Public/UNIDATA/PropList.txt
	 *
	 * @type {Array.<string>}
	 * @memberOf strings
	 */
	var WHITE_SPACE_CHARACTERS = [
		'\\u0009',
		'\\u000A',
		'\\u000B',
		'\\u000C',
		'\\u000D',
		'\\u0020',
		'\\u0085',
		'\\u00A0',
		'\\u1680',
		'\\u180E',
		'\\u2000',
		'\\u2001',
		'\\u2002',
		'\\u2003',
		'\\u2004',
		'\\u2005',
		'\\u2006',
		'\\u2007',
		'\\u2008',
		'\\u2009',
		'\\u200A',
		'\\u2028',
		'\\u2029',
		'\\u202F',
		'\\u205F',
		'\\u3000'
	];

	/**
	 * Characters that delimit boundaries of words.
	 *
	 * These include whitespaces, hyphens, and punctuation.
	 *
	 * @type {Array.<string>}
	 * @memberOf strings
	 */
	var WORD_BREAKING_CHARACTERS = [
		'\u0041-', '\u005A', '\u0061-', '\u007A', '\u00AA', '\u00B5', '\u00BA',
		'\u00C0-', '\u00D6', '\u00D8-', '\u00F6', '\u00F8-',

		'\u02C1',  '\u02C6-', '\u02D1', '\u02E0-', '\u02E4', '\u02EC', '\u02EE',
		'\u0370-', '\u0374',  '\u0376', '\u0377',  '\u037A-', '\u037D',
		'\u0386',  '\u0388-', '\u038A', '\u038C',  '\u038E-', '\u03A1',
		'\u03A3-', '\u03F5', '\u03F7-', '\u0481', '\u048A-', '\u0525',
		'\u0531-', '\u0556', '\u0559', '\u0561-', '\u0587', '\u05D0-', '\u05EA',
		'\u05F0-', '\u05F2', '\u0621-', '\u064A', '\u066E', '\u066F', '\u0671-',
		'\u06D3', '\u06D5', '\u06E5', '\u06E6', '\u06EE', '\u06EF', '\u06FA-',
		'\u06FC', '\u06FF', '\u0710', '\u0712-', '\u072F', '\u074D-', '\u07A5',
		'\u07B1', '\u07CA-', '\u07EA', '\u07F4', '\u07F5', '\u07FA', '\u0800-',
		'\u0815', '\u081A', '\u0824', '\u0828', '\u0904-', '\u0939', '\u093D',
		'\u0950', '\u0958-', '\u0961', '\u0971', '\u0972', '\u0979-', '\u097F',
		'\u0985-', '\u098C', '\u098F', '\u0990', '\u0993-', '\u09A8', '\u09AA-',
		'\u09B0', '\u09B2', '\u09B6-', '\u09B9', '\u09BD', '\u09CE', '\u09DC',
		'\u09DD', '\u09DF-', '\u09E1', '\u09F0', '\u09F1',

		'\u0A05-', '\u0A0A', '\u0A0F', '\u0A10', '\u0A13-', '\u0A28', '\u0A2A-',
		'\u0A30', '\u0A32', '\u0A33', '\u0A35', '\u0A36', '\u0A38', '\u0A39',
		'\u0A59-', '\u0A5C', '\u0A5E', '\u0A72-', '\u0A74', '\u0A85-', '\u0A8D',
		'\u0A8F-', '\u0A91', '\u0A93-', '\u0AA8', '\u0AAA-', '\u0AB0', '\u0AB2',
		'\u0AB3', '\u0AB5-', '\u0AB9', '\u0ABD', '\u0AD0', '\u0AE0', '\u0AE1',

		'\u0B05-', '\u0B0C', '\u0B0F', '\u0B10', '\u0B13-', '\u0B28', '\u0B2A-',
		'\u0B30', '\u0B32', '\u0B33', '\u0B35-', '\u0B39', '\u0B3D', '\u0B5C',
		'\u0B5D', '\u0B5F-', '\u0B61', '\u0B71', '\u0B83', '\u0B85-', '\u0B8A',
		'\u0B8E-', '\u0B90', '\u0B92-', '\u0B95', '\u0B99', '\u0B9A', '\u0B9C',
		'\u0B9E', '\u0B9F', '\u0BA3', '\u0BA4', '\u0BA8-', '\u0BAA', '\u0BAE-',
		'\u0BB9', '\u0BD0',

		'\u0C05-', '\u0C0C', '\u0C0E-', '\u0C10', '\u0C12-', '\u0C28',
		'\u0C2A-', '\u0C33', '\u0C35-', '\u0C39', '\u0C3D', '\u0C58', '\u0C59',
		'\u0C60', '\u0C61', '\u0C85-', '\u0C8C', '\u0C8E-', '\u0C90', '\u0C92-',
		'\u0CA8', '\u0CAA-', '\u0CB3', '\u0CB5-', '\u0CB9', '\u0CBD', '\u0CDE',
		'\u0CE0', '\u0CE1',

		'\u0D05-', '\u0D0C', '\u0D0E-', '\u0D10', '\u0D12-', '\u0D28',
		'\u0D2A-', '\u0D39', '\u0D3D', '\u0D60', '\u0D61', '\u0D7A-', '\u0D7F',
		'\u0D85-', '\u0D96', '\u0D9A-', '\u0DB1', '\u0DB3-', '\u0DBB', '\u0DBD',
		'\u0DC0-', '\u0DC6',

		'\u0E01-', '\u0E30', '\u0E32', '\u0E33', '\u0E40-', '\u0E46', '\u0E81',
		'\u0E82', '\u0E84', '\u0E87', '\u0E88', '\u0E8A', '\u0E8D', '\u0E94-',
		'\u0E97', '\u0E99-', '\u0E9F', '\u0EA1-', '\u0EA3', '\u0EA5', '\u0EA7',
		'\u0EAA', '\u0EAB', '\u0EAD-', '\u0EB0', '\u0EB2', '\u0EB3', '\u0EBD',
		'\u0EC0-', '\u0EC4', '\u0EC6', '\u0EDC', '\u0EDD',

		'\u0F00', '\u0F40-', '\u0F47', '\u0F49-', '\u0F6C', '\u0F88-', '\u0F8B',

		'\u1000-', '\u102A', '\u103F', '\u1050-', '\u1055', '\u105A-', '\u105D',
		'\u1061', '\u1065', '\u1066', '\u106E-', '\u1070', '\u1075-', '\u1081',
		'\u108E', '\u10A0-', '\u10C5', '\u10D0-', '\u10FA', '\u10FC',

		'\u1100-', '\u1248', '\u124A-', '\u124D', '\u1250-', '\u1256', '\u1258',
		'\u125A-', '\u125D', '\u1260-', '\u1288', '\u128A-', '\u128D',
		'\u1290-', '\u12B0', '\u12B2-', '\u12B5', '\u12B8-', '\u12BE', '\u12C0',
		'\u12C2-', '\u12C5', '\u12C8-', '\u12D6', '\u12D8-', '\u1310',
		'\u1312-', '\u1315', '\u1318-', '\u135A', '\u1380-', '\u138F',
		'\u13A0-', '\u13F4', '\u1401-', '\u166C', '\u166F-', '\u167F',
		'\u1681-', '\u169A', '\u16A0-', '\u16EA', '\u1700-', '\u170C',
		'\u170E-', '\u1711', '\u1720-', '\u1731', '\u1740-', '\u1751',
		'\u1760-', '\u176C', '\u176E-', '\u1770', '\u1780-', '\u17B3', '\u17D7',
		'\u17DC', '\u1820-', '\u1877', '\u1880-', '\u18A8', '\u18AA', '\u18B0-',
		'\u18F5', '\u1900-', '\u191C', '\u1950-', '\u196D', '\u1970-', '\u1974',
		'\u1980-', '\u19AB', '\u19C1-', '\u19C7',

		'\u1A00-', '\u1A16', '\u1A20-', '\u1A54', '\u1AA7', '\u1B05-', '\u1B33',
		'\u1B45-', '\u1B4B', '\u1B83-', '\u1BA0', '\u1BAE', '\u1BAF', '\u1C00-',
		'\u1C23', '\u1C4D-', '\u1C4F', '\u1C5A-', '\u1C7D', '\u1CE9-', '\u1CEC',
		'\u1CEE-', '\u1CF1', '\u1D00-', '\u1DBF', '\u1E00-', '\u1F15',
		'\u1F18-', '\u1F1D', '\u1F20-', '\u1F45', '\u1F48-', '\u1F4D',
		'\u1F50-', '\u1F57', '\u1F59', '\u1F5B', '\u1F5D', '\u1F5F-', '\u1F7D',
		'\u1F80-', '\u1FB4', '\u1FB6-', '\u1FBC', '\u1FBE', '\u1FC2-', '\u1FC4',
		'\u1FC6-', '\u1FCC', '\u1FD0-', '\u1FD3', '\u1FD6-', '\u1FDB',
		'\u1FE0-', '\u1FEC', '\u1FF2-', '\u1FF4', '\u1FF6-', '\u1FFC',

		'\u2071', '\u207F', '\u2090-', '\u2094', '\u2102', '\u2107', '\u210A-',
		'\u2113', '\u2115', '\u2119-', '\u211D', '\u2124', '\u2126', '\u2128',
		'\u212A-', '\u212D', '\u212F-', '\u2139', '\u213C-', '\u213F',
		'\u2145-', '\u2149', '\u214E', '\u2183', '\u2184', '\u2C00-', '\u2C2E',
		'\u2C30-', '\u2C5E', '\u2C60-', '\u2CE4', '\u2CEB-', '\u2CEE',
		'\u2D00-', '\u2D25', '\u2D30-', '\u2D65', '\u2D6F', '\u2D80-', '\u2D96',
		'\u2DA0-', '\u2DA6', '\u2DA8-', '\u2DAE', '\u2DB0-', '\u2DB6',
		'\u2DB8-', '\u2DBE', '\u2DC0-', '\u2DC6', '\u2DC8-', '\u2DCE',
		'\u2DD0-', '\u2DD6', '\u2DD8-', '\u2DDE', '\u2E2F',

		'\u3005', '\u3006', '\u3031-', '\u3035', '\u303B', '\u303C', '\u3041-',
		'\u3096', '\u309D-', '\u309F', '\u30A1-', '\u30FA', '\u30FC-', '\u30FF',
		'\u3105-', '\u312D', '\u3131-', '\u318E', '\u31A0-', '\u31B7',
		'\u31F0-', '\u31FF', '\u3400-',

		'\u4DB5', '\u4E00-',

		'\u9FCB',

		'\uA000-', '\uA48C', '\uA4D0-', '\uA4FD', '\uA500-', '\uA60C',
		'\uA610-', '\uA61F', '\uA62A', '\uA62B', '\uA640-', '\uA65F', '\uA662-',
		'\uA66E', '\uA67F-', '\uA697', '\uA6A0-', '\uA6E5', '\uA717-', '\uA71F',
		'\uA722-', '\uA788', '\uA78B', '\uA78C', '\uA7FB-', '\uA801', '\uA803-',
		'\uA805', '\uA807-', '\uA80A', '\uA80C-', '\uA822', '\uA840-', '\uA873',
		'\uA882-', '\uA8B3', '\uA8F2-', '\uA8F7', '\uA8FB', '\uA90A-', '\uA925',
		'\uA930-', '\uA946', '\uA960-', '\uA97C', '\uA984-', '\uA9B2', '\uA9CF',
		'\uAA00-', '\uAA28', '\uAA40-', '\uAA42', '\uAA44-', '\uAA4B',
		'\uAA60-', '\uAA76', '\uAA7A', '\uAA80-', '\uAAAF', '\uAAB1', '\uAAB5',
		'\uAAB6', '\uAAB9-', '\uAABD', '\uAAC0', '\uAAC2', '\uAADB-', '\uAADD',
		'\uABC0-', '\uABE2', '\uAC00-',

		'\uD7A3', '\uD7B0-', '\uD7C6', '\uD7CB-', '\uD7FB',

		'\uF900-', '\uFA2D', '\uFA30-', '\uFA6D', '\uFA70-', '\uFAD9',
		'\uFB00-', '\uFB06', '\uFB13-', '\uFB17', '\uFB1D', '\uFB1F-', '\uFB28',
		'\uFB2A-', '\uFB36', '\uFB38-', '\uFB3C', '\uFB3E', '\uFB40', '\uFB41',
		'\uFB43', '\uFB44', '\uFB46-', '\uFBB1', '\uFBD3-', '\uFD3D', '\uFD50-',
		'\uFD8F', '\uFD92-', '\uFDC7', '\uFDF0-', '\uFDFB', '\uFE70-', '\uFE74',
		'\uFE76-', '\uFEFC', '\uFF21-', '\uFF3A', '\uFF41-', '\uFF5A',
		'\uFF66-', '\uFFBE', '\uFFC2-', '\uFFC7', '\uFFCA-', '\uFFCF',
		'\uFFD2-', '\uFFD7', '\uFFDA-', '\uFFDC'
	];

	/**
	 * Regular expression that matches a white space character.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var WHITE_SPACE = new RegExp('[' + WHITE_SPACE_CHARACTERS.join('') + ']');

	/**
	 * Regular expression that matches one or more white space characters.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var WHITE_SPACES = new RegExp('[' + WHITE_SPACE_CHARACTERS.join('') + ']+');

	/**
	 * Regular expression that matches a zero width character.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var ZERO_WIDTH_SPACE = new RegExp('[' + ZERO_WIDTH_CHARACTERS.join('') + ']');

	/**
	 * Regular expression that matches a non breaking space character.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var NON_BREAKING_SPACE = new RegExp('[' + NON_BREAKING_SPACE_CHARACTERS.join('') + ']');

	var joinedWhiteSpaces = WHITE_SPACE_CHARACTERS.join('');

	/**
	 * Matches space characters.
	 *
	 * This includes all white space characters (matched with "\s"), and
	 * the zero-width character ("\u200B").
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var SPACE = new RegExp('['
	          + joinedWhiteSpaces
	          + ZERO_WIDTH_CHARACTERS.join('')
	          + NON_BREAKING_SPACE_CHARACTERS.join('')
	          + ']');

	/**
	 * Matches non-space characters.  Complement to Strings.SPACE.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var NOT_SPACE = new RegExp('[^'
	              + joinedWhiteSpaces
	              + ZERO_WIDTH_CHARACTERS.join('')
	              + NON_BREAKING_SPACE_CHARACTERS.join('')
	              + ']');

	var wbc = WORD_BREAKING_CHARACTERS.join('');

	/**
	 * This RegExp is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf strings
	 */
	var WORD_BREAKING_CHARACTER = new RegExp('[' + wbc + ']');

	/**
	 * Matches a word boundary.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var WORD_BOUNDARY = new RegExp('[^' + wbc + ']');

	/**
	 * Matches one or more sequence of characters denoting a word boundary from
	 * the end of a string.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var WORD_BOUNDARY_FROM_END = new RegExp('[^' + wbc + '][' + wbc + ']*$');

	/**
	 * Regex matches C0 and C1 control codes, which seems to be good enough.
	 * "The C0 set defines codes in the range 00HEX–1FHEX and the C1
	 * set defines codes in the range 80HEX–9FHEX."
	 * In addition, we include \x007f which is "delete", which just
	 * seems like a good idea.
	 * http://en.wikipedia.org/wiki/List_of_Unicode_characters
	 * http://en.wikipedia.org/wiki/C0_and_C1_control_codes
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var CONTROL_CHARACTER = /[\x00-\x1f\x7f-\x9f]/;

	/**
	 * Matches white spaces at the beginning or ending of a string.
	 *
	 * @type {RegExp}
	 * @memberOf strings
	 */
	var TERMINAL_WHITE_SPACES = new RegExp(
		'^[' + joinedWhiteSpaces + ']+|[' + joinedWhiteSpaces + ']+$'
	);

	/**
	 * Splits a string into a list of individual words.
	 *
	 * Words are non-empty sequences of non-space characaters.
	 *
	 * @param  {string} str
	 * @return {Array.<string>}
	 * @memberOf strings
	 */
	function words(str) {
		str = str.trim().replace(TERMINAL_WHITE_SPACES, '');
		if (isEmpty(str)) {
			return [];
		}
		return str.split(/\s+/g);
	}

	/**
	 * Converts a dashes form into camel cased form.
	 *
	 * The given string should be all lowercase and should not begin with a
	 * dash.
	 *
	 * For example 'data-my-attr' becomes 'dataMyAttr'.
	 *
	 * @param {string} str
	 * @memberOf strings
	 */
	var dashesToCamelCase = (function () {
		var dashPrefixedCharacter = /[\-]([a-z])/gi;
		var raiseCase = function (all, upper) {
			return upper.toUpperCase();
		};
		return function dashesToCamelCase(str) {
			return str.replace(dashPrefixedCharacter, raiseCase);
		};
	}());

	/**
	 * Converts a camel cased form into dashes form.
	 *
	 * The given string should begin with a lowercase letter and should not
	 * contain dashes.
	 *
	 * For example
	 * 'dataMyAttr' becomes 'data-my-attr',
	 * 'dataAB'     becomes 'data-a-b'.
	 *
	 * @param  {string} str
	 * @return {string}
	 * @memberOf strings
	 */
	var camelCaseToDashes = (function () {
		var uppercaseCharacter = /[A-Z]/g;
		var addDashes = function (match) {
			return '-' + match.toLowerCase();
		};
		return function camelCaseToDashes(str) {
			return str.replace(uppercaseCharacter, addDashes);
		};
	}());

	/**
	 * Split `str` along `pattern`, including matches in the result.
	 *
	 * splitIncl("foo-bar", /\-/g) results in ["foo", "-", "bar"]
	 *
	 * whereas
	 *
	 * "foo-bar".split(/\-/g) results in ["foo", "bar"]
	 *
	 * The given regular expression must include the g flag, otherwise will
	 * result in an endless loop.
	 *
	 * The resulting list of substrings will be in the order they appeared in
	 * `str`.
	 *
	 * @param  {RegExp} pattern
	 * @return {Array.<string>}
	 * @memberOf strings
	 */
	function splitIncl(str, pattern) {
		var result = [];
		var lastIndex = 0;
		var match;
		while (null != (match = pattern.exec(str))) {
			if (lastIndex < match.index) {
				result.push(str.substring(lastIndex, match.index));
				lastIndex = match.index;
			}
			lastIndex += match[0].length;
			result.push(match[0]);
		}
		if (lastIndex < str.length) {
			result.push(str.substring(lastIndex, str.length));
		}
		return result;
	}

	/**
	 * Returns true for the empty string, null and undefined.
	 *
	 * @param  {string=} str
	 * @return {boolean}
	 * @memberOf strings
	 */
	function isEmpty(str) {
		return '' === str || null == str;
	}

	/**
	 * Returns true if the given character is a control character.  Control
	 * characters are usually not rendered if they are inserted into the DOM.
	 *
	 * Returns false for whitespace 0x20 (which may or may not be rendered see
	 * Html.isUnrenderedWhitespace()) and non-breaking whitespace 0xa0 but
	 * returns true for tab 0x09 and linebreak 0x0a and 0x0d.
	 *
	 * @param  {string} chr
	 * @return {boolean}
	 * @memberOf strings
	 */
	function isControlCharacter(chr) {
		return CONTROL_CHARACTER.test(chr);
	}

	/**
	 * Adds one or more entries to a space-delimited list.
	 * Will return a new space delimited list with the new
	 * entries added to the end.
	 *
	 * The function is designed to deal with shoddy
	 * whitespace separations such as multiple spaces or
	 * even newlines, that may be used on a DOM Element's
	 * class attribute.
	 *
	 * @param  {!string}    list
	 * @param  {...!string} entry
	 * @return {string}
	 * @memberOf strings
	 */
	function addToList(list) {
		var listEntries = list.split(WHITE_SPACES);
		var newEntries = Arrays.coerce(arguments).slice(1);
		var newList = [];

		for (var i=0; i<listEntries.length; i++) {
			if (listEntries[i]) {
				newList.push(listEntries[i]);
			}
		}
		for (i=0; i<newEntries.length; i++) {
			if (newEntries[i]) {
				newList.push(newEntries[i]);
			}
		}
		return newList.join(' ');
	}

	/**
	 * Removes one or more entries from a space-delimited list.
	 * Will return a new space delimited list with the specified
	 * entries removed.
	 *
	 * The function is designed to deal with shoddy
	 * whitespace separations such as multiple spaces or
	 * even newlines, that may be used on a DOM Element's
	 * class attribute.
	 *
	 * @param  {!string}    list
	 * @param  {...!string} entry
	 * @return {string}
	 * @memberOf strings
	 */
	function removeFromList(list) {
		var listArray = list.split(WHITE_SPACES);
		var removeEntries = Arrays.coerce(arguments).slice(1);
		return Arrays.difference(listArray, removeEntries).join(' ');
	}

	/**
	 * Produces a space-delimited list with unique entries from
	 * the provided list. Example:
	 * 'one two three two four two four five' => 'one two three four five'
	 *
	 * @param  {!string} list
	 * @return {string}
	 * @memberOf strings
	 */
	function uniqueList(list) {
		return Arrays.unique(list.split(WHITE_SPACES)).join(' ');
	}

	return {
		addToList                     : addToList,
		removeFromList                : removeFromList,
		uniqueList                    : uniqueList,

		words                         : words,
		splitIncl                     : splitIncl,

		dashesToCamelCase             : dashesToCamelCase,
		camelCaseToDashes             : camelCaseToDashes,

		isEmpty                       : isEmpty,
		isControlCharacter            : isControlCharacter,

		CONTROL_CHARACTER             : CONTROL_CHARACTER,
		SPACE                         : SPACE,
		NOT_SPACE                     : NOT_SPACE,
		WHITE_SPACE                   : WHITE_SPACE,
		WHITE_SPACES                  : WHITE_SPACES,
		ZERO_WIDTH_SPACE              : ZERO_WIDTH_SPACE,
		NON_BREAKING_SPACE            : NON_BREAKING_SPACE,
		WORD_BOUNDARY                 : WORD_BOUNDARY,
		WORD_BOUNDARY_FROM_END        : WORD_BOUNDARY_FROM_END,
		WORD_BREAKING_CHARACTER       : WORD_BREAKING_CHARACTER,
		TERMINAL_WHITE_SPACES         : TERMINAL_WHITE_SPACES,

		ZERO_WIDTH_CHARACTERS         : ZERO_WIDTH_CHARACTERS,
		WHITE_SPACE_CHARACTERS        : WHITE_SPACE_CHARACTERS,
		WORD_BREAKING_CHARACTERS      : WORD_BREAKING_CHARACTERS,
		NON_BREAKING_SPACE_CHARACTERS : NON_BREAKING_SPACE_CHARACTERS
	};
});

/**
 * dom/attributes.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('dom/attributes',[
	'maps',
	'strings',
	'functions'
], function (
	Maps,
	Strings,
	Fn
) {
	

	/**
	 * Gets the attributes of the given element.
	 *
	 * Correctly handles the case that IE7 and IE8 have approx 70-90
	 * default attributes on each and every element.
	 *
	 * Attribute values will always be strings, but possibly empty Strings.
	 *
	 * @param  {Element}        elem
	 * @return {Map.<string,string>}
	 * @memberOf dom
	 */
	function attrs(elem) {
		var attrsMap = {};
		var attributes = elem.attributes;
		for (var i = 0, len = attributes.length; i < len; i++) {
			var attr = attributes[i];
			if (typeof attr.specified === 'undefined' || attr.specified) {
				attrsMap[attr.name] = attr.value;
			}
		}
		return attrsMap;
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @alias removeAttr
	 * @memberOf dom
	 */
	function remove(elem, name) {
		elem.removeAttribute(name);
	}

	/**
	 * Removes all attributes from `element`.
	 * @alias removeAttrs
	 * @memberOf dom
	 * @param {Element} element
	 */
	function removeAll(element) {
		Maps.keys(attrs(element)).forEach(Fn.partial(remove, element));
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @alias setAttr
	 * @memberOf dom
	 */
	function set(elem, name, value) {
		if (null == value) {
			remove(elem, name);
		} else {
			elem.setAttribute(name, value);
		}
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @alias getAttr
	 * @memberOf dom
	 */
	function get(elem, name) {
		return elem.getAttribute(name);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @alias getAttrNS
	 * @memberOf dom
	 */
	function getNS(elem, ns, name) {
		return elem.getAttributeNS(ns, name);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @alias removeAttrNS
	 * @memberOf dom
	 */
	function removeNS(elem, ns, name) {
		// TODO is removeAttributeNS(null, ...) the same as removeAttribute(...)?
		if (null != ns) {
			elem.removeAttributeNS(ns, name);
		} else {
			remove(elem, name);
		}
	}

	/**
	 * NB: Internet Explorer supports the setAttributeNS method from
	 * version 9, but only for HTML documents, not for XML documents.
	 *
	 * @param {Element} elem
	 * @param {string}  ns
	 * @param {string}  name
	 * @param {string}  value
	 * @alias setAttrNS
	 * @memberOf dom
	 */
	function setNS(elem, ns, name, value) {
		// TODO is setAttributeNS(null, ...) the same as setAttribute(...)?
		if (null != ns) {
			elem.setAttributeNS(ns, name, value);
		} else {
			set(elem, name, value);
		}
	}

	/**
	 * Checks whether or not the given node contains one or more
	 * attributes non-empty attributes.
	 * @alias hasAttrs
	 * @param  {Node}    node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function has(node) {
		return !Maps.vals(attrs(node)).every(Strings.isEmpty);
	}

	return {
		attrs     : attrs,
		get       : get,
		getNS     : getNS,
		has       : has,
		remove    : remove,
		removeAll : removeAll,
		removeNS  : removeNS,
		set       : set,
		setNS     : setNS
	};
});

/**
 * misc.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * Contains miscellaneous utility functions that don't fit anywhere else.
 */
define('misc',[], function () {
	

	/**
	 * Returns true if any regex in the given rxs array tests true
	 * against str.
	 */
	function anyRx(rxs, str) {
		var i,
		    len;
		for (i = 0, len = rxs.length; i < len; i++) {
			if (rxs[i].test(str)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given value is defined.
	 * @param {*} value
	 *        An object of any type
	 * @return {boolean}
	 *         True of the value of the given object is not undefined.
	 */
	function defined(value) {
		return 'undefined' !== typeof value;
	}

	function copy(obj) {
		if (!obj) {
			return obj;
		}
		var prop;
		var copied = {};
		for (prop in obj) {
			if (obj.hasOwnProperty(prop)) {
				copied[prop] = obj[prop];
			}
		}
		return copied;
	}

	return {
		anyRx   : anyRx,
		defined : defined,
		copy    : copy
	};
});

/**
 * dom/nodes.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('dom/nodes',[
	'misc',
	'arrays'
], function (
	Misc,
	Arrays
) {
	

	/**
	 * Numeric codes that represent the type of DOM interface node types.
	 *
	 * @type {Object.<string, number>}
	 * @enum {number}
	 * @memberOf dom
	 */
	var Nodes = {
		ELEMENT: 1,
		ATTR: 2,
		TEXT: 3,
		CDATA_SECTION: 4,
		ENTITY_REFERENCE: 5,
		ENTITY: 6,
		PROCESSING_INSTRUCTION: 7,
		COMMENT: 8,
		DOCUMENT: 9,
		DOCUMENTTYPE: 10,
		DOCUMENT_FRAGMENT: 11,
		NOTATION: 12
	};

	/**
	 * Returns `true` if `node` is a text node.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function isTextNode(node) {
		return Nodes.TEXT === node.nodeType;
	}

	/**
	 * Checks whether the given node is an Element.
	 *
	 * @param {Node} node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function isElementNode(node) {
		return Nodes.ELEMENT === node.nodeType;
	}

	/**
	 * Checks whether the given node is an document fragment element.
	 *
	 * @param   {Node} node
	 * @returns {boolean}
	 * @memberOf dom
	 */
	function isFragmentNode(node) {
		return Nodes.DOCUMENT_FRAGMENT === node.nodeType;
	}

	/**
	 * Calculates the number of child nodes contained in the given DOM element.
	 *
	 * NB elem.childNodes.length is unreliable because "IE up to 8 does not count
	 * empty text nodes." (http://www.quirksmode.org/dom/w3c_core.html)
	 *
	 * @param  {Element} elem
	 * @return {number} Number of children contained in the given node.
	 * @memberOf dom
	 */
	function numChildren(elem) {
		return elem.childNodes.length;
	}

	/**
	 * Returns a non-live array of all child nodes belonging to `elem`.
	 *
	 * @param  {Element} elem
	 * @return {Array.<Node>}
	 * @memberOf dom
	 */
	function children(elem) {
		return Arrays.coerce(elem.childNodes);
	}

	/**
	 * Calculates the positional index of the given node inside of its parent
	 * element.
	 *
	 * @param  {Node} node
	 * @return {number} The zero-based index of the given node's position.
	 * @memberOf dom
	 */
	function nodeIndex(node) {
	    var i = 0;
	    while ((node = node.previousSibling)) {
	        i++;
	    }
	    return i;
	}

	/**
	 * Determines the length of the given DOM node.
	 *
	 * @param  {Node} node
	 * @return {number} Length of the given node.
	 * @memberOf dom
	 */
	function nodeLength(node) {
		if (isElementNode(node) || isFragmentNode(node)) {
			return numChildren(node);
		}
		if (isTextNode(node)) {
			return node.length;
		}
		return 0;
	}

	/**
	 * Checks is `element` has children
	 * @param  {Element} element
	 * @return {boolean}
	 * @memberOf dom
	 */
	function hasChildren(element) {
		return numChildren(element) > 0;
	}

	/**
	 * Get the nth (zero based) child of the given element.
	 * 
	 * NB elem.childNodes.length is unreliable because "IE up to 8 does not count
	 * empty text nodes." (http://www.quirksmode.org/dom/w3c_core.html)
	 *
	 * @param  {Element} elem
	 * @param  {number}  offset Offset of the child to return.
	 * @return {Element} The child node at the given offset.
	 * @memberOf dom
	 */
	function nthChild(elem, offset) {
		return elem.childNodes[offset];
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @param node if a text node, should have a parent node.
	 * @memberOf dom
	 */
	function nodeAtOffset(node, offset) {
		if (isElementNode(node) && offset < nodeLength(node)) {
			node = nthChild(node, offset);
		} else if (isTextNode(node) && offset === node.length) {
			node = node.nextSibling || node.parentNode;
		}
		return node;
	}

	/**
	 * Checks whether the given node is an empty text node, conveniently.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function isEmptyTextNode(node) {
		return isTextNode(node) && 0 === nodeLength(node);
	}

	/**
	 * Checks is `node1` is the same as `node2`.
	 * @param {Node} node1
	 * @param {Node} node2
	 * @returns {boolean}
	 * @memberOf dom
	 */
	function isSameNode(node1, node2) {
		return node1 === node2;
	}

	function translateNodeIndex(elem, normalizedIndex, realIndex) {
		var index = 0;
		var currNormalizedIndex = 0;
		var child = elem.firstChild;
		for (;;) {
			if (currNormalizedIndex >= normalizedIndex) {
				return index;
			}
			if (index >= realIndex) {
				return currNormalizedIndex;
			}
			if (!child) {
				break;
			}
			if (isTextNode(child)) {
				var nonEmptyRealIndex = -1;
				while (child && isTextNode(child)) {
					if (!isEmptyTextNode(child)) {
						nonEmptyRealIndex = index;
					}
					child = child.nextSibling;
					index += 1;
				}
				if (-1 !== nonEmptyRealIndex) {
					if (nonEmptyRealIndex >= realIndex) {
						return currNormalizedIndex;
					}
					currNormalizedIndex += 1;
				}
			} else {
				child = child.nextSibling;
				index += 1;
				currNormalizedIndex += 1;
			}
		}
		throw Error();
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf dom
	 */
	function realFromNormalizedIndex(elem, normalizedIndex) {
		return translateNodeIndex(elem, normalizedIndex, Number.POSITIVE_INFINITY);
	}

	function normalizedFromRealIndex(elem, realIndex) {
		return translateNodeIndex(elem, Number.POSITIVE_INFINITY, realIndex);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf dom
	 */
	function normalizedNumChildren(elem) {
		return normalizedFromRealIndex(elem, numChildren(elem));
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf dom
	 */
	function normalizedNodeIndex(node) {
		return normalizedFromRealIndex(node.parentNode, nodeIndex(node));
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf dom
	 */
	function normalizedNthChild(elem, normalizedIndex) {
		return nthChild(elem, realFromNormalizedIndex(elem, normalizedIndex));
	}

	/**
	 * Returns `true` if node `b` is a descendant of node `a`, `false`
	 * otherwise.
	 *
	 * @see http://ejohn.org/blog/comparing-document-position/
	 * @see http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
	 *
	 * @TODO Contains seems to be problematic on Safari, is this an issue for us?
	 * Should we just use compareDocumentPosition() since we only need IE > 9 anyway?
	 * https://code.google.com/p/google-web-toolkit/issues/detail?id=1218
	 *
	 * @param  {Node} a
	 * @param  {Node} b
	 * @return {boolean}
	 * @memberOf dom
	 */
	function contains(a, b) {
		return (isElementNode(a)
				? (a.compareDocumentPosition
				   ? !!(a.compareDocumentPosition(b) & 16)
				   : (a !== b
				      // Because IE returns false for elemNode.contains(textNode).
				      && (isElementNode(b)
				          ? a.contains(b)
				          : (b.parentNode
				             && (a === b.parentNode || a.contains(b.parentNode))))))
		        : false);
	}

	/**
	 * Checks whether node `other` comes after `node` in the document order.
	 *
	 * @fixme rename to follows()
	 * @param  {Node} node
	 * @param  {Node} other
	 * @return {boolean}
	 * @memberOf dom
	 */
	function followedBy(node, other) {
		return !!(node.compareDocumentPosition(other) & 4);
	}

	/**
	 * Calculates the offset of the given node inside the document.
	 *
	 * @param  {Node} node
	 * @return {Object.<string, number>}
	 * @memberOf dom
	 */
	function offset(node) {
		if (!Misc.defined(node.getBoundingClientRect)) {
			return {
				top: 0,
				left: 0
			};
		}
		var box = node.getBoundingClientRect();
		return {
			top  : box.top  + window.pageYOffset - node.ownerDocument.body.clientTop,
			left : box.left + window.pageXOffset - node.ownerDocument.body.clientLeft
		};
	}

	/**
	 * Gets the textContent from a node.
	 *
	 * @param  {Node} node
	 * @return {string}
	 * @memberOf dom
	 */
	function text(node) {
		return node.textContent;
	}

	/**
	 * Checks the givne element has any textContent.
	 *
	 * @param  {Element} element
	 * @return {boolean}
	 * @memberOf dom
	 */
	function hasText(element) {
		return text(element).trim().length > 0;
	}

	/**
	 * Checks whether two nodes are equal.
	 *
	 * @param  {Node} node
	 * @param  {Node} other
	 * @return {boolean}
	 * @memberOf dom
	 */
	function equals(node, other) {
		return node.isEqualNode(other);
	}

	/**
	 * Returns a deep clone of the given node.
	 *
	 * @param  {Node}    node
	 * @param  {boolean} deeply Whether or not to do a deep clone
	 * @return {Node}
	 * @memberOf dom
	 */
	function clone(node, deeply) {
		return node.cloneNode(('boolean' === typeof deeply) ? deeply : true);
	}

	/**
	 * Returns a shallow clone of the given node.
	 *
	 * @param  {Node} node
	 * @return {Node}
	 * @memberOf dom
	 */
	function cloneShallow(node) {
		return node.cloneNode(false);
	}

	/**
	 * Retrieve the HTML of the entire given node.
	 * This is equivalent as outerHTML for element nodes.
	 * This function is an alternative for outerHTML with for document
	 * fragments.
	 *
	 * @param  {Node}
	 * @return {string}
	 * @memberOf dom
	 */
	function outerHtml(node) {
		var div = node.ownerDocument.createElement('div');
		// Because if node is a document fragment, appending it will cause it to
		// be emptied of its child nodes
		div.appendChild(node.cloneNode(true));
		return div.innerHTML;
	}

	return {
		Nodes    : Nodes,
		offset   : offset,
		children : children,

		nodeAtOffset : nodeAtOffset,
		nthChild     : nthChild,
		numChildren  : numChildren,
		nodeIndex    : nodeIndex,
		nodeLength   : nodeLength,
		hasChildren  : hasChildren,

		normalizedNthChild      : normalizedNthChild,
		normalizedNodeIndex     : normalizedNodeIndex,
		realFromNormalizedIndex : realFromNormalizedIndex,
		normalizedNumChildren   : normalizedNumChildren,

		isTextNode      : isTextNode,
		isElementNode   : isElementNode,
		isFragmentNode  : isFragmentNode,
		isEmptyTextNode : isEmptyTextNode,
		isSameNode      : isSameNode,

		text    : text,
		hasText : hasText,

		equals     : equals,
		contains   : contains,
		followedBy : followedBy,

		clone        : clone,
		cloneShallow : cloneShallow,

		outerHtml    : outerHtml
	};
});

/**
 * dom/classes.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('dom/classes',[
	'dom/nodes',
	'arrays',
	'strings'
], function (
	Nodes,
	Arrays,
	Strings
) {
	

	/**
	 * Uses a string modifier function such as Strings.addToList to
	 * modify the classList of an element
	 *
	 * @private
	 * @param {!Element}                   elem
	 * @param {function(...string):string} modify
	 * @param {Array.<string>} classes
	 */
	function modifyClassList(elem, modify, classes) {
		elem.className = Strings.uniqueList(modify.apply(
			null,
			[elem.className].concat(classes)
		));
	}

	/**
	 * Adds one or more classes to current classes of the given Element.
	 *
	 * @param {!Element}  elem
	 * @param {...string} className
	 * @alias addClass
	 * @memberOf dom
	 */
	function add(elem) {
		modifyClassList(elem, Strings.addToList, Arrays.coerce(arguments).slice(1));
	}

	/**
	 * Removes one or more class names from the given element's
	 * classList.
	 *
	 * @param  {!Element}  elem
	 * @param  {...string} className
	 * @alias removeClass
	 * @memberOf dom
	 */
	function remove(elem) {
		modifyClassList(elem, Strings.removeFromList, Arrays.coerce(arguments).slice(1));
	}

	/**
	 * Checks whether the given node has the specified class.
	 *
	 * @param  {Node}    node
	 * @param  {string}  value
	 * @return {boolean}
	 * @alias hasClass
	 * @memberOf dom
	 */
	function has(node, value) {
		return Nodes.isElementNode(node)
		    && node.className.trim().split(Strings.WHITE_SPACES).indexOf(value) >= 0;
	}

	function toggle(node, name, flag) {
		if (true === flag) {
			add(node, name);
		} else if (false === flag) {
			remove(node, name);
		} else {
			toggle(node, name, has(node, name));
		}
	}

	return {
		has    : has,
		add    : add,
		remove : remove,
		toggle : toggle
	};
});

/**
 * dom/mutation.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('dom/mutation',[
	'dom/nodes',
	'functions',
	'arrays'
], function (
	Nodes,
	Fn,
	Arrays
) {
	

	/**
	 * Inserts the given node before `ref`, unless `atEnd` is true, in which
	 * case `node` is inserted at the end of `ref` children nodes.
	 *
	 * @param {Node}    node
	 * @param {Node}    ref
	 * @param {boolean} atEnd
	 * @memberOf dom
	 */
	function insert(node, ref, atEnd) {
		if (atEnd) {
			ref.appendChild(node);
		} else {
			ref.parentNode.insertBefore(node, ref);
		}
	}

	/**
	 * Inserts the given node after `ref`.
	 *
	 * @param {Node} node
	 * @param {Node} ref
	 * @memberOf dom
	 */
	function insertAfter(node, ref) {
		insert(node, ref.nextSibling || ref.parentNode, !ref.nextSibling);
	}

	/**
	 * Like insertBefore, inserts node `first` into `parent` before
	 * `reference`, except that it also inserts all the following siblings of
	 * `first`.
	 *
	 * @param {Element} parent
	 * @param {Node}    first
	 * @param {Node}    reference
	 * @memberOf dom
	 */
	function moveNextAll(parent, first, reference) {
		var next;
		while (first) {
			next = first.nextSibling;
			parent.insertBefore(first, reference);
			first = next;
		}
	}

	/**
	 * Insert node at end of destination.
	 *
	 * @param {Node}    node
	 * @param {Element} destination
	 * @memberOf dom
	 */
	function append(node, destination) {
		insert(node, destination, true);
	}

	/**
	 * Applies `func` on the elements of `list` until the given predicate
	 * returns true.
	 *
	 * @private
	 * @param  {Array.<*>}           list
	 * @param  {function(*)}         func
	 * @param  {function(*):boolean} until
	 * @return {Array.<*>}           dropWhile() of the given list
	 */
	function walkUntil(list, func, until) {
		var lists = Arrays.split(list, until || Fn.returnFalse);
		lists[0].forEach(func);
		return lists[1];
	}

	/**
	 * Moves the list of nodes into the end of destination element, until
	 * `until` returns true.
	 *
	 * @param  {Array.<Nodes>}          nodes
	 * @param  {Element}                destination
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Nodes>}          The nodes that were not moved
	 * @memberOf dom
	 */
	function move(nodes, destination, until) {
		return walkUntil(nodes, function (node) {
			append(node, destination);
		}, until);
	}

	/**
	 * Copies the list of nodes into a destination element, until `until`
	 * returns true.
	 *
	 * @param  {Array.<Nodes>}          nodes
	 * @param  {Element}                destination
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Nodes>}          The nodes that were not copied
	 * @memberOf dom
	 */
	function copy(nodes, destination, until) {
		return walkUntil(nodes, function (node) {
			append(Nodes.clone(node), destination);
		}, until);
	}

	/**
	 * Moves the list of nodes before the reference element, until `until`
	 * returns true.
	 *
	 * @param  {Array.<Nodes>}          nodes
	 * @param  {Element}                reference
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Nodes>}          The nodes that were not moved
	 * @memberOf dom
	 */
	function moveBefore(nodes, reference, until) {
		return walkUntil(nodes, function (node) {
			insert(node, reference, false);
		}, until);
	}

	/**
	 * Moves the list of nodes after the reference element, until `until`
	 * returns true.
	 *
	 * @param  {Array.<Nodes>}          nodes
	 * @param  {Element}                reference
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Nodes>}          The nodes that were not moved
	 * @memberOf dom
	 */
	function moveAfter(nodes, reference, until) {
		return walkUntil(nodes, function (node) {
			insertAfter(node, reference);
			reference = node;
		}, until);
	}

	/**
	 * Replaces the given node with `replacement`.
	 *
	 * @param  {Node} node
	 * @param  {Node} replacement
	 * @return {Node} Replaced node
	 * @memberOf dom
	 */
	function replace(node, replacement) {
		return node.parentNode.replaceChild(replacement, node);
	}

	/**
	 * Replaces the given element while preserving its contents.
	 *
	 * This function facilitates re-wrapping of contents from one element to
	 * another.
	 *
	 * The element that replaces `element` will receive all of the given
	 * element's content.
	 *
	 * @param  {Element} element
	 * @param  {Element} replacement
	 * @return {Element} Replaced element
	 * @memberOf dom
	 */
	function replaceShallow(element, replacement) {
		move(Nodes.children(element), replacement);
		return replace(element, replacement);
	}

	/**
	 * Detaches the given node.
	 *
	 * @param {Node} node
	 * @memberOf dom
	 */
	function remove(node) {
		node.parentNode.removeChild(node);
	}

	/**
	 * Removes the given node while keeping it's content intact.
	 *
	 * @param {Node} node
	 * @memberOf dom
	 */
	function removeShallow(node) {
		moveBefore(Nodes.children(node), node);
		remove(node);
	}

	/**
	 * Wraps `node` in given `wrapper` element.
	 *
	 * @param {Node}    node
	 * @param {Element} wrapper
	 * @memberOf dom
	 */
	function wrap(node, wrapper) {
		append(replace(node, wrapper), wrapper);
	}

	/**
	 * Wrap the node with a `nodeName` element.
	 *
	 * @param  {Element} node
	 * @param  {string}  nodeName
	 * @return {Element} The wrapper element
	 * @memberOf dom
	 */
	function wrapWith(node, nodeName) {
		var wrapper = node.ownerDocument.createElement(nodeName);
		wrap(node, wrapper);
		return wrapper;
	}

	/**
	 * Removes all children from `node`.
	 * @param {Node} node
	 * @memberOf dom
	 */
	function removeChildren(node) {
		Nodes.children(node).forEach(remove);
	}

	/**
	 * Merges all contents of `right` into `left` by appending them to the end
	 * of `left`, and then removing `right`.
	 *
	 * Will not merge text nodes since this would require that ranges be
	 * preserved.
	 *
	 * @param {Node} left
	 * @param {Node} right
	 * @memberOf dom
	 */
	function merge(left, right) {
		var next;
		while (left && right && (left.nodeName === right.nodeName)) {
			if (Nodes.isTextNode(left)) {
				return;
			}
			next = right.firstChild;
			moveNextAll(left, next, null);
			remove(right);
			if (!next) {
				return;
			}
			right = next;
			left = right.previousSibling;
		}
	}

	return {
		append            : append,
		merge             : merge,
		moveNextAll       : moveNextAll,
		moveAfter         : moveAfter,
		moveBefore        : moveBefore,
		move              : move,
		copy              : copy,
		wrap              : wrap,
		wrapWith          : wrapWith,
		insert            : insert,
		insertAfter       : insertAfter,
		replace           : replace,
		replaceShallow    : replaceShallow,
		remove            : remove,
		removeShallow     : removeShallow,
		removeChildren    : removeChildren
	};
});

/**
 * dom/styles.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('dom/style',['strings'], function (Strings) {
	

	/**
	 * Sets a style on the given element by modifying its style attribute.
	 *
	 * @param  {Element} elem
	 * @param  {string}  name  Style property
	 * @return {string}  value Style property value
	 * @alias setStyle
	 * @memberOf dom
	 */
	function set(elem, name, value) {
		name = Strings.dashesToCamelCase(name);
		var styles = elem.style;
		if (name in styles) {
			styles[name] = value;
		}
	}

	/**
	 * Gets a style from the given element's style attribute.
	 * Note that this is different from the computed/inherited style.
	 *
	 * @param  {Element} elem
	 * @param  {string}  name Style property
	 * @return {?string} Style value or null if none is found
	 * @alias getStyle
	 * @memberOf dom
	 */
	function get(elem, name) {
		// Because IE7 needs dashesToCamelCase().
		name = Strings.dashesToCamelCase(name);
		return elem.style[name];
	}

	/**
	 * Gets a style from the given element's style attribute.
	 * Note that this is different from the computed/inherited style.
	 *
	 * The return value will be an object of computed style values mapped
	 * agains their name.
	 *
	 * @param  {Element}                 elem
	 * @param  {Array.<string>}          names
	 * @return {Object.<string, string>}
	 * @memberOf dom
	 */
	function getComputedStyles(elem, names) {
		var props = {};
		var doc = elem.ownerDocument;
		if (doc && doc.defaultView && doc.defaultView.getComputedStyle) {
			var styles = doc.defaultView.getComputedStyle(elem, null);
			if (styles) {
				names.forEach(function (name) {
					props[name] = styles[name] || styles.getPropertyValue(name);
				});
			}
		} else if (elem.currentStyle) {
			names.forEach(function (name) {
				props[name] = elem.currentStyle[name];
			});
		}
		return props;
	}

	/**
	 * Gets the computed/inherited style of the given node.
	 *
	 * @param  {Element} elem
	 * @param  {string}  name Style property name.
	 * @return {?string} Computed style, or `null` if no such style is set
	 * @memberOf dom
	 */
	function getComputedStyle(elem, name) {
		var doc = elem.ownerDocument;
		if (doc && doc.defaultView && doc.defaultView.getComputedStyle) {
			var styles = doc.defaultView.getComputedStyle(elem, null);
			if (styles) {
				return styles[name] || styles.getPropertyValue(name);
			}
		}
		if (elem.currentStyle) {
			// Because IE7 needs dashesToCamelCase().
			name = Strings.dashesToCamelCase(name);
			return elem.currentStyle[name];
		}
		return null;
	}

	/**
	 * Removes the given style property from the given DOM element.
	 *
	 * The style attribute is removed completely if it is left empty
	 * after removing the style.
	 *
	 * @param {Element} elem
	 * @param {string}  styleName
	 * @alias removeStyle
	 * @memberOf dom
	 */
	function remove(elem, styleName) {
		elem.style.removeProperty(styleName);
		if (Strings.isEmpty(elem.getAttribute('style'))) {
			elem.removeAttribute('style');
		}
	}

	return {
		set               : set,
		get               : get,
		remove            : remove,
		getComputedStyle  : getComputedStyle,
		getComputedStyles : getComputedStyles
	};
});

/* dom/traversing.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @reference:
 * 	http://www.w3.org/TR/DOM-Level-3-Core/glossary.html#dt-document-order
 *	https://en.wikipedia.org/wiki/Tree_traversal#Pre-order
 */
define('dom/traversing',[
	'dom/nodes',
	'functions',
	'arrays'
], function (
	Nodes,
	Fn,
	Arrays
) {
	

	/**
	 * Given a node, will return the node that succeeds it in the document order.
	 *
	 * For example, if this function is called recursively, starting from the
	 * DIV root node in the following DOM tree:
	 *
	 * <pre>
	 * <div>
     *     "one"
     *     <b>
     *         "two"
     *         <u>
     *             <i>
     *                 "three"
     *             </i>
     *         </u>
     *         "four"
     *     </b>
     *     "five"
	 * </div>
	 * </pre>
	 *
	 * forward() will return nodes in the following order:
	 *
	 * <pre>"one" <b>, "two", <u>, <i>, "three", "four", "five"</pre>
	 *
	 * This is depth-first pre-order traversal:
	 * https://en.wikipedia.org/wiki/Tree_traversal#Pre-order
	 *
	 * <pre>
	 *         <div>
	 *         / | \
	 *       /   |   \
	 *     one  <b>  five
	 *         / | \
	 *       /   |   \
	 *     two  <u>  four
	 *           |
	 *          <i>
	 *           |
	 *         three
	 * </pre>
	 *
	 * @param  {!Node} node
	 * @return {?Node}
	 * @memberOf dom
	 */
	function forward(node) {
		if (node.firstChild) {
			return node.firstChild;
		}
		var next = node;
		while (next && !next.nextSibling) {
			next = next.parentNode;
		}
		return next && next.nextSibling;
	}

	/**
	 * Given a node, will return the node that preceeds it in the document
	 * order.
	 *
	 * This backwards depth-first in-order traversal:
	 * https://en.wikipedia.org/wiki/Tree_traversal#In-order
	 *
	 * For example, if this function is called recursively, starting from the
	 * DIV root node in the DOM tree below:
	 * <pre>
	 * <div>
	 *     "one"
	 *     <b>
	 *         "two"
	 *         <u>
	 *             <i>
	 *                 "three"
	 *             </i>
	 *         </u>
	 *         "four"
	 *     </b>
	 *     "five"
	 * </div>
	 * </pre>
	 * backward() will return nodes in the following order:
	 *
	 * <pre>"five", "four", "three", <i>, <u>, "two", <b>, "one"</pre>
	 *
	 * <pre>
	 *         <div>
	 *         / | \
	 *       /   |   \
	 *     one  <b>  five
	 *         / | \
	 *       /   |   \
	 *     two  <u>  four
	 *           |
	 *          <i>
	 *           |
	 *         three
	 * </pre>
	 *
	 * @param  {!Node} node
	 * @return {?Node}
	 * @memberOf dom
	 */
	function backward(node) {
		var prev = node.previousSibling;
		while (prev && prev.lastChild) {
			prev = prev.lastChild;
		}
		return prev || node.parentNode;
	}

	/**
	 * Finds the first node for which `match` returns true by traversing through
	 * `step`.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} match
	 * @param  {function(Node):boolean} until
	 * @param  {function(Node):Node}    step
	 * @return {Node}
	 */
	function find(node, match, until, step) {
		until = until || Fn.returnFalse;
		if (until(node)) {
			return null;
		}
		do {
			node = step(node);
			if (!node || until(node)) {
				return null;
			}
			if (match(node)) {
				return node;
			}
		} while (node);
		return null;
	}

	/**
	 * Finds the first DOM object, ahead of the given node which matches the
	 * predicate `match` but before `until` returns true for any node that is
	 * traversed during the search, in which case null is returned.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} match
	 * @param  {function(Node):boolean} until
	 * @return {Node}
	 * @memberOf dom
	 */
	function findForward(node, match, until) {
		return find(node, match, until, forward);
	}

	/**
	 * Finds the first DOM object, behind the given node which matches the
	 * predicate `match`.  If `until` returns true the given node, for any other
	 * node during traversal, null is returned.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} match
	 * @param  {function(Node):boolean} until
	 * @return {Node}
	 * @memberOf dom
	 */
	function findBackward(node, match, until) {
		return find(node, match, until, backward);
	}

	/**
	 * Walk backward in pre-order-backtracing traversal until the given
	 * predicate returns true.
	 *
	 * Given the following tree structure:
	 *
	 * <pre><div><b>one<i>two</i><b>three</div></pre>
	 *
	 * Will encounter the nodes in the following order:
	 * div, three, b, i, two, i, one, b, div
	 *
	 * @see    https://en.wikipedia.org/wiki/Depth-first_search#Vertex_orderings
	 * @param  {!Node}                   node
	 * @param  {!function(Node):boolean} pred
	 * return  {Node}
	 */
	function backwardPreorderBacktraceUntil(node, pred) {
		var backtracing = true;
		do {
			if (!backtracing && node.lastChild) {
				node = node.lastChild;
			} else if (node.previousSibling) {
				node = node.previousSibling;
				backtracing = false;
			} else {
				node = node.parentNode;
				backtracing = true;
			}
		} while (!pred(node, backtracing));
		return node;
	}

	/**
	 * Walk forward in pre-order-backtracing traversal until the given
	 * predicate returns true.
	 *
	 * @see    https://en.wikipedia.org/wiki/Depth-first_search#Vertex_orderings
	 * @param  {!Node}                   node
	 * @param  {!function(Node):boolean} pred
	 * return  {Node}
	 */
	function forwardPreorderBacktraceUntil(node, pred) {
		var backtracing = false;
		do {
			if (!backtracing && node.firstChild) {
				node = node.firstChild;
			} else if (node.nextSibling) {
				node = node.nextSibling;
				backtracing = false;
			} else {
				node = node.parentNode;
				backtracing = true;
			}
		} while (!pred(node, backtracing));
		return node;
	}

	/**
	 * Returns the given node's next sibling.
	 *
	 * @param  {Node} node
	 * @return {Node}
	 * @memberOf dom
	 */
	function nextSibling(node) {
		return node.nextSibling;
	}

	/**
	 * Returns the given node's previous sibling.
	 *
	 * @param  {Node} node
	 * @return {Node}
	 * @memberOf dom
	 */
	function prevSibling(node) {
		return node.previousSibling;
	}

	/**
	 * Returns the given node's parent element.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {Element}
	 * @memberOf dom
	 */
	function parent(node) {
		return node.parentNode;
	}

	/**
	 * Returns the given node's parent element.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):Node}    step
	 * @param  {function(Node):boolean} cond
	 * @param  {*}                      arg
	 * @return {Element}
	 */
	function stepWhile(node, step, cond, arg) {
		while (node && cond(node, arg)) {
			node = step(node, arg);
		}
		return node;
	}

	/**
	 * Steps through the node tree according to `step` and `next` while the
	 * given condition is true.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node, *?)}     step
	 * @param  {function(Node):Node}    next
	 * @param  {function(Node):boolean} cond
	 * @param  {*}                      arg
	 * @return {Node}
	 */
	function stepNextWhile(node, step, next, cond, arg) {
		return stepWhile(node, function (node) {
			var n = next(node);
			step(node, arg);
			return n;
		}, cond, arg);
	}

	/**
	 * Steps through the node tree acording to `step` and `next` until the
	 * given condition is true.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node, *?)}     step
	 * @param  {function(Node):Node}    next
	 * @param  {function(Node):boolean} cond
	 * @param  {*}                      arg
	 * @return {Node}
	 * @memberOf dom
	 */
	function stepNextUntil(node, step, next, until, arg) {
		return stepNextWhile(node, step, next, Fn.complement(until), arg);
	}

	/**
	 * Starting from the given node and moving forward, traverses the set of
	 * `node`'s sibiling nodes until either the predicate `cond` returns false
	 * or the last sibling of `node`'s parent element is reached.
	 *
	 * @param  {Node}                       node
	 * @param  {function(Node, *?):boolean} cond
	 * @param  {*}                          arg Optional arbitrary value that
	 *                                          will be passed to `cond()`
	 * @return {Node}
	 * @memberOf dom
	 */
	function nextWhile(node, cond, arg) {
		return stepWhile(node, nextSibling, cond, arg);
	}

	/**
	 * Starting from the given node and moving backwards, traverses the set of
	 * `node`'s sibilings until either the predicate `cond` returns false or we
	 * reach the last sibling of `node`'s parent element.
	 *
	 * @param {Node}                       node
	 * @param {function(Node, *?):boolean} cond
	 * @param {*}                          arg Optional arbitrary value that
	 *                                         will be passed to the `cond()`
	 *                                         predicate.
	 * @return {Node}
	 * @memberOf dom
	 */
	function prevWhile(node, cond, arg) {
		return stepWhile(node, prevSibling, cond, arg);
	}

	/**
	 * Traverse up node's ancestor chain while the given condition is true.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} cond
	 * @return {Node}
	 * @memberOf dom
	 */
	function upWhile(node, cond) {
		return stepWhile(node, parent, cond);
	}

	/**
	 * Applies the given function `func()`, to the the given node `node` and
	 * it's next siblings, until the given `until()` function retuns `true` or
	 * all next siblings have been walked.
	 *
	 * @param {Node} node
	 * @param {function(Node, *?)} func
	 *        Callback function to apply to the traversed nodes.  Will receive
	 *        the each node as the first argument, and the value of `arg` as the
	 *        second argument.
	 * @param {function(Node, *?):boolean} until
	 *        Predicate function to test each traversed nodes.  Walking will be
	 *        terminated when this function returns `true`.  Will receive the
	 *        each node as the first argument, and the value of `arg` as the
	 *        second argument.
	 * @param {*} arg
	 *        A value that will be passed to `func()` as the second argument.
	 * @memberOf dom
	 */
	function nextUntil(node, func, until, arg) {
		stepNextUntil(node, func, nextSibling, until, arg);
	}

	/**
	 * Like nextUntil() but in reverse.
	 * @memberOf dom
	 */
	function prevUntil(node, func, until, arg) {
		stepNextUntil(node, func, prevSibling, until, arg);
	}

	/**
	 * Climbs up the given node's ancestors until the predicate until() returns
	 * true.  Starting with the given node, applies func() to each node in the
	 * traversal.
	 *
	 * @param {Node}                   node
	 * @param {function(Node, *?)}     func
	 * @param {function(Node):boolean} until
	 * @param {*} arg
	 *        A value that will be passed to `func()` as the second argument.
	 * @memberOf dom
	 */
	function climbUntil(node, func, until, arg) {
		stepNextUntil(node, func, parent, until, arg);
	}

	/**
	 * Applies the given function `func()`, to the the given node `node` and all
	 * it's next siblings.
	 *
	 * @param {Node}               node
	 * @param {function(Node, *?)} fn
	 *        Callback function to apply to the traversed nodes.  Will receive
	 *        the each node as the first argument, and the value of `arg` as the
	 *        second argument.
	 * @param {*}                  arg
	 *        A value that will be passed to `func()` as the second argument.
	 * @memberOf dom
	 */
	function walk(node, func, arg) {
		nextUntil(node, func, Fn.returnFalse, arg);
	}

	/**
	 * Depth-first postwalk of the given DOM node.
	 *
	 * @param  {Node}               node
	 * @param  {function(Node, *?)} func
	 * @return {*}                  arg
	 * @memberOf dom
	 */
	function walkRec(node, func, arg) {
		if (Nodes.isElementNode(node)) {
			walk(node.firstChild, function (node) {
				walkRec(node, func, arg);
			});
		}
		func(node, arg);
	}

	/**
	 * Applies the given function `func()`, to the the given node `node` and
	 * it's next siblings, until `untilNode` is encountered or the last sibling
	 * is reached.
	 *
	 * @param {Node}              node
	 * @param {function(Node, *)} fn
	 *        Callback function to apply to the traversed nodes.  Will receive
	 *        the each node as the first argument, and the value of `arg` as the
	 *        second argument.
	 * @param {Node} untilNode
	 *        Terminal node.
	 * @param {*}                  arg
	 *        A value that will be passed to `func()` as the second argument.
	 * @memberOf dom
	 */
	function walkUntilNode(node, func, untilNode, arg) {
		nextUntil(node, func, function (nextNode) {
			return nextNode === untilNode;
		}, arg);
	}

	/**
	 * Traverses up the given node's ancestors, collecting all parent nodes,
	 * until the given predicate returns true.
	 *
	 * @param {Node} node
	 * @param {function(Node):boolean} pred
	 *        Predicate function which will receive nodes as they are traversed.
	 *        This function returns `true`, it will terminate the traversal.
	 * @return {Array.<Node>}
	 *         A set of parent elements of the given node.
	 * @memberOf dom
	 */
	function parentsUntil(node, pred) {
		var parents = [];
		var parent = node.parentNode;
		while (parent && !pred(parent)) {
			parents.push(parent);
			parent = parent.parentNode;
		}
		return parents;
	}

	/**
	 * Starting with the given node, traverses up the given node's ancestors,
	 * collecting each parent node, until the first ancestor that causes the
	 * given predicate function to return true. The given node is *not* passed
	 * to the predicate (@see childAndParents).
	 *
	 * @param {Node} node
	 * @param {function(Node):boolean} pred
	 *        Predicate function which will receive nodes as they are traversed.
	 *        This function returns `true`, it will terminate the traversal.
	 * @return {Array.<Node>}
	 *         A set of parent element of the given node.
	 * @memberOf dom
	 */
	function parentsUntilIncl(node, pred) {
		var parents = parentsUntil(node, pred);
		var topmost = parents.length ? parents[parents.length - 1] : node;
		if (topmost.parentNode) {
			parents.push(topmost.parentNode);
		}
		return parents;
	}

	/**
	 * Collects all ancestors of the given node until the first ancestor that
	 * causes the given predicate function to return true.
	 *
	 * @param {Node} node
	 * @param {function(Node):boolean} pred
	 *        Predicate function which will receive nodes as they are traversed.
	 *        This function returns `true`, it will terminate the traversal.
	 * @return {Array.<Node>}
	 *         A set of parent element of the given node.
	 * @memberOf dom
	 */
	function childAndParentsUntil(node, pred) {
		if (pred(node)) {
			return [];
		}
		var parents = parentsUntil(node, pred);
		parents.unshift(node);
		return parents;
	}

	/**
	 * Collects the given node, and all its ancestors until the first ancestor
	 * that causes the given predicate function to return true.
	 *
	 * @param {Node} node
	 * @param {function(Node):boolean} pred
	 *        Predicate function which will receive nodes as they are traversed.
	 *        This function returns `true`, it will terminate the traversal.
	 * @return {Array.<Node>}
	 *         A set of parent element of the given node.
	 * @memberOf dom
	 */
	function childAndParentsUntilIncl(node, pred) {
		if (pred(node)) {
			return [node];
		}
		var parents = parentsUntilIncl(node, pred);
		parents.unshift(node);
		return parents;
	}

	/**
	 * Collects all ancestors of the given node until `untilNode` is reached.
	 *
	 * @param {Node}   node
	 * @param {Node}   untilNode Terminal ancestor.
	 * @return {Array<Node>} A set of parent element of the given node.
	 * @memberOf dom
	 */
	function childAndParentsUntilNode(node, untilNode) {
		return childAndParentsUntil(node, function (nextNode) {
			return nextNode === untilNode;
		});
	}

	/**
	 * Collects the given node, and all its ancestors until `untilInclNode` is
	 * reached.
	 *
	 * @param {Node} node
	 * @param {Node} untilInclNode
	 *        Terminal ancestor.  Will be included in results.
	 * @return {Array.<Node>}
	 *         A set of parent element of the given node.
	 * @memberOf dom
	 */
	function childAndParentsUntilInclNode(node, untilInclNode) {
		return childAndParentsUntilIncl(node, function (nextNode) {
			return nextNode === untilInclNode;
		});
	}

	/**
	 * Returns the nearest node (in the document order) to the given node that
	 * is not an ancestor.
	 *
	 * @param  {Node} start
	 * @param  {boolean} previous
	 *         If true, will look for the nearest preceding node, otherwise the
	 *         nearest subsequent node.
	 * @param  {function(Node):boolean} match
	 * @param  {function(Node):boolean} until
	 *         (Optional) Predicate, which will be applied to each node in the
	 *         traversal step.  If this function returns true, traversal will
	 *         terminal and will return null.
	 * @return {Node}
	 * @memberOf dom
	 */
	function nextNonAncestor(start, previous, match, until) {
		match = match || Fn.returnTrue;
		until = until || Fn.returnFalse;
		var next;
		var node = start;
		while (node) {
			next = previous ? node.previousSibling : node.nextSibling;
			if (next) {
				if (until(next)) {
					return null;
				}
				if (match(next)) {
					return next;
				}
				node = next;
			} else {
				if (!node.parentNode || until(node.parentNode)) {
					return null;
				}
				node = node.parentNode;
			}
		}
	}

	/**
	 * Executes a query selection (-all) in the given context and returns a
	 * non-live list of results.
	 *
	 * @param  {string}  selector
	 * @param  {Element} context
	 * @return {Array.<Node>}
	 * @memberOf dom
	 */
	function query(selector, context) {
		return Arrays.coerce(context.querySelectorAll(selector));
	}

	/**
	 * Returns a non-live list of the given node's preceeding siblings until the
	 * predicate returns true. The node one which `until` terminates is not
	 * included.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Node>}
	 * @memberOf dom
	 */
	function prevSiblings(node, until) {
		if (!node.previousSibling) {
			return [];
		}
		var nodes = [];
		prevUntil(node.previousSibling, function (next) {
			nodes.push(next);
		}, until || Fn.returnFalse);
		return nodes.reverse();
	}

	/**
	 * Returns a non-live list of the given node's subsequent siblings until the
	 * predicate returns true. The node one which `until` terminates is not
	 * included.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Node>}
	 * @memberOf dom
	 */
	function nextSiblings(node, until) {
		if (!node.nextSibling) {
			return [];
		}
		var nodes = [];
		nextUntil(node.nextSibling, function (next) {
			nodes.push(next);
		}, until || Fn.returnFalse);
		return nodes;
	}

	/**
	 * Returns a non-live list of any of the given node and it's preceeding
	 * siblings until the predicate returns true. The node one which `until`
	 * terminates is not included.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Node>}
	 * @memberOf dom
	 */
	function nodeAndPrevSiblings(node, until) {
		return (until && until(node)) ? [] : prevSiblings(node, until).concat(node);
	}

	/**
	 * Returns a non-live list of any of the given node and it's subsequent
	 * siblings until the predicate returns true. The node one which `until`
	 * terminates is not included.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Node>}
	 * @memberOf dom
	 */
	function nodeAndNextSiblings(node, until) {
		return (until && until(node)) ? [] : [node].concat(nextSiblings(node, until));
	}

	/**
	 * Returns a list of the given nodes and any siblings inbetween.
	 *
	 * @param  {!Node} start
	 * @param  {!Node} end
	 * @return {Array.<Node>}
	 */
	function nodesAndSiblingsBetween(start, end) {
		return (start === end) ? [start] : [start].concat(
			nextSiblings(start, function (node) { return node === end; }),
			end
		);
	}

	return {
		query                        : query,

		nextNonAncestor              : nextNonAncestor,

		nextUntil                    : nextUntil,
		nextWhile                    : nextWhile,
		nextSibling                  : nextSibling,
		nextSiblings                 : nextSiblings,

		prevUntil                    : prevUntil,
		prevWhile                    : prevWhile,
		prevSibling                  : prevSibling,
		prevSiblings                 : prevSiblings,

		nodeAndPrevSiblings          : nodeAndPrevSiblings,
		nodeAndNextSiblings          : nodeAndNextSiblings,
		nodesAndSiblingsBetween      : nodesAndSiblingsBetween,

		walk                         : walk,
		walkRec                      : walkRec,
		walkUntilNode                : walkUntilNode,

		forward                      : forward,
		backward                     : backward,
		findForward                  : findForward,
		findBackward                 : findBackward,

		upWhile                      : upWhile,
		climbUntil                   : climbUntil,
		childAndParentsUntil         : childAndParentsUntil,
		childAndParentsUntilIncl     : childAndParentsUntilIncl,
		childAndParentsUntilNode     : childAndParentsUntilNode,
		childAndParentsUntilInclNode : childAndParentsUntilInclNode,
		parentsUntil                 : parentsUntil,
		parentsUntilIncl             : parentsUntilIncl,

		forwardPreorderBacktraceUntil  : forwardPreorderBacktraceUntil,
		backwardPreorderBacktraceUntil : backwardPreorderBacktraceUntil
	};
});

/**
 * browsers.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library 
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace browsers
 */
define('browsers',[], function () {
	

	/**
	 * CSS vendor prefix string for the host user agent.
	 *
	 * @type {string}
	 * @memberOf browsers
	 */
	var VENDOR_PREFIX = '';
	var testElem = document.createElement('div');
	var prefixes = ['', '-webkit-', '-moz-', '-ms-', '-o-'];
	var style = testElem.style;
	for (var i = 0; i < prefixes.length; i++) {
		if (style.hasOwnProperty(prefixes[i] + 'transform')) {
			VENDOR_PREFIX = prefixes[i];
		}
	}

	// Adapted from http://code.jquery.com/jquery-migrate-git.js
	var ua = navigator.userAgent.toLowerCase();
	var info = /(chrome)[ \/]([\w.]+)/.exec(ua)
	        || /(webkit)[ \/]([\w.]+)/.exec(ua)
	        || /(opera)(?:.*version|)[ \/]([\w.]+)/.exec(ua)
	        || /(msie) ([\w.]+)/.exec(ua)
	        || (ua.indexOf('compatible') < 0
	            && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec(ua));
	/** This property is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf browsers*/
	var vendor;
	/** This property is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf browsers*/
	var version;
	/** @TODO we should export ie */
	var ie7;
	/** This property is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf browsers*/
	var chrome;
	/** This property is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf browsers*/
	var safari;
	/** This property is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf browsers*/
	var webkit;
	ie7 = chrome = safari = webkit = false;

	if (info) {
		vendor = info[1];
		version = info[2];
		ie7 = ('msie' === version) && (parseInt(version, 10) < 8);

		// Chrome is Webkit, but Webkit is also Safari.
		if ('chrome' === vendor) {
			webkit = true;
		} else if ('webkit' === vendor) {
			safari = true;
		}
	}

	var exports = {
		ie7           : ie7,
		chrome        : chrome,
		webkit        : webkit,
		safari        : safari,
		vendor        : vendor,
		version       : version,
		VENDOR_PREFIX : VENDOR_PREFIX
	};

	if (info) {
		exports[vendor] = true;
	}

	return exports;
});

/**
 * dom.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace dom
 */
define('dom',[
	'functions',
	'dom/attributes',
	'dom/classes',
	'dom/mutation',
	'dom/nodes',
	'dom/style',
	'dom/traversing',
	'browsers'
], function (
	Fn,
	Attributes,
	Classes,
	Mutation,
	Nodes,
	Style,
	Traversing,
	Browsers
) {
	

	/**
	 * Checks whether the given node is content editable.  An editing host is a
	 * node that is either an Element with a contenteditable attribute set to
	 * the true state, or the Element child of a Document whose designMode is
	 * enabled.
	 *
	 * An element with the class "aloha-editable" is considered an editing host.
	 *
	 * @param {!Node} node
	 * @return {boolean} True if `node` is content editable.
	 * @memberOf dom
	 */
	function isEditingHost(node) {
		if (!Nodes.isElementNode(node)) {
			return false;
		}
		if ('true' === node.getAttribute('contentEditable')) {
			return true;
		}
		if (Classes.has(node, 'aloha-editable')) {
			return true;
		}
		var parent = node.paretNode;
		if (!parent) {
			return false;
		}
		if (parent.nodeType === Nodes.Nodes.DOCUMENT && 'on' === parent.designMode) {
			return true;
		}
	}

	/**
	 * Check if the given node's contentEditable attribute is `true`.
	 *
	 * @param  {Element} node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function isContentEditable(node) {
		return Nodes.isElementNode(node) && 'true' === node.contentEditable;
	}

	/**
	 * Checks whether the given element is editable.
	 *
	 * An element with the class "aloha-editable" is considered editable.
	 *
	 * @see:
	 * http://www.whatwg.org/specs/web-apps/current-work/multipage/editing.html#contenteditable
	 * http://www.whatwg.org/specs/web-apps/current-work/multipage/editing.html#designMode
	 *
	 * @param {!Node} node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function isEditable(node) {
		if (!Nodes.isElementNode(node)) {
			return false;
		}
		var contentEditable = node.getAttribute('contentEditable');
		if ('true' === contentEditable || '' === contentEditable) {
			return true;
		}
		if ('false' === contentEditable) {
			return false;
		}
		// Because the value of `contentEditable` can be "inherited" according
		// to specification, and null according to browser implementation.
		if (Classes.has(node, 'aloha-editable')) {
			return true;
		}
		var parent = node.parentNode;
		if (!parent) {
			return false;
		}
		if (parent.nodeType === Nodes.Nodes.DOCUMENT && 'on' === parent.designMode) {
			return true;
		}
		return isEditable(parent);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf dom
	 */
	function isEditableNode(node) {
		return isEditable(Nodes.isTextNode(node) ? node.parentNode : node);
	}

	/**
	 * Gets the given node's editing host.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf dom
	 */
	function editingHost(node) {
		if (isEditingHost(node)) {
			return node;
		}
		if (!isEditableNode(node)) {
			return null;
		}
		var ancestor = node.parentNode;
		while (ancestor && !isEditingHost(ancestor)) {
			ancestor = ancestor.parentNode;
		}
		return ancestor;
	}

	/**
	 * Finds the nearest editable ancestor of the given node.
	 *
	 * @param  {Node} node
	 * @return {Element}
	 * @memberOf dom
	 */
	function editableParent(node) {
		var ancestor = node.parentNode;
		while (ancestor && !isEditable(ancestor)) {
			ancestor = ancestor.parentNode;
		}
		return ancestor;
	}

	/**
	 * Used to serialize outerHTML of DOM elements in older (pre-HTML5) Gecko,
	 * Safari, and Opera browsers.
	 *
	 * Beware that XMLSerializer generates an XHTML string (<div class="team" />
	 * instead of <div class="team"></div>).  It is noted here:
	 * http://stackoverflow.com/questions/1700870/how-do-i-do-outerhtml-in-firefox
	 * that some browsers (like older versions of Firefox) have problems with
	 * XMLSerializer, and an alternative, albeit more expensive option, is
	 * described.
	 *
	 * @type {XMLSerializer}
	 */
	var Serializer = window.XMLSerializer && new window.XMLSerializer();

	function serialize(node) {
		return Serializer.serializeToString(node);
	}

	/**
	 * true if obj is a Node
	 * @param  {*} node
	 * @return {boolean}
	 */
	function isNode(obj) {
		var str = Object.prototype.toString.call(obj);
		// TODO: is this really the best way to do it?
		return (/^\[object (Text|Comment|HTML\w*Element)\]$/).test(str);
	}

	var expandoIdCnt = 0;
	var expandoIdProp = '!aloha-expando-node-id';

	/**
	 * @fix me jslint hates this. it generates 4 errors
	 */
	function ensureExpandoId(node) {
		return node[expandoIdProp] = node[expandoIdProp] || ++expandoIdCnt;
	}

	function enableSelection(elem) {
		elem.removeAttribute('unselectable', 'on');
		Style.set(elem, Browsers.VENDOR_PREFIX + 'user-select', 'all');
		elem.onselectstart = null;
	}

	function disableSelection(elem) {
		elem.removeAttribute('unselectable', 'on');
		Style.set(elem, Browsers.VENDOR_PREFIX + 'user-select', 'none');
		elem.onselectstart = Fn.returnFalse;
	}

	/**
	 * Gets the window to which the given document belongs.
	 *
	 * @param   {Document} doc
	 * @returns {Window}
	 * @memberOf dom
	 */
	function documentWindow(doc) {
		return doc['defaultView'] || doc['parentWindow'];
	}

	/**
	 * Returns scroll position from top.
	 *
	 * @param  {!Document} doc
	 * @return {number}
	 * @memberOf dom
	 */
	function scrollTop(doc) {
		var win = documentWindow(doc);
		if (!Fn.isNou(win.pageYOffset)) {
			return win.pageYOffset;
		}
		var docElem = doc.documentElement;
		var scrollTopElem = docElem.clientHeight ? docElem : doc.body;
		return scrollTopElem.scrollTop;
	}

	/**
	 * Returns scroll position from left.
	 *
	 * @param  {!Document} doc
	 * @return {number}
	 * @memberOf dom
	 */
	function scrollLeft(doc) {
		var win = documentWindow(doc);
		if (!Fn.isNou(win.pageXOffset)) {
			return win.pageXOffset;
		}
		var docElem = doc.documentElement;
		var scrollLeftElem = docElem.clientWidth ? docElem : doc.body;
		return scrollLeftElem.scrollLeft;
	}

	/**
	 * Calculate absolute offsetTop or offsetLeft properties
	 * for an element
	 *
	 * @private
	 * @param {!Element} element
	 * @param {string}   property
	 * @return {integer}
	 */
	function absoluteOffset(element, property) {
		var offset = element[property];
		var parent = element.offsetParent;
		while (parent) {
			offset += parent[property];
			parent = parent.offsetParent;
		}
		return offset;
	}

	/**
	 * Calculates the absolute top position
	 * of an element
	 *
	 * @param {!Element} element
	 * @return {integer}
	 * @memberOf dom
	 */
	function absoluteTop(element) {
		return absoluteOffset(element, 'offsetTop');
	}

	/**
	 * Calculates the absolute left position
	 * of an element
	 *
	 * @param {!Element} element
	 * @return {integer}
	 * @memberOf dom
	 */
	function absoluteLeft(element) {
		return absoluteOffset(element, 'offsetLeft');
	}

	return {
		Nodes                   : Nodes.Nodes,
		offset                  : Nodes.offset,
		cloneShallow            : Nodes.cloneShallow,
		clone                   : Nodes.clone,
		text                    : Nodes.text,
		children                : Nodes.children,
		nthChild                : Nodes.nthChild,
		numChildren             : Nodes.numChildren,
		nodeIndex               : Nodes.nodeIndex,
		nodeLength              : Nodes.nodeLength,
		hasChildren             : Nodes.hasChildren,
		nodeAtOffset            : Nodes.nodeAtOffset,
		normalizedNthChild      : Nodes.normalizedNthChild,
		normalizedNodeIndex     : Nodes.normalizedNodeIndex,
		realFromNormalizedIndex : Nodes.realFromNormalizedIndex,
		normalizedNumChildren   : Nodes.normalizedNumChildren,
		isNode                  : isNode,
		isTextNode              : Nodes.isTextNode,
		isElementNode           : Nodes.isElementNode,
		isFragmentNode          : Nodes.isFragmentNode,
		isEmptyTextNode         : Nodes.isEmptyTextNode,
		isSameNode              : Nodes.isSameNode,
		equals                  : Nodes.equals,
		contains                : Nodes.contains,
		followedBy              : Nodes.followedBy,
		hasText                 : Nodes.hasText,
		outerHtml               : Nodes.outerHtml,

		append            : Mutation.append,
		merge             : Mutation.merge,
		moveNextAll       : Mutation.moveNextAll,
		moveBefore        : Mutation.moveBefore,
		moveAfter         : Mutation.moveAfter,
		move              : Mutation.move,
		copy              : Mutation.copy,
		wrap              : Mutation.wrap,
		wrapWith          : Mutation.wrapWith,
		insert            : Mutation.insert,
		insertAfter       : Mutation.insertAfter,
		replace           : Mutation.replace,
		replaceShallow    : Mutation.replaceShallow,
		remove            : Mutation.remove,
		removeShallow     : Mutation.removeShallow,
		removeChildren    : Mutation.removeChildren,

		addClass     : Classes.add,
		removeClass  : Classes.remove,
		toggleClass  : Classes.toggle,
		hasClass     : Classes.has,

		attrs        : Attributes.attrs,
		getAttr      : Attributes.get,
		getAttrNS    : Attributes.getNS,
		hasAttrs     : Attributes.has,
		removeAttr   : Attributes.remove,
		removeAttrNS : Attributes.removeNS,
		removeAttrs  : Attributes.removeAll,
		setAttr      : Attributes.set,
		setAttrNS    : Attributes.setNS,

		removeStyle       : Style.remove,
		setStyle          : Style.set,
		getStyle          : Style.get,
		getComputedStyle  : Style.getComputedStyle,
		getComputedStyles : Style.getComputedStyles,

		query                        : Traversing.query,
		nextNonAncestor              : Traversing.nextNonAncestor,
		nextWhile                    : Traversing.nextWhile,
		nextUntil                    : Traversing.nextUntil,
		nextSibling                  : Traversing.nextSibling,
		nextSiblings                 : Traversing.nextSiblings,
		prevWhile                    : Traversing.prevWhile,
		prevUntil                    : Traversing.prevUntil,
		prevSibling                  : Traversing.prevSibling,
		prevSiblings                 : Traversing.prevSiblings,
		nodeAndNextSiblings          : Traversing.nodeAndNextSiblings,
		nodeAndPrevSiblings          : Traversing.nodeAndPrevSiblings,
		nodesAndSiblingsBetween      : Traversing.nodesAndSiblingsBetween,
		walk                         : Traversing.walk,
		walkRec                      : Traversing.walkRec,
		walkUntilNode                : Traversing.walkUntilNode,
		forward                      : Traversing.forward,
		backward                     : Traversing.backward,
		findForward                  : Traversing.findForward,
		findBackward                 : Traversing.findBackward,
		upWhile                      : Traversing.upWhile,
		climbUntil                   : Traversing.climbUntil,
		childAndParentsUntil         : Traversing.childAndParentsUntil,
		childAndParentsUntilIncl     : Traversing.childAndParentsUntilIncl,
		childAndParentsUntilNode     : Traversing.childAndParentsUntilNode,
		childAndParentsUntilInclNode : Traversing.childAndParentsUntilInclNode,
		parentsUntil                 : Traversing.parentsUntil,
		parentsUntilIncl             : Traversing.parentsUntilIncl,
		forwardPreorderBacktraceUntil  : Traversing.forwardPreorderBacktraceUntil,
		backwardPreorderBacktraceUntil : Traversing.backwardPreorderBacktraceUntil,

		serialize         : serialize,
		ensureExpandoId   : ensureExpandoId,

		enableSelection   : enableSelection,
		disableSelection  : disableSelection,

		// FIXME: move to html.js
		isEditable        : isEditable,
		isEditableNode    : isEditableNode,
		isEditingHost     : isEditingHost,
		isContentEditable : isContentEditable,

		documentWindow    : documentWindow,
		editingHost       : editingHost,
		editableParent    : editableParent,
		scrollTop         : scrollTop,
		scrollLeft        : scrollLeft,
		absoluteTop       : absoluteTop,
		absoluteLeft      : absoluteLeft
	};
});

/**
 * ranges.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @see
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#deleting-the-selection
 */
define('ranges',['dom', 'arrays'], function (Dom, Arrays) {
	

	/**
	 * Creates a range object with boundaries defined by containers, and offsets
	 * in those containers.
	 *
	 * @param  {!Element} sc
	 * @param  {number}   so
	 * @param  {!Element} ec
	 * @param  {number}   eo
	 * @return {Range}
	 */
	function create(sc, so, ec, eo) {
		var range = sc.ownerDocument.createRange();
		range.setStart(sc, so || 0);
		if (ec) {
			range.setEnd(ec, eo || 0);
		} else {
			range.setEnd(sc, so || 0);
		}
		return range;
	}

	/**
	 * Creates a range from the horizontal and vertical offset pixel positions
	 * relative to upper-left corner the document body.
	 *
	 * Returns a collapsed range for the position where the text insertion
	 * indicator would be rendered.
	 *
	 * @see
	 * http://dev.w3.org/csswg/cssom-view/#dom-document-caretpositionfrompoint
	 * http://stackoverflow.com/questions/3189812/creating-a-collapsed-range-from-a-pixel-position-in-ff-webkit
	 * http://jsfiddle.net/timdown/ABjQP/8/
	 * http://lists.w3.org/Archives/Public/public-webapps/2009OctDec/0113.html
	 *
	 * @private
	 * @param  {number}    x
	 * @param  {number}    y
	 * @param  {!Document} doc
	 * @return {?Range}
	 */
	function fromPoint(x, y, doc) {
		if (x < 0 || y < 0) {
			return null;
		}
		if (doc['caretRangeFromPoint']) {
			return doc['caretRangeFromPoint'](x, y);
		}
		if (doc.caretPositionFromPoint) {
			var pos = doc.caretPositionFromPoint(x, y);
			return create(pos.offsetNode, pos.offset);
		}
		if (doc.elementFromPoint) {
			return fromPointIE(x, y, doc);
		}

		throw 'fromPoint() unimplemented for this browser';
	}

	/**
	 * Returns whether x and y are inside or above the given
	 * rectangle as created by range.getClientRects()
	 * @see http://jsfiddle.net/timdown/ABjQP/8/
	 *
	 * @param {int} x
	 * @param {int} y
	 * @param {Rectangle} rect
	 * @return {boolean}
	 */
	function pointIsInOrAboveRect(x, y, rect) {
		return y < rect.bottom && x >= rect.left && x <= rect.right;
	}

	/**
	 * Transforms a collapsed range into mockup
	 * client rectange object, by exchanging the
	 * left property with the provided one.
	 *
	 * @see stepTextNode
	 *
	 * @param  {Range} range
	 * @param  {?int}  left
	 * @return {Object|null}
	 */
	function collapsedRangeToRect(range, left) {
		var clientRect = Arrays.last(range.getClientRects());
		if (!clientRect) {
			return null;
		}
		return {
			left   : left || clientRect.left,
			right  : clientRect.right,
			bottom : clientRect.bottom
		};
	}

	/**
	 * Will extend a range inside node until it covers
	 * x and y and then return an offset object containing
	 * the offset node and the actual offset index.
	 * The method will call itself recursively, using the
	 * lastLeft parameter, which holds the left offset from
	 * the last iteration. Don't pass lastLeft when calling
	 * the function yourself.
	 *
	 * Because client rectangle calculation (range.getClientRects)
	 * is broken in Internet Explorer 11, this function will 
	 * use a collapsed range to match the x and y positions 
	 * and create a rectangle using the lastLeft parameter
	 * internally. Not using this approach will lead to bogus
	 * results for range.getClientRects when clicking inside
	 * an text node thats nested inside an li element.
	 *
	 * @param  {!Node} node
	 * @param  {!Range} range
	 * @param  {!integer} offset
	 * @param  {!integer} x
	 * @param  {!integer} y
	 * @param  {?integer} lastLeft
	 */
	function stepTextNode(node, range, offset, x, y, lastLeft) {
		range.setStart(node, offset);
		range.setEnd(node, offset);
		var rect = collapsedRangeToRect(range, lastLeft);
		if (rect && pointIsInOrAboveRect(x, y, rect)) {
			if (rect.right - x > x - rect.left) {
				offset--;
			}
			return {
				node  : node,
				index : offset
			};
		}
		if (offset < node.length) {
			return stepTextNode(node, range, ++offset, x, y, rect ? rect.left : null);
		} 
		return null;
	}

	/**
	 * Will extend range inside a node until it covers 
	 * the x & y position to return an offset object
	 * that contains an offset node and the offset itself
	 *
	 * @param  {!Node}    node
	 * @param  {!Range}   range
	 * @param  {!integer} x
	 * @param  {!integer} y
	 * @return {Object}
	 */
	function findOffset(node, range, x, y) {
		if (Dom.isTextNode(node)) {
			var offset = stepTextNode(node, range, 0, x, y);
			if (offset) {
				return offset;
			}
		} else {
			range.setEndAfter(node);
			var rect = Arrays.last(range.getClientRects());
			if (rect && pointIsInOrAboveRect(x, y, rect)) {
				return {
					node  : node.parentNode,
					index : Dom.nodeIndex(node)
				};
			}
		}

		if (node.nextSibling) {
			return findOffset(node.nextSibling, range, x, y);
		}

		return {
			node  : node.parentNode,
			index : Dom.nodeIndex(node)
		};
	}

	/**
	 * Creates a Range object from click coordinates 
	 * x and y on the document. Meant to be a drop-in
	 * replacement for @see fromPoint which works in
	 * Internet Explorer
	 *
	 * Based on http://jsfiddle.net/timdown/ABjQP/8/
	 *
	 * @param  {!integer}  x
	 * @param  {!integer}  y
	 * @param  {!Document} doc
	 * @return {Range}
	 */
	function fromPointIE(x, y, doc) {
		var el = doc.elementFromPoint(x, y);
		var range = doc.createRange();
		var offset = {
			node  : el.firstChild,
			index : -1
		};

		range.selectNodeContents(el);
		range.collapse(true);

		if (!offset.node) {
			offset = {
				node  : el.parentNode,
				index : Dom.nodeIndex(el)
			};
		} else {
			offset = findOffset(offset.node, range, x, y);
		}
		return create(offset.node, offset.index);
	}


	/**
	 * Gets the given node's nearest non-editable parent.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {?Element}
	 */
	function parentBlock(node) {
		var block = Dom.isEditable(node) ? Dom.editingHost(node) : node;
		var parent = Dom.upWhile(block, function (node) {
			return node.parentNode && !Dom.isEditable(node.parentNode);
		});
		return (Dom.Nodes.DOCUMENT === parent.nodeType) ? null : parent;
	}

	/**
	 * Derives a range from the horizontal and vertical offset pixel positions
	 * relative to upper-left corner of the document that is visible within the
	 * view port.
	 *
	 * It is important that the x, y coordinates given are not only within the
	 * dimensions of the document, but also viewport (ie: they are visible on
	 * the screen).
	 *
	 * Returns null if no suitable range can be determined from within an
	 * editable.
	 *
	 * @param  {number}    x
	 * @param  {number}    y
	 * @param  {!Document} doc
	 * @return {?Range}
	 */
	function fromPosition(x, y, doc) {
		x -= Dom.scrollLeft(doc);
		y -= Dom.scrollTop(doc);
		var range = fromPoint(x, y, doc);
		if (!range) {
			return null;
		}
		if (Dom.isEditableNode(range.commonAncestorContainer)) {
			return range;
		}
		var block = parentBlock(range.commonAncestorContainer);
		if (!block || !block.parentNode) {
			return null;
		}
		var body = doc.body;
		var offsets = Dom.offset(block);
		var offset = Dom.nodeIndex(block);
		var pointX = x + body.scrollLeft;
		var blockX = offsets.left + body.scrollLeft + block.offsetWidth;
		if (pointX > blockX) {
			offset += 1;
		}
		return create(block.parentNode, offset);
	}

	/**
	 * Checks whether two ranges are equal. Ranges are equal if their
	 * corresponding boundary containers and offsets are strictly equal.
	 *
	 * @param  {Range} a
	 * @param  {Range} b
	 * @return {boolean}
	 */
	function equals(a, b) {
		return a.startContainer === b.startContainer
			&& a.startOffset    === b.startOffset
			&& a.endContainer   === b.endContainer
			&& a.endOffset      === b.endOffset;
	}

	/**
	 * Returns true if the given value is a Range object as created by
	 * document.createRange()
	 *
	 * @param  {*} obj
	 * @return {boolean}
	 * @memberOf selections
	 */
	function is(obj) {
		return obj
		    && obj.hasOwnProperty
		    && obj.hasOwnProperty('commonAncestorContainer')
		    && obj.hasOwnProperty('collapsed')
		    && obj.hasOwnProperty('startContainer')
		    && obj.hasOwnProperty('startOffset');
	}

	return {
		is           : is,
		equals       : equals,
		create       : create,
		fromPosition : fromPosition
	};
});

/**
 * boundaries.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace boundaries
 */
define('boundaries',[
	'dom',
	'ranges',
	'arrays',
	'assert'
], function (
	Dom,
	Ranges,
	Arrays,
	Assert
) {
	

	/**
	 * Creates a "raw" (un-normalized) boundary from the given node and offset.
	 *
	 * @param  {Node} node
	 * @param  {number} offset
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function raw(node, offset) {
		return [node, offset];
	}

	/**
	 * Returns a boundary's container node.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 * @memberOf boundaries
	 */
	function container(boundary) {
		return boundary[0];
	}

	/**
	 * Returns a boundary's offset.
	 *
	 * @param  {Boundary} boundary
	 * @return {number}
	 * @memberOf boundaries
	 */
	function offset(boundary) {
		return boundary[1];
	}

	/**
	 * Returns the document associated with the given boundary.
	 *
	 * @param  {!Boundary} boundary
	 * @return {Document}
	 * @memberOf boundaries
	 */
	function document(boundary) {
		return container(boundary).ownerDocument;
	}

	/**
	 * Returns a boundary that in front of the given node.
	 *
	 * @param  {Node} node
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function fromFrontOfNode(node) {
		return raw(node.parentNode, Dom.nodeIndex(node));
	}

	/**
	 * Returns a boundary that is behind the given node.
	 *
	 * @param  {Node} node
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function fromBehindOfNode(node) {
		return raw(node.parentNode, Dom.nodeIndex(node) + 1);
	}

	/**
	 * Returns a boundary that is at the start position inside the given node.
	 *
	 * @param  {Node} node
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function fromStartOfNode(node) {
		return raw(node, 0);
	}

	/**
	 * Returns a boundary that is at the end position inside the given node.
	 *
	 * @param  {Node} node
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function fromEndOfNode(node) {
		return raw(node, Dom.nodeLength(node));
	}

	/**
	 * Normalizes the boundary point (represented by a container and an offset
	 * tuple) such that it will not point to the start or end of a text node.
	 *
	 * This normalization reduces the number of states the a boundary can be
	 * in, and thereby slightly increases the robusteness of the code written
	 * against it.
	 *
	 * It should be noted that native ranges controlled by the browser's DOM
	 * implementation have the habit of changing by themselves, so even if a
	 * range is set using a boundary that has been normalized this way, the
	 * range could revert to an un-normalized state.  See StableRange().
	 *
	 * The returned value will either be a normalized copy of the given
	 * boundary, or the given boundary itself if no normalization was done.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function normalize(boundary) {
		var node = container(boundary);
		if (Dom.isTextNode(node)) {
			Assert.assertNotNou(node.parentNode);
			var boundaryOffset = offset(boundary);
			if (0 === boundaryOffset) {
				return fromFrontOfNode(node);
			}
			if (boundaryOffset >= Dom.nodeLength(node)) {
				return fromBehindOfNode(node);
			}
		}
		return boundary;
	}

	/**
	 * Creates a node boundary representing an (positive integer) offset
	 * position inside of a container node.
	 *
	 * The resulting boundary will be a normalized boundary, such that the
	 * boundary will never describe a terminal position in a text node.
	 *
	 * @param  {Node} node
	 * @param  {number} offset
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function create(node, offset) {
		Assert.assert(offset > -1, 'Boundaries.create(): Offset must be 0 or greater');
		return normalize(raw(node, offset));
	}

	/**
	 * Compares two boundaries for equality. Boundaries are equal if their
	 * corresponding containers and offsets are equal.
	 *
	 * @param  {Boundary} a
	 * @param  {Boundary} b
	 * @retufn {boolean}
	 * @memberOf boundaries
	 */
	function equals(a, b) {
		return (container(a) === container(b)) && (offset(a) === offset(b));
	}

	/**
	 * Sets the given range's start boundary.
	 *
	 * @param {Range}    range Range to modify.
	 * @param {Boundary} boundary
	 * @memberOf boundaries
	 */
	function setRangeStart(range, boundary) {
		boundary = normalize(boundary);
		range.setStart(container(boundary), offset(boundary));
	}

	/**
	 * Sets the given range's end boundary.
	 *
	 * @param {Range} range Range to modify
	 * @param {Boundary}
	 * @memberOf boundaries
	 */
	function setRangeEnd(range, boundary) {
		boundary = normalize(boundary);
		range.setEnd(container(boundary), offset(boundary));
	}

	/**
	 * Modifies the given range's start and end positions to the two respective
	 * boundaries.
	 *
	 * @param {Range}    range
	 * @param {Boundary} start
	 * @param {Boundary} end
	 * @memberOf boundaries
	 */
	function setRange(range, start, end) {
		setRangeStart(range, start);
		setRangeEnd(range, end);
	}

	/**
	 * Sets the start and end position of a list of ranges from the given list
	 * of boundaries.
	 *
	 * Because the range at index i in `ranges` will be modified using the
	 * boundaries at index 2i and 2i + 1 in `boundaries`, the size of `ranges`
	 * must be no less than half the size of `boundaries`.
	 *
	 * Because the list of boundaries will need to be partitioned into pairs of
	 * start/end tuples, it is required that the length of `boundaries` be
	 * even.  See Arrays.partition().
	 *
	 * @param {Array.<Range>}    ranges     List of ranges to modify
	 * @param {Array.<Boundary>} boundaries Even list of boundaries
	 * @memberOf boundaries
	 */
	function setRanges(ranges, boundaries) {
		Arrays.partition(boundaries, 2).forEach(function (boundaries, i) {
			setRange(ranges[i], boundaries[0], boundaries[1]);
		});
	}

	/**
	 * Creates a boundary from the given range's start position.
	 *
	 * @param  {Range} range
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function fromRangeStart(range) {
		return create(range.startContainer, range.startOffset);
	}

	/**
	 * Creates a boundary from the given range's end position.
	 *
	 * @param  {Range} range
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function fromRangeEnd(range) {
		return create(range.endContainer, range.endOffset);
	}

	/**
	 * Returns a start/end boundary tuple representing the start and end
	 * positions of the given range.
	 *
	 * @param  {Range} range
	 * @return {Array.<Boundary>}
	 * @memberOf boundaries
	 */
	function fromRange(range) {
		return [fromRangeStart(range), fromRangeEnd(range)];
	}

	/**
	 * Returns an even-sized contiguous sequence of start/end boundaries
	 * aligned in their pairs.
	 *
	 * @param  {Array.<Range>} ranges
	 * @return {Array.<Boundary>}
	 * @memberOf boundaries
	 */
	function fromRanges(ranges) {
		// TODO: after refactoring range-preserving functions to use
		// boundaries we can remove this.
		ranges = ranges || [];
		return Arrays.mapcat(ranges, fromRange);
	}

	/**
	 * Checks if a boundary (when normalized) represents a position at the
	 * start of its container's content.
	 *
	 * The start boundary of the given ranges is at the start position:
	 * <b><i>f</i>[oo]</b> and <b><i>{f</i>oo}</b>
	 * The first is at the start of the text node "oo" and the other at start
	 * of the <i> element.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 * @memberOf boundaries
	 */
	function isAtStart(boundary) {
		return 0 === offset(normalize(boundary));
	}

	/**
	 * Checks if a boundary represents a position at the end of its container's
	 * content.
	 *
	 * The end boundary of the given selection is at the end position:
	 * <b><i>f</i>{oo]</b> and <b><i>f</i>{oo}</b>
	 * The first is at end of the text node "oo"and the other at end of the <b>
	 * element.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 * @memberOf boundaries
	 */
	function isAtEnd(boundary) {
		boundary = normalize(boundary);
		return offset(boundary) === Dom.nodeLength(container(boundary));
	}

	/**
	 * Checks if the un-normalized boundary is at the start position of it's
	 * container.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 */
	function isAtRawStart(boundary) {
		return 0 === offset(boundary);
	}

	/**
	 * Checks if the un-normalized boundary is at the end position of it's
	 * container.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 */
	function isAtRawEnd(boundary) {
		return offset(boundary) === Dom.nodeLength(container(boundary));
	}

	/**
	 * Checks whether the given boundary is a position inside of a text nodes.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 * @memberOf boundaries
	 */
	function isTextBoundary(boundary) {
		return Dom.isTextNode(container(boundary));
	}

	/**
	 * Checks whether the given boundary is a position between nodes (as
	 * opposed to a position inside of a text node).
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 * @memberOf boundaries
	 */
	function isNodeBoundary(boundary) {
		return !isTextBoundary(boundary);
	}

	/**
	 * Returns the node that is after the given boundary position.
	 * Will return null if the given boundary is at the end position.
	 *
	 * Note that the given boundary will be normalized.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 * @memberOf boundaries
	 */
	function nodeAfter(boundary) {
		boundary = normalize(boundary);
		return isAtEnd(boundary) ? null : Dom.nthChild(container(boundary), offset(boundary));
	}

	/**
	 * Returns the node that is before the given boundary position.
	 * Will returns null if the given boundary is at the start position.
	 *
	 * Note that the given boundary will be normalized.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 * @memberOf boundaries
	 */
	function nodeBefore(boundary) {
		boundary = normalize(boundary);
		return isAtStart(boundary) ? null : Dom.nthChild(container(boundary), offset(boundary) - 1);
	}

	/**
	 * Returns the node after the given boundary, or the boundary's container
	 * if the boundary is at the end position.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 * @memberOf boundaries
	 */
	function nextNode(boundary) {
		boundary = normalize(boundary);
		return nodeAfter(boundary) || container(boundary);
	}

	/**
	 * Returns the node before the given boundary, or the boundary container if
	 * the boundary is at the end position.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 * @memberOf boundaries
	 */
	function prevNode(boundary) {
		boundary = normalize(boundary);
		return nodeBefore(boundary) || container(boundary);
	}

	/**
	 * Skips the given boundary over the node that is next to the boundary.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function jumpOver(boundary) {
		return fromBehindOfNode(nextNode(boundary));
	}

	/**
	 * Returns a boundary that is at the previous position to the given.
	 *
	 * If the given boundary represents a position inside of a text node, the
	 * returned boundary will be moved behind that text node.
	 *
	 * Given the markup below:
	 *
	 * <pre>
	 * <div>
	 *	foo
	 *	<p>
	 *		bar
	 *		<b>
	 *			<u></u>
	 *			baz
	 *		</b>
	 *	</p>
	 * </div>
	 * </pre>
	 *
	 * the boundary positions which can be traversed with this function are
	 * those marked with the pipe ("|") below:
	 *
	 * <pre>|foo|<p>|bar|<b>|<u>|</u>|baz|<b>|</p>|</pre>
	 *
	 * This function complements Boundaries.next()
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function prev(boundary) {
		boundary = normalize(boundary);
		var node = container(boundary);
		if (Dom.isTextNode(node) || isAtStart(boundary)) {
			return fromFrontOfNode(node);
		}
		node = Dom.nthChild(node, offset(boundary) - 1);
		return Dom.isTextNode(node)
		     ? fromFrontOfNode(node)
		     : fromEndOfNode(node);
	}

	/**
	 * Like Boundaries.prev(), but returns the boundary position that follows
	 * from the given.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function next(boundary) {
		boundary = normalize(boundary);
		var node = container(boundary);
		var boundaryOffset = offset(boundary);
		if (Dom.isTextNode(node) || isAtEnd(boundary)) {
			return jumpOver(boundary);
		}
		var nextNode = Dom.nthChild(node, boundaryOffset);
		return Dom.isTextNode(nextNode)
		     ? fromBehindOfNode(nextNode)
		     : fromStartOfNode(nextNode);
	}

	/**
	 * Like Boundaries.prev() but treats the given boundary as an unnormalized
	 * boundary.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function prevRawBoundary(boundary) {
		var node = container(boundary);
		if (isAtRawStart(boundary)) {
			return fromFrontOfNode(node);
		}
		if (isTextBoundary(boundary)) {
			return fromStartOfNode(node);
		}
		node = Dom.nthChild(node, offset(boundary) - 1);
		return fromEndOfNode(node);
	}

	/**
	 * Like Boundaries.next() but treats the given boundary as an unnormalized
	 * boundary.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function nextRawBoundary(boundary) {
		var node = container(boundary);
		if (isAtRawEnd(boundary)) {
			return fromBehindOfNode(node);
		}
		if (isTextBoundary(boundary)) {
			return fromEndOfNode(node);
		}
		return fromStartOfNode(Dom.nthChild(node, offset(boundary)));
	}

	/**
	 * Steps through boundaries while the given condition is true.
	 *
	 * @param  {Boundary}                    boundary Start position
	 * @param  {function(Boundary):boolean}  cond     Predicate
	 * @param  {function(Boundary):Boundary} step     Gets the next boundary
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function stepWhile(boundary, cond, step) {
		var pos = boundary;
		while (cond(pos)) {
			pos = step(pos);
		}
		return pos;
	}

	/**
	 * Steps forward while the given condition is true.
	 *
	 * @param  {Boundary}                   boundary
	 * @param  {function(Boundary):boolean} cond
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function nextWhile(boundary, cond) {
		return stepWhile(boundary, cond, next);
	}

	/**
	 * Steps backwards while the given condition is true.
	 *
	 * @param  {Boundary}                   boundary
	 * @param  {function(Boundary):boolean} cond
	 * @return {Boundary}
	 * @memberOf boundaries
	 */
	function prevWhile(boundary, cond) {
		return stepWhile(boundary, cond, prev);
	}

	/**
	 * Walks along boundaries according to step(), applying callback() to each
	 * boundary along the traversal until cond() returns false.
	 *
	 * @param  {Boundary}                    boundary Start position
	 * @param  {function(Boundary):boolean}  cond     Predicate
	 * @param  {function(Boundary):Boundary} step     Gets the next boundary
	 * @param  {function(Boundary)}          callback Applied to each boundary
	 * @memberOf boundaries
	 */
	function walkWhile(boundary, cond, step, callback) {
		var pos = boundary;
		while (pos && cond(pos)) {
			callback(pos);
			pos = step(pos);
		}
	}

	/**
	 * Gets the boundaries of the currently selected range from the given
	 * document element.
	 *
	 * @param  {Document} doc
	 * @return {?Array<Boundary>}
	 * @memberOf boundaries
	 */
	function get(doc) {
		var selection = doc.getSelection();
		return (selection.rangeCount > 0)
		     ? fromRange(selection.getRangeAt(0))
		     : null;
	}

	/**
	 * Creates a range based on the given start and end boundaries.
	 *
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @return {Range}
	 * @alias range
	 * @memberOf boundaries
	 */
	function toRange(start, end) {
		return Ranges.create(
			container(start),
			offset(start),
			container(end),
			offset(end)
		);
	}

	/**
	 * Sets the a range to the browser selection according to the given start
	 * and end boundaries.  This operation will cause the selection to be
	 * visually rendered by the user agent.
	 *
	 * @param {Boundary}  start
	 * @param {Boundary=} end
	 * @memberOf boundaries
	 */
	function select(start, end) {
		if (!end) {
			end = start;
		}
		var range = toRange(start, end);
		var doc = range.commonAncestorContainer.ownerDocument;
		var selection = doc.getSelection();
		selection.removeAllRanges();
		selection.addRange(range);
	}

	/**
	 * Return the ancestor container that contains both the given boundaries.
	 *
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @return {Node}
	 * @memberOf boundaries
	 */
	function commonContainer(start, end) {
		return toRange(start, end).commonAncestorContainer;
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf boundaries
	 */
	function fromPosition(x, y, doc) {
		var range = Ranges.fromPosition(x, y, doc);
		return range && fromRange(range)[0];
	}

	/**
	 * Returns true if the given value is a Boundary object.
	 *
	 * @param  {*} obj
	 * @return {boolean}
	 */
	function is(obj) {
		return Arrays.is(obj) && Dom.isNode(obj[0]) && typeof obj[1] === 'number';
	}

	return {
		is                  : is,
		get                 : get,
		select              : select,

		raw                 : raw,
		create              : create,
		normalize           : normalize,

		equals              : equals,

		container           : container,
		offset              : offset,
		document            : document,

		range               : toRange,

		fromRange           : fromRange,
		fromRanges          : fromRanges,
		fromRangeStart      : fromRangeStart,
		fromRangeEnd        : fromRangeEnd,
		fromFrontOfNode     : fromFrontOfNode,
		fromBehindOfNode    : fromBehindOfNode,
		fromStartOfNode     : fromStartOfNode,
		fromEndOfNode       : fromEndOfNode,
		fromPosition        : fromPosition,

		/* these functions should be in ranges.js */
		setRange            : setRange,
		setRanges           : setRanges,
		setRangeStart       : setRangeStart,
		setRangeEnd         : setRangeEnd,

		isAtStart           : isAtStart,
		isAtEnd             : isAtEnd,
		isAtRawStart        : isAtRawStart,
		isAtRawEnd          : isAtRawEnd,
		isTextBoundary      : isTextBoundary,
		isNodeBoundary      : isNodeBoundary,

		next                : next,
		prev                : prev,
		nextRawBoundary     : nextRawBoundary,
		prevRawBoundary     : prevRawBoundary,

		jumpOver            : jumpOver,

		nextWhile           : nextWhile,
		prevWhile           : prevWhile,
		stepWhile           : stepWhile,
		walkWhile           : walkWhile,

		nextNode            : nextNode,
		prevNode            : prevNode,
		nodeAfter           : nodeAfter,
		nodeBefore          : nodeBefore,

		commonContainer     : commonContainer
	};
});

/**
 * keys.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @see:
 * https://lists.webkit.org/pipermail/webkit-dev/2007-December/002992.html
 *
 * @todo:
 * consider https://github.com/nostrademons/keycode.js/blob/master/keycode.js
 * @namespace keys
 */
define('keys',[
	'maps',
	'strings',
	'boundaries'
], function (
	Maps,
	Strings,
	Boundaries
) {
	

	var CODE_KEY = {
		8  : 'backspace',
		9  : 'tab',
		12 : 'f1',
		13 : 'enter',
		16 : 'shift',
		17 : 'ctrl',
		18 : 'alt',
		20 : 'capslock',
		23 : 'end',
		24 : 'home',
		27 : 'escape',
		32 : 'space',
		33 : 'pageUp',
		34 : 'pageDown',
		37 : 'left',
		38 : 'up',
		39 : 'right',
		40 : 'down',
		46 : 'delete',
		65 : 'selectAll',
		66 : 'bold',
		73 : 'italic',
		85 : 'underline',
		90 : 'undo',
		91 : 'meta'
	};

	/**
	 * A map of key names to their keycode.
	 *
	 * @type {Object.<string, number>}
	 * @memberOf keys
	 */
	var CODES = {};
	Maps.forEach(CODE_KEY, function (current, index) {
		CODES[current] = parseInt(index, 10);
	});

	/**
	 * Arrow keys
	 *
	 * @type {Object.<number, string>}
	 * @memberOf keys
	 */
	var ARROWS = {
		37 : 'left',
		38 : 'up',
		39 : 'right',
		40 : 'down'
	};

	/**
	 * Returns a string of all meta keys for the given event.
	 *
	 * @private
	 * @param  {Event} event
	 * @return {string}
	 */
	function metaKeys(event) {
		var meta = [];
		if (event.altKey && (CODES['alt'] !== event.which)) {
			meta.push('alt');
		}
		if (event.ctrlKey && (CODES['ctrl'] !== event.which)) {
			meta.push('ctrl');
		}
		if (event.metaKey) {
			meta.push('meta');
		}
		if (event.shiftKey && (CODES['shift'] !== event.which)) {
			meta.push('shift');
		}
		return meta.join('+');
	}

	var EVENTS = {
		'keyup'    : true,
		'keydown'  : true,
		'keypress' : true
	};

	/**
	 * Provides meta, keycode
	 *
	 * @param  {!AlohaEvent} event
	 * @return {AlohaEvent}
	 * @memberOf keys
	 */
	function middleware(event) {
		var keys = parseKeys(event.nativeEvent);
		event.meta = keys.meta;
		event.keycode = event.keycode || keys.keycode;
		return event;
	}

	/**
	 * Parses keys for a browser event. Will return an object as follows:
	 *
	 * <pre>
	 * {
	 *     meta    : 'cmd+shift', // active meta keys
	 *     keycode : 32,          // currently active keycode
	 *     key     : 'space',     // associated key
	 *     char    : ''           // corresponding lowercase character key
	 * }
	 * </pre>
	 *
	 * @param  {!Event} event
	 * @return {Object.<string, *>}
	 * @memberOf keys
	 */
	function parseKeys(event) {
		return {
			meta    : metaKeys(event),
			keycode : event.which,
			key     : CODE_KEY[event.which],
			chr     : String.fromCharCode(event.which).toLowerCase()
		};
	}

	/**
	 * Goes through the shortcutHandlers object to find a shortcutHandler that
	 * matches the pressed meta keys along with the provided keycode. The
	 * shortcutHandler array must be structured as follows:
	 *
	 * <pre>
	 * // add a shortcut handler for meta+esc on keydown
	 * shortcutHandlers = {
	 *     'meta+escape'  : function () {},
	 *     'meta+shift+b' : function () {}
	 * }
	 * </pre>
	 *
	 * The order of meta keys in the shortcutHandlers array MUST be in
	 * alphabetical order, as provided by
	 *
	 * @see Keys.parseKeys
	 * @param  {!string}  meta
	 * @param  {!integer} keycode
	 * @param  {!Object}  shortcutHandlers
	 * @return {*} null if no handler could be found
	 * @memberOf keys
	 */
	function shortcutHandler(meta, keycode, shortcutHandlers) {
		// Try to resolve special keys outside the 40 (delete) to 91 (meta)
		// range. This range might need tweaking!
		var key = keycode <= 46 || keycode >= 91
		        ? CODE_KEY[keycode] || keycode
		        : String.fromCharCode(keycode).toLowerCase();
		return shortcutHandlers[meta ? meta + '+' + key : key]
		    || shortcutHandlers['*+' + key]
		    || null;
	}

	return {
		CODES           : CODES,
		EVENTS          : EVENTS,
		ARROWS          : ARROWS,
		middleware      : middleware,
		shortcutHandler : shortcutHandler,
		parseKeys       : parseKeys
	};
});

/**
 * html/predicates.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('html/predicates',[], function () {
	

	/**
	 * Void elements are elements which are not permitted to contain content.
	 * https://developer.mozilla.org/en-US/docs/Web/HTML/Element
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var VOID_ELEMENTS = {
		'AREA'    : true,
		'BASE'    : true,
		'BR'      : true,
		'COL'     : true,
		'COMMAND' : true,
		'EMBED'   : true,
		'HR'      : true,
		'IMG'     : true,
		'INPUT'   : true,
		'KEYGEN'  : true,
		'LINK'    : true,
		'META'    : true,
		'PARAM'   : true,
		'SOURCE'  : true,
		'TRACK'   : true,
		'WBR'     : true
	};

	/**
	 * A map of node tag names which are classified as block-level element.
	 *
	 * NB: "block-level" is not technically defined for elements that are new in
	 * HTML5.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var BLOCK_LEVEL_ELEMENTS = {
		'ADDRESS'    : true,
		'ARTICLE'    : true,
		'ASIDE'      : true,
		'AUDIO'      : true,
		'BLOCKQUOTE' : true,
		'CANVAS'     : true,
		'DD'         : true,
		'DIV'        : true,
		'DL'         : true,
		'FIELDSET'   : true,
		'FIGCAPTION' : true,
		'FIGURE'     : true,
		'FOOTER'     : true,
		'FORM'       : true,
		'H1'         : true,
		'H2'         : true,
		'H3'         : true,
		'H4'         : true,
		'H5'         : true,
		'H6'         : true,
		'HEADER'     : true,
		'HGROUP'     : true,
		'HR'         : true,
		'NOSCRIPT'   : true,
		'OL'         : true,
		'OUTPUT'     : true,
		'P'          : true,
		'PRE'        : true,
		'SECTION'    : true,
		'TABLE'      : true,
		'TFOOT'      : true,
		'UL'         : true,
		'VIDEO'      : true
	};

	/**
	 * Elements which don't constitue a word boundaries limit.
	 *
	 * @see
	 * http://www.w3.org/TR/html5/text-level-semantics.html
	 * https://developer.mozilla.org/en/docs/Web/Guide/HTML/HTML5/HTML5_element_list#Text-level_semantics
	 *
	 * @private
	 * @type {Object.<string, true>}
	 */
	var TEXT_LEVEL_SEMANTIC_ELEMENTS = {
		'A'      : true,
		'ABBR'   : true,
		'B'      : true,
		'BDI'    : true,
		'BDO'    : true,
		'BR'     : true,
		'CITE'   : true,
		'CODE'   : true,
		'DATA'   : true,
		'DFN'    : true,
		'EM'     : true,
		'I'      : true,
		'KBD'    : true,
		'MARK'   : true,
		'Q'      : true,
		'RP'     : true,
		'RT'     : true,
		'RUBY'   : true,
		'S'      : true,
		'SAMP'   : true,
		'SMALL'  : true,
		'SPAN'   : true,
		'STRONG' : true,
		'SUB'    : true,
		'SUP'    : true,
		'TIME'   : true,
		'U'      : true,
		'VAR'    : true,
		'WBR'    : true
	};

	/**
	 * Tags representing list container elements.
	 *
	 * @type {Object.<string, boolean>}
	 */
	var LIST_CONTAINERS = {
		'OL'   : true,
		'UL'   : true,
		'DL'   : true,
		'MENU' : true
	};

	/**
	 * Tags representing list item elements.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var LIST_ITEMS = {
		'LI' : true,
		'DT' : true,
		'DD' : true
	};

	/**
	 * These element's cannot be simply unwrapped because they have dependent
	 * children.
	 *
	 * @private
	 * @see   GROUPED_CONTAINERS
	 * @param {Object.<string, boolean>}
	 */
	var GROUP_CONTAINERS = {
		'FIELDSET' : true,
		'OBJECT'   : true,
		'FIGURE'   : true,
		'AUDIO'    : true,
		'SELECT'   : true,
		'COLGROUP' : true,
		'HGROUP'   : true,
		'TABLE'    : true,
		'TBODY'    : true,
		'TR'       : true,
		'OL'       : true,
		'UL'       : true,
		'DL'       : true,
		'MENU'     : true
	};

	/**
	 * These element's cannot be simply unwrapped because they parents only
	 * allows these as their immediate child nodes.
	 *
	 * @private
	 * @see   GROUP_CONTAINERS
	 * @param {Object.<string, Array.<string>>}
	 */
	var GROUPED_ELEMENTS = {
		'LI'    : ['OL', 'UL', 'DL'],
		'DT'    : ['DL'],
		'DD'    : ['DL'],
		'TBODY' : ['TABLE'],
		'TR'    : ['TABLE', 'TBODY'],
		'TH'    : ['TABLE', 'TBODY'],
		'TD'    : ['TR', 'TH']
	};

	/**
	 * Checks if the given node is grouping container.
	 *
	 * Grouping containers include TABLE, FIELDSET, SELECT.  
	 *
	 * @see    GROUP_CONTAINERS
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isGroupContainer(node) {
		return GROUP_CONTAINERS[node.nodeName];
	}

	/**
	 * Checks if the given node an element that can only be a child of a group
	 * container.
	 *
	 * LI, TD are the classic cases.
	 *
	 * @see    GROUPED_CONTAINER
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isGroupedElement(node) {
		return !!GROUPED_ELEMENTS[node.nodeName];
	}

	/**
	 * Checks if the given node is one of the 4 list item elements.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isListItem(node) {
		return !!LIST_ITEMS[node.nodeName];
	}

	/**
	 * Checks if the given node is one of the 4 list grouping containers.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isListContainer(node) {
		return !!LIST_CONTAINERS[node.nodeName];
	}

	/**
	 * Checks whether `node` is the TABLE element.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isTableContainer(node) {
		return node.nodeName === 'TABLE';
	}

	/**
	 * Check whether the given node is a void element type.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isVoidNode(node) {
		return !!VOID_ELEMENTS[node.nodeName];
	}

	/**
	 * Similar to hasBlockStyle() except relies on the nodeName of the given
	 * node which works for attached as well as and detached nodes.
	 *
	 * Will return true if the given node is a block node type--regardless of
	 * how it is rendered.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isBlockNode(node) {
		return !!BLOCK_LEVEL_ELEMENTS[node.nodeName];
	}

	/**
	 * Similar to hasInlineStyle() in the same sense as isBlockNode() is similar
	 * to hasBlockStyle().
	 *
	 * Will return true if the given node is an inline node type--regardless of
	 * how it is rendered.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isInlineNode(node) {
		return !isBlockNode(node);
	}

	/**
	 * Check whether the given node is a text-level semantic element type.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isTextLevelSemanticNode(node) {
		return !!TEXT_LEVEL_SEMANTIC_ELEMENTS[node.nodeName];
	}

	/**
	 * Heading tag names.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var HEADINGS = {
		'H1' : true,
		'H2' : true,
		'H3' : true,
		'H4' : true,
		'H5' : true,
		'H6' : true
	};

	/**
	 * Whether the given node is a heading element.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isHeading(node) {
		return !!HEADINGS[node.nodeName];
	}

	return {
		isBlockNode             : isBlockNode,
		isGroupContainer        : isGroupContainer,
		isGroupedElement        : isGroupedElement,
		isHeading               : isHeading,
		isInlineNode            : isInlineNode,
		isListContainer         : isListContainer,
		isListItem              : isListItem,
		isTableContainer        : isTableContainer,
		isTextLevelSemanticNode : isTextLevelSemanticNode,
		isVoidNode              : isVoidNode
	};
});

/**
 * html/styles.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('html/styles',[
	'dom',
	'html/predicates'
], function (
	Dom,
	Predicates
) {
	

	/**
	 * Tags representing non-block-level elements which are nevertheless line
	 * breaking.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var LINE_BREAKING_VOID_ELEMENTS = {
		'BR'  : true,
		'HR'  : true,
		'IMG' : true
	};

	/**
	 * Tags representing list item elements.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var LIST_ITEMS = {
		'LI' : true,
		'DT' : true,
		'DD' : true
	};

	/**
	 * Map of CSS values for the display property that would cause an element
	 * to be rendered with inline style.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var nonBlockDisplayValuesMap = {
		'inline'       : true,
		'inline-block' : true,
		'inline-table' : true,
		'none'         : true
	};

	/**
	 * Checks whether the given node is rendered with block style.
	 *
	 * A block node is either an Element whose "display" property does not have
	 * resolved value "inline" or "inline-block" or "inline-table" or "none", or
	 * a Document, or a DocumentFragment.
	 *
	 * Note that this function depends on style inheritance which only works if
	 * the given node is attached to the document.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function hasBlockStyle(node) {
		if (!node) {
			return false;
		}
		switch (node.nodeType) {
		case Dom.Nodes.DOCUMENT:
		case Dom.Nodes.DOCUMENT_FRAGMENT:
			return true;
		case Dom.Nodes.ELEMENT:
			var style = Dom.getComputedStyle(node, 'display');
			return style ? !nonBlockDisplayValuesMap[style] : Predicates.isBlockNode(node);
		default:
			return false;
		}
	}

	/**
	 * Checks whether the given node is rendered with inline style.
	 *
	 * An inline node is a node that is not a block node.
	 *
	 * Note that this function depends on style inheritance which only works if
	 * the given node is attached to the document.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function hasInlineStyle(node) {
		return !hasBlockStyle(node);
	}

	/**
	 * Returns true for nodes that introduce linebreaks.
	 *
	 * Unlike hasBlockStyle...
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function hasLinebreakingStyle(node) {
		return LINE_BREAKING_VOID_ELEMENTS[node.nodeName]
		    || LIST_ITEMS[node.nodeName]
		    || hasBlockStyle(node);
	}

	/**
	 * Checks whether the given string represents a whitespace preservation
	 * style property.
	 *
	 * @param  {string} string
	 * @return {boolean}
	 */
	function isWhiteSpacePreserveStyle(cssWhiteSpaceValue) {
		return cssWhiteSpaceValue === 'pre'
		    || cssWhiteSpaceValue === 'pre-wrap'
		    || cssWhiteSpaceValue === '-moz-pre-wrap';
	}

	/**
	 * A map of style properties that are not inheritable.
	 *
	 * TODO This list is incomplete but should look something like
	 * http://www.w3.org/TR/CSS21/propidx.html
	 *
	 * @type {Object.<string, boolean>}
	 */
	var notInheritedStyles = {
		'background-color': true,
		'underline': true
	};

	/**
	 * Checks whether the given style name is among those that are not
	 * inheritable.
	 *
	 * TODO complete the list of inherited/notInheritedStyles
	 *
	 * @param  {string} styleName
	 * @return {boolean}
	 */
	function isStyleInherited(styleName) {
		return !notInheritedStyles[styleName];
	}

	return {
		isStyleInherited          : isStyleInherited,
		isWhiteSpacePreserveStyle : isWhiteSpacePreserveStyle,
		hasBlockStyle             : hasBlockStyle,
		hasInlineStyle            : hasInlineStyle,
		hasLinebreakingStyle      : hasLinebreakingStyle
	};
});

/**
 * cursors.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('cursors',[
	'dom',
	'boundaries'
], function (
	Dom,
	Boundaries
) {
	

	/**
	 * Cursor abstraction of the startContainer/startOffset and
	 * endContainer/endOffset range boundary points.
	 *
	 * @type {Cursor}
	 */
	function Cursor(node, atEnd) {
		this.node = node;
		this.atEnd = atEnd;
	}

	/**
	 * Creates a cursor instance.
	 *
	 * A cursor has the added utility over other iteration methods of iterating
	 * over the end position of an element. The start and end positions of an
	 * element are immediately before the element and immediately after the last
	 * child respectively. All node positions except end positions can be
	 * identified just by a node. To distinguish between element start and end
	 * positions, the additional atEnd boolean is necessary.
	 *
	 * @param {Node} node
	 *        The container in which the cursor is in.
	 * @param {boolean} atEnd
	 *        Whether or not the cursor is at the end of the container.
	 * @return {Cursor}
	 */
	function create(node, atEnd) {
		return new Cursor(node, atEnd);
	}

	/**
	 * Creates a new cursor from the given container and offset.
	 *
	 * @param {Node} container
	 *        If a text node, should have a parent node.
	 * @param {number} offset
	 *        If container is a text node, the offset will be ignored.
	 * @return {Cursor}
	 */
	function createFromBoundary(container, offset) {
		return create(
			Dom.nodeAtOffset(container, offset),
			Boundaries.isAtEnd(Boundaries.raw(container, offset))
		);
	}

	Cursor.prototype.next = function () {
		var node = this.node;
		var next;
		if (this.atEnd || !Dom.isElementNode(node)) {
			next = node.nextSibling;
			if (next) {
				this.atEnd = false;
			} else {
				next = node.parentNode;
				if (!next) {
					return false;
				}
				this.atEnd = true;
			}
			this.node = next;
		} else {
			next = node.firstChild;
			if (next) {
				this.node = next;
			} else {
				this.atEnd = true;
			}
		}
		return true;
	};

	Cursor.prototype.prev = function () {
		var node = this.node;
		var prev;
		if (this.atEnd) {
			prev = node.lastChild;
			if (prev) {
				this.node = prev;
				if (!Dom.isElementNode(prev)) {
					this.atEnd = false;
				}
			} else {
				this.atEnd = false;
			}
		} else {
			prev = node.previousSibling;
			if (prev) {
				if (Dom.isElementNode(prev)) {
					this.atEnd = true;
				}
			} else {
				prev = node.parentNode;
				if (!prev) {
					return false;
				}
			}
			this.node = prev;
		}
		return true;
	};

	Cursor.prototype.skipPrev = function (cursor) {
		var prev = this.prevSibling();
		if (prev) {
			this.node = prev;
			this.atEnd = false;
			return true;
		}
		return this.prev();
	};

	Cursor.prototype.skipNext = function (cursor) {
		if (this.atEnd) {
			return this.next();
		}
		this.atEnd = true;
		return this.next();
	};

	Cursor.prototype.nextWhile = function (cond) {
		while (cond(this)) {
			if (!this.next()) {
				return false;
			}
		}
		return true;
	};

	Cursor.prototype.prevWhile = function (cond) {
		while (cond(this)) {
			if (!this.prev()) {
				return false;
			}
		}
		return true;
	};

	Cursor.prototype.parent = function () {
		return this.atEnd ? this.node : this.node.parentNode;
	};

	Cursor.prototype.prevSibling = function () {
		return this.atEnd ? this.node.lastChild : this.node.previousSibling;
	};

	Cursor.prototype.nextSibling = function () {
		return this.atEnd ? null : this.node.nextSibling;
	};

	Cursor.prototype.equals = function (cursor) {
		return cursor.node === this.node && cursor.atEnd === this.atEnd;
	};

	Cursor.prototype.setFrom = function (cursor) {
		this.node = cursor.node;
		this.atEnd = cursor.atEnd;
	};

	Cursor.prototype.clone = function () {
		return create(this.node, this.atEnd);
	};

	Cursor.prototype.insert = function (node) {
		return Dom.insert(node, this.node, this.atEnd);
	};

	Cursor.prototype['next'] = Cursor.prototype.next;
	Cursor.prototype['prev'] = Cursor.prototype.prev;
	Cursor.prototype['skipPrev'] = Cursor.prototype.skipPrev;
	Cursor.prototype['skipNext'] = Cursor.prototype.skipNext;
	Cursor.prototype['nextWhile'] = Cursor.prototype.nextWhile;
	Cursor.prototype['prevWhile'] = Cursor.prototype.prevWhile;
	Cursor.prototype['parent'] = Cursor.prototype.parent;
	Cursor.prototype['prevSibling'] = Cursor.prototype.prevSibling;
	Cursor.prototype['nextSibling'] = Cursor.prototype.nextSibling;
	Cursor.prototype['equals'] = Cursor.prototype.equals;
	Cursor.prototype['setFrom'] = Cursor.prototype.setFrom;
	Cursor.prototype['clone'] = Cursor.prototype.clone;
	Cursor.prototype['insert'] = Cursor.prototype.insert;

	/**
	 * Sets the start boundary of a given range from the given range position.
	 *
	 * @param {Cursor} pos
	 * @param {Range} range
	 * @return {Range}
	 *         The modified range.
	 */
	function setRangeStart(range, pos) {
		if (pos.atEnd) {
			range.setStart(pos.node, Dom.nodeLength(pos.node));
		} else {
			range.setStart(pos.node.parentNode, Dom.nodeIndex(pos.node));
		}
		return range;
	}

	/**
	 * Sets the end boundary of a given range from the given range position.
	 *
	 * @param {Range} range
	 * @param {Cursor} pos
	 * @return {Range}
	 *         The given range, having been modified.
	 */
	function setRangeEnd(range, pos) {
		if (pos.atEnd) {
			range.setEnd(pos.node, Dom.nodeLength(pos.node));
		} else {
			range.setEnd(pos.node.parentNode, Dom.nodeIndex(pos.node));
		}
		return range;
	}

	/**
	 * Transforms a cursor to a boundary.
	 * @param {Cursor} cursor
	 * @return {Boundary}
	 */
	function toBoundary(cursor) {
		if (cursor.atEnd) {
			return Boundaries.create(cursor.node, Dom.nodeLength(cursor.node));
		}
		return Boundaries.create(cursor.node.parentNode, Dom.nodeIndex(cursor.node));
	}

	/**
	 * Sets the startContainer/startOffset and endContainer/endOffset boundary
	 * points of the given range, based on the given start and end Cursors.
	 *
	 * @param {Range} range
	 * @param {Cursor} start
	 * @param {Cursor} end
	 * @return {Range}
	 *         The given range, having had its boundary points modified.
	 */
	function setToRange(range, start, end) {
		if (start) {
			setRangeStart(range, start);
		}
		if (end) {
			setRangeEnd(range, end);
		}
		return range;
	}

	return {
		cursor                  : create,
		cursorFromBoundaryPoint : createFromBoundary,
		create                  : create,
		createFromBoundary      : createFromBoundary,
		setToRange              : setToRange,
		setRangeStart           : setRangeStart,
		setRangeEnd             : setRangeEnd,
		toBoundary              : toBoundary
	};
});

/**
 * html/elements.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('html/elements',[
	'html/styles',
	'html/predicates',
	'dom',
	'arrays',
	'cursors',
	'strings'
], function (
	Styles,
	Predicates,
	Dom,
	Arrays,
	Cursors,
	Strings
) {
	

	/**
	 * Checks whether the given node should be treated like a void element.
	 *
	 * Void elements like IMG and INPUT are considered as void type, but so are
	 * "block" (elements inside of editale regions that are not themselves
	 * editable).
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isVoidType(node) {
		return Predicates.isVoidNode(node) || !Dom.isEditableNode(node);
	}

	/**
	 * Returns true if the given node is unrendered whitespace, with the caveat
	 * that it only examines the given node and not any siblings.  An additional
	 * check is necessary to determine whether the node occurs after/before a
	 * linebreaking node.
	 *
	 * Taken from
	 * http://code.google.com/p/rangy/source/browse/trunk/src/js/modules/rangy-cssclassapplier.js
	 * under the MIT license.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isUnrenderedWhitespaceNoBlockCheck(node) {
		if (!Dom.isTextNode(node)) {
			return false;
		}
		if (!node.length) {
			return true;
		}
		if (Strings.NOT_SPACE.test(node.nodeValue)
				|| Strings.NON_BREAKING_SPACE.test(node.nodeValue)) {
			return false;
		}
		var cssWhiteSpace;
		if (node.parentNode) {
			cssWhiteSpace = Dom.getComputedStyle(node.parentNode, 'white-space');
			if (Styles.isWhiteSpacePreserveStyle(cssWhiteSpace)) {
				return false;
			}
		}
		if ('pre-line' === cssWhiteSpace) {
            if (/[\r\n]/.test(node.data)) {
                return false;
            }
        }
		return true;
	}

	/**
	 * Tags representing non-block-level elements which are nevertheless line
	 * breaking.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var LINE_BREAKING_VOID_ELEMENTS = {
		'BR'  : true,
		'HR'  : true,
		'IMG' : true
	};

	/**
	 * Returns true if the node at point is unrendered, with the caveat that it
	 * only examines the node at point and not any siblings.  An additional
	 * check is necessary to determine whether the whitespace occurrs
	 * after/before a linebreaking node.
	 *
	 * @private
	 */
	function isUnrenderedAtPoint(point) {
		return (isUnrenderedWhitespaceNoBlockCheck(point.node)
				|| (Dom.isElementNode(point.node)
					&& Styles.hasInlineStyle(point.node)
					&& !LINE_BREAKING_VOID_ELEMENTS[point.node]));
	}

	/**
	 * Tries to move the given point to the end of the line, stopping to the
	 * left of a br or block node, ignoring any unrendered nodes. Returns true
	 * if the point was successfully moved to the end of the line, false if some
	 * rendered content was encountered on the way. point will not be mutated
	 * unless true is returned.
	 *
	 * @private
	 * @param  {Cursor} point
	 * @return {boolean} True if the cursor is moved
	 */
	function skipUnrenderedToEndOfLine(point) {
		var cursor = point.clone();
		cursor.nextWhile(isUnrenderedAtPoint);
		if (!Styles.hasLinebreakingStyle(cursor.node)) {
			return false;
		}
		point.setFrom(cursor);
		return true;
	}

	/**
	 * Tries to move the given point to the start of the line, stopping to the
	 * right of a br or block node, ignoring any unrendered nodes. Returns true
	 * if the point was successfully moved to the start of the line, false if
	 * some rendered content was encountered on the way. point will not be
	 * mutated unless true is returned.
	 *
	 * @private
	 * @param {Cursor} point
	 * @return {boolean} True if the cursor is moved
	 */
	function skipUnrenderedToStartOfLine(point) {
		var cursor = point.clone();
		cursor.prev();
		cursor.prevWhile(isUnrenderedAtPoint);
		if (!Styles.hasLinebreakingStyle(cursor.node)) {
			return false;
		}
		var isBr = ('BR' === cursor.node.nodeName);
		cursor.next(); // after/out of the linebreaking node
		// Because point may be to the right of a br at the end of a
		// block, in which case the line starts before the br.
		if (isBr) {
			var endOfBlock = point.clone();
			if (skipUnrenderedToEndOfLine(endOfBlock) && endOfBlock.atEnd) {
				cursor.skipPrev(); // before the br
				cursor.prevWhile(isUnrenderedAtPoint);
				if (!Styles.hasLinebreakingStyle(cursor.node)) {
					return false;
				}
				cursor.next(); // after/out of the linebreaking node
			}
		}
		point.setFrom(cursor);
		return true;
	}

	/**
	 * Returns true if the given node is unrendered whitespace.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isUnrenderedWhitespace(node) {
		if (!isUnrenderedWhitespaceNoBlockCheck(node)) {
			return false;
		}
		return skipUnrenderedToEndOfLine(Cursors.cursor(node, false))
		    || skipUnrenderedToStartOfLine(Cursors.cursor(node, false));
	}

	/**
	 * Returns true if node is either the first or last child of its parent.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isTerminalNode(node) {
		var parent = node.parentNode;
		return parent
		    && (node === parent.firstChild || node === parent.lastChild);
	}

	/**
	 * Checks whether the given node is next to a block level element.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isAdjacentToBlock(node) {
		return (node.previousSibling && Predicates.isBlockNode(node.previousSibling))
		    || (node.nextSibling && Predicates.isBlockNode(node.nextSibling));
	}

	/**
	 * Checks whether the given node is visually rendered according to HTML5
	 * specification.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isUnrendered(node) {
		if (!Predicates.isVoidNode(node)
				// Because empty list elements are rendered
				&& !Predicates.isListItem(node)
				&& 0 === Dom.nodeLength(node)) {
			return true;
		}

		if (node.firstChild && !Dom.nextWhile(node.firstChild, isUnrendered)) {
			return true;
		}

		// Because isUnrenderedWhiteSpaceNoBlockCheck() will give us false
		// positives but never false negatives, the algorithm that will follow
		// will make certain, and will also consider unrendered <br>s
		var maybeUnrenderedNode = isUnrenderedWhitespaceNoBlockCheck(node);

		// Because a <br> element that is a child node adjacent to its parent's
		// end tag (terminal sibling) must not be rendered
		if (!maybeUnrenderedNode) {
			if ('BR' === node.nodeName
				&& isTerminalNode(node)
				&& Styles.hasLinebreakingStyle(node.parentNode)) {
				if (node.nextSibling && 'BR' === node.nextSibling.nodeName) {
					return true;
				}
				if (node.previousSibling && 'BR' === node.previousSibling.nodeName) {
					return true;
				}
				if (node.nextSibling && Dom.nextWhile(node.nextSibling, isUnrendered)) {
					return true;
				}
				if (node.previousSibling && Dom.prevWhile(node.previousSibling, isUnrendered)) {
					return true;
				}
			}
			return false;
		}

		if (isTerminalNode(node)) {
			if (!Dom.isTextNode(node)) {
				return false;
			}
			var inlineNode = Dom.nextNonAncestor(node, false, function (node) {
				return Predicates.isInlineNode(node) && !isUnrendered(node);
			}, function (node) {
				return Styles.hasLinebreakingStyle(node) || Dom.isEditingHost(node);
			});
			return !inlineNode;
		}

		return isAdjacentToBlock(node)
		    || skipUnrenderedToEndOfLine(Cursors.create(node, false))
		    || skipUnrenderedToStartOfLine(Cursors.create(node, false));
	}

	/**
	 * Returns true of the given node is rendered.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 * @memberOf html
	 */
	function isRendered(node) {
		return !isUnrendered(node);
	}

	/**
	 * Parses the given markup string and returns an array of detached top-level
	 * elements. Event handler attributes will not trigger immediately to prevent
	 * XSS, so aloha.editor.parse('<img src="" onerror="alert(1)">', document);
	 * will NOT generate an alert box. See https://github.com/alohaeditor/Aloha-Editor/issues/1270
	 * for details.
	 *
	 * @param  {string}   html
	 * @param  {Document} doc
	 * @return {Array.<Node>}
	 * @memberOf html
	 */
	function parse(html, doc) {
		var context = (doc.implementation && doc.implementation.createHTMLDocument)
			? doc.implementation.createHTMLDocument()
			: doc;
		var parser = context.createElement('DIV');
		parser.innerHTML = html;
		var nodes = Arrays.coerce(parser.childNodes);
		nodes.forEach(Dom.remove);
		return nodes;
	}

	/**
	 * Checks whether the given node is a BR element that is placed within an
	 * otherwise empty line-breaking element to ensure that the line-breaking
	 * element it will be rendered (with one line-height).
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isProppingBr(node) {
		var parent = node.parentNode;
		if ('BR' !== node.nodeName || !parent) {
			return false;
		}
		if (!Styles.hasLinebreakingStyle(parent)) {
			return false;
		}
		var rendered = Dom.children(parent).filter(isRendered);
		return 1 === rendered.length && node === rendered[0];
	}

	return {
		parse                              : parse,
		isVoidType                         : isVoidType,
		isRendered                         : isRendered,
		isUnrendered                       : isUnrendered,
		isUnrenderedWhitespace             : isUnrenderedWhitespace,
		isUnrenderedWhitespaceNoBlockCheck : isUnrenderedWhitespaceNoBlockCheck,
		isProppingBr                       : isProppingBr,
		skipUnrenderedToEndOfLine          : skipUnrenderedToEndOfLine,
		skipUnrenderedToStartOfLine        : skipUnrenderedToStartOfLine
	};
});

/**
 * paths.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace paths
 */
define('paths',[
	'dom',
	'arrays',
	'boundaries'
], function (
	Dom,
	Arrays,
	Boundaries
) {
	

	/**
	 * Returns a "path" from the given boundary position, up to the specified
	 * node.  The `limit` node must contain the given boundary position.
	 *
	 * @param  {Node}     limit
	 * @param  {Boundary} boundary
	 * @return {Array.<number>}
	 * @memberOf paths
	 */
	function fromBoundary(limit, boundary) {
		var offset = Boundaries.offset(boundary);
		var container = Boundaries.container(boundary);
		if (container === limit) {
			return [offset];
		}
		var chain = Dom.childAndParentsUntilNode(container, limit);
		var path = chain.reduce(function (path, node) {
			return path.concat(Dom.nodeIndex(node));
		}, [offset]);
		path.reverse();
		return path;
	}

	/**
	 * Resolves the given path to a boundary positioned inside DOM tree whose
	 * root is `container`.
	 *
	 * @param  {Node}           container
	 * @param  {Array.<number>} path
	 * @return {Boundary}
	 * @memberOf paths
	 */
	function toBoundary(container, path) {
		var node = path.slice(0, -1).reduce(function (node, offset) {
			return node.childNodes[offset] || node;
		}, container);
		return Boundaries.raw(node, Arrays.last(path) || 0);
	}

	return {
		toBoundary   : toBoundary,
		fromBoundary : fromBoundary
	};
});

/**
 * html/traversing.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('html/traversing',[
	'html/elements',
	'html/styles',
	'html/predicates',
	'dom',
	'paths',
	'arrays',
	'boundaries',
	'strings'
], function (
	Elements,
	Styles,
	Predicates,
	Dom,
	Paths,
	Arrays,
	Boundaries,
	Strings
) {
	

	/**
	 * Tags which represent elements that do not imply a word boundary.
	 *
	 * eg: <b>bar</b>camp where there is no word boundary in "barcamp".
	 *
	 * In HTML5 parlance, these would be many of those elements that fall in
	 * the category of "Text Level Semantics":
	 * http://www.w3.org/TR/html5/text-level-semantics.html
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var IN_WORD_TAGS = {
		'A'       : true,
		'ABBR'    : true,
		'B'       : true,
		'CITE'    : true,
		'CODE'    : true,
		'DEL'     : true,
		'EM'      : true,
		'I'       : true,
		'INS'     : true,
		'S'       : true,
		'SMALL'   : true,
		'SPAN'    : true,
		'STRONG'  : true,
		'SUB'     : true,
		'SUP'     : true,
		'U'       : true,
		'#text'   : true
	};

	var zwChars = Strings.ZERO_WIDTH_CHARACTERS.join('');
	var breakingWhiteSpaces = Arrays.difference(
		Strings.WHITE_SPACE_CHARACTERS,
		Strings.NON_BREAKING_SPACE_CHARACTERS
	).join('');

	var WSP_FROM_END = new RegExp('[' + breakingWhiteSpaces + ']+[' + zwChars + ']*$');
	var NOT_WSP_FROM_END = new RegExp('[^' + breakingWhiteSpaces + ']'
	                     + '[' + breakingWhiteSpaces + zwChars + ']*$');
	var NOT_WSP = new RegExp('[^' + breakingWhiteSpaces + zwChars + ']');
	var NOT_ZWSP = new RegExp('[^' + zwChars + ']');

	/**
	 * Returns the previous node to the given node that is not one of it's
	 * ancestors.
	 *
	 * @param  {Node} node
	 * @return {Node}
	 */
	function prevNonAncestor(node, match, until) {
		return Dom.nextNonAncestor(node, true, match, until || Dom.isEditingHost);
	}

	/**
	 * Returns the next node to the given node that is not one of it's
	 * ancestors.
	 *
	 * @param  {Node} node
	 * @return {Node}
	 */
	function nextNonAncestor(node, match, until) {
		return Dom.nextNonAncestor(node, false, match, until || Dom.isEditingHost);
	}

	/**
	 * Checks whether any white space sequence immediately after the specified
	 * offset in the given node is "significant."
	 *
	 * White Space Handling
	 * --------------------
	 *
	 * The HTML specification stipulates that not all "white spaces" in markup
	 * are visible.  Only those deemed "significant" are to be rendered visibly
	 * by the user agent.
	 *
	 * Therefore, if the position from which we are to determine the next
	 * visible character is adjacent to a "white space" (space, tabs,
	 * line-feed), or adjacent to line-breaking elements, determining the next
	 * visible character becomes non-trivial.
	 *
	 * The following rules apply:
	 *
	 * Note that for the purposes of these rules, the set of "white space" does
	 * not include non-breaking spaces (&nbsp;).
	 *
	 * 1. The first sequence of white space immediately after the opening tag
	 *    of a line-breaking element is insignificant and is ignored:
	 *
	 *     ignore
	 *       ||
	 *       vv
	 *    <p>  foo</p>
	 *       ..
	 *
	 *    will be rendered like <p>foo</p>
	 *
	 * 2. The first sequence of white space immediately after the opening tag
	 *    of a non-line-breaking element which is the first visible child of a
	 *    line-breaking element (or whose non-line-breaking ancestors are all
	 *    first visible children) is insignificant and is ignored:
	 *
	 *          ignore
	 *          |   |
	 *          v   v
	 *    <p><i> <b> foo</b></i></p>
	 *          .   .
	 *          ^
	 *          |
	 *          `-- unrendered text node
	 *
	 *    will be rendered like <p><i><b>foo</b></i></p>
	 *
	 * 3. The last sequence of white space immediately before the closing tag
	 *    of a line-breaking element is insignificant and is ignored:
	 *
	 *        ignore
	 *          |
	 *          v
	 *    <p>foo </p>
	 *          .
	 *
	 *    will be rendered like <p>foo</p>
	 *
	 *
	 * 4. The last sequence of white space immediately before the closing tag
	 *    of a non-line-breaking element which is the last visible child of a
	 *    line-breaking element (or whose non-line-breaking ancestors are all
	 *    last visible children) is insignificant and is ignored:
	 *
	 *           ignore               ignore  ignore
	 *             |                    ||    |    |
	 *             v                    vv    v    v
	 *    <p><b>foo </b></p><p><i><b>bar  </b> </i> </p>
	 *             .                    ..   .    .
	 *
	 *    will be rendered like <p><b>bar</b></p><p><i><b>bar</b></i></p>
	 *
	 * 5. The last sequence of white space immediately before the opening tag
	 *    of line-breaking elements or the first sequence of white space
	 *    immediately after the closing tag of line-breaking elements is
	 *    insignificant and is ignored:
	 *
	 *          ignore      ignore
	 *            |          |||
	 *            v          vvv
	 *    <div>foo <p>bar</p>    baz</div>
	 *            .          ...
	 *
	 * 6. The first sequence of white space immediately after a white space
	 *    character is insignificant and is ignored:
	 *
	 *         ignore
	 *           ||
	 *           vv
	 *    foo<b>   bar</b>
	 *          ...
	 *          ^
	 *          |
	 *          `-- significant
	 *
	 * @see For more information on white space handling:
	 *      http://www.w3.org/TR/REC-xml/#sec-white-space
	 *      http://www.w3.org/TR/xhtml1/overview.html#C_15
	 *      http://lists.w3.org/Archives/Public/www-dom/1999AprJun/0007.html
	 *      http://www.whatwg.org/specs/web-apps/current-work/multipage/editing.html
	 *      		#best-practices-for-in-page-editors
	 *
	 * @private
	 * @param  {Boundary} boundary Text boundary
	 * @return {boolean}
	 */
	function areNextWhiteSpacesSignificant(boundary) {
		var node = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		var isTextNode = Dom.isTextNode(node);

		if (isTextNode && node.data.substr(0, offset).search(WSP_FROM_END) > -1) {
			// Because we have preceeding whitespaces behind the given boundary
			// see rule #6
			return false;
		}

		if (0 === offset) {
			return !!prevNonAncestor(node, function (node) {
				return Predicates.isInlineNode(node) && Elements.isRendered(node);
			}, function (node) {
				return Styles.hasLinebreakingStyle(node) || Dom.isEditingHost(node);
			});
		}
		if (isTextNode && 0 !== node.data.substr(offset).search(WSP_FROM_END)) {
			return true;
		}
		return !!nextNonAncestor(node, function (node) {
			return Predicates.isInlineNode(node) && Elements.isRendered(node);
		}, function (node) {
			return Styles.hasLinebreakingStyle(node) || Dom.isEditingHost(node);
		});
	}

	/**
	 * Returns the visible character offset immediately behind the given text
	 * boundary.
	 *
	 * @param  {Boundary} boundary Text boundary
	 * @return {number}
	 */
	function prevSignificantOffset(boundary) {
		var textnode = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		var text = textnode.data.substr(0, offset);

		// "" → return -1
		//
		// " "  or "  " or "   " → return 1
		//  .       ..      ...
		if (!NOT_WSP.test(text)) {
			// Because `text` may be a sequence of white spaces so we need to
			// check if any of them are significant.
			return areNextWhiteSpacesSignificant(Boundaries.raw(textnode, 0))
			     ?  1
			     : -1;
		}

		// "a"    → spaces=0 → return offset - 0
		//
		// "a "   → spaces=1 → return offset - 0
		//   .
		//
		// "a  "  → spaces=2 → return offset - 1
		//   ..
		//
		// "a   " → spaces=3 → return offset - 2
		//   ...
		var spaces = text.match(NOT_WSP_FROM_END)[0].length - 1;

		offset = (spaces < 2) ? offset : offset - spaces + 1;

		if (0 === offset) {
			return 0;
		}

		var raw = Boundaries.raw(textnode, offset - 1);
		var isAtWhiteSpace = !NOT_WSP.test(text.charAt(offset - 1));
		var isAtVisibleChar = !isAtWhiteSpace || areNextWhiteSpacesSignificant(raw);

		return isAtVisibleChar ? offset : prevSignificantOffset(raw);
	}

	/**
	 * Returns the visible character offset immediately after the given
	 * text boundary.
	 *
	 * @param  {Boundary} boundary Text boundary
	 * @return {number}
	 */
	function nextSignificantOffset(boundary) {
		var textnode = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		var index = textnode.data.substr(offset).search(
			areNextWhiteSpacesSignificant(boundary) ? NOT_ZWSP : NOT_WSP
		);
		return (-1 === index) ? -1 : offset + index;
	}

	/**
	 * Returns the boundary of the next visible character.
	 *
	 * All insignificant characters (including "zero-width" characters are
	 * ignored).
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {?Boundary}
	 */
	function nextCharacterBoundary(boundary) {
		if (Boundaries.isNodeBoundary(boundary)) {
			return null;
		}
		var offset = nextSignificantOffset(boundary);
		return (-1 === offset)
		     ? null
		     : Boundaries.create(Boundaries.container(boundary), offset + 1);
	}

	/**
	 * Returns the boundary of the previous visible character from the given
	 * position in the document.
	 *
	 * All insignificant characters (including "zero-width" characters are
	 * ignored).
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {?Boundary}
	 */
	function prevCharacterBoundary(boundary) {
		if (Boundaries.isNodeBoundary(boundary)) {
			return null;
		}
		var offset = prevSignificantOffset(boundary);
		return (-1 === offset)
		     ? null
		     : Boundaries.create(Boundaries.container(boundary), offset - 1);
	}

	/**
	 * Expands the boundary.
	 *
	 * @private
	 * @param  {Boundary}                   boundary
	 * @param  {function(Boundary, function(Boundary):boolean):Boundary}
	 *                                      step
	 * @param  {function(Boundary):Node}    nodeAt
	 * @param  {function(Boundary):boolean} isAtStart
	 * @param  {function(Boundary):boolean} isAtEnd
	 * @return {Boundary}
	 */
	function expand(boundary, step, nodeAt, isAtStart, isAtEnd) {
		return Boundaries.normalize(step(boundary, function (boundary) {
			var node = nodeAt(boundary);
			if (Elements.isUnrendered(node)) {
				return true;
			}
			if (isAtEnd(boundary)) {
				//       <    >
				// <host>| or |</host>
				if (Dom.isEditingHost(node)) {
					return false;
				}
				//    < >
				// <li>|</li>
				if (Predicates.isListItem(node) && isAtStart(boundary)) {
					return false;
				}
				//    <    >
				// <p>| or |</p>
				return true;
			}
			return !Dom.isTextNode(node) && !Elements.isVoidType(node);
		}));
	}

	/**
	 * Steps forward (according to stepForward) while the given condition is
	 * true.
	 *
	 * @private
	 * @param  {Boundary}                   boundary
	 * @param  {function(Boundary):boolean} cond
	 * @return {Boundary}
	 */
	function nextBoundaryWhile(boundary, cond) {
		return Boundaries.stepWhile(boundary, cond, stepForward);
	}

	/**
	 * Steps backwards while the given condition is true.
	 *
	 * @private
	 * @param  {Boundary}                   boundary
	 * @param  {function(Boundary):boolean} cond
	 * @return {Boundary}
	 */
	function prevBoundaryWhile(boundary, cond) {
		return Boundaries.stepWhile(boundary, cond, stepBackward);
	}

	/**
	 * Expands the boundary backward.
	 *
	 * Drilling through...
	 *
	 * >
	 * |</p></li><li><b><i>foo...
	 *
	 * should result in
	 *
	 * </p></li><li><b><i>|foo...
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function expandBackward(boundary) {
		return expand(
			boundary,
			prevBoundaryWhile,
			Boundaries.prevNode,
			Boundaries.isAtEnd,
			Boundaries.isAtStart
		);
	}

	/**
	 * Expands the boundary forward.
	 * Similar to expandBackward().
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function expandForward(boundary) {
		return expand(
			boundary,
			nextBoundaryWhile,
			Boundaries.nextNode,
			Boundaries.isAtStart,
			Boundaries.isAtEnd
		);
	}

	/**
	 * Returns an node/offset namedtuple of the next visible position in the
	 * document.
	 *
	 * The next visible position is always the next visible character, space,
	 * or line break or space.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @param  {Object}   steps
	 * @return {Boundary}
	 */
	function stepVisualBoundary(boundary, steps) {
		// Inside of text node
		//    < >
		// <#te|xt>
		if (Boundaries.isTextBoundary(boundary)) {
			var next = steps.nextCharacter(boundary);
			if (next) {
				return next;
			}
		}

		var node = steps.nodeAt(boundary);

		// At start or end of editable
		//       <    >
		// <host>| or |</host>
		if (Dom.isEditingHost(node)) {
			return boundary;
		}

		if (Dom.isTextNode(node) || Elements.isUnrendered(node)) {
			return stepVisualBoundary(steps.stepBoundary(boundary), steps);
		}

		if (Styles.hasLinebreakingStyle(node)) {
			return steps.expand(steps.stepBoundary(boundary));
		}

		while (true) {
			// At space consuming tag
			// >               <
			// |<#text> or <br>|
			if (Elements.isRendered(node)) {
				if (Dom.isTextNode(node)
						|| Styles.hasLinebreakingStyle(node)
							|| Dom.isEditingHost(node)) {
					break;
				}
			}
			// At inline nodes
			//    >             <
			// <p>|<i>  or  </b>|<br>
			boundary = steps.stepBoundary(boundary);
			node = steps.nodeAt(boundary);
		}

		return stepVisualBoundary(boundary, steps);
	}

	/**
	 * Like Boundaries.next() except that it will skip over void-type nodes.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function stepForward(boundary) {
		if (Boundaries.isNodeBoundary(boundary)) {
			var node = Boundaries.nodeAfter(boundary);
			if (node && Elements.isVoidType(node)) {
				return Boundaries.jumpOver(boundary);
			}
		}
		return Boundaries.nextRawBoundary(boundary);
	}

	/**
	 * Like Boundaries.prev() except that it will skip over void-type nodes.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function stepBackward(boundary) {
		if (Boundaries.isNodeBoundary(boundary)) {
			var node = Boundaries.nodeBefore(boundary);
			if (node && Elements.isVoidType(node)) {
				return Boundaries.fromFrontOfNode(node);
			}
		}
		return Boundaries.prevRawBoundary(boundary);
	}

	var forwardSteps = {
		nextCharacter      : nextCharacterBoundary,
		stepBoundary       : stepForward,
		expand             : expandForward,
		adjacentNode       : Boundaries.nodeAfter,
		nodeAt             : Boundaries.nextNode,
		followingSibling   : function followingSibling(node) {
			return node.nextSibling;
		},
		stepVisualBoundary : function stepVisualBoundary(node) {
			return nextVisualBoundary(Boundaries.raw(node, 0));
		}
	};

	var backwardSteps = {
		nextCharacter      : prevCharacterBoundary,
		stepBoundary       : stepBackward,
		expand             : expandBackward,
		adjacentNode       : Boundaries.nodeBefore,
		nodeAt             : Boundaries.prevNode,
		followingSibling   : function followingSibling(node) {
			return node.previousSibling;
		},
		stepVisualBoundary : function stepVisualBoundary(node) {
			return prevVisualBoundary(Boundaries.raw(node, Dom.nodeLength(node)));
		}
	};

	/**
	 * Checks whether or not the given node is a word breaking node.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isWordbreakingNode(node) {
		return !IN_WORD_TAGS[node.nodeName];
	}

	/**
	 * Steps to the next visual boundary ahead of the given boundary.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function nextVisualBoundary(boundary) {
		return stepVisualBoundary(boundary, forwardSteps);
	}

	/**
	 * Steps to the next visual boundary behind of the given boundary.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function prevVisualBoundary(boundary) {
		return stepVisualBoundary(boundary, backwardSteps);
	}

	/**
	 * Moves the boundary over any insignificant positions.
	 *
	 * Insignificant boundary positions are those where the boundary is
	 * immediately before unrendered content.  Since such content is invisible,
	 * the boundary is rendered as though it is after the insignificant content.
	 * This function simply moves the boundary forward so that the given
	 * boundary is infact where it seems to be visually.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function nextSignificantBoundary(boundary) {
		var next = boundary;
		var node;

		if (Boundaries.isTextBoundary(next)) {
			var offset = nextSignificantOffset(next);

			// Because there may be no visible characters following the node
			// boundary in its container.
			//
			// "foo| "</p> or "foo| "" bar" or "foo|"<br>
			//      .              .  .
			if (-1 === offset) {
				node = Boundaries.nodeAfter(next);
				if (node && Elements.isUnrendered(node)) {
					return nextSignificantBoundary(Boundaries.jumpOver(next));
				}
				if (node && Elements.isVoidType(node)) {
					return next;
				}
				return nextSignificantBoundary(Boundaries.next(next));
			}

			// Because the boundary may already be at a significant offset.
			//
			// "|foo"
			if (Boundaries.offset(next) === offset) {
				return next;
			}

			// "foo | bar"
			//       .
			next = Boundaries.create(Boundaries.container(next), offset);
			return nextSignificantBoundary(next);
		}

		node = Boundaries.nextNode(next);

		// |"foo" or <p>|" foo"
		//                .
		if (Dom.isTextNode(node)) {
			return nextSignificantBoundary(Boundaries.nextRawBoundary(next));
		}

		while (!Dom.isEditingHost(node) && Elements.isUnrendered(node)) {
			next = Boundaries.next(next);
			node = Boundaries.nextNode(next);
		}

		return next;
	}

	/**
	 * Checks whether the left boundary is at the same visual position as the
	 * right boundary.
	 *
	 * Take note that the order of the boundary is important:
	 * (left, right) is not necessarily the same as (right, left).
	 *
	 * @param  {Boundary} left
	 * @param  {Boundary} right
	 * @return {boolean}
	 * @memberOf traversing
	 */
	function isBoundariesEqual(left, right) {
		var node, consumesOffset;

		left = nextSignificantBoundary(Boundaries.normalize(left));
		right = nextSignificantBoundary(Boundaries.normalize(right));

		while (left && !Boundaries.equals(left, right)) {
			node = Boundaries.nextNode(left);

			if (Dom.isEditingHost(node)) {
				return false;
			}

			consumesOffset = Dom.isTextNode(node)
			              || Elements.isVoidType(node)
			              || Styles.hasLinebreakingStyle(node);

			if (consumesOffset && Elements.isRendered(node)) {
				return false;
			}

			left = nextSignificantBoundary(Boundaries.next(left));
		}

		return true;
	}

	/**
	 * Moves the given boundary backwards over any positions that are (visually
	 * insignificant)invisible.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function prevSignificantBoundary(boundary) {
		var next = boundary;
		var node;

		if (Boundaries.isTextBoundary(next)) {
			var offset = prevSignificantOffset(next);

			// Because there may be no visible characters following the node
			// boundary in its container
			//
			// <p>" |foo"</p>
			//     .
			if (-1 === offset) {
				var after = Boundaries.prev(next);

				//     ,-----+-- equal
				//     |     |
				//     v     v
				// "foo "</p> </div>..
				//     .     .
				while (isBoundariesEqual(after, next)) {
					// Because linebreaks are significant positions
					if (Styles.hasLinebreakingStyle(Boundaries.prevNode(after))) {
						break;
					}
					after = Boundaries.prev(after);
				}
				return prevSignificantBoundary(after);
			}

			// "foo|"
			if (Boundaries.offset(next) === offset) {
				return next;
			}

			// "foo | bar"
			//       .
			next = Boundaries.create(Boundaries.container(next), offset);
			return prevSignificantBoundary(next);
		}

		node = Boundaries.prevNode(next);

		// <b>"foo"|</b>
		if (Dom.isTextNode(node)) {
			return prevSignificantBoundary(Boundaries.prevRawBoundary(next));
		}

		while (!Dom.isEditingHost(node) && Elements.isUnrendered(node)) {
			next = Boundaries.prev(next);
			node = Boundaries.prevNode(next);
		}

		return next;
	}

	/**
	 * Returns the next word boundary offset ahead of the given text boundary.
	 *
	 * Returns -1 if no word boundary is found.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {number}
	 */
	function nextWordBoundaryOffset(boundary) {
		var node   = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		var text   = node.data.substr(offset);
		var index  = text.search(Strings.WORD_BOUNDARY);
		return (-1 === index) ? -1 : offset + index;
	}

	/**
	 * Returns the next word boundary offset behind the given text boundary.
	 *
	 * Returns -1 if no word boundary is found.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {number}
	 */
	function prevWordBoundaryOffset(boundary) {
		var node   = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		var text   = node.data.substr(0, offset);
		var index  = text.search(Strings.WORD_BOUNDARY_FROM_END);
		return (-1 === index) ? -1 : index + 1;
	}

	/**
	 * Returns the next word boundary position.
	 *
	 * This will always be a position in front of a word or punctuation, but
	 * never in front of a space.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function nextWordBoundary(boundary) {
		var node, next;

		if (Boundaries.isNodeBoundary(boundary)) {
			node = Boundaries.nextNode(boundary);
			next = Boundaries.nextRawBoundary(boundary);

			//         .---- node ----.
			//         |              |
			//         v              v
			// "foo"|</p> or "foo"|<input>
			if (isWordbreakingNode(node)) {
				return boundary;
			}

			return nextWordBoundary(next);
		}

		var offset = nextWordBoundaryOffset(boundary);

		// Because there may be no word boundary ahead of `offset` in the
		// boundary's container, we need to step out of the text node to
		// continue looking forward.
		//
		// "fo|o" or "foo|"
		if (-1 === offset) {
			next = Boundaries.next(boundary);
			node = Boundaries.nextNode(next);

			//         .---- node ----.
			//         |              |
			//         v              v
			// "foo"|</p> or "foo"|<input>
			if (isWordbreakingNode(node)) {
				return next;
			}

			return nextWordBoundary(next);
		}

		if (offset === Boundaries.offset(boundary)) {
			return boundary;
		}

		return Boundaries.raw(Boundaries.container(boundary), offset);
	}

	/**
	 * Returns the previous word boundary position.
	 *
	 * This will always be a position in front of a word or punctuation, but
	 * never in front of a space.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function prevWordBoundary(boundary) {
		var node, prev;

		if (Boundaries.isNodeBoundary(boundary)) {
			node = Boundaries.prevNode(boundary);
			prev = Boundaries.prevRawBoundary(boundary);

			//         .---- node ----.
			//         |              |
			//         v              v
			// "foo"|</p> or "foo"|<input>
			if (isWordbreakingNode(node)) {
				return boundary;
			}

			return prevWordBoundary(prev);
		}

		var offset = prevWordBoundaryOffset(boundary);

		// Because there may be no word boundary behind of `offset` in the
		// boundary's container, we need to step out of the text node to
		// continue looking backward.
		//
		// "fo|o" or "foo|"
		if (-1 === offset) {
			prev = Boundaries.prev(boundary);
			node = Boundaries.prevNode(prev);

			//         .---- node ----.
			//         |              |
			//         v              v
			// "foo"|</p> or "foo"|<input>
			if (isWordbreakingNode(node)) {
				return prev;
			}

			return prevWordBoundary(prev);
		}

		if (offset === Boundaries.offset(boundary)) {
			return boundary;
		}

		return Boundaries.raw(Boundaries.container(boundary), offset);
	}

	/**
	 * Moves the boundary forward by a unit measure.
	 *
	 * The second parameter `unit` specifies the unit with which to move the
	 * boundary.  This value may be one of the following strings:
	 *
	 * "char" -- Move in front of the next visible character.
	 *
	 * "word" -- Move in front of the next word.
	 *
	 *		A word is the smallest semantic unit.  It is a contigious sequence
	 *		of visible characters terminated by a space or puncuation character
	 *		or a word-breaker (in languages that do not use space to delimit
	 *		word boundaries).
	 *
	 * "boundary" -- Move in front of the next boundary and skip over void
	 *               elements.
	 *
	 * "offset" -- Move in front of the next visual offset.
	 *
	 *		A visual offset is the smallest unit of consumed space.  This can
	 *		be a line break, or a visible character.
	 *
	 * "node" -- Move in front of the next visible node.
	 *
	 * @param  {Boundary} boundary
	 * @param  {string=}  unit Defaults to "offset"
	 * @return {?Boundary}
	 */
	function next(boundary, unit) {
		if ('node' === unit) {
			return Boundaries.next(boundary);
		}
		boundary = nextSignificantBoundary(Boundaries.normalize(boundary));
		var nextBoundary;
		switch (unit) {
		case 'char':
			nextBoundary = nextCharacterBoundary(boundary);
			break;
		case 'word':
			nextBoundary = nextWordBoundary(boundary);
			// "| foo" or |</p>
			if (isBoundariesEqual(boundary, nextBoundary)) {
				nextBoundary = nextVisualBoundary(boundary);
			}
			break;
		case 'boundary':
			nextBoundary = stepForward(boundary);
			break;
		default:
			nextBoundary = nextVisualBoundary(boundary);
			break;
		}
		return nextBoundary;
	}

	/**
	 * Moves the boundary backwards by a unit measure.
	 *
	 * The second parameter `unit` specifies the unit with which to move the
	 * boundary. This value may be one of the following strings:
	 *
	 * "char" -- Move behind the previous visible character.
	 *
	 * "word" -- Move behind the previous word.
	 *
	 *		A word is the smallest semantic unit. It is a contigious sequence of
	 *		visible characters terminated by a space or puncuation character or
	 *		a word-breaker (in languages that do not use space to delimit word
	 *		boundaries).
	 *
	 * "boundary" -- Move in behind of the previous boundary and skip over void
	 *               elements.
	 *
	 * "offset" -- Move behind the previous visual offset.
	 *
	 *		A visual offset is the smallest unit of consumed space. This can be
	 *		a line break, or a visible character.
	 *
	 * "node" -- Move in front of the previous visible node.
	 *
	 * @param  {Boundary} boundary
	 * @param  {string=}  unit Defaults to "offset"
	 * @return {?Boundary}
	 */
	function prev(boundary, unit) {
		if ('node' === unit) {
			return Boundaries.prev(boundary);
		}
		boundary = prevSignificantBoundary(Boundaries.normalize(boundary));
		var prevBoundary;
		switch (unit) {
		case 'char':
			prevBoundary = prevCharacterBoundary(boundary);
			break;
		case 'word':
			prevBoundary = prevWordBoundary(boundary);
			// "foo |" or <p>|
			if (isBoundariesEqual(prevBoundary, boundary)) {
				prevBoundary = prevVisualBoundary(boundary);
			}
			break;
		case 'boundary':
			prevBoundary = stepBackward(boundary);
			break;
		default:
			prevBoundary = prevVisualBoundary(boundary);
			break;
		}
		return prevBoundary && prevSignificantBoundary(prevBoundary);
	}

	/**
	 * Checks whether a boundary represents a position that at the apparent end
	 * of its container's content.
	 *
	 * Unlike Boundaries.isAtEnd(), it considers the boundary position with
	 * respect to how it is visually represented, rather than simply where it
	 * is in the DOM tree.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 * @memberOf traversing
	 */
	function isAtEnd(boundary) {
		if (Boundaries.isAtEnd(boundary)) {
			// |</p>
			return true;
		}
		if (Boundaries.isTextBoundary(boundary)) {
			// "fo|o" or "foo| "
			return !NOT_WSP.test(Boundaries.container(boundary).data.substr(
				Boundaries.offset(boundary)
			));
		}
		var node = Boundaries.nodeAfter(boundary);
		// foo|<br></p> or foo|<i>bar</i>
		return !Dom.nextWhile(node, Elements.isUnrendered);
	}

	/**
	 * Checks whether a boundary represents a position that at the apparent
	 * start of its container's content.
	 *
	 * Unlike Boundaries.isAtStart(), it considers the boundary position with
	 * respect to how it is visually represented, rather than simply where it
	 * is in the DOM tree.
	 *
	 * @param  {Boundary} boundary
	 * @return {boolean}
	 * @memberOf traversing
	 */
	function isAtStart(boundary) {
		if (Boundaries.isAtStart(boundary)) {
			return true;
		}
		if (Boundaries.isTextBoundary(boundary)) {
			return !NOT_WSP.test(Boundaries.container(boundary).data.substr(
				0,
				Boundaries.offset(boundary)
			));
		}
		var node = Boundaries.nodeBefore(boundary);
		return !Dom.prevWhile(node, Elements.isUnrendered);
	}

	/**
	 * Like Boundaries.nextNode(), except that it considers whether a boundary
	 * is at the end position with respect to how the boundary is visual
	 * represented, rather than simply where it is in the DOM structure.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 */
	function nextNode(boundary) {
		return isAtEnd(boundary)
		     ? Boundaries.container(boundary)
		     : Boundaries.nodeAfter(boundary);
	}

	/**
	 * Like Boundaries.prevNode(), except that it considers whether a boundary
	 * is at the start position with respect to how the boundary is visual
	 * represented, rather than simply where it is in the DOM structure.
	 *
	 * @param  {Boundary} boundary
	 * @return {Node}
	 */
	function prevNode(boundary) {
		return isAtEnd(boundary)
		     ? Boundaries.container(boundary)
		     : Boundaries.nodeBefore(boundary);
	}

	/**
	 * Traverses between the given start and end boundaries in document order
	 * invoking step() with a list of siblings that are wholey contained within
	 * the two boundaries.
	 *
	 * @param  {!Boundary}              start
	 * @param  {!Boundary}              end
	 * @param  {function(Array.<Node>)} step
	 * @return {Array.<Boundary>}
	 */
	function walkBetween(start, end, step) {
		var cac = Boundaries.commonContainer(start, end);
		var ascent = Paths.fromBoundary(cac, start).reverse();
		var descent = Paths.fromBoundary(cac, end);
		var node = Boundaries.container(start);
		var children = Dom.children(node);
		step(children.slice(
			ascent[0],
			node === cac ? descent[0] : children.length
		));
		ascent.slice(1, -1).reduce(function (node, start) {
			var children = Dom.children(node);
			step(children.slice(start + 1, children.length));
			return node.parentNode;
		}, node.parentNode);
		if (ascent.length > 1) {
			step(Dom.children(cac).slice(Arrays.last(ascent) + 1, descent[0]));
		}
		descent.slice(1).reduce(function (node, end) {
			var children = Dom.children(node);
			step(children.slice(0, end));
			return children[end];
		}, Dom.children(cac)[descent[0]]);
		return [start, end];
	}

	return {
		prev                    : prev,
		next                    : next,
		prevNode                : prevNode,
		nextNode                : nextNode,
		prevSignificantOffset   : prevSignificantOffset,
		nextSignificantOffset   : nextSignificantOffset,
		prevSignificantBoundary : prevSignificantBoundary,
		nextSignificantBoundary : nextSignificantBoundary,
		stepForward             : stepForward,
		stepBackward            : stepBackward,
		isAtStart               : isAtStart,
		isAtEnd                 : isAtEnd,
		isBoundariesEqual       : isBoundariesEqual,
		expandBackward          : expandBackward,
		expandForward           : expandForward,
		walkBetween             : walkBetween
	};
});

/**
 * mutation.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('mutation',[
	'dom',
	'boundaries',
	'html/traversing'
], function (
	Dom,
	Boundaries,
	Traversing
) {
	

	/**
	 * Checks whether a node can be split at the given offset to yeild two
	 * nodes.
	 *
	 * @private
	 * @param {!Node} node
	 * @param {number} offset
	 * @return {boolean}
	 */
	function wouldSplitTextNode(node, offset) {
		return 0 < offset && offset < node.nodeValue.length;
	}

	/**
	 * Splits the given text node at the given offset.
	 *
	 * @TODO: could be optimized with insertData() so only a single text node is
	 *        inserted instead of two.
	 *
	 * @param {!Node} node
	 *        DOM text node.
	 * @param {number} offset
	 *        Number between 0 and the length of text of `node`.
	 * @return {!Node}
	 */
	function splitTextNode(node, offset) {
		// Because node.splitText() is buggy on IE, split it manually.
		// http://www.quirksmode.org/dom/w3c_core.html
		if (!wouldSplitTextNode(node, offset)) {
			return node;
		}
		var parent = node.parentNode;
		var text = node.nodeValue;
		var doc = parent.ownerDocument;
		var before = doc.createTextNode(text.substring(0, offset));
		var after = doc.createTextNode(
			text.substring(offset, text.length)
		);
		parent.insertBefore(before, node);
		parent.insertBefore(after, node);
		parent.removeChild(node);
		return before;
	}

	function adjustBoundaryAfterSplit(boundary, splitNode, splitOffset,
	                                  newNodeBeforeSplit) {
		var container = boundary[0];
		var offset = boundary[1];
		if (container === splitNode) {
			if (offset <= splitOffset || !splitOffset) {
				container = newNodeBeforeSplit;
			} else {
				container = newNodeBeforeSplit.nextSibling;
				offset -= splitOffset;
			}
		} else if (container === newNodeBeforeSplit.parentNode) {
			var nidx = Dom.nodeIndex(newNodeBeforeSplit);
			if (offset > nidx) {
				offset += 1;
			}
		}
		return [container, offset];
	}

	function adjustBoundaryAfterJoin(boundary, node, nodeLen, sibling,
	                                 siblingLen, parentNode, nidx, prev) {
		var container = boundary[0];
		var offset = boundary[1];
		if (container === node) {
			container = sibling;
			offset += prev ? siblingLen : 0;
		} else if (container === sibling) {
			offset += prev ? 0 : nodeLen;
		} else if (container === parentNode) {
			if (offset === nidx) {
				container = sibling;
				offset = prev ? siblingLen : 0;
			} else if (!prev && offset === nidx + 1) {
				container = sibling;
				offset = nodeLen;
			} else if (offset > nidx) {
				offset -= 1;
			}
		}
		return [container, offset];
	}

	function adjustBoundaryAfterRemove(boundary, node, parentNode, nidx) {
		var container = boundary[0];
		var offset = boundary[1];
		if (container === node || Dom.contains(node, container)) {
			container = parentNode;
			offset = nidx;
		} else if (container === parentNode) {
			if (offset > nidx) {
				offset -= 1;
			}
		}
		return [container, offset];
	}

	function adjustBoundaryAfterInsert(boundary, insertContainer, insertOff, len, insertBefore) {
		var container = boundary[0];
		var offset = boundary[1];
		if (insertContainer === container && (insertBefore ? offset >= insertOff : offset > insertOff)) {
			boundary = [container, offset + len];
		}
		return boundary;
	}

	function adjustBoundaryAfterTextInsert(boundary, node, off, len, insertBefore) {
		boundary = Boundaries.normalize(boundary);
		var container = boundary[0];
		var offset = boundary[1];
		// Because we must adjust boundaries adjacent to the insert
		// correctly, even if they are not inside the text node but
		// between nodes, we must move them in temporarily and normalize
		// again afterwards.
		if (!Dom.isTextNode(container)) {
			var next = offset < Dom.numChildren(container) ? Dom.nthChild(container, offset) : null;
			var prev = offset > 0 ? Dom.nthChild(container, offset - 1) : null;
			if (next === node) {
				boundary = [next, 0];
			} else if (prev === node) {
				boundary = [prev, Dom.nodeLength(prev)];
			}
		}
		return Boundaries.normalize(adjustBoundaryAfterInsert(boundary, node, off, len, insertBefore));
	}

	function adjustBoundaryAfterNodeInsert(boundary, node, insertBefore) {
		boundary = Boundaries.normalize(boundary);
		return adjustBoundaryAfterInsert(boundary, node.parentNode, Dom.nodeIndex(node), 1, insertBefore);
	}

	function adjustBoundaries(fn, boundaries) {
		var args = Array.prototype.slice.call(arguments, 2);
		return boundaries.map(function (boundary) {
			return fn.apply(null, [boundary].concat(args));
		});
	}

	/**
	 * Splits the given text node at the given offset and, if the given
	 * range happens to have start or end containers equal to the given
	 * text node, adjusts it such that start and end position will point
	 * at the same position in the new text nodes.
	 */
	function splitBoundary(boundary, ranges) {
		var splitNode = boundary[0];
		var splitOffset = boundary[1];
		if (Dom.isTextNode(splitNode) && wouldSplitTextNode(splitNode, splitOffset)) {
			var boundaries = Boundaries.fromRanges(ranges);
			boundaries.push(boundary);
			var nodeBeforeSplit = splitTextNode(splitNode, splitOffset);
			var adjusted = adjustBoundaries(
				adjustBoundaryAfterSplit,
				boundaries,
				splitNode,
				splitOffset,
				nodeBeforeSplit
			);
			boundary = adjusted.pop();
			Boundaries.setRanges(ranges, adjusted);
		}
		return boundary;
	}

	/**
	 * List of nodes that must not be split.
	 *
	 * @private
	 * @type {Object.<string, boolean>}
	 */
	var UNSPLITTABLE = {
		'BODY'    : true,
		'HTML'    : true,
		'STYLE'   : true,
		'SCRIPT'  : true,
		'AREA'    : true,
		'BASE'    : true,
		'BR'      : true,
		'COL'     : true,
		'COMMAND' : true,
		'EMBED'   : true,
		'HR'      : true,
		'IMG'     : true,
		'INPUT'   : true,
		'KEYGEN'  : true,
		'LINK'    : true,
		'META'    : true,
		'PARAM'   : true,
		'SOURCE'  : true,
		'TRACK'   : true,
		'WBR'     : true
	};

	/**
	 * Splits the given boundary's ancestors until the boundary position
	 * returns true when applyied to the given predicate.
	 *
	 * @param  {Boundary}                    boundary
	 * @param  {function(Boundary):boolean} predicate
	 * @return {Boundary}
	 */
	function splitBoundaryUntil(boundary, predicate) {
		boundary = Boundaries.normalize(boundary);
		if (predicate && predicate(boundary)) {
			return boundary;
		}
		if (Boundaries.isTextBoundary(boundary)) {
			return splitBoundaryUntil(splitBoundary(boundary), predicate);
		}
		var container = Boundaries.container(boundary);
		if (UNSPLITTABLE[container.nodeName]) {
			return boundary;
		}
		var duplicate = Dom.cloneShallow(container);
		var node = Boundaries.nodeAfter(boundary);
		if (node) {
			Dom.move(Dom.nodeAndNextSiblings(node), duplicate);
		}
		Dom.insertAfter(duplicate, container);
		return splitBoundaryUntil(Traversing.stepForward(boundary), predicate);
	}

	/**
	 * Splits text containers in the given range.
	 *
	 * @param {!Range} range
	 * @return {!Range}
	 *         The given range, potentially adjusted.
	 */
	function splitTextContainers(range) {
		splitBoundary(Boundaries.fromRangeStart(range), [range]);
		splitBoundary(Boundaries.fromRangeEnd(range), [range]);
	}

	function joinTextNodeOneWay(node, sibling, ranges, prev) {
		if (!sibling || !Dom.isTextNode(sibling)) {
			return node;
		}
		var boundaries = Boundaries.fromRanges(ranges);
		var parentNode = node.parentNode;
		var nidx = Dom.nodeIndex(node);
		var nodeLen = node.length;
		var siblingLen = sibling.length;
		sibling.insertData(prev ? siblingLen : 0, node.data);
		parentNode.removeChild(node);
		boundaries = adjustBoundaries(
			adjustBoundaryAfterJoin,
			boundaries,
			node,
			nodeLen,
			sibling,
			siblingLen,
			parentNode,
			nidx,
			prev
		);
		Boundaries.setRanges(ranges, boundaries);
		return sibling;
	}

	function joinTextNode(node, ranges) {
		if (!Dom.isTextNode(node)) {
			return;
		}
		node = joinTextNodeOneWay(node, node.previousSibling, ranges, true);
		joinTextNodeOneWay(node, node.nextSibling, ranges, false);
	}

	/**
	 * Joins the given node with its adjacent sibling.
	 *
	 * @param {!Node} A text node
	 * @param {!Range} range
	 * @return {!Range} The given range, modified if necessary.
	 */
	function joinTextNodeAdjustRange(node, range) {
		joinTextNode(node, [range]);
	}

	function adjustRangesAfterTextInsert(node, off, len, insertBefore, boundaries, ranges) {
		boundaries.push([node, off]);
		boundaries = adjustBoundaries(
			adjustBoundaryAfterTextInsert,
			boundaries,
			node,
			off,
			len,
			insertBefore
		);
		var boundary = boundaries.pop();
		Boundaries.setRanges(ranges, boundaries);
		return boundary;
	}

	function adjustRangesAfterNodeInsert(node, insertBefore, boundaries, ranges) {
		boundaries.push([node.parentNode, Dom.nodeIndex(node)]);
		boundaries = adjustBoundaries(adjustBoundaryAfterNodeInsert, boundaries, node, insertBefore);
		var boundary = boundaries.pop();
		Boundaries.setRanges(ranges, boundaries);
		return boundary;
	}

	function insertTextAtBoundary(text, boundary, insertBefore, ranges) {
		var boundaries = Boundaries.fromRanges(ranges);
		// Because empty text nodes are generally not nice and even cause
		// problems with IE8 (elem.childNodes).
		if (!text.length) {
			return boundary;
		}
		var container = boundary[0];
		var offset = boundary[1];
		if (Dom.isTextNode(container) && offset < Dom.nodeLength(container)) {
			container.insertData(offset, text);
			return adjustRangesAfterTextInsert(
				container,
				offset,
				text.length,
				insertBefore,
				boundaries,
				ranges
			);
		}
		var node = Dom.nodeAtOffset(container, offset);
		var atEnd = Boundaries.isAtEnd(Boundaries.raw(container, offset));
		// Because if the node following the insert position is already a text
		// node we can just reuse it.
		if (Dom.isTextNode(node)) {
			node.insertData(0, text);
			return adjustRangesAfterTextInsert(node, 0, text.length, insertBefore, boundaries, ranges);
		}
		// Because if the node preceding the insert position is already a text
		// node we can just reuse it.
		var prev = atEnd ? node.lastChild : node.previousSibling;
		if (prev && Dom.isTextNode(prev)) {
			var off = Dom.nodeLength(prev);
			prev.insertData(off, text);
			return adjustRangesAfterTextInsert(prev, off, text.length, insertBefore, boundaries, ranges);
		}
		// Because if we can't reuse any text nodes, we have to insert a new
		// one.
		var textNode = node.ownerDocument.createTextNode(text);
		Dom.insert(textNode, node, atEnd);
		return adjustRangesAfterNodeInsert(textNode, insertBefore, boundaries, ranges);
	}

	function insertNodeAtBoundary(node, boundary, insertBefore, ranges) {
		var boundaries = Boundaries.fromRanges(ranges);
		boundary = splitBoundary(boundary, ranges);
		var ref = Boundaries.nextNode(boundary);
		var atEnd = Boundaries.isAtEnd(boundary);
		Dom.insert(node, ref, atEnd);
		return adjustRangesAfterNodeInsert(node, insertBefore, boundaries, ranges);
	}

	/**
	 * Removes the given node while maintaing the given Ranges.
	 *
	 * @param {!Node} node
	 * @param {!Array.<!Range>} ranges
	 */
	function removePreservingRanges(node, ranges) {
		// Because the range may change due to the DOM modification
		// (automatically by the browser).
		var boundaries = Boundaries.fromRanges(ranges);
		var parentNode = node.parentNode;
		var nidx = Dom.nodeIndex(node);
		parentNode.removeChild(node);
		var adjusted = adjustBoundaries(
			adjustBoundaryAfterRemove,
			boundaries,
			node,
			parentNode,
			nidx
		);
		Boundaries.setRanges(ranges, adjusted);
	}

	/**
	 * Removes the given node while maintaing the given range.
	 *
	 * @param {!Node} node
	 * @param {!Range} range
	 */
	function removePreservingRange(node, range) {
		removePreservingRanges(node, [range]);
	}

	function boundaryToRange(boundary) {
		var container = Boundaries.container(boundary);
		var range = container.ownerDocument.createRange();
		var offset = Boundaries.offset(boundary);
		range.setStart(container, offset);
		range.setEnd(container, offset);
		return range;
	}

	function boundaryFromRange(range) {
		return Boundaries.fromRange(range)[0];
	}

	function removeNode(node, boundaries) {
		var ranges = boundaries.map(boundaryToRange);
		removePreservingRanges(node, ranges);
		return ranges.map(boundaryFromRange);
	}

	function preserveCursorForShallowRemove(node, cursor) {
		if (cursor.node === node) {
			if (cursor.node.firstChild) {
				cursor.next();
			} else {
				cursor.skipNext();
			}
		}
	}

	/**
	 * Does a shallow removal of the given node (see removeShallow()), while
	 * preserving the cursors.
	 *
	 * @param {!Node} node
	 * @param {!Array.<!Cursor>} cursors
	 */
	function removeShallowPreservingCursors(node, cursors) {
		cursors.forEach(function (cursor) {
			preserveCursorForShallowRemove(node, cursor);
		});
		Dom.removeShallow(node);
	}

	function replaceShallowPreservingBoundaries(node, replacement, boundaries) {
		var replaced = Dom.replaceShallow(node, replacement);
		return boundaries.reduce(function (list, boundary) {
			return list.concat((replaced === Boundaries.container(boundary))
			     ? [Boundaries.create(replacement, Boundaries.offset(boundary))]
			     : [boundary]);
		}, []);
	}

	return {
		removeNode                         : removeNode,
		removePreservingRange              : removePreservingRange,
		removePreservingRanges             : removePreservingRanges,
		removeShallowPreservingCursors     : removeShallowPreservingCursors,
		replaceShallowPreservingBoundaries : replaceShallowPreservingBoundaries,
		insertTextAtBoundary               : insertTextAtBoundary,
		insertNodeAtBoundary               : insertNodeAtBoundary,
		splitTextNode                      : splitTextNode,
		splitTextContainers                : splitTextContainers,
		joinTextNodeAdjustRange            : joinTextNodeAdjustRange,
		joinTextNode                       : joinTextNode,
		splitBoundary                      : splitBoundary,
		splitBoundaryUntil                 : splitBoundaryUntil
	};
});

/**
 * content.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @todo consider moving this into html/
 * @namespace content
 */
define('content',['maps', 'arrays'], function (Maps, Arrays) {
	

	var TABLE_CHILDREN = {
		'CAPTION'  : true,
		'COLGROUP' : true,
		'THEAD'    : true,
		'TBODY'    : true,
		'TFOOT'    : true,
		'TR'       : true
	};

	var TR_CHILDREN = {
		'TH' : true,
		'TD' : true
	};

	var SELECT_CHILDREN = {
		'OPTION'   : true,
		'OPTGROUP' : true
	};

	var RUBY_CHILDREN = {
		'_PHRASING_' : true,
		'RT'         : true,
		'RP'         : true
	};

	var MENU_CHILDREN = {
		'LI'     : true,
		'_FLOW_' : true
	};

	var HGROUP_CHILDREN = {
		'H1' : true,
		'H2' : true,
		'H3' : true,
		'H4' : true,
		'H5' : true,
		'H6' : true
	};

	var FIGURE_CHILDREN = {
		'FIGCAPTION' : true,
		'_FLOW_'     : true
	};

	var FIELDSET_CHILDREN = {
		'LEGEND' : true,
		'_FLOW_' : true
	};

	var DL_CHILDREN = {
		'DT' : true,
		'DD' : true
	};

	var DETAILS = {
		'SUMMARY' : true,
		'_FLOW_'  : true
	};

	var DATALIST = {
		'_PHRASING_' : true,
		'OPTION'     : true
	};

	/**
	 * The complete set of HTML(5) elements, mapped to their respective content
	 * models, which define what types of HTML nodes are permitted as their
	 * children.
	 *
	 * http://www.w3.org/html/wg/drafts/html/master/index.html#elements-1
	 * http://www.whatwg.org/specs/web-apps/current-work/#elements-1
	 */
	var ALLOWED_CHILDREN = {
		'A'                  : '_PHRASING_', // transparent
		'ABBR'               : '_PHRASING_',
		'ADDRESS'            : '_FLOW_',
		'AREA'               : '_EMPTY_',
		'ARTICLE'            : '_FLOW_',
		'ASIDE'              : '_FLOW_',
		'AUDIO'              : 'SOURCE', // transparent
		'B'                  : '_PHRASING_',
		'BASE'               : '_EMPTY_',
		'BDO'                : '_PHRASING_',
		'BLOCKQUOTE'         : '_PHRASING_',
		'BODY'               : '_FLOW_',
		'BR'                 : '_EMPTY_',
		'BUTTON'             : '_PHRASING_',
		'CANVAS'             : '_PHRASING_', // transparent
		'CAPTION'            : '_FLOW_',
		'CITE'               : '_PHRASING_',
		'CODE'               : '_PHRASING_',
		'COL'                : '_EMPTY_',
		'COLGROUP'           : 'COL',
		'COMMAND'            : '_EMPTY_',
		'DATALIST'           : DATALIST,
		'DD'                 : '_FLOW_',
		'DEL'                : '_PHRASING_',
		'DIV'                : '_FLOW_',
		'DETAILS'            : DETAILS,
		'DFN'                : '_FLOW_',
		'DL'                 : DL_CHILDREN,
		'DT'                 : '_PHRASING_', // varies
		'EM'                 : '_PHRASING_',
		'EMBED'              : '_EMPTY_',
		'FIELDSET'           : FIELDSET_CHILDREN,
		'FIGCAPTION'         : '_FLOW_',
		'FIGURE'             : FIGURE_CHILDREN,
		 // Because processing content orginating from paste events my contain
		 // font nodes, we need to accomodate this element, even though it is
		 // non-standard.
		 // http://htmlhelp.com/reference/html40/special/font.html
		'FONT'               : '_PHRASING_',
		'FOOTER'             : '_FLOW_',
		'FORM'               : '_FLOW_',
		'H1'                 : '_PHRASING_',
		'H2'                 : '_PHRASING_',
		'H3'                 : '_PHRASING_',
		'H4'                 : '_PHRASING_',
		'H5'                 : '_PHRASING_',
		'H6'                 : '_PHRASING_',
		'HEAD'               : '_META_DATA_',
		'HEADER'             : '_FLOW_',
		'HGROUP'             : HGROUP_CHILDREN,
		'HR'                 : '_EMPTY_',
		'I'                  : '_PHRASING_',
		'IFRAME'             : '#TEXT',
		'IMG'                : '_EMPTY_',
		'INPUT'              : '_EMPTY_',
		'INS'                : '_PHRASING_', // transparent
		'KBD'                : '_PHRASING_',
		'KEYGEN'             : '_EMPTY_',
		'LABEL'              : '_PHRASING_',
		'LEGEND'             : '_PHRASING_',
		'LI'                 : '_FLOW_',
		'LINK'               : '_EMPTY_',
		'MAP'                : 'AREA', // transparent
		'MARK'               : '_PHRASING_',
		'MENU'               : MENU_CHILDREN,
		'META'               : '_EMPTY_',
		'METER'              : '_PHRASING_',
		'NAV'                : '_FLOW_',
		'NOSCRIPT'           : '_PHRASING_', // varies
		'OBJECT'             : 'PARAM', // transparent
		'OL'                 : 'LI',
		'OPTGROUP'           : 'OPTION',
		'OPTION'             : '#TEXT',
		'OUTPUT'             : '_PHRASING_',
		'P'                  : '_PHRASING_',
		'PARAM'              : '_EMPTY_',
		'PRE'                : '_PHRASING_',
		'PROGRESS'           : '_PHRASING_',
		'Q'                  : '_PHRASING_',
		'RP'                 : '_PHRASING_',
		'RT'                 : '_PHRASING_',
		'RUBY'               : RUBY_CHILDREN,
		'S'                  : '_PHRASING_',
		'SAMP'               : 'pharsing',
		'SCRIPT'             : '#script', //script
		'SECTION'            : '_FLOW_',
		'SELECT'             : SELECT_CHILDREN,
		'SMALL'              : '_PHRASING_',
		'SOURCE'             : '_EMPTY_',
		'SPAN'               : '_PHRASING_',
		'STRONG'             : '_PHRASING_',
		'STYLE'              : '_PHRASING_', // varies
		'SUB'                : '_PHRASING_',
		'SUMMARY'            : '_PHRASING_',
		'SUP'                : '_PHRASING_',
		'TABLE'              : TABLE_CHILDREN,
		'TBODY'              : 'TR',
		'TD'                 : '_FLOW_',
		'TEXTAREA'           : '#TEXT',
		'TFOOT'              : 'TR',
		'TH'                 : '_PHRASING_',
		'THEAD'              : 'TR',
		'TIME'               : '_PHRASING_',
		'TITLE'              : '#TEXT',
		'TR'                 : TR_CHILDREN,
		'TRACK'              : '_EMPTY_',
		'U'                  : '_PHRASING_',
		'UL'                 : 'LI',
		'VAR'                : '_PHRASING_',
		'VIDEO'              : 'SOURCE', // transparent
		'WBR'                : '_EMPTY_',
		'#DOCUMENT-FRAGMENT' : '_FLOW_'
	};

	var FLOW_PHRASING_CATEGORY = {
		'_FLOW_'     : true,
		'_PHRASING_' : true
	};

	var FLOW_SECTIONING_CATEGORY = {
		'_FLOW_'     : true,
		'_PHRASING_' : true
	};

	var FLOW_HEADING_CATEGORY = {
		'_FLOW_'     : true,
		'_HEADER_'   : true
	};

	var FLOW_CATEGORY = {
		'_FLOW_'     : true
	};

	var CONTENT_CATEGORIES = {
		'A'          : {
			'_FLOW_'        : true,
			'_INTERACTIVE_' : true,
			'_PHRASING_'    : true
		},
		'ABBR'       : FLOW_PHRASING_CATEGORY,
		'ADDRESS'    : FLOW_CATEGORY,
		'AREA'       : FLOW_PHRASING_CATEGORY,
		'ARTICLE'    : FLOW_SECTIONING_CATEGORY,
		'ASIDE'      : FLOW_SECTIONING_CATEGORY,
		'AUDIO'      : {
			'_EMBEDDED_'    : true,
			'_FLOW_'        : true,
			'_INTERACTIVE_' : true,
			'_PHRASING_'    : true
		},
		'B'          : FLOW_PHRASING_CATEGORY,
		'BASE'       : {
			'_META_DATA_'   : true
		},
		'BDI'        : FLOW_PHRASING_CATEGORY,
		'BDO'        : FLOW_PHRASING_CATEGORY,
		'BLOCKQUOTE' : {
			'_FLOW_'            : true,
			'_SECTIONING_ROOT_' : true
		},
		'BODY'       : {
			'_SECTIONING_ROOT_' : true
		},
		'BR'         : FLOW_PHRASING_CATEGORY,
		'BUTTON'     : {
			'_EMBEDDED_'        : true,
			'_FLOW_'            : true,
			'_INTERACTIVE_'     : true,
			'_PHRASING_'        : true,
			'_LISTED_'          : true,
			'_LABELABLE_'       : true,
			'_SUBMITTABLE_'     : true,
			'_REASSOCIATABLE_'  : true,
			'_FORM_ASSOCIATED_' : true
		},
		'CANVAS'     : {
			'_EMBEDDED_'        : true,
			'_FLOW_'            : true,
			'_PHRASING_'        : true
		},
		'CAPTION'    : {},
		'CITE'       : FLOW_PHRASING_CATEGORY,
		'CODE'       : FLOW_PHRASING_CATEGORY,
		'COL'        : {},
		'COLGROUP'   : {},
		'COMMAND'    : {}, // ?
		'DATALIST'   : FLOW_PHRASING_CATEGORY,
		'DD'         : {},
		'DEL'        : FLOW_PHRASING_CATEGORY,
		'DETAILS'    : {
			'_FLOW_'            : true,
			'_SECTIONING_ROOT_' : true,
			'_INTERACTIVE_'     : true
		},
		'DFN'        : FLOW_PHRASING_CATEGORY,
		'DIV'        : FLOW_CATEGORY,
		'DL'         : FLOW_CATEGORY,
		'DT'         : {},
		'EM'         : FLOW_PHRASING_CATEGORY,
		'EMBED'      : {
			'_EMBEDDED_'        : true,
			'_FLOW_'            : true,
			'_INTERACTIVE_'     : true,
			'_PHRASING_'        : true
		},
		'FIELDSET'   : {
			'_FLOW_'            : true,
			'_FORM_ASSOCIATED_' : true,
			'_LISTED_'          : true,
			'_REASSOCIATABLE_'  : true,
			'_SECTIONING_ROOT_' : true
		},
		'FIGCAPTION' : {},
		'FIGURE'     : {
			'_FLOW_'            : true,
			'_SECTIONING_ROOT_' : true
		},
		'FONT'       : FLOW_PHRASING_CATEGORY,
		'FOOTER'     : FLOW_CATEGORY,
		'FORM'       : FLOW_CATEGORY,
		'H1'         : FLOW_HEADING_CATEGORY,
		'H2'         : FLOW_HEADING_CATEGORY,
		'H3'         : FLOW_HEADING_CATEGORY,
		'H4'         : FLOW_HEADING_CATEGORY,
		'H5'         : FLOW_HEADING_CATEGORY,
		'H6'         : FLOW_HEADING_CATEGORY,
		'HEADER'     : FLOW_CATEGORY,
		'HGROUP'     : FLOW_HEADING_CATEGORY,
		'HR'         : FLOW_CATEGORY,
		'I'          : FLOW_PHRASING_CATEGORY,
		'IFRAME'     : {
			'_EMBEDDED_'        : true,
			'_FLOW_'            : true,
			'_INTERACTIVE_'     : true,
			'_PHRASING_'        : true
		},
		'IMG'        : {
			'_EMBEDDED_'        : true,
			'_FLOW_'            : true,
			'_FORM_ASSOCIATED_' : true,
			'_INTERACTIVE_'     : true,
			'_PHRASING_'        : true
		},
		'INPUT'      : {
			'_FLOW_'            : true,
			'_FORM_ASSOCIATED_' : true,
			'_INTERACTIVE_'     : true,
			'_LABELABLE_'       : true,
			'_LISTED_'          : true,
			'_PHRASING_'        : true,
			'_REASSOCIATABLE_'  : true,
			'_RESETTABLE_'      : true,
			'_SUBMITTABLE_'     : true
		},
		'INS'        : FLOW_PHRASING_CATEGORY,
		'KBD'        : FLOW_PHRASING_CATEGORY,
		'KEYGEN'     : {
			'_FLOW_'            : true,
			'_FORM_ASSOCIATED_' : true,
			'_INTERACTIVE_'     : true,
			'_LABELABLE_'       : true,
			'_LISTED_'          : true,
			'_PHRASING_'        : true,
			'_REASSOCIATABLE_'  : true,
			'_RESETTABLE_'      : true,
			'_SUBMITTABLE_'     : true
		},
		'LABEL'      : {
			'_FLOW_'            : true,
			'_FORM_ASSOCIATED_' : true,
			'_INTERACTIVE_'     : true,
			'_PHRASING_'        : true,
			'_REASSOCIATABLE_'  : true
		},
		'LEGEND'     : {},
		'LI'         : {},
		'LINK'       : {
			'_FLOW_'            : true,
			'_METADATA_'        : true,
			'_PHRASING_'        : true
		},
		'MAIN'       : FLOW_CATEGORY,
		'MAP'        : FLOW_PHRASING_CATEGORY,
		'MARK'       : FLOW_PHRASING_CATEGORY,
		'MENU'       : FLOW_CATEGORY,
		'MENUITEM'   : FLOW_CATEGORY,
		'META'       : {
			'_FLOW_'            : true,
			'_METADATA_'        : true,
			'_PHRASING_'        : true
		},
		'METER'      : {
			'_FLOW_'            : true,
			'_LABELABLE_'       : true,
			'_PHRASING_'        : true
		},
		'NAV'        : {
			'_FLOW_'            : true,
			'_SECTIONING_'      : true
		},
		'NOSCRIPT'   : {
			'_FLOW_'            : true,
			'_METADATA_'        : true,
			'_PHRASING_'        : true
		},
		'OBJECT'     : {
			'_FLOW_'            : true,
			'_EMBEDDABLE_'      : true,
			'_FORM_ASSOCIATED_' : true,
			'_INTERACTIVE_'     : true,
			'_LISTED_'          : true,
			'_PHRASING_'        : true,
			'_REASSOCIATABLE_'  : true,
			'_SUBMITTABLE_'     : true
		},
		'OL'         : FLOW_CATEGORY,
		'OPTGROUP'   : {},
		'OPTION'     : {},
		'OUTPUT'     : {
			'_FLOW_'            : true,
			'_PHRASING_'        : true,
			'_LISTED_'          : true,
			'_LABELABLE_'       : true,
			'_RESETTALBE_'      : true,
			'_REASSOCIATABLE_'  : true,
			'_FORM_ASSOCIATED_' : true
		},
		'P'          : FLOW_CATEGORY,
		'PARAM'      : {},
		'PRE'        : FLOW_CATEGORY,
		'PROGRESS'   : {
			'_FLOW_'            : true,
			'_PHRASING_'        : true,
			'_LABELABLE_'       : true
		},
		'Q'          : FLOW_PHRASING_CATEGORY,
		'RP'         : {},
		'RT'         : {},
		'RUBY'       : FLOW_PHRASING_CATEGORY,
		'S'          : FLOW_PHRASING_CATEGORY,
		'SAMP'       : FLOW_PHRASING_CATEGORY,
		'SCRIPT'     : {
			'_FLOW_'              : true,
			'_PHRASING_'          : true,
			'_METADATA_'          : true,
			'_SCRIPT_SUPPORTING_' : true
		},
		'SECTION'    : {
			'_FLOW_'             : true,
			'_SECTIONING_'       : true
		},
		'SELECT'     : {
			'_FLOW_'            : true,
			'_PHRASING_'        : true,
			'_INTERACTIVE_'     : true,
			'_LISTED_'          : true,
			'_LABELABLE_'       : true,
			'_SUBMITTALBE_'     : true,
			'_RESETTALBE_'      : true,
			'_REASSOCIATABLE_'  : true,
			'_FORM_ASSOCIATED_' : true
		},
		'SMALL'      : FLOW_PHRASING_CATEGORY,
		'SOURCE'     : {},
		'SPAN'       : FLOW_PHRASING_CATEGORY,
		'STRONG'     : FLOW_PHRASING_CATEGORY,
		'STYLE'      : {
			'_FLOW_'            : true,
			'_METADATA_'        : true
		},
		'SUB'        : FLOW_PHRASING_CATEGORY,
		'SUMMARY'    : {},
		'SUP'        : FLOW_PHRASING_CATEGORY,
		'TABLE'      : FLOW_CATEGORY,
		'TBODY'      : {},
		'TD'         : {
			'_SECTIONING_ROOT_' : true
		},
		'TEMPLATE'   : {
			'_FLOW_'              : true,
			'_METADATA_'          : true,
			'_PHRASING_'          : true,
			'_SCRIPT_SUPPORTING_' : true
		},
		'TEXTAREA'   : {
			'_FLOW_'            : true,
			'_PHRASING_'        : true,
			'_INTERACTIVE_'     : true,
			'_LISTED_'          : true,
			'_LABELABLE_'       : true,
			'_SUBMITTALBE_'     : true,
			'_RESETTALBE_'      : true,
			'_REASSOCIATABLE_'  : true,
			'_FORM_ASSOCIATED_' : true
		},
		'TFOOT'      : {},
		'TH'         : {},
		'THEAD'      : {},
		'TIME'       : FLOW_PHRASING_CATEGORY,
		'TITLE'      : {
			'_METADATA_'        : true
		},
		'TR'         : {},
		'TRACK'      : {},
		'U'          : FLOW_PHRASING_CATEGORY,
		'UL'         : FLOW_CATEGORY,
		'VAR'        : FLOW_PHRASING_CATEGORY,
		'VIDEO'      : {
			'_FLOW_'            : true,
			'_PHRASING_'        : true,
			'_EMBEDDED_'        : true,
			'_INTERACTIVE_'     : true
		},
		'WBR'        : FLOW_PHRASING_CATEGORY,
		'#TEXT'      : FLOW_PHRASING_CATEGORY
	};

	/**
	 * @private
	 * @type {Object.<string, Array.<string>>}
	 */
	var DEFAULT_ATTRIBUTES_WHITELIST = {
		'IMG' : ['alt', 'src'],
		'A'   : ['href', 'name', '_target'],
		'TD'  : ['colspan', 'rowspan'],
		'TH'  : ['colspan', 'rowspan'],
		'OL'  : ['start', 'type'],
		'*'   : ['xstyle']
	};

	/**
	 * @private
	 * @type {Object.<string, Array.<string>>}
	 */
	var DEFAULT_STYLES_WHITELIST = {
		'TABLE' : ['width'],
		'IMG'   : ['width', 'height'],
		'*'     : [
			// '*',
			'color',
			'font-family', 'font-size', 'font-weight', 'font-stlye', 'font-decoration',
			'background', 'background-image', 'background-color'
		]
	};

	/**
	 * @private
	 * @type {Array.<string>}
	 */
	var NODES_BLACKLIST = {
		'AUDIO'    : true,
		'COMMAND'  : true,
		'COLGROUP' : true,
		'IFRAME'   : true,
		'INPUT'    : true,
		'INS'      : true,
		'KBD'      : true,
		'KEYGEN'   : true,
		'LINK'     : true,
		'META'     : true,
		'NOSCRIPT' : true,
		'OUTPUT'   : true,
		'Q'        : true,
		'RUBY'     : true,
		'SAMP'     : true,
		'SCRIPT'   : true,
		'SELECT'   : true,
		'STYLE'    : true,
		'TEMPLATE' : true,
		'TEXTAREA' : true,
		'TITLE'    : true,
		'WBR'      : true
	};

	/**
	 * @private
	 * @type {Object.<string, string>}
	 */
	var DEFAULT_TRANSLATION = {
		'FONT': 'SPAN'
	};

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf content
	 */
	function allowedStyles(overrides) {
		return Maps.merge({}, DEFAULT_STYLES_WHITELIST, overrides);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf content
	 */
	function allowedAttributes(overrides) {
		return Maps.merge({}, DEFAULT_ATTRIBUTES_WHITELIST, overrides);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf content
	 */
	function disallowedNodes(overrides) {
		return Maps.merge({}, NODES_BLACKLIST, overrides);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * @memberOf content
	 */
	function nodeTranslations(overrides) {
		return Maps.merge({}, DEFAULT_TRANSLATION, overrides);
	}

	/**
	 * Checks whether the node name `outer` is allowed to contain a node with
	 * the node name `inner` as a direct descendant based on the HTML5
	 * specification.
	 *
	 * Reference:
	 * http://www.w3.org/html/wg/drafts/html/master/index.html#elements-1
	 * http://www.whatwg.org/specs/web-apps/current-work/#elements-1
	 *
	 * @param  {string} outer
	 * @param  {string} inner
	 * @return {boolean}
	 * @memberOf content
	 */
	function allowsNesting(outer, inner) {
		var categories;
		outer = outer.toUpperCase();
		inner = inner.toUpperCase();
		var allowed = ALLOWED_CHILDREN[outer];
		if (!allowed) {
			return false;
		}
		if ('string' === typeof allowed) {
			if (allowed === inner) {
				return true;
			}
			categories = CONTENT_CATEGORIES[inner];
			if (categories && categories[allowed]) {
				return true;
			}
		} else {
			if (allowed[inner]) {
				return true;
			}
			categories = CONTENT_CATEGORIES[inner];
			var category;
			for (category in categories) {
				if (categories.hasOwnProperty(category)) {
					if (allowed[category]) {
						return true;
					}
				}
			}
		}
		return false;
	}

	return {
		allowsNesting     : allowsNesting,
		allowedStyles     : allowedStyles,
		allowedAttributes : allowedAttributes,
		disallowedNodes   : disallowedNodes,
		nodeTranslations  : nodeTranslations
	};
});

/**
 * html/mutation.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('html/mutation',[
	'html/elements',
	'html/styles',
	'html/traversing',
	'html/predicates',
	'dom',
	'mutation',
	'arrays',
	'content',
	'boundaries',
	'functions',
	'browsers'
], function (
	Elements,
	Styles,
	Traversing,
	Predicates,
	Dom,
	Mutation,
	Arrays,
	Content,
	Boundaries,
	Fn,
	Browsers
) {
	

	/**
	 * Get the first ancestor element that is editable, beginning at the given
	 * node and climbing up through the ancestors tree.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function closestEditable(node) {
		return Dom.upWhile(node, Fn.complement(Dom.isContentEditable));
	}

	/**
	 * "Props up" the given element if needed.
	 *
	 * The HTML specification stipulates that empty block-level elements should
	 * not be rendered. This becomes a problem if an editing operation results
	 * in one of these elements being emptied of all its child nodes. If this
	 * were to happen, standard conformant browsers will no longer render that
	 * empty block element even though it will remain in the document. Because
	 * the element is invisible, it will no longer be possible for the caret to
	 * be placed into it.
	 *
	 * In order to prevent littering the editable with invisible block-level
	 * elements, we prop them up by ensuring the empty block-level elements are
	 * given a <br> child node to force them to be rendered with one line
	 * height.
	 *
	 * The notable exception to this rule is the Microsoft's non-standard
	 * conformant Trident engine which automatically renders empty editable
	 * block level elements with one line height.
	 *
	 * @param {Element} elem
	 * @memberOf html
	 */
	function prop(elem) {
		if (!Predicates.isBlockNode(elem) || (Browsers.msie && closestEditable(elem))) {
			return;
		}
		if (!elem.firstChild || !Dom.nextWhile(elem.firstChild, Elements.isUnrenderedWhitespace)) {
			Dom.insert(elem.ownerDocument.createElement('br'), elem, true);
		}
	}

	/**
	 * Finds the closest line-breaking node between `above` and `below` in
	 * document order.
	 *
	 * @private
	 * @param  {Boundary} above
	 * @param  {Boundary} below
	 * @return {Boundary}
	 */
	function nextLineBreak(above, below) {
		return Boundaries.nextWhile(above, function (boundary) {
			if (Boundaries.equals(boundary, below)) {
				return false;
			}
			if (Styles.hasLinebreakingStyle(Boundaries.nextNode(boundary))) {
				return false;
			}
			if (!Boundaries.isAtEnd(boundary)) {
				return true;
			}
			return !Dom.isEditingHost(Boundaries.container(boundary));
		});
	}

	/**
	 * Determine whether the boundary `left` is visually adjacent to `right`.
	 *
	 * @private
	 * @param  {Boundary} left
	 * @param  {Boundary} right
	 * @return {boolean}
	 */
	function isVisuallyAdjacent(left, right) {
		var adjacent = false;
		Boundaries.prevWhile(right, function (pos) {
			if (Boundaries.equals(left, pos)) {
				adjacent = true;
				return false;
			}
			if (Boundaries.offset(pos) > 0) {
				// TODO:
				// var node = Boundaries.nodeBefore(pos);
				var node = Dom.nodeAtOffset(
					Boundaries.container(pos),
					Boundaries.offset(pos) - 1
				);
				if ((Dom.isTextNode(node) || Predicates.isVoidNode(node)) && Elements.isRendered(node)) {
					adjacent = false;
					return false;
				}
			}
			return true;
		});
		return adjacent;
	}

	/**
	 * Checks whether the given node has any rendered children inside of it.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function hasRenderedContent(node) {
		if (Dom.isTextNode(node)) {
			return Elements.isRendered(node);
		}
		var children = Dom.children(node).filter(function (node) {
			return Predicates.isListItem(node) || Elements.isRendered(node);
		});
		return children.length > 0;
	}

	/**
	 * Removes the visual line break between the adjacent boundaries `above`
	 * and `below` by moving the nodes after `below` over to before `above`.
	 *
	 * @param  {Boundary} above
	 * @param  {Boundary} below
	 * @return {Array.<Boundary>}
	 */
	function removeBreak(above, below) {
		if (!isVisuallyAdjacent(above, below)) {
			return [above, below];
		}
		var right        = Boundaries.nextNode(below);
		var container    = Boundaries.container(above);
		var linebreak    = nextLineBreak(above, below);
		var cannotRemove = function (node) {
			if (container === node) {
				return true;
			}
			if (Elements.isProppingBr(node)) {
				return false;
			}
			if (Predicates.isListItem(node)) {
				return hasRenderedContent(node);
			}
			return Elements.isRendered(node);
		};
		if (Boundaries.equals(linebreak, below)) {
			Dom.climbUntil(right, Dom.remove, cannotRemove);
			return [above, above];
		}
		var parent = right.parentNode;
		var siblings = Dom.nodeAndNextSiblings(right, Styles.hasLinebreakingStyle);
		if (0 === siblings.length) {
			parent = right;
		}
		siblings.reduce(function (boundary, node) {
			return Mutation.insertNodeAtBoundary(node, boundary, true);
		}, linebreak);
		if (parent) {
			Dom.climbUntil(parent, Dom.remove, cannotRemove);
		}
		return [above, above];
	}

	/**
	 * Checks whether or not the given node is a significant BR element.
	 *
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isRenderedBr(node) {
		if ('BR' !== node.nodeName) {
			return false;
		}

		var ignorable = function (node) {
			return 'BR' !== node.nodeName && Elements.isUnrendered(node);
		};

		var prev = node.previousSibling
		        && Dom.prevWhile(node.previousSibling, ignorable);

		var next = node.nextSibling
		        && Dom.nextWhile(node.nextSibling, ignorable);

		// Because a br between two visible siblings in an inline node is
		// rendered
		if (prev && next && Predicates.isInlineNode(node.parentNode)) {
			return true;
		}

		// Because a br between two br or inline nodes is rendered
		if ((prev && ('BR' === prev.nodeName || !Styles.hasLinebreakingStyle(prev)))
				&&
				(next && ('BR' === next.nodeName || !Styles.hasLinebreakingStyle(next)))) {
			return true;
		}

		// Because a br next to another br will mean that both are rendered
		if ((prev && ('BR' === prev.nodeName))
				||
				(next && ('BR' === next.nodeName))) {
			return true;
		}

		// Because a br is the first space-consuming *tag* inside of a
		// line-breaking element is rendered
		var boundary = Boundaries.fromFrontOfNode(node);
		while (Traversing.isAtStart(boundary)) {
			if (Styles.hasLinebreakingStyle(Boundaries.container(boundary))) {
				return true;
			}
			boundary = Boundaries.prev(boundary);
		}

		boundary = Boundaries.jumpOver(Boundaries.fromFrontOfNode(node));
		while (Traversing.isAtEnd(boundary)) {
			if (Styles.hasLinebreakingStyle(Boundaries.container(boundary))) {
				return false;
			}
			boundary = Boundaries.next(boundary);
		}

		return !Styles.hasLinebreakingStyle(Traversing.nextNode(boundary));
	}

	/**
	 * Inserts a <br> element behind the given boundary position.
	 *
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function insertLineBreak(boundary) {
		var doc = Boundaries.container(boundary).ownerDocument;
		var br = doc.createElement('br');
		boundary = Mutation.insertNodeAtBoundary(br, boundary, true);
		return isRenderedBr(br)
		     ? boundary
		     : Mutation.insertNodeAtBoundary(doc.createElement('br'), boundary);
	}

	/**
	 * Inserts a breaking node behind the given boundary.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function insertBreakAtBoundary(boundary, defaultBreakingElement) {
		var container = Boundaries.container(boundary);
		if (!Content.allowsNesting(container.nodeName, defaultBreakingElement)) {
			return insertLineBreak(boundary);
		}
		var breaker = container.ownerDocument.createElement(defaultBreakingElement);
		Mutation.insertNodeAtBoundary(breaker, boundary);
		return Boundaries.create(breaker, 0);
	}

	/**
	 * Checks whether that the given node is a line breaking node.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isBreakingContainer(node) {
		return !Elements.isVoidType(node)
		    && (Styles.hasLinebreakingStyle(node) || Dom.isEditingHost(node));
	}

	/**
	 * Splits the given boundary's ancestors up to the first linebreaking
	 * element which will not be split.
	 *
	 * @example
	 * <div><p><b>one<i><u>t¦wo</u></i></b></p></div>
	 * will be split to...
	 * <div><p><b>one<i><u>t</u></i></b>|<b><i><u>wo</u></i></b></p></div>
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function splitToBreakingContainer(boundary) {
		return Mutation.splitBoundaryUntil(boundary, function (boundary) {
			return isBreakingContainer(Boundaries.container(boundary));
		});
	}

	/**
	 * Recursively removes the given boundary's invisible containers.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Array.<Boundary>}
	 */
	function removeInvisibleContainers(boundary, boundaries) {
		Dom.climbUntil(
			Boundaries.container(boundary),
			function (node) {
				boundaries = Mutation.removeNode(node, boundaries);
			},
			function (node) {
				return Styles.hasLinebreakingStyle(node)
				    || Dom.isEditingHost(node)
				    || Elements.isRendered(node);
			}
		);
		return boundaries;
	}

	/**
	 * Returns any br that is adjacent to the given boundary.
	 *
	 * @private
	 * @param  {Boundary}
	 * @return {?Element}
	 */
	function adjacentBr(boundary) {
		var before = Boundaries.nodeBefore(boundary);
		var after = Boundaries.nodeAfter(boundary);
		if (before && isRenderedBr(before)) {
			return before;
		}
		if (after && isRenderedBr(after)) {
			return after;
		}
		return null;
	}

	/**
	 * Splits the list at the given list element.
	 *
	 * @param {Element} li
	 */
	function separateListItem(li) {
		var prev = Dom.prevSiblings(li).filter(Predicates.isListItem);
		var next = Dom.nextSiblings(li).filter(Predicates.isListItem);
		var list = li.parentNode;
		if (prev.length > 0) {
			var prevList = Dom.cloneShallow(list);
			Dom.moveBefore([prevList], list);
			Dom.move(prev, prevList);
		}
		if (next.length > 0) {
			var nextList = Dom.cloneShallow(list);
			Dom.moveAfter([nextList], list);
			Dom.move(next, nextList);
		}
	}

	/**
	 * Unwraps the given list item.
	 *
	 * @param  {Element} li
	 * @return {Array.<Element>}
	 */
	function unwrapListItem(li) {
		separateListItem(li);
		Dom.removeShallow(li.parentNode);
		var doc = li.ownerDocument;
		var nodes = Dom.children(li).filter(Elements.isRendered);
		var split;
		var container;
		var lines = [];
		while (nodes.length > 0) {
			if (Styles.hasLinebreakingStyle(nodes[0])) {
				lines.push(nodes.shift());
			} else {
				split = Arrays.split(nodes, Styles.hasLinebreakingStyle);
				container = doc.createElement('p');
				Dom.move(split[0], container);
				lines.push(container);
				nodes = split[1];
			}
		}
		Dom.moveAfter(lines, li);
		Dom.remove(li);
		return lines;
	}

	/**
	 * Break out of the list at the given empty li element.
	 *
	 * @private
	 * @param  {Element} li
	 * @param  {string}  defaultBreakingElement
	 * @return {Boundary}
	 */
	function breakOutOfList(li, defaultBreakingElement) {
		separateListItem(li);
		var doc = li.ownerDocument;
		//           ,-- ul   ,-- li
		//           |        |
		// ...</ul><ul><li></li></ul><ul>...
		var ul = li.parentNode;
		if (!Predicates.isListItem(ul.parentNode)) {
			var replacement = doc.createElement(defaultBreakingElement);
			Dom.removeShallow(ul);
			Dom.replaceShallow(li, replacement);
			prop(replacement);
			return Boundaries.create(replacement, 0);
		}
		// Because we are in a nested list ...
		var boundary = Mutation.splitBoundaryUntil(
			Boundaries.fromFrontOfNode(ul),
			function (boundary) {
				return Predicates.isListContainer(Boundaries.container(boundary));
			}
		);
		Dom.remove(ul);
		var next = Boundaries.nextNode(boundary);
		if (Dom.children(next).filter(Elements.isRendered).length) {
			li = doc.createElement('li');
			prop(li);
			Mutation.insertNodeAtBoundary(li, boundary, true);
			return Boundaries.create(li, 0);
		}
		return Boundaries.create(next, 0);
	}

	/**
	 * Inserts a visual line break after the given boundary position.
	 *
	 * @param  {Boundary} boundary
	 * @param  {string}   defaultBreakingElement
	 * @return {Boundary}
	 */
	function insertBreak(boundary, defaultBreakingElement) {
		var br = adjacentBr(boundary);
		if (br) {
			boundary = Mutation.insertNodeAtBoundary(
				br.ownerDocument.createElement('br'),
				boundary,
				true
			);
		}

		var container = Boundaries.container(boundary);

		// ...<li>|</li>...
		if (Predicates.isListItem(container) && !container.firstChild) {
			return breakOutOfList(container, defaultBreakingElement);
		}

		var split = splitToBreakingContainer(boundary);
		var next = Boundaries.nodeAfter(split);
		var children = next ? Dom.nodeAndNextSiblings(next) : [];
		container = Boundaries.container(split);

		// ...foo</p>|<h1>bar...
		if (next && isBreakingContainer(next)) {
			split = insertBreakAtBoundary(split, defaultBreakingElement);

		// <host>foo|bar</host>
		} else if (Dom.isEditingHost(container)) {
			split = insertBreakAtBoundary(split, defaultBreakingElement);
			var breaker = Boundaries.container(split);
			var remainder = Dom.move(children, breaker, isBreakingContainer);
			Dom.moveAfter(remainder, breaker);

		// <host><p>foo|bar</p></host>
		} else {
			split = Mutation.splitBoundaryUntil(split, function (boundary) {
				return Boundaries.container(boundary) === container.parentNode;
			});
		}

		var left = Boundaries.prevWhile(split, function (boundary) {
			var node = Boundaries.prevNode(boundary);
			return !(Boundaries.isAtStart(boundary)
			    || Elements.isVoidType(node)
			    || Dom.isTextNode(node));
		});

		var right = Boundaries.nextWhile(split, function (boundary) {
			var node = Boundaries.nextNode(boundary);
			return !(Boundaries.isAtEnd(boundary)
			    || Elements.isVoidType(node)
			    || Dom.isTextNode(node));
		});

		//             split
		//       left    |   right
		//          |    |   |
		//          v    v   v
		// <p><b>one|</b>|<b>|two</b></p>
		var boundaries = [left, right];

		boundaries = removeInvisibleContainers(boundaries[0], boundaries);
		boundaries = removeInvisibleContainers(boundaries[1], boundaries);
		container = Boundaries.container(boundaries[1]);

		if (!container.firstChild && Predicates.isHeading(container)) {
			boundaries = Mutation.replaceShallowPreservingBoundaries(
				container,
				container.ownerDocument.createElement(defaultBreakingElement),
				boundaries
			);
		}

		left = boundaries[0];
		right = boundaries[1];

		prop(Boundaries.container(left));
		prop(Boundaries.container(right));

		var node = Boundaries.nodeAfter(right);
		var visible = node && Dom.nextWhile(node, function (node) {
			return !isRenderedBr(node) && Elements.isUnrendered(node);
		});

		// <li>|<ul>...
		if (visible && isBreakingContainer(visible)) {
			return Mutation.insertNodeAtBoundary(
				visible.ownerDocument.createElement('br'),
				right
			);
		}

		return right;
	}

	return {
		prop               : prop,
		removeBreak        : removeBreak,
		insertBreak        : insertBreak,
		insertLineBreak    : insertLineBreak,
		nextLineBreak      : nextLineBreak,
		isRenderedBr       : isRenderedBr,
		isVisuallyAdjacent : isVisuallyAdjacent,
		unwrapListItem     : unwrapListItem,
		separateListItem   : separateListItem
	};
});

/**
 * html.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @see
 * https://en.wikipedia.org/wiki/HTML_element#Content_vs._presentation
 * https://developer.mozilla.org/en-US/docs/Web/HTML/Content_categories
 * http://www.whatwg.org/specs/web-apps/2007-10-26/multipage/section-contenteditable.html
 * http://lists.whatwg.org/htdig.cgi/whatwg-whatwg.org/2011-May/031577.html
 * https://dvcs.w3.org/hg/domcore/raw-file/tip/overview.html#concept-range-bp
 * http://lists.whatwg.org/htdig.cgi/whatwg-whatwg.org/2011-May/031577.html
 * @namespace html
 */
define('html',[
	'html/styles',
	'html/elements',
	'html/mutation',
	'html/traversing',
	'html/predicates',
	'browsers', // Hack for require-pronto
	'cursors' // Hack for require-pronto
], function (
	Styles,
	Elements,
	Mutation,
	Traversing,
	Predicates,
	__hack1__,
	__hack2__
) {
	

	return {
		isRendered                : Elements.isRendered,
		isUnrendered              : Elements.isUnrendered,
		isUnrenderedWhitespace    : Elements.isUnrenderedWhitespace,
		parse                     : Elements.parse,
		isVoidType                : Elements.isVoidType,

		isStyleInherited          : Styles.isStyleInherited,
		isWhiteSpacePreserveStyle : Styles.isWhiteSpacePreserveStyle,
		hasBlockStyle             : Styles.hasBlockStyle,
		hasInlineStyle            : Styles.hasInlineStyle,
		hasLinebreakingStyle      : Styles.hasLinebreakingStyle,

		prop                      : Mutation.prop,
		insertBreak               : Mutation.insertBreak,
		removeBreak               : Mutation.removeBreak,
		insertLineBreak           : Mutation.insertLineBreak,
		isRenderedBr              : Mutation.isRenderedBr,
		nextLineBreak             : Mutation.nextLineBreak,
		isVisuallyAdjacent        : Mutation.isVisuallyAdjacent,

		prev                      : Traversing.prev,
		next                      : Traversing.next,
		prevNode                  : Traversing.prevNode,
		nextNode                  : Traversing.nextNode,
		prevSignificantOffset     : Traversing.prevSignificantOffset,
		nextSignificantOffset     : Traversing.nextSignificantOffset,
		prevSignificantBoundary   : Traversing.prevSignificantBoundary,
		nextSignificantBoundary   : Traversing.nextSignificantBoundary,
		isAtStart                 : Traversing.isAtStart,
		isAtEnd                   : Traversing.isAtEnd,
		isBoundariesEqual         : Traversing.isBoundariesEqual,
		expandBackward            : Traversing.expandBackward,
		expandForward             : Traversing.expandForward,
		stepForward               : Traversing.stepForward,
		stepBackward              : Traversing.stepBackward,
		walkBetween               : Traversing.walkBetween,

		isBlockNode               : Predicates.isBlockNode,
		isGroupContainer          : Predicates.isGroupContainer,
		isGroupedElement          : Predicates.isGroupedElement,
		isHeading                 : Predicates.isHeading,
		isInlineNode              : Predicates.isInlineNode,
		isListContainer           : Predicates.isListContainer,
		isListItem                : Predicates.isListItem,
		isTableContainer          : Predicates.isTableContainer,
		isTextLevelSemanticNode   : Predicates.isTextLevelSemanticNode,
		isVoidNode                : Predicates.isVoidNode
	};
});

/**
 * stable-range.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('stable-range',[
	'dom',
	'arrays',
	'functions'
], function (
	Dom,
	Arrays,
	Fn
) {
	

	/**
	 * Creates a "stable" copy of the given range.
	 *
	 * A native range is live, which means that modifying the DOM may mutate the
	 * range. Also, using setStart/setEnd may not set the properties correctly
	 * (the browser may perform its own normalization of boundary points). The
	 * behaviour of a native range is very erratic and should be converted to a
	 * stable range as the first thing in any algorithm.
	 *
	 * The StableRange implementation exposes a minimal API that mimics
	 * the corresponding DOM API, such that most functions that accept a
	 * live range can also accept a StableRange. A full implementation
	 * of the DOM API is not the goal of this implementation.
	 *
	 * @param {Range} range
	 * @return {StableRange}
	 */
	function StableRange(range) {
		if (!(this instanceof StableRange)) {
			return new StableRange(range);
		}
		range = range || {};
		this.startContainer = range.startContainer;
		this.startOffset = range.startOffset;
		this.endContainer = range.endContainer;
		this.endOffset = range.endOffset;
		this.commonAncestorContainer = range.commonAncestorContainer;
		this.collapsed = range.collapsed;
	}

	function commonAncestorContainer(start, end) {
		// Because we can avoid going all the way up and outside the
		// editable if start and end are relatively close together in
		// the DOM (which I assume to be the common case).
		var startAncestor = start;
		var i;
		for (i = 0; i < 4; i++) {
			startAncestor = startAncestor.parentNode || startAncestor;
		}
		var startAncestors = Dom.childAndParentsUntilInclNode(
			start,
			startAncestor
		);
		var endAncestors = Dom.childAndParentsUntilInclNode(
			end,
			startAncestor
		);

		if (startAncestor !== Arrays.last(endAncestors)) {
			startAncestors = Dom.childAndParentsUntil(start, Fn.returnFalse);
			endAncestors = Dom.childAndParentsUntil(end, Fn.returnFalse);
		}

		return Arrays.intersect(
			startAncestors,
			endAncestors
		)[0];
	}

	StableRange.prototype.update = function () {
		var start = this.startContainer;
		var end = this.endContainer;
		var startOffset = this.startOffset;
		var endOffset = this.endOffset;
		if (!start || !end) {
			return;
		}
		this.collapsed = (start === end && startOffset === endOffset);
		this.commonAncestorContainer = commonAncestorContainer(start, end);
	};

	StableRange.prototype.setStart = function (sc, so) {
		this.startContainer = sc;
		this.startOffset = so;
		this.update();
	};

	StableRange.prototype.setEnd = function (ec, eo) {
		this.endContainer = ec;
		this.endOffset = eo;
		this.update();
	};

	return StableRange;
});

define('accessor',['functions', 'assert'], function (Fn, Assert) {
	

	function ensureAccessorLength(get, set) {
		var getLen = get.length;
		var setLen = set.length;
		Assert.assert(getLen >= 1, Assert.GETTER_AT_LEAST_1_ARG);
		Assert.assert(setLen >= 2, Assert.SETTER_1_MORE_THAN_GETTER);
		Assert.assert(getLen === setLen - 1, Assert.SETTER_1_MORE_THAN_GETTER);
		return getLen;
	}

	function accessorFn(get, set, getLen) {
		return function () {
			return (arguments.length - getLen > 0
			        ? set.apply(null, arguments)
			        : get.apply(null, arguments));
		};
	}

	/**
	 * Creates an accessor from a getter and a setter.
	 *
	 * An accessor is a function that is composed of the getter and the
	 * setter, and which, if called with one more argument than the
	 * number of arguments acceptable by the getter function, calls the
	 * setter function, or otherwise calls the getter function.
	 *
	 * A getter must have at least one argument, the object something is
	 * being gotten from. The setter must have at least one more
	 * argument than the getter, which is presumably the value that is
	 * to be set.
	 *
	 * The getter and setter the accessor is composed of are available
	 * as the accessor.get and accessor.set properties on the accessor
	 * function. Using the getter and setter directly should only be
	 * done for optimizing accesses when it is really needed.
	 *
	 * The Function.length property of the given function is examined
	 * and may be either 0, no matter how many arguments the function
	 * expects, or if not 0, must be the actual number of arguments the
	 * function expects.
	 *
	 * @param get {function}
	 * @param set {function}
	 * @return {!Accessor}
	 */
	function Accessor(get, set) {
		var getLen = ensureAccessorLength(get, set);
		// Optimize the common case of getLen <= 2
		var accessor = (1 === getLen) ? function (obj, value) {
			return (arguments.length > 1
			        ? set(obj, value)
			        : get(obj));
		} : (2 === getLen) ? function (obj, arg, value) {
			return (arguments.length > 2
			        ? set(obj, arg, value)
			        : get(obj, arg));
		} : accessorFn(get, set, getLen);
		accessor.get = get;
		accessor.set = set;
		return accessor;
	}

	/**
	 * Creates a method accessor from an existing accessor.
	 *
	 * A method accessor is an acccessor where the accessor function
	 * takes one less argument than the acesssor function of a normal
	 * accessor, and which uses the `this` special variable in place of
	 * the missing argument to pass to the getter and setter.
	 *
	 * As with Fn.asMethod() the original accessor function is available
	 * as the fn property on the accessor to allow easy switching
	 * between method and function form.
	 *
	 * The Function.length property of the given function is examined
	 * and may be either 0, no matter how many arguments the function
	 * expects, or if not 0, must be the actual number of arguments the
	 * function expects.
	 */
	function asMethod(accessor) {
		var get = accessor.get;
		var set = accessor.set;
		var getLen = ensureAccessorLength(get, set);
		// Optimize the common case of getLen <= 2
		var method = (1 === getLen) ? function (value) {
			return (arguments.length
			        ? set(this, value)
			        : get(this));
		} : (2 === getLen) ? function (arg, value) {
			return (arguments.length > 1
			        ? set(this, arg, value)
			        : get(this, arg));
		} : Fn.asMethod(accessorFn(get, set, getLen));
		method.get = get;
		method.set = set;
		return method;
	}

	/**
	 * Creates an accessor from a string.
	 */
	function fromString(key) {
		return Accessor(function (m) {
			return m[key];
		}, function (m, value) {
			m[key] = value;
			return m;
		});
	}

	/**
	 * Creates an accessor from a symbol.
	 */
	function fromSymbol(obj) {
		for (var symbol in obj) {
			if (obj.hasOwnProperty(symbol)) {
				return fromString(symbol);
			}
		}
		Assert.error(Assert.MISSING_SYMBOL);
	}

	Accessor.fromString = fromString;
	Accessor.fromSymbol = fromSymbol;
	Accessor.asMethod = asMethod;
	return Accessor;
});

/**
 * Design goals and features:
 * - persistent datastructure with custom setters and custom memoized getters
 * - small code size
 * - fast for a small constant number of fields
 * - compatible with google closure compiler advanced optimizations
 * - fields can have getters and setters on the prototype for convenience
 * - private fields without needing getters and setters on the prototype
 * - record.merge(values) shares values array (values must be persistent)
 * - transient support with linear-time record.asTransient()
 * - optional init function (default calls record.merge())
 */
define('record',['functions', 'maps', 'accessor', 'assert'], function (Fn, Maps, Accessor, Assert) {
	

	var NOT_TRANSIENT = 0;
	var DISCARDED_TRANSIENT = 1;
	var TRANSIENT = 2;
	var SPECIAL_PRIVATE_VALUE = {};

	/**
	 * Gets the values from a record, ensuring that it has the correct
	 * length, expanding it using the given defaults if necessary.
	 * @private
	 */
	function ensureDefaults(record, defaults) {
		var values = record._record_values;
		var valuesLen = values.length;
		if (valuesLen < defaults.length) {
			values = values.concat(defaults.slice(valuesLen));
			record._record_values = values;
		}
		return values;
	}

	/**
	 * Creates a new record instance given an existing instance.
	 * @private
	 */
	function newInstanceFromExisting(record, values) {
		var Record = record.constructor;
		return new Record(values, SPECIAL_PRIVATE_VALUE);
	}

	/**
	 * Create a clone of the given record and its value array,
	 * presumably so that the values array can be written to without
	 * mutating the existing record.
	 * @private
	 */
	function cloneForWrite(record) {
		return newInstanceFromExisting(record, record._record_values.slice(0));
	}

	/**
	 * Ensures the record is of the correct type and its transience is
	 * of the correct level.
	 * @private
	 */
	function assertRead(record, Record) {
		Assert.assert(!Record || record.constructor === Record,
		              Assert.RECORD_WRONG_TYPE);
		var transience = record._record_transience;
		Assert.assert(transience === NOT_TRANSIENT || transience === TRANSIENT,
		              Assert.READ_FROM_DISCARDED_TRANSIENT);
	}

	/**
	 * Ensures the record is of the correct type and its transience is
	 * of the correct level.
	 * @private
	 */
	function assertWrite(record, Record) {
		Assert.assert(!Record || record.constructor === Record,
		              Assert.RECORD_WRONG_TYPE);
		Assert.assert(record._record_transience === NOT_TRANSIENT,
		              Assert.PERSISTENT_WRITE_TO_TRANSIENT);
	}

	/**
	 * Ensures the record is of the correct type and its transience is
	 * of the correct level.
	 * @private
	 */
	function assertTransientWrite(record, Record) {
		Assert.assert(!Record || record.constructor === Record,
		              Assert.RECORD_WRONG_TYPE);
		Assert.assert(record._record_transience === TRANSIENT,
		              Assert.TRANSIENT_WRITE_TO_PERSISTENT);
	}

	/**
	 * Returns the given transient record as a persistent record.
	 *
	 * Discards the given transient record. See asTransient().
	 *
	 * @param record {!Record} transient record
	 * @return {!Record} persistent record
	 */
	function asPersistent(record) {
		assertTransientWrite(record);
		record._record_transience = DISCARDED_TRANSIENT;
		return newInstanceFromExisting(record, record._record_values);
	}

	/**
	 * Allocates a new transient record and discards the given transient
	 * record.
	 * @private
	 */
	function discardTransient(record) {
		var newRecord = asPersistent(record);
		newRecord._record_transience = TRANSIENT;
		return newRecord;
	}

	/**
	 * Returns the given record as a transient record.
	 *
	 * A record's set() and delay() functions are persistent, which
	 * means they will return a new record instead of mutating a given
	 * record. Obviously, returning a new record is less efficient than
	 * just mutating an existing record. Transients are an optimization
	 * that can be used when lots of updates to a record happen at once.
	 *
	 * The returned transient record can be used with the transient
	 * versions of set() and delay(). By convention, transient versions
	 * of setters are suffixed with the 'T' character. Non transient
	 * setters can't be used with transients and cause an error.
	 *
	 * Updating a transient record, or converting a transient record to
	 * a persistent record will discard the transient record, and any
	 * reads or updates to the discarded record will cause an error.
	 *
	 * One can freely convert between transient and persistent records
	 * by using persitentRecord.asTransient() and
	 * transientRecord.asPersistent().
	 *
	 * @param record {!Record} persistent record
	 * @return {!Record} transient record
	 */
	function asTransient(record) {
		assertRead(record);
		var newRecord = cloneForWrite(record);
		newRecord._record_transience = TRANSIENT;
		return newRecord;
	}

	/**
	 * Gets a value of a record.
	 * @private
	 */
	function getValue(record, offset, defaults) {
		var values = ensureDefaults(record, defaults);
		return values[offset];
	}

	/**
	 * Sets a value on a record.
	 * @private
	 */
	function setValue(record, newValue, offset, defaults) {
		var newRecord = cloneForWrite(record);
		var values = ensureDefaults(newRecord, defaults);
		values[offset] = newValue;
		return newRecord;
	}

	/**
	 * Like setValue() but accepts a transient record.
	 * @private
	 */
	function setValueT(record, newValue, offset, defaults) {
		var newRecord = discardTransient(record);
		var values = ensureDefaults(newRecord, defaults);
		values[offset] = newValue;
		return newRecord;
	}

	/**
	 * Checks whether a value is a value delayed by calling delay().
	 * @private
	 */
	function isDelayValue(value) {
		return Array.isArray(value) && value[0] === SPECIAL_PRIVATE_VALUE;
	}

	/**
	 * Realizes a value that was delayed by calling delay().
	 * @private
	 */
	function realizeDelayValue(delayValue) {
		var value;
		if (delayValue.length > 3) {
			value = delayValue[3];
		} else {
			value = delayValue[1](delayValue[2]);
			delayValue[3] = value;
		}
		return value;
	}

	/**
	 * Adds a field to the given type.
	 *
	 * The only way to access the field is through the returned accessor
	 * (see accessor.js).
	 *
	 * Fields may be added at any time, even after instances have been
	 * constructed, and using the returned accessor to get the field's
	 * value from already existing instances works as expected. Adding
	 * fields to types one didn't define and has no control over is a
	 * feature and allows one to avoid wrapping records.
	 *
	 * The optional defaultValue argument can be used to set the value
	 * the field has before being set the first time.
	 *
	 * @param Record {function}
	 * @param defaultValue {*}
	 * @return {!Field} the new field
	 */
	function addField(Record, defaultValue) {
		var defaults = Record._record_defaults;
		var offset = Record._record_defaults.length;
		function get(record) {
			assertRead(record, Record);
			var value = getValue(record, offset, defaults);
			if (isDelayValue(value)) {
				value = realizeDelayValue(value);
				var values = ensureDefaults(record, defaults);
				values[offset] = value;
			}
			return value;
		}
		function set(record, newValue) {
			assertWrite(record, Record);
			return setValue(record, newValue, offset, defaults);
		}
		function setT(record, newValue) {
			assertTransientWrite(record, Record);
			return setValueT(record, newValue, offset, defaults);
		}
		defaults.push(defaultValue);
		set.setT = setT;
		return Accessor(get, set);
	}

	/**
	 * Sets the value of the given field to the given value.
	 *
	 * The given record will not be modified, instead a new record will
	 * be returned that reflects the updated field.
	 *
	 * @param record {!Record}
	 * @param field {!Field} a field of the given record
	 * @param newValue {*} the value to set the field to
	 * @return {!Record} a new record with the given field updated in it
	 */
	function set(record, field, newValue) {
		return field.set(record, newValue);
	}

	/**
	 * Gets the value of the given field from the given record.
	 *
	 * @param record {!Record}
	 * @param field {!Field} a field of the given record
	 * @return {*} the value of the given field on the given record
	 */
	function get(record, field) {
		return field.get(record);
	}

	/**
	 * Like set() but accepts a transient record.
	 */
	function setT(record, field, newValue) {
		return field.set.setT(record, newValue);
	}

	/**
	 * Sets a field on the given record to the value returned by the
	 * given function.
	 *
	 * The call to the given function will be delayed until the field is
	 * accessed, and will be cached such that the function will only be
	 * called once. The optional `arg` argument may be used as an
	 * argument to the given function.
	 *
	 * The given record will not be modified, instead a new record will
	 * be returned that reflects the updated field.
	 *
	 * @param record {!Record}
	 * @param field {!Field} a field of the given record
	 * @param fn {function} a function that returns the value to set
	 * @param arg {*} an optional argument to the given function
	 * @return a new record with the given field updated in it
	 */
	function delay(record, field, fn, arg) {
		return set(record, field, [SPECIAL_PRIVATE_VALUE, fn, arg]);
	}

	/**
	 * Like delay() but accepts a transient record.
	 */
	function delayT(record, field, fn, arg) {
		return setT(record, field, [SPECIAL_PRIVATE_VALUE, fn, arg]);
	}

	/**
	 * Adds fields to the given type.
	 *
	 * The given fieldMap is a map of field name to defaultValue. The
	 * field will be added to the type using Type.addField(), and it
	 * will be made available on the type's prototype as a method.
	 *
	 * @param Record {function}
	 * @param fieldMap {Object.<string,*>}
	 * @return void
	 */
	function extend(Record, fieldMap) {
		Maps.forEach(fieldMap, function (defaultValue, name) {
			var field = addField(Record, defaultValue);
			Record.prototype[name] = Accessor.asMethod(field);
		});
	}

	/**
	 * Defines a record.
	 *
	 * The optional fieldMap can be used to add some initial
	 * fields. Providing it in the call to define() is the same as
	 * calling Type.extend() after the call to define().
	 *
	 * The optional init function will be called every time a new
	 * instance of the defined record is created. The init function
	 * accepts two arguments, the first of which is the new record
	 * instance, which it must return (possibly updated), and the second
	 * of which is an optional argument passed to the constructor
	 * function.
	 *
	 * The returned type is a constructor function. The prototype of the
	 * constructor function can be freely modified. The constructor
	 * function must not be invoked with the new keyword, it must
	 * instead be used as a function.
	 *
	 * Example:
	 *
	 * <pre>
	 * var MyType = define({
	 *     myField: someDefaultValue
	 * }, function (record, optionalArgument) {
	 *     record = record.myField(optionalArgument);
	 *     return record;
	 * });
	 *
	 * var myInstance = MyType(optionalArgument);
	 * </pre>
	 *
	 * @param fieldMap {?Object.<string,*>}
	 * @param init {?function}
	 * @return a new record type
	 * @memberOf Record
	 */
	function define(fieldMap, init) {
		if (Fn.is(fieldMap)) {
			init = fieldMap;
			fieldMap = null;
		}
		init = init || Fn.identity;
		var defaults = [];
		function Record(arg, specialPrivateValue) {
			if (specialPrivateValue !== SPECIAL_PRIVATE_VALUE) {
				return init(new Record(defaults, SPECIAL_PRIVATE_VALUE), arg);
			}
			this._record_values = arg;
			this._record_transience = NOT_TRANSIENT;
		}
		Fn.extendType(Record, {
			asTransient : asTransient,
			asPersistent: asPersistent,
			get: get,
			set: set,
			setT: setT,
			delay: delay,
			delayT: delayT
		});
		Record.addField = Fn.partial(addField, Record);
		Record.extend   = Fn.partial(extend, Record);
		Record._record_defaults = defaults;
		if (fieldMap) {
			extend(Record, fieldMap);
		}
		return Record;
	}

	/**
	 * Returns a new field that can be used in place of the given field
	 * and which calls the given hooks whenever the field is updated.
	 *
	 * The given setters will be called after the field is updated, must
	 * return the record given to them, and may update it.
	 *
	 * Useful to recompute other fields whenever a field is set.
	 *
	 * @param field {!Field} a field of a Record
	 * @param afterSet {function} invoked after the field is set
	 * @param afterSetT {?function} like afterSet but accepts a transient record
	 * @return field {!Field} a new field
	 * @memberOf Record
	 */
	function hookSetter(field, afterSet, afterSetT) {
		var set = field.set;
		var setT = set.setT;
		afterSetT = afterSetT || function (record) {
			return afterSet(record.asPersistent()).asTransient();
		};
		var newAccessor = Accessor(field.get, function (record, value) {
			record = set(record, value);
			record = afterSet(record);
			return record;
		});
		newAccessor.set.setT = function (record, value) {
			record = setT(record, value);
			record = afterSetT(record);
			return record;
		};
		return newAccessor;
	}

	/**
	 * Similar to hookSetter() except the given recopute and recomputeT
	 * functions recompute a value of the given computedField instead of
	 * updating the record directly.
	 *
	 * @param observedField {!Field} a field of a Record
	 * @param computedField {!Field} a field that is recomputed
	 * @param recompute {function} given a record computes the value for computedField
	 * @param recomputeT {?function} like recompute but gets a transient record
	 * @return field {!Field} a new field
	 * @memberOf Record
	 */
	function hookSetterRecompute(observedField, computedField, recompute, recomputeT) {
		return hookSetter(observedField, function (record) {
			return computedField.set(record, recompute(record));
		}, recomputeT ? function (recordT) {
			return computedField.set.setT(recordT, recomputeT(recordT));
		} : null);
	}

	return {
		'define'              : define,
		'hookSetter'          : hookSetter,
		'hookSetterRecompute' : hookSetterRecompute
	};
});

define('delayed-map',['functions', 'maps', 'assert'], function (Fn, Maps, Assert) {
	

	var REALIZED_NONE = 0;
	var REALIZED_KEYS = 1;
	var REALIZED_KEYS_VALUES = 2;
	var NOT_REALIZED_PLACEHOLDER = {};
	var NOT_PRESENT_PLACEHOLDER = {};
	var SPECIAL_PRIVATE_VALUE = {};

	/**
	 * Constructs a new DelayedMap.
	 *
	 * `opts` is a map or an object implementing how the delayed map is
	 * realized. It has the following methods:
	 *
	 *     {
	 *         realize: function (source) { ... return realizedMap; },
	 *         get: function (source, name, default_) {
	 *             ...
	 *             return hasName ? valueForName : default_;
	 *         },
	 *         has: function (source, name) { ... return hasName; },
	 *         keys: function (source) { ... return keysArray; }
	 *     }
	 *
	 * Only realize and get are required.
	 *
	 * `source` is an optional argument that is passed as the first
	 * argument to the given functions. If it is not supplied, undefined
	 * is passed instead.
	 *
	 * This function must not be invoked with the new keyword, it must
	 * instead be used as a function.
	 *
	 * @param opts {!Object.<string,function>}
	 * @param source {*}
	 * @return {!DelayedMap}
	 */
	function DelayedMap(opts, source, _specialPrivateValue) {
		if (_specialPrivateValue !== SPECIAL_PRIVATE_VALUE) {
			return new DelayedMap(opts, source, SPECIAL_PRIVATE_VALUE);
		}
		this._map_data = null;
		this._map_source = source;
		this._map_opts = opts;
		this._map_realized = REALIZED_NONE;
	}

	/**
	 * Internal implementation to get cached data from the given map,
	 * allocating a new map if necessary.
	 */
	function ensureData(map) {
		var data = map._map_data = map._map_data || {};
		return data;
	}

	/**
	 * Realizes the given map.
	 *
	 * Will call the realize() implementation the given delayed map was
	 * constructed with, and cache its return value.
	 *
	 * @param map {!DelayedMap}
	 * @return {!Object.<string, *>}
	 */
	function realize(map) {
		var realized = map._map_realized;
		if (realized & REALIZED_KEYS_VALUES) {
			return map._map_data;
		}
		var opts = map._map_opts;
		var data = opts.realize(map._map_source);
		map._map_data = data;
		map._map_realized = realized | REALIZED_KEYS | REALIZED_KEYS_VALUES;
		// Allow memory reclamation
		map._map_source = null;
		return data;
	}

	/**
	 * Returns true if the given delayed map is realized.
	 *
	 * @param map {!DelayedMap}
	 * @return {boolean}
	 */
	function isRealized(map) {
		return !!(map._map_realized & REALIZED_KEYS_VALUES);
	}

	/**
	 * Gets the keys from the a delayed map.
	 *
	 * Will realize the map if it was constructed without an
	 * implementation for keys().
	 *
	 * @param map {!DelayedMap}
	 * @return {Array.<string>}
	 */
	function keys(map) {
		var data = ensureData(map);
		var realized = map._map_realized;
		if (realized & REALIZED_KEYS) {
			return Maps.keys(data).filter(function (key) {
				return data[key] !== NOT_PRESENT_PLACEHOLDER;
			});
		}
		var opts = map._map_opts;
		var optsKeys;
		if (opts.keys) {
			optsKeys = opts.keys(map._map_source);
			optsKeys.forEach(function (key) {
				if (!data.hasOwnProperty(key)) {
					data[key] = NOT_REALIZED_PLACEHOLDER;
				}
			});
			map._map_realized = realized | REALIZED_KEYS;
		} else {
			optsKeys = Maps.keys(realize(map));
		}
		return optsKeys;
	}

	/**
	 * Internal implementation that uses the given getFn to get the real
	 * value or a presence value from the given map, first checking
	 * whether the real or presence value is available without invoking
	 * getFn.
	 */
	function getCached(map, name, getFn) {
		var data = ensureData(map);
		if (data.hasOwnProperty(name)) {
			return data[name];
		} else if (map._map_realized & REALIZED_KEYS) {
			return NOT_PRESENT_PLACEHOLDER;
		}
		var value = data[name] = getFn(map, name);
		return value;
	}

	/**
	 * Internal implementation that gets the presence value
	 * (NOT_REALIZED_PLACEHOLDER or NOT_PRESENT_PLACEHOLDER) from the
	 * given map.
	 */
	function getPresence(map, name) {
		var opts = map._map_opts;
		var has = false;
		if (opts.has) {
			has = opts.has(map._map_source, name);
		} else {
			// TODO use opts.keys first and only try getCached if there
			// is no opts.keys to avoid realizing values unless
			// absolutely necessary.
			has = NOT_PRESENT_PLACEHOLDER !== getCached(map, name, getValue);
		}
		return has ? NOT_REALIZED_PLACEHOLDER : NOT_PRESENT_PLACEHOLDER;
	}

	/**
	 * Internal implementation that gets a value from the given delayed
	 * map.
	 */
	function getValue(map, name) {
		var opts = map._map_opts;
		return opts.get(map._map_source, name, NOT_PRESENT_PLACEHOLDER);
	}

	/**
	 * Returns true if the given delayed map has a key with the given
	 * name.
	 *
	 * If the key for the given name has not yet been fetched from the
	 * delayed map's source, it will be fetched, possibly realizing the
	 * value as well if the delayed map was constructed without an
	 * implementation for has(). 
	 *
	 * @param map {!DelayedMap}
	 * @param name {string}
	 * @return {boolean}
	 */
	function has(map, name) {
		return NOT_PRESENT_PLACEHOLDER !== getCached(map, name, getPresence);
	}

	/**
	 * Gets a value form a delayed map.
	 *
	 * If there is no key that matches the given name, will return
	 * the optional default_ value instead.
	 *
	 * If the value for the given name has not yet been realized from
	 * the delayed map's source, it will be realized and cached for
	 * subsequent gets.
	 *
	 * @param map {!DelayedMap}
	 * @param name {string}
	 * @param default_ {*}
	 * @return {*}
	 */
	function get(map, name, default_) {
		var value = getCached(map, name, getValue);
		if (NOT_PRESENT_PLACEHOLDER === value) {
			value = default_;
		} else if (NOT_REALIZED_PLACEHOLDER === value) {
			var data = ensureData(map);
			value = data[name] = getValue(map, name);
			Assert.asserT(value !== NOT_PRESENT_PLACEHOLDER);
		}
		return value;
	}

	/**
	 * Internal implementation of DelayedMap options for a literal map.
	 */
	var realizedFromMapOpts = {
		realize: Fn.identity,
		get: Assert.error // not called if the map is realized
	};

	/**
	 * Returns a new DelayedMap backed by a javascript object.
	 *
	 * Since the source of the map is a non-delayed javascript object,
	 * the delayed map itself will be realized from the beginning.
	 *
	 * @param literalMap {!Object} any javascript object
	 * @return {!DelayedMap}
	 */
	function realized(literalMap) {
		var map = DelayedMap(realizedFromMapOpts, literalMap);
		map.realize();
		return map;
	}

	/**
	 * The options passed to the DelayedMap constructor ask for a getter
	 * function that accepts a default value. Most of the time, a getter
	 * will simply return null to indicate there was no value, and don't
	 * accept a given default value. This function can be used to turn
	 * the given getFn into a getter function that accepts a default
	 * that will be returned instad of the given getFn's return value
	 * whenever that value tests true with the given useDefaultFn
	 * predicate.
	 *
	 * For example
	 *
	 *    DelayedMap({
	 *        get: DelayedMap.makeGetWithDefault(myGet, Fn.isNou),
	 *        realize: ...
	 *    }, ...);
	 *
	 * @param getFn {function} accepts two arguments and returns a value
	 * @param useDefaultFn {function} used to test values returned by getFn
	 * @return {function} a composition of the given functions
	 */
	function makeGetWithDefault(getFn, useDefaultFn) {
		return function (source, name, default_) {
			var value = getFn(source, name);
			return useDefaultFn(value) ? default_ : value;
		};
	}

	/**
	 * Internal implementation of DelayedMap options for merging two
	 * DelayedMaps.
	 */
	var mergeOpts = {
		realize: function (mergeSource) {
			return Maps.merge(mergeSource.map.realize(),
			                  mergeSource.obj);
		},
		get: function (mergeSource, name, default_) {
			var map = mergeSource.map;
			var obj = mergeSource.obj;
			if (obj.hasOwnProperty(name)) {
				return obj[name];
			}
			return map.get(name, default_);
		}
	};

	/**
	 * Merges an object into a delayed map.
	 *
	 * Performs the same merging logic as in Maps.merge(), but does so
	 * lazily by keeping references to the passed map and object.
	 *
	 * If longLived is true, and if map is the result of a previous
	 * merge and is not yet realized, will do a non-lazy merge of the
	 * last merge's obj and this merge's obj. This helps prevent a
	 * memory leak from occuring when merges happen a lot over time.
	 *
	 * @param map {!DelayedMap}
	 * @param obj {!Object}
	 * @param longLived {?boolean}
	 * @return {!DelayedMap}
	 */
	function mergeObject(map, obj, longLived) {
		if (longLived
		    && map._map_opts === mergeOpts
		    && !(map._map_realized & REALIZED_KEYS_VALUES)) {
			obj = Maps.merge(map._map_source.obj, obj);
			map = map._map_source.map;
		}
		return DelayedMap(mergeOpts, {map: map, obj: obj});
	}

	Fn.extendType(DelayedMap, {
		keys: keys,
		realize: realize,
		isRealized: isRealized,
		get: get,
		has: has,
		mergeObject: mergeObject
	});

	DelayedMap.realized = realized;
	DelayedMap.makeGetWithDefault = makeGetWithDefault;

	return DelayedMap;
});

/**
 * "One does not simply mutate the DOM" - Boromir
 *
 * Boromir - represent a DOM with immutable javascript data structures
 * (arrays and objects), transform the representation with functional
 * algorithms (no update in place), and efficiently update the DOM from
 * the transformed representation.
 *
 * Uses cases:
 *
 * present a normalized view (no empty text nodes, subsequent text nodes
 *   joined) (wouldn't call normalize() unnecessarily with a DOM
 *   representation),
 * 
 * shadow-dom - ignore shadow-dom/ephemera/non-visible elements for the
 *   purpose of the algorithm - whether or not ephemera/non-visible
 *   elements can be ignored completely depends on the algorithm - for
 *   example when inserting a node, does it matter whether it comes
 *   after or before a non-visible or ephemera node? - what is
 *   considered shadow DOM and what isn't is controllable,
 * 
 * inject ranges in the the content (wouldn't do it with the DOM,
 *   especially if the algorithm is read-only (read algorithms shouldn't
 *   mutate, otherwise composability is affected))
 * 
 * inject things like arrays into the content to allow a more convenient
 *   structure to be seen (wouldn't do it with the DOM, see above)
 * 
 * any mutate algorithms is also a read-only algorithm - apply a
 *   mutating algorithm and interpret the result without mutating the
 *   DOM directly (can clone the DOM and mutate the clone, but the
 *   mapping back to the DOM would be lost, also efficiency)
 * 
 * as-if algorithms - insert content into the tree without mutating the
 *   DOM, and apply algorithms as if the content was there, and
 *   interpret the results
 *
 * schema validation decoupled from algorithms (invalid DOM structure
 *   never inserted into the DOM, and multiple anti-violation strategies
 *   possible like don't mutate anything on violation (rollback), or
 *   automatically clean invalid nesting and try to preserve valid
 *   content).
 * 
 * immutable datastructure - functional programming - no suprising
 *   mutation effects in an algorithm (impossible with the DOM).
 * 
 * no surprises like walking up the ancestors - does your algorithm
 *   require to be attached to an editable, or not? dom nodes implicity
 *   carry around the context with them, which is fragile in practice
 *   (if I pass the dom node, what else do I really pass?).
 * 
 * read once, write once (otherwise, either within an algorithm, or even
 *   if an algorithm itself does read-once write-once, when composing
 *   multiple algoirthms, you'd have read multiple times, write multiple
 *   times)
 * 
 * optimal DOM updating (no split text node and re-insert, then split
 *   again reinsert and join and reinsert etc.)
 */
define('boromir',[
	'functions',
	'maps',
	'accessor',
	'record',
	'delayed-map',
	'dom',
	'strings',
	'assert'
], function (
	Fn,
	Maps,
	Accessor,
	Record,
	DelayedMap,
	Dom,
	Strings,
	Assert
) {
	

	var idCounter = 0;
	var CHANGE_NONE = 0;
	var CHANGE_INSERT = 1;
	var CHANGE_REMOVE = 2;
	var CHANGE_REF = 4;
	var AFFINITY_DOM = 1;
	var AFFINITY_MODEL = 2;
	var AFFINITY_DEFAULT = AFFINITY_DOM | AFFINITY_MODEL;
	var CHANGED_INIT = 1;
	var CHANGED_NAME = 2;
	var CHANGED_TEXT = 4;
	var CHANGED_ATTRS = 8;
	var CHANGED_STYLES = 16;
	var CHANGED_CHILDREN = 32;
	var CHANGED_AFFINITY = 64;
	var ELEMENT = 1;
	var TEXT = 3;

	function allocateId() {
		return ++idCounter;
	}

	function typeFromDomNode(domNode) {
		return domNode.nodeType;
	}

	function childrenFromDomNode(domNode) {
		var childNodes = domNode.childNodes;
		var nodes = [];
		for (var i = 0, len = childNodes.length; i < len; i++) {
			nodes.push(Boromir(childNodes[i]));
		}
		return nodes;
	}

	function nameFromDomNode(domNode) {
		return domNode.nodeName;
	}

	function textFromDomNode(domNode) {
		return domNode.data;
	}

	var delayedAttrsFromDom = {
		realize: Dom.attrs,
		get: DelayedMap.makeGetWithDefault(Dom.getAttr, Fn.isNou)
	};

	var delayedStylesFromDom = {
		realize: Assert.notImplemented,
		get: DelayedMap.makeGetWithDefault(Dom.getStyle, Fn.isNou)
	};

	function setPropsFromDomNodeT(nodeT, domNode) {
		var delayedAttrs  = DelayedMap(delayedAttrsFromDom, domNode);
		var delayedStyles = DelayedMap(delayedStylesFromDom, domNode);
		nodeT = nodeT.setT(nodeT.domNode, domNode);
		nodeT = nodeT.delayT(nodeT.type, typeFromDomNode, domNode);
		nodeT = nodeT.delayT(nodeT.name, nameFromDomNode, domNode);
		nodeT = nodeT.delayT(nodeT.text, textFromDomNode, domNode);
		nodeT = nodeT.delayT(nodeT.children, childrenFromDomNode, domNode);
		nodeT = nodeT.setT(delayedAttrsField, delayedAttrs);
		nodeT = nodeT.setT(delayedStylesField, delayedStyles);
		var node = nodeT.asPersistent();
		nodeT = node.asTransient().delayT(classesField, classesFromNodeAttrs, node);
		return nodeT;
	}

	function setTextPropsT(nodeT, props) {
		Assert.assertNou(props.name);
		Assert.assertNou(props.nodeType);
		var affinity = props.affinity || AFFINITY_DEFAULT;
		nodeT = nodeT.setT(nodeT.domNode, props.domNode);
		nodeT = nodeT.setT(nodeT.type, TEXT);
		nodeT = nodeT.setT(nodeT.text, props.text);
		nodeT = nodeT.setT(nodeT.affinity, affinity);
		return nodeT;
	}

	function setElementPropsT(nodeT, props) {
		Assert.assertNou(props.text);
		Assert.assertNou(props.nodeType);
		var name = props.name;
		var attrs = props.attrs || {};
		var styles = props.styles || {};
		var children = props.children || [];
		var affinity = props.affinity || AFFINITY_DEFAULT;
		Assert.assert(Fn.isNou(attrs['style']), Assert.STYLE_NOT_AS_ATTR);
		nodeT = nodeT.setT(nodeT.domNode, props.domNode);
		nodeT = nodeT.setT(nodeT.type, ELEMENT);
		nodeT = nodeT.setT(nodeT.name, name);
		nodeT = nodeT.setT(nodeT.children, children);
		nodeT = nodeT.setT(nodeT.affinity, affinity);
		nodeT = nodeT.setT(delayedAttrsField, DelayedMap.realized(attrs));
		nodeT = nodeT.setT(delayedStylesField, DelayedMap.realized(styles));
		return nodeT;
	}

	function initWithDomNodeOrPropsT(nodeT, domNodeOrProps) {
		if (!domNodeOrProps) {
			return nodeT;
		}
		if (domNodeOrProps.nodeType) {
			nodeT = setPropsFromDomNodeT(nodeT, domNodeOrProps);
		} else if (!Fn.isNou(domNodeOrProps.text)) {
			nodeT = setTextPropsT(nodeT, domNodeOrProps);
		} else if (!Fn.isNou(domNodeOrProps.name)) {
			nodeT = setElementPropsT(nodeT, domNodeOrProps);
		} else {
			Assert.error(Assert.INVALID_ARGUMENT);
		}
		return nodeT;
	}

	var Boromir = Record.define({
		domNode      : null,
		type         : null,
		name         : null,
		text         : null,
		classes      : {},
		children     : null,
		affinity     : AFFINITY_DEFAULT
	}, function (node, domNodeOrProps) {
		var nodeT = node.asTransient();
		nodeT = initWithDomNodeOrPropsT(nodeT, domNodeOrProps);
		nodeT = nodeT.setT(idField, allocateId());
		// We start listening for changes after all changable fields
		// have been initialized.
		nodeT = nodeT.setT(changedField, CHANGED_INIT);
		node = nodeT.asPersistent();
		node = unchangedField.set(node, node);
		return node;
	});
	var classesField       = Boromir.prototype.classes;
	var unchangedField     = Boromir.addField();
	var idField            = Boromir.addField();
	var delayedAttrsField  = Boromir.addField();
	var delayedStylesField = Boromir.addField();
	var changedAttrsField  = Boromir.addField();
	var changedStylesField = Boromir.addField();
	var changedField       = Boromir.addField();

	function updateMask(node, changedMask, set) {
		var changed = changedField.get(node);
		if ((changed & CHANGED_INIT)
		    && changedMask !== (changed & changedMask)) {
			changed |= changedMask;
			node = set(node, changed);
		}
		return node;
	}

	function hookUpdateChanged(field, changedMask) {
		return Record.hookSetter(field, function (node) {
			return updateMask(node, changedMask, changedField.set);
		} , function (node) {
			return updateMask(node, changedMask, changedField.set.setT);
		});
	}

	function assertElement(node) {
		Assert.assert(ELEMENT === node.type.get(node), Assert.EXPECT_ELEMENT);
	}

	function getChangedOrDelayed(changedMapField, delayedField, node, name) {
		var changedMap = changedMapField.get(node);
		if (changedMap && changedMap.hasOwnProperty(name)) {
			return changedMap[name];
		}
		var delayedMap = delayedField.get(node);
		return delayedMap.get(name);
	}

	function setChanged(changedMapField, changedMask, node, name, value) {
		var changedMap = changedMapField.get(node);
		changedMap = Maps.cloneSet(changedMap || {}, name, value);
		node = node.asTransient();
		node = node.setT(changedMapField, changedMap);
		node = updateMask(node, changedMask, changedField.set.setT);
		return node.asPersistent();
	}

	/**
	 * Gets the value of the attribute with the given name.
	 *
	 * The node has to be of the Element type, and the name mustn't be
	 * "style" (use node.style(name) instead).
	 *
	 * The reason the style attribute isn't accessible is that
	 * individual styles can be updated on the Boromir node without
	 * affecting the DOM node, and the serialization/deserialization of
	 * individual styles from/into an attribute value isn't currently
	 * implemented as part of Boromir.
	 *
	 * Attribtes are read from the element lazily and cached. This also
	 * means that, should attributes be added to the DOM node that
	 * haven't been read through the Boromir node before the update,
	 * they will become available after the DOM update, but attributes
	 * removed from the DOM will still be readable through Boromir,
	 * which may result in an unexpected view of the DOM.
	 */
	function getAttr(node, name) {
		assertElement(node);
		Assert.assert('style' !== name, Assert.STYLE_NOT_AS_ATTR);
		return getChangedOrDelayed(changedAttrsField, delayedAttrsField, node, name);
	}

	function parseClasses(classStr) {
		return Maps.fillKeys({}, Strings.words(classStr), true);
	}

	function setAttr(node, name, value) {
		assertElement(node);
		Assert.assert('style' !== name, Assert.STYLE_NOT_AS_ATTR);
		node = setChanged(changedAttrsField, CHANGED_ATTRS, node, name, value);
		if ('class' === name) {
			node = updateClassesFromAttr(node);
		}
		return node;
	}

	/**
	 * Gets the value of a style with the given name.
	 *
	 * Same caveates regarding DOM update as with getAttr().
	 */
	function getStyle(node, name) {
		assertElement(node);
		return getChangedOrDelayed(changedStylesField, delayedStylesField, node, name);
	}

	function setStyle(node, name, value) {
		assertElement(node);
		return setChanged(changedStylesField, CHANGED_STYLES, node, name, value);
	}

	function getAttrs(node) {
		assertElement(node);
		var delayedMap = delayedAttrsField.get(node);
		var changedMap = changedAttrsField.get(node);
		var attrs = delayedMap.realize();
		attrs = Maps.extend({}, attrs, changedMap);
		attrs = Maps.filter(attrs, Fn.complement(Fn.isNou));
		delete attrs['style']; // safe because Maps.extends copies the map
		return attrs;
	}

	function setAttrs(node, attrs) {
		assertElement(node);
		Assert.assert(Fn.isNou(attrs['style']), Assert.STYLE_NOT_AS_ATTR);
		var delayedMap = delayedAttrsField.get(node);
		var removedMap = Maps.fillKeys({}, delayedMap.keys(), null);
		var changedMap = Maps.extend(removedMap, attrs);
		delete changedMap['style']; // safe because fillKeys copies the map
		node = node.asTransient();
		node = updateMask(node, CHANGED_ATTRS, changedField.set.setT);
		node = node.setT(changedAttrsField, changedMap);
		return node.asPersistent();
	}

	function updateInUnchanged(node, field, value, nodeSet) {
		var unchangedNode = unchangedField.get(node);
		unchangedNode = field.set(unchangedNode, value);
		return nodeSet(node, unchangedNode);
	}

	function getInUnchanged(node, field) {
		var unchangedNode = unchangedField.get(node);
		return field.get(unchangedNode);
	}

	function updateName(node) {
		Assert.notImplemented();
	}

	function updateText(node) {
		var text = node.text.get(node);
		var domNode = node.domNode.get(node);
		domNode.data = text;
		return updateInUnchanged(node, node.text, text, unchangedField.set);
	}

	function updateDomNodeFromMap(domNode, map, updateDom) {
		Maps.forEach(map, function (value, name) {
			updateDom(domNode, name, value);
		});
	}

	function updateChangedAndDelayed(changedField, delayedField, updateDom, node) {
		var changedMap = changedField.get(node);
		if (!changedMap) {
			return node;
		}
		var domNode = node.domNode.get(node);
		updateDomNodeFromMap(domNode, changedMap, updateDom);
		var newAttrs = delayedField.get(node).mergeObject(changedMap, true);
		node = node.asTransient();
		node = node.setT(changedField, null);
		node = node.setT(delayedField, newAttrs);
		return node.asPersistent();
	}

	var updateAttrs = Fn.partial(updateChangedAndDelayed, changedAttrsField,
	                             delayedAttrsField, Dom.setAttr);
	var updateStyles = Fn.partial(updateChangedAndDelayed, changedStylesField,
	                              delayedStylesField, Dom.setStyle);

	function createElementNode(node, doc) {
		var name = node.name.get(node);
		var attrs = node.attrs.get(node);
		var domNode = doc.createElement(name);
		updateDomNodeFromMap(domNode, attrs, Dom.setAttr);
		var delayedAttrMap = delayedAttrsField.get(node);
		Dom.setAttr(domNode, 'style', delayedAttrMap.get('style'));
		var changedStylesMap = changedStylesField.get(node);
		updateDomNodeFromMap(domNode, changedStylesMap, Dom.setStyle);
		return domNode;
	}

	function createTextNode(node, doc) {
		return doc.createTextNode(node.text.get(node));
	}

	function createDomNode(node, doc) {
		var type = node.type.get(node);
		if (ELEMENT === type) {
			return createElementNode(node, doc);
		} else if (TEXT === type) {
			return createTextNode(node, doc);
		} else {
			Assert.notImplemented();
		}
	}

	// TODO use insertIndex to move elements if a node is
	// being removed in the old tree during a recursive
	// update.
	// TODO support normalized update that will join
	// inserted text nodes and re-use existing text nodes if
	// the content is the same instead of replacing them, so
	// that you can split up Boromir text nodes any way you
	// want and it will not result in changes to the DOM.
	function insertChild(domNode, childNodes, child, i, doc, insertIndex) {
		var refNode = i < childNodes.length ? childNodes[i] : null;
		var childDomNode = createDomNode(child, doc);
		domNode.insertBefore(childDomNode, refNode);
		child = child.asTransient();
		child = child.setT(child.domNode, childDomNode);
		child = child.setT(idField, allocateId());
		child = updateInUnchanged(child, child.children, [], unchangedField.set.setT);
		child = child.setT(changedField, changedField.get(child) | CHANGED_CHILDREN);
		child = child.asPersistent();
		child = updateDomRec(child, doc, insertIndex);
		return child;
	}

	function removeChild(domNode, childNodes, i) {
		domNode.removeChild(childNodes[i]);
	}

	/**
	 * Creates a map that maps the ids of the given children to the
	 * given value.
	 * @private
	 */
	function indexChildren(children, index, value) {
		children.forEach(function (child) {
			index[idField.get(child)] = value;
		});
		return index;
	}

	/**
	 * Determines, given an old and a new children array, which ones
	 * were inserted or removed, based on the id of the node.
	 *
	 * Fast for common cases, but may have a suboptimal result (too many
	 * removes/inserts) when siblings are moved around rather than just
	 * inserted and removed.
	 *
	 * @param oldChildren {!Array.<!Boromir>}
	 * @param newChildren {!Array.<!Boromir>}
	 * @return {!Array.<int>}
	 */
	function childrenChangedInParent(oldChildren, newChildren) {
		if (newChildren === oldChildren) {
			return null;
		}
		var oldChild;
		var newChild;
		var newIndex = null;
		var i = 0;
		var j = 0;
		var oldLen = oldChildren.length;
		var newLen = newChildren.length;
		var changedInParent = [];
		var changed = false;
		while (i < oldLen && j < newLen) {
			oldChild = oldChildren[i];
			newChild = newChildren[j];
			var oldId = idField.get(oldChild);
			var newId = idField.get(newChild);
			var change;
			if (oldId === newId) {
				if (oldChild === newChild) {
					change = CHANGE_NONE;
				} else {
					changed = true;
					change = CHANGE_REF;
				}
				i += 1;
				j += 1;
			} else {
				newIndex = newIndex || indexChildren(newChildren, {}, true);
				if (!newIndex[oldId]) {
					changed = true;
					change = CHANGE_REMOVE;
					i += 1;
				} else {
					changed = true;
					change = CHANGE_INSERT;
					j += 1;
				}
			}
			changedInParent.push(change);
		}
		for (; i < oldLen; i++) {
			oldChild = oldChildren[i];
			changed = true;
			changedInParent.push(CHANGE_REMOVE);
		}
		for (; j < newLen; j++) {
			newChild = newChildren[j];
			changed = true;
			changedInParent.push(CHANGE_INSERT);
		}
		if (!changed) {
			return null;
		}
		return changedInParent;
	}

	/**
	 * Updates the children of the DOM node wrapped by the given boromir
	 * node.
	 * @private
	 */
	function updateChildren(node, doc, insertIndex) {
		var newChildren = node.children.get(node);
		var oldChildren = getInUnchanged(node, node.children);
		var changedInParent = childrenChangedInParent(oldChildren, newChildren);
		if (!changedInParent) {
			return node;
		}
		var domNode = node.domNode.get(node);
		var childNodes = domNode.childNodes;
		var children = [];
		var oldI = 0, newI = 0, domI = 0;
		changedInParent.forEach(function (change) {
			var child;
			if (change & CHANGE_INSERT) {
				child = newChildren[newI];
				newI += 1;
			} else if (change & CHANGE_REMOVE) {
				child = oldChildren[oldI];
				oldI += 1;
			} else {
				child = newChildren[newI];
				newI += 1;
				oldI += 1;
			}
			var affinity = child.affinity.get(child);
			var unchangedAffinity = getInUnchanged(node, node.affinity);
			if ((affinity & AFFINITY_DOM) !== (unchangedAffinity & AFFINITY_DOM)) {
				if ((affinity & AFFINITY_DOM) && !(change & CHANGE_REMOVE)) {
					child = insertChild(domNode, childNodes, child, domI, doc, insertIndex);
					domI += 1;
				} else if (!(change & CHANGE_INSERT)) {
					removeChild(domNode, childNodes, domI);
				}
			} else if (affinity & AFFINITY_DOM) {
				if (change & CHANGE_INSERT) {
					child = insertChild(domNode, childNodes, child, domI, doc, insertIndex);
					domI += 1;
				} else if (change & CHANGE_REMOVE) {
					removeChild(domNode, childNodes, domI);
				} else {
					if (change & CHANGE_REF) {
						child = updateDomRec(child, doc, insertIndex);
					}
					domI += 1;
				}
			}
			if (!(change & CHANGE_REMOVE)) {
				children.push(child);
			}
		});
		node = node.set(node.children, children);
		node = updateInUnchanged(node, node.children, children, unchangedField.set);
		return node;
	}

	/**
	 * Recursively updates the DOM wrapped by the given boromir tree.
	 * @private
	 */
	function updateDomRec(node, doc, insertIndex) {
		var type = node.type.get(node);
		var changed = changedField.get(node);
		if (ELEMENT === type) {
			if (changed & CHANGED_NAME) {
				node = updateName(node);
			}
			if (changed & CHANGED_ATTRS) {
				node = updateAttrs(node);
			}
			if (changed & CHANGED_STYLES) {
				node = updateStyles(node);
			}
			if (changed & CHANGED_CHILDREN) {
				node = updateChildren(node, doc, insertIndex);
			}
		} else if (TEXT === type) {
			if (changed & CHANGED_TEXT) {
				node = updateText(node);
			}
		} else {
			// Nothing to do for other nodes
		}
		return node;
	}

	/**
	 * Given a boromir node that wraps a DOM tree, updates the wrapped
	 * DOM tree to reflect the given boromir tree, and returns a new
	 * boromir tree that reflects the update.
	 *
	 * Subtrees that don't have modifications, will not be visited.
	 *
	 * The DOM must not have been modified since it was wrapped with
	 * boromir nodes, otherwise the behviour is undefined and may cause
	 * errors to be thrown. The assumption that the tree wasn't modified
	 * is necessary so that the DOM can be updated in the most efficient
	 * manner possible, without peforming any redundant read operations
	 * on the DOM.
	 *
	 * For the reason above, after the DOM has been updated, the boromir
	 * tree given to updateDom() can't be used any more to update the
	 * DOM. Instead, a new boromir tree is returned that reflects the
	 * updated DOM and which can be used in a subsequent updateDom()
	 * operation.
	 *
	 * @param node {!Boromir}
	 * @param opts {Map.<string,*>} no options currently
	 * @return {!Boromir}
	 */
	function updateDom(node, opts) {
		var domNode = node.domNode.get(node);
		var doc = domNode.ownerDocument;
		Assert.assert(doc, Assert.ELEMENT_NOT_ATTACHED);
		return updateDomRec(node, doc, {});
	}

	/**
	 * Creates a new dom tree that represents the given boromir tree.
	 *
	 * Doesn't change any existing DOM nodes.
	 *
	 * @param node {!Boromir}
	 * @param doc {!Document} the document to creat the new DOM tree with
	 * @return {!Node} a dom node representing the given boromir tree
	 */
	function asDom(node, doc) {
		var domNode;
		if (ELEMENT === node.type()) {
			domNode = createElementNode(node, doc);
			node.children().forEach(function (child) {
				domNode.appendChild(asDom(child, doc));
			});
		} else if (TEXT === node.type()) {
			domNode = createTextNode(node, doc);
		} else {
			Assert.notImplemented();
		}
		return domNode;
	}

	function classesFromNodeAttrs(node) {
		var cls = getAttr(node, 'class');
		return Fn.isNou(cls) ? {} : parseClasses(cls);
	}

	function updateClassesFromAttr(node) {
		return node.set(classesField, classesFromNodeAttrs(node));
	}

	function updateAttrFromClasses(node) {
		var classStr = Maps.keys(node.get(node.classes)).join(' ');
		return setChanged(changedAttrsField, CHANGED_ATTRS, node, 'class', classStr);
	}

	function hasClass(node, cls) {
		return node.get(node.classes)[cls];
	}

	function addClass(node, cls) {
		var classMap = node.get(node.classes);
		if (classMap[cls]) {
			return node;
		}
		classMap = Maps.cloneSet(classMap, cls, true);
		node = node.set(node.classes, classMap);
		return node;
	}

	function removeClass(node, cls) {
		var classMap = node.get(node.classes);
		if (!classMap[cls]) {
			return node;
		}
		classMap = Maps.cloneDelete(classMap, cls);
		node = node.set(node.classes, classMap);
		return node;
	}

	var hookedName     = hookUpdateChanged(Boromir.prototype.name, CHANGED_NAME);
	var hookedText     = hookUpdateChanged(Boromir.prototype.text, CHANGED_TEXT);
	var hookedChildren = hookUpdateChanged(Boromir.prototype.children, CHANGED_CHILDREN);
	var hookedAffinity = hookUpdateChanged(Boromir.prototype.affinity, CHANGED_AFFINITY);

	Maps.extend(Boromir.prototype, {
		name         : Accessor.asMethod(hookedName),
		text         : Accessor.asMethod(hookedText),
		children     : Accessor.asMethod(hookedChildren),
		affinity     : Accessor.asMethod(hookedAffinity),
		attrs        : Accessor.asMethod(Accessor(getAttrs, setAttrs)),
		attr         : Accessor.asMethod(Accessor(getAttr, setAttr)),
		style        : Accessor.asMethod(Accessor(getStyle, setStyle)),
		updateDom    : Fn.asMethod(updateDom),
		asDom        : Fn.asMethod(asDom),
		create       : Boromir,
		hasClass     : Fn.asMethod(hasClass),
		addClass     : Fn.asMethod(addClass),
		removeClass  : Fn.asMethod(removeClass)
	});

	Boromir.prototype.attrs = Accessor.asMethod(
		Record.hookSetterRecompute(Boromir.prototype.attrs,
		                           classesField,
		                           classesFromNodeAttrs,
		                           classesFromNodeAttrs)
	);
	Boromir.prototype.classes = Accessor.asMethod(
		Record.hookSetter(classesField,
		                  updateAttrFromClasses,
		                  updateAttrFromClasses)
	);

	Boromir.CHANGE_INSERT    = CHANGE_INSERT;
	Boromir.CHANGE_REMOVE    = CHANGE_REMOVE;
	Boromir.CHANGE_REF       = CHANGE_REF;
	Boromir.CHANGE_NONE      = CHANGE_NONE;
	Boromir.AFFINITY_DOM     = AFFINITY_DOM;
	Boromir.AFFINITY_MODEL   = AFFINITY_MODEL;
	Boromir.AFFINITY_DEFAULT = AFFINITY_DEFAULT;
	Boromir.childrenChangedInParent = childrenChangedInParent;
	Boromir.ELEMENT          = ELEMENT;
	Boromir.TEXT             = TEXT;

	return Boromir;
});

/**
 * zippers.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @reference:
 * http://hackage.haskell.org/package/rosezipper-0.2/docs/src/Data-Tree-Zipper.html
 *
 * @usage:
 *	var boundaries = Boundaries.get(document);
 *	var zip = zipper(Dom.editingHost(Boundaries.container(boundaries[0])), {
 *		start : boundaries[0],
 *		end   : boundaries[1]
 *	});
 *	var loc = zip.loc;
 *	var markers = zip.markers;
 *	loc = splitAt(loc, markers.start);
 *	loc = insert(loc, contents(createRecord('#text'), ['↵']));
 *	loc = splitAt(loc, markers.end);
 *	var preserved = update(root(loc));
 *	console.log(aloha.markers.hint([preserved.start, preserved.end]));
 *
 * @TODO Add documentation, then add namespace zippers here.
 */
define('zippers',[
	'dom',
	'maps',
	'html',
	'paths',
	'arrays',
	'record',
	'boromir',
	'boundaries',
	'functions'
], function (
	Dom,
	Maps,
	Html,
	Paths,
	Arrays,
	Record,
	Boromir,
	Boundaries,
	Fn
) {
	

	function isTextRecord(record) {
		return '#text' === record.name();
	}

	/**
	 * Gets the contents of the given record. If a second argument is passed,
	 * will set the contents of this record.
	 *
	 * @private
	 * @param  {!Record}                 record
	 * @param  {Array.<!Record|string>=} content
	 * @return {Record}
	 */
	function contents(record, content) {
		record = arguments[0];
		if (1 === arguments.length) {
			return isTextRecord(record) ? record.text() : record.children();
		}
		return isTextRecord(record)
		     ? record.text(content.join(''))
		     : record.children(content);
	}

	/**
	 * Represents a position between nodes inside a tree.
	 *
	 * @private
	 * @type   Location
	 * @param  {Array.<!Record>}   lefts
	 * @param  {Array.<!Record>}   rights
	 * @param  {Array.<!Location>} frames
	 * @return {Location}
	 */
	function Location(lefts, rights, frames) {
		return {
			lefts  : lefts,
			rights : rights,
			frames : frames
		};
	}

	/**
	 * Return the parent frame of the given location.
	 *
	 * @param  {!Location} loc;
	 * @return {Location}
	 * @memberOf zippers
	 */
	function peek(loc) {
		return Arrays.last(loc.frames);
	}

	/**
	 * The node before the given tree position.
	 *
	 * @param  {!Location} loc
	 * @return {Record}
	 * @memberOf zippers
	 */
	function before(loc) {
		return Arrays.last(loc.lefts);
	}

	/**
	 * The node after the given tree position.
	 *
	 * @param  {!Location} loc
	 * @return {Record}
	 * @memberOf zippers
	 */
	function after(loc) {
		return loc.rights[0];
	}

	/**
	 * Returns previous location from the given position.
	 * If a second argument is specified, it will specify the number steps to
	 * shift the location.
	 *
	 * @param  {!Location} loc
	 * @param  {number=}   stride
	 * @return {Location}
	 * @memberOf zippers
	 */
	function prev(loc, stride) {
		stride = 'number' === typeof stride ? stride : 1;
		return 0 === stride ? loc : Location(
			loc.lefts.slice(0, -stride),
			loc.lefts.slice(-stride).concat(loc.rights),
			loc.frames.concat()
		);
	}

	/**
	 * Returns next location from the given position.
	 * If a second argument is specified, it will specify the number steps to
	 * shift the location.
	 *
	 * @param  {!Location} loc
	 * @param  {number=}   stride
	 * @return {Location}
	 * @memberOf zippers
	 */
	function next(loc, stride) {
		stride = 'number' === typeof stride ? stride : 1;
		return 0 === stride ? loc : Location(
			loc.lefts.concat(loc.rights.slice(0, stride)),
			loc.rights.slice(stride),
			loc.frames.concat()
		);
	}

	/**
	 * Descends into the record at the given tree location.
	 *
	 * @param  {!Location} loc
	 * @return {Location}
	 * @memberOf zippers
	 */
	function down(loc) {
		return Location([], contents(after(loc)), loc.frames.concat(loc));
	}

	/**
	 * Ascends to the front of the parent of the current location.
	 *
	 * @param  {!Location} loc
	 * @return {Location}
	 * @memberOf zippers
	 */
	function up(loc) {
		var content = loc.lefts.concat(loc.rights);
		var frame = Arrays.last(loc.frames);
		var first = contents(after(frame), content);
		return Location(
			frame.lefts.concat(),
			[first].concat(frame.rights.slice(1)),
			loc.frames.slice(0, -1)
		);
	}

	/**
	 * Ascends to the root of the tree.
	 * The location returned will be the create() position.
	 *
	 * @param  {!Location} loc
	 * @return {Location}
	 * @memberOf zippers
	 */
	function root(loc) {
		return loc.frames.reduce(up, loc);
	}

	/**
	 * Creates a tree location pointing to the given DOM node as its root.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {Location}
	 * @memberOf zippers
	 */
	function create(root) {
		return Location([], [Boromir(root)], []);
	}

	/**
	 * Checks whether the given locations points to a text record.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @return {Location}
	 */
	function isTextLocation(loc) {
		return 'string' === typeof after(loc);
	}

	/**
	 * Counts the cumulative length of the given lists of text fragment records.
	 *
	 * @private
	 * @param  {Array.<!Record>} fragments
	 * @return {number}
	 */
	function fragmentsLength(fragments) {
		return fragments.reduce(function (sum, record) {
			return sum + record.text().length;
		}, 0);
	}

	/**
	 * Post order traversal throught the tree, mapping each not with a given
	 * mutate() function.
	 *
	 * @private
	 * @param  {!Location}                                         loc
	 * @param  {!function(Record, Array.<number>):Array.<!Record>} mutate
	 * @return {Location}
	 */
	function mapInPostOrder(loc, mutate) {
		loc = root(loc);
		var replacements;
		var trail = [];
		var offset;
		while (true) {
			if (isAtEnd(loc)) {
				loc = up(loc);
				if (isRoot(loc)) {
					break;
				}
				trail = trail.slice(0, -1);
				replacements = mutate(after(loc), trail);
				loc = next(replace(loc, replacements), replacements.length);
			} else if (isVoid(loc)) {
				offset = isFragmentedText(after(up(loc)))
				       ? fragmentsLength(loc.lefts.filter(isTextRecord))
				       : loc.lefts.length;
				replacements = mutate(after(loc), trail.concat(offset));
				loc = next(replace(loc, replacements), replacements.length);
			} else {
				if (!isRoot(loc)) {
					trail.push(loc.lefts.length);
				}
				loc = down(loc);
			}
		}
		return loc;
	}

	/**
	 * Walks the tree in depth-first pre order traversal until pred() returns
	 * false.
	 *
	 * @private
	 * @param  {!Location}                   loc
	 * @param  {function(Location):boolean=} pred
	 * @return {Location}
	 * @memberOf zippers
	 */
	function walkInPreOrderWhile(loc, pred) {
		pred = pred || Fn.returnTrue;
		loc = root(loc);
		while (pred(loc)) {
			if (isAtEnd(loc)) {
				do {
					if (isRoot(loc)) {
						return loc;
					}
					loc = up(loc);
				} while (isAtEnd(loc));
				loc = next(loc);
			} else if (isVoid(loc)) {
				loc = next(loc);
			} else {
				loc = down(loc);
			}
		}
		return loc;
	}

	/**
	 * Reconstitues a fragmented text record.
	 *
	 * @private
	 * @param  {!Record} record
	 * @return {Record}
	 */
	function defragmentText(record) {
		var text = record.children().filter(isTextRecord).reduce(function (strings, string) {
			return strings.concat(string.text());
		}, []).join('');
		return (record.original && record.original.text() === text)
		     ? record.original
		     : createRecord('#text', [text]);
	}

	function dom(loc) {
		return after(loc).domNode();
	}

	/**
	 * Updates the DOM tree below this location and returns a map of named boundaries
	 * that are found therein.
	 *
	 * @param  {!Location} loc
	 * @return {Map.<string, Boundary>}
	 * @memberOf zippers
	 */
	function update(loc) {
		var paths = [];
		loc = mapInPostOrder(loc, function (record, trail) {
			if (isMarker(record)) {
				if (record.marker) {
					paths.push({
						name : record.marker,
						path : trail
					});
				}
				return [];
			}
			return [isFragmentedText(record) ? defragmentText(record) : record];
		});
		var editable = after(loc).updateDom().domNode();
		return paths.reduce(function (markers, mark) {
			markers[mark.name] = Paths.toBoundary(editable, mark.path);
			return markers;
		}, {});
	}

	/**
	 * Prints the content.
	 *
	 * @private
	 * @param  {!Record|string} content
	 * @return {string}
	 */
	function print(content) {
		return 'string' === typeof content
		     ? '“' + content + '”'
		     : isTextRecord(content)
		     ? content.text()
		     : content.domNode().outerHTML;
	}

	/**
	 * Return a partial representation of the given location in the tree.
	 *
	 * @param  {!Location} loc
	 * @return {string}
	 * @memberOf zippers
	 */
	function hint(loc) {
		return loc.lefts.map(print).concat('▓', loc.rights.map(print)).join('');
	}

	/**
	 * Normalizes the given offset inside the given record by ignoring any
	 * markers.
	 *
	 * @private
	 * @param  {!Record} record
	 * @param  {number}  offset
	 * @return {number}
	 */
	function normalizeOffset(record, offset) {
		return offset + record.children().slice(0, offset).filter(isMarker).length;
	}

	/**
	 * Maps the given offsets from a intergral text record to that of a
	 * fragmented representation of it.
	 *
	 * @private
	 * @param  {!Record} record
	 * @param  {number}  offset
	 * @return {number}
	 */
	function fragmentedOffset(record, offset) {
		if (0 === offset) {
			return [0];
		}
		var fragments = record.children();
		var len = fragments.length;
		var remainder = offset;
		var index = 0;
		var overflow;
		var fragment;
		while (index < len) {
			fragment = fragments[index];
			if (isMarker(fragment)) {
				index++;
				continue;
			}
			overflow = remainder - fragment.text().length;
			if (0 === overflow) {
				return [index + 1];
			}
			if (overflow < 0) {
				return [index, remainder];
			}
			index++;
			remainder = overflow;
		}
		throw 'Text offset out of bounds';
	}

	/**
	 * Splits text into two halves at the givne offset.
	 *
	 * @private
	 * @param  {string} text
	 * @param  {number} offset
	 * @return {Array.<string>}
	 */
	function splitText(text, offset) {
		return [text.substr(0, offset), text.substr(offset)];
	}

	/**
	 * Descends into an offset inside a text location.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @param  {number}    offset
	 * @return {Location}
	 */
	function locationInText(loc, offset) {
		var text = splitText(contents(after(loc)), offset);
		return Location([text[0]], [text[1]], loc.frames.concat(loc));
	}

	/**
	 * Descends into an offset inside a element location.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @param  {number}    offset
	 * @return {Location}
	 */
	function locationInElement(loc, offset) {
		return next(down(loc), normalizeOffset(after(loc), offset));
	}

	/**
	 * Descends into an offset inside a fragmented text location.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @param  {number}    offset
	 * @return {Location}
	 */
	function locationInFragment(loc, offset) {
		var record = after(loc);
		var offsets = fragmentedOffset(record, offset);
		var atText = next(down(loc), offsets[0]);
		if (1 === offsets.length) {
			return atText;
		}
		var text = splitText(contents(after(atText)), offsets[1]);
		return next(splice(atText, 1, [
			contents(createRecord('#text'), [text[0]]),
			contents(createRecord('#text'), [text[1]])
		]));
	}

	/**
	 * Traverses the given path.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @param  {!Path}     path
	 * @return {Location}
	 */
	function traverse(loc, path) {
		var offset;
		var trail = path.concat().reverse();
		while (trail.length) {
			while (isMarker(after(loc))) {
				loc = next(loc);
			}
			offset = trail.pop();
			if (isTextRecord(after(loc))) {
				return locationInText(loc, offset);
			}
			loc = isFragmentedText(after(loc))
			    ? locationInFragment(loc, offset)
			    : locationInElement(loc, offset);
		}
		return loc;
	}

	/**
	 * Clips the sub section of the given path that is common with `root`.
	 *
	 * @private
	 * @param  {!Path} root
	 * @param  {!Path} path
	 * @return {Path}
	 */
	function clipCommonRoot(root, path) {
		for (var i = 0; i < root.length; i++) {
			if (path[i] !== root[i]) {
				return [];
			}
		}
		return path.slice(i);
	}

	/**
	 * Splices the rights of the given location.
	 *
	 * @param  {!Location}                loc
	 * @param  {number}                   num
	 * @param  {!Record|Array.<!Record>=} items
	 * @return {Location}
	 */
	function splice(loc, num, items) {
		items = items
		      ? (items.constructor === Boromir || items.constructor === Record)
		      ? [items]
		      : items
		      : [];
		return Location(
			loc.lefts.concat(),
			items.concat(loc.rights.slice(num)),
			loc.frames.concat()
		);
	}

	/**
	 * Inserts the given items at this location.
	 *
	 * @param  {!Location}               loc
	 * @param  {!Record|Array.<!Record>} items
	 * @return {Location}
	 */
	function insert(loc, items) {
		return splice(loc, 0, items);
	}

	/**
	 * Replaces the record at this location with the given.
	 *
	 * @param  {!Location} loc
	 * @param  {!Record}   item
	 * @return {Location}
	 */
	function replace(loc, item) {
		return splice(loc, 1, item);
	}

	/**
	 * Removes the record at this location.
	 *
	 * @param  {!Location} loc
	 * @return {Location}
	 */
	function remove(loc) {
		return splice(loc, 1);
	}

	/**
	 * FragmentedText implementation is backed by a 'Q' element in order to be
	 * able to visualize it in the document for debugging.
	 *
	 * FIXME: isFragmentedText and original won't be preserved on cloning.
	 *
	 * @private
	 * @type FragmentedText
	 * @param  {!Location} loc
	 * @return {Location}
	 */
	function FragmentedText(loc) {
		var atText = up(loc);
		var wrapper = Boromir(document.createElement('q'));
		wrapper.isFragmentedText = true;
		wrapper.original = after(atText);
		return Location(
			[contents(createRecord('#text'), loc.lefts)],
			[contents(createRecord('#text'), loc.rights)],
			down(replace(atText, wrapper)).frames.concat()
		);
	}

	function isFragmentedText(record) {
		return true === record.isFragmentedText;
	}

	var markerCount = 0;

	/**
	 * Creates a markers.
	 *
	 * FIXME: isFragmentedText and original and isMarker won't be preserved on cloning.
	 *
	 * @param  {string} name
	 * @return {Record}
	 * @memberOf zippers
	 */
	function createMarker(name) {
		var node = document.createElement('code');
		node.innerHTML = ++markerCount;
		var record = Boromir(node);
		record.isMarker = true;
		record.marker = name;
		return record;
	}

	/**
	 * @memberOf zippers
	 */
	function isMarker(record) {
		return true === record.isMarker;
	}

	/**
	 * Markup the tree with the given marker.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @param  {!Marker}   marked
	 * @return {Object}
	 */
	function markup(loc, marked) {
		var markers = {};
		Maps.forEach(marked, function (boundary, name) {
			var result = markTree(loc, boundary, name);
			markers[name] = result.marker;
			loc = result.loc;
		});
		return {
			loc     : loc,
			markers : markers
		};
	}

	/**
	 * Creates a zipper with the given set of named boundaries laced into the
	 * tree.
	 *
	 * @type   zipper
	 * @param  {!Element}                    element
	 * @param  {!Object.<string, !Boundary>} boundaries
	 * @return {Location}
	 */
	function zipper(element, boundaries) {
		return markup(create(element), boundaries);
	}

	/**
	 * Inserts a marker at the given boundary
	 *
	 * @private
	 * @param  {!Location}  loc
	 * @param  {!Boundary}  boundary
	 * @param  {markerName} string
	 * @return {Object}
	 */
	function markTree(loc, boundary, markerName) {
		loc = root(loc);
		var element = dom(loc);
		var body = element.ownerDocument.body;
		var origin = Paths.fromBoundary(body, Boundaries.fromFrontOfNode(element));
		var path = Paths.fromBoundary(body, boundary);
		var clipped = clipCommonRoot(origin, path);
		if (0 === clipped.length) {
			return {
				loc    : loc,
				marker : null
			};
		}
		var marker = createMarker(markerName);
		loc = traverse(loc, clipped);
		loc = insert(isTextLocation(loc) ? FragmentedText(loc) : loc, marker);
		return {
			loc    : loc,
			marker : marker
		};
	}

	/**
	 * Creates a record of the given type and fills it with the given content.
	 *
	 * @private
	 * @param  {string}                 type
	 * @param  {Array.<!Record|string>} content
	 * @return {Record}
	 */
	function createRecord(type, content) {
		var node = '#text' === type
		         ? document.createTextNode('')
		         : document.createElement(type);
		return 'undefined' === typeof content
		     ? Boromir(node)
		     : contents(Boromir(node), content);
	}

	/**
	 * Clones a record.
	 *
	 * @param  {!Record} record
	 * @return {Record}
	 */
	function clone(record) {
		return Boromir(Dom.cloneShallow(record.domNode()));
	}

	/**
	 * Checks whether this location is the root of the tree.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @return {boolean}
	 */
	function isRoot(loc) {
		return 0 === loc.frames.length;
	}

	/**
	 * Checks whether this location is which cannot be descended.
	 *
	 * @private
	 * @param  {!Location} loc
	 * @return {boolean}
	 */
	function isVoid(loc) {
		var record = after(loc);
		return '#text' === record.name()
		    || isMarker(record)
		    || Html.isVoidNode(record.domNode());
	}

	/**
	 * Checks whether this location is at the start of its parent node.
	 *
	 * @param  {!Location} loc
	 * @return {boolean}
	 */
	function isAtStart(loc) {
		return 0 === loc.lefts.length;
	}

	/**
	 * Checks whether this location is at the end of its parent node.
	 *
	 * @param  {!Location} loc
	 * @return {boolean}
	 */
	function isAtEnd(loc) {
		return 0 === loc.rights.length;
	}

	/**
	 * Splits the tree down until until() returns true or we reach the editing
	 * host.
	 *
	 * @param  {!Location}                   location
	 * @param  {!function(Location):boolean} until
	 * @return {boolean}
	 * @memberOf zippers
	 */
	function split(loc, until) {
		until = until || Fn.returnFalse;
		if (isRoot(peek(loc)) || until(loc)) {
			return loc;
		}
		var left, right;
		var upper = up(loc);
		if (isTextLocation(loc)) {
			left = createRecord('#text');
			right = createRecord('#text');
		} else {
			left = clone(after(upper));
			right = clone(after(upper));
		}
		left = contents(left, loc.lefts);
		right = contents(right, loc.rights);
		loc = Location(
			upper.lefts.concat(left),
			[right].concat(upper.rights.slice(1)),
			upper.frames.concat()
		);
		return split(loc, until);
	}

	/**
	 * Go the the location at this given marker.
	 *
	 * @param  {!Location} loc
	 * @param  {!Marker}   marker
	 * @return {?Location}
	 * @memberOf zippers
	 */
	function go(loc, marker) {
		loc = walkInPreOrderWhile(root(loc), function (loc) {
			var record = after(loc);
			return !(record && isMarker(record) && record === marker);
		});
		return isRoot(loc) ? null : loc;
	}

	/**
	 * Splits at the given marker location.
	 *
	 * @param  {!Location} loc
	 * @param  {!Marker}   marker
	 * @param  {!function(Location):boolean}
	 * @memberOf zippers
	 */
	function splitAt(loc, marker, until) {
		return split(go(loc, marker), until);
	}

	/**
	 * Insert content at the given marker location.
	 *
	 * @param  {!Location} loc
	 * @param  {!Marker}   marker
	 * @param  {!function(Location):boolean}
	 * @memberOf zippers
	 */
	function insertAt(loc, marker, inserts) {
		return insert(go(loc, marker), inserts);
	}

	return {
		go           : go,
		dom          : dom,
		hint         : hint,
		update       : update,
		before       : before,
		after        : after,
		prev         : prev,
		next         : next,
		up           : up,
		down         : down,
		root         : root,
		peek         : peek,
		split        : split,
		splice       : splice,
		insert       : insert,
		replace      : replace,
		remove       : remove,
		zipper       : zipper,
		isAtStart    : isAtStart,
		isAtEnd      : isAtEnd,
		splitAt      : splitAt,
		insertAt     : insertAt,
		isMarker     : isMarker,
		createMarker : createMarker
	};
});

/**
 * lists.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace lists
 */
define('lists',[
	'functions',
	'dom',
	'html',
	'arrays',
	'assert',
	'boromir',
	'zippers',
	'strings',
	'content',
	'mutation',
	'boundaries'
], function (
	Fn,
	Dom,
	Html,
	Arrays,
	Assert,
	Boromir,
	Zip,
	Strings,
	Content,
	Mutation,
	Boundaries
) {
	

	/**
	 * Whether the given node is an inline node and is not right below the
	 * editing host.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function hasInlineStyle(node) {
		return !Html.hasLinebreakingStyle(node)
		    && !(node.parentNode && Dom.isEditingHost(node.parentNode));
	}

	/**
	 * Reduces a list of nodes (if any are visible) into an LI element among the
	 * given list by moving nodes into LI elements.
	 *
	 * This function is to be used in a reduce() call.
	 *
	 * @private
	 * @param  {Array.<Element>} list collection of list items
	 * @param  {Array.<Node>}    children
	 * @return {Array.<Element>}
	 */
	function reduceGroup(list, children) {
		list = list.concat();
		var visible = children.filter(Html.isRendered);
		if (visible.length > 0) {
			var li = visible[0].ownerDocument.createElement('li');
			Dom.move(visible, li);
			list.push(li);
		}
		return list;
	}

	/**
	 * Recursively removes the given node and its ancestors if they are
	 * invisible.
	 *
	 * Empty list items will be removed even though it would be considered
	 * visible in general cases.
	 *
	 * @see build
	 * @private
	 * @param {!Node} node
	 */
	function removeInvisibleNodes(node) {
		var boundaries = [];
		Dom.climbUntil(
			node,
			function (node) {
				boundaries = Mutation.removeNode(node, boundaries);
			},
			function (node) {
				if (Html.isListItem(node) && 0 === Dom.children(node).length) {
					return false;
				}
				return !node.parentNode
				    || Dom.isEditingHost(node)
				    || Html.isRendered(node);
			}
		);
	}

	function isCollectLimit(node) {
		return !Html.isListItem(node) && Html.hasLinebreakingStyle(node);
	}

	/**
	 * Collects siblings between `start` and `end` and any adjacent inline nodes
	 * next to each.
	 *
	 * @see format
	 * @private
	 * @param  {!Node} start
	 * @param  {!Node} end
	 * @return {Array.<Node>}
	 */
	function collectSiblings(start, end) {
		var nodes = Dom.prevSiblings(start, isCollectLimit).concat(start);
		if (start !== end) {
			nodes = nodes.concat(Dom.nextSiblings(start, function (node) {
				return node === end;
			}), end);
		}
		return nodes.concat(Dom.nextSiblings(end, isCollectLimit));
	}

	/**
	 * Given a list of nodes, will process the list to create a groups of nodes
	 * that should be placed to gether in LI's.
	 *
	 * A `parents` arrays will also be created of nodes that may need be removed
	 * once the grouped elements have been moved into their respective
	 * destinations. This is required because we need to later remove any
	 * elements which will become empty once their children are moved into list
	 * elements.
	 *
	 * @see build
	 * @private
	 * @param  {Array.<Node>} siblings
	 * @return {Object.<string, Array.<Node>>}
	 */
	function groupNodes(siblings) {
		var groups = [];
		var parents = [];
		var nodes = siblings.concat();
		var collection;
		var split;
		var node;
		while (nodes.length > 0) {
			node = nodes.shift();
			var canUnwrap = !Html.isGroupContainer(node)
			             && !Html.isVoidType(node)
			             && !Html.isHeading(node);
			if (Html.hasLinebreakingStyle(node) && canUnwrap) {
				collection = Dom.children(node);
				parents.push(node);
			} else {
				collection = [node];
				parents.push(node.parentNode);
			}
			split = Arrays.split(nodes, Html.hasLinebreakingStyle);
			collection = collection.concat(split[0]);
			nodes = split[1];
			if (collection.length > 0) {
				groups.push(collection);
			}
		}
		return {
			groups  : groups,
			parents : parents
		};
	}

	/**
	 * Builds a list of type `type` using the given list of nodes.
	 *
	 * @private
	 * @param  {string}       type
	 * @param  {Array.<Node>} nodes
	 */
	function build(type, nodes) {
		if (0 === nodes.length) {
			return;
		}
		var node = Dom.upWhile(nodes[0], hasInlineStyle);
		if (Html.isListItem(node) && !Dom.prevSibling(node)) {
			node = node.parentNode;
		}
		Assert.assert(
			Content.allowsNesting(node.parentNode.nodeName, type),
			'Lists.format#Cannot create ' + type + ' inside of a ' + node.parentNode.nodeName
		);
		var list = node.ownerDocument.createElement(type);
		var grouping = groupNodes(nodes);
		Dom.insert(list, node);
		Dom.move(grouping.groups.reduce(reduceGroup, []), list);
		grouping.parents.forEach(removeInvisibleNodes);
	}

	/**
	 * Creates a list of the given type.
	 *
	 * @param  {string}    type Either 'ul' or 'ol'
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Boundary>}
	 * @memberOf lists
	 */
	function format(type, start, end) {
		Assert.assert(
			Html.isListContainer({nodeName: type.toUpperCase()}),
			'Lists.format#' + type + ' is not a valid list container'
		);
		var node;
		if (Html.isBoundariesEqual(start, end)) {
			node = Dom.upWhile(Boundaries.nextNode(start), function (node) {
				return !Html.hasLinebreakingStyle(node)
				    && !Dom.isEditingHost(node.parentNode);
			});
			build(type, collectSiblings(node, node));
			return [start, end];
		}
		var cac = Boundaries.commonContainer(start, end);
		if (!Html.hasLinebreakingStyle(cac)) {
			node = Dom.upWhile(cac, function (node) {
				return node.parentNode
				    && !isCollectLimit(node.parentNode)
				    && !Dom.isEditingHost(node.parentNode);
			});
			build(type, collectSiblings(node, node));
			return [start, end];
		}
		var startNode = Dom.upWhile(Boundaries.nextNode(start), function (node) {
			return !isCollectLimit(node)
				&& !Dom.isEditingHost(node.parentNode);
		});
		var endNode = Dom.upWhile(Boundaries.prevNode(end), function (node) {
			return !isCollectLimit(node)
				&& !Dom.isEditingHost(node.parentNode);
		});

		// <div>
		//  <p>tw[o}</p>
		//  <p>three</p>
		// </div>
		//
		// ... or ...
		//
		// <div>
		//  <p>{t]wo</p>
		//  <p>three</p>
		// </div>
		/*
		if (startNode === cac) {
			startNode = endNode;
		} else if (endNode === cac) {
			endNode = startNode;
		}
		*/
		build(type, collectSiblings(startNode, endNode));
		return [start, end];
	}

	/**
	 * Unwraps all LI elements in the given collection of siblings.
	 *
	 * @private
	 * @param  {Array.<Node>} nodes
	 * @return {Array.<Node>} List of unwrapped nodes
	 */
	function unwrapItems(nodes) {
		return nodes.filter(Html.isListItem).reduce(function (lines, node) {
			return lines.concat(Html.unwrapListItem(node));
		}, []);
	}

	/**
	 * Removes list formatting around the given boundaries.
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Boundary>}
	 * @memberOf lists
	 */
	function unformat(start, end) {
		var nearestItem = function (node) {
			return !Html.isListItem(node) && !Dom.isEditingHost(node.parentNode);
		};
		var sc = Boundaries.container(start);
		var so = Boundaries.offset(start);
		var ec = Boundaries.container(end);
		var eo = Boundaries.offset(end);
		var startLi = Dom.upWhile(sc, nearestItem);
		var endLi = Dom.upWhile(ec, nearestItem);
		var lines;
		if (Html.isListItem(startLi)) {
			lines = unwrapItems(Dom.nodeAndNextSiblings(startLi));
			if (Html.isListItem(sc)) {
				sc = lines[0];
				so = 0;
			}
		}
		if (sc === ec) {
			return [Boundaries.create(sc, so), end];
		}
		if (Html.isListItem(endLi)) {
			lines = unwrapItems(Dom.nodeAndNextSiblings(endLi));
			if (Html.isListItem(ec)) {
				ec = lines[0];
				eo = 0;
			}
		}
		return [Boundaries.create(sc, so), Boundaries.create(ec, eo)];
	}

	/**
	 * Formats the content between the given boundaries into a list.
	 * If the content is already a list, it will either unformat the content or
	 * reformat the content into the given list type.
	 *
	 * @param  {string}   type Either 'ul' or 'ol'
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Boundary>}
	 * @memberOf lists
	 */
	function toggle(type, start, end) {
		var sc = Boundaries.container(start);
		var ec = Boundaries.container(end);
		var si = Dom.upWhile(sc, Html.isListItem);
		var ei = Dom.upWhile(ec, Html.isListItem);
		if (Html.isListItem(si) && Html.isListItem(ei) && si.parentNode === ei.parentNode) {
			if (si.parentNode.nodeName.toLowerCase() === type) {
				return unformat(start, end);
			}
		}
		return format(type, start, end);
	}

	/**
	 * Starting with the given, returns the first node that matches the given
	 * predicate.
	 *
	 * @private
	 * @param  {!Node}                  node
	 * @param  {function(Node):boolean} pred
	 * @return {Node}
	 */
	function nearest(node, pred) {
		return Dom.upWhile(node, function (node) {
			return !pred(node)
			    && !(node.parentNode && Dom.isEditingHost(node.parentNode));
		});
	}

	/**
	 * Checks if the given (normalized) boundary is (visually) at the start of a
	 * list item element.
	 *
	 * Strategy:
	 *
	 * 1. If a text boundary is given, there must not be any non-collapsable
	 *    characters in front of the boundary.
	 *
	 *    ... otherwise ...
	 *
	 * 2. The boundary must be container inside of a list item. This means that
	 *    it must have a list item as its container or an ancestor of its
	 *    container.
	 *
	 *    ... and ...
	 *
	 *    We must not find any visible nodes between  the start of this element
	 *    and the boundary that was given to start of with.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {boolean}
	 */
	function isAtStartOfListItem(boundary) {
		var node = Boundaries.prevNode(boundary);
		if (Dom.isTextNode(node)) {
			var text = node.data;
			var offset = Boundaries.offset(boundary);
			var prefix = text.substr(0, offset);
			var isVisible = Strings.NOT_SPACE.test(prefix)
			             || Strings.NON_BREAKING_SPACE.test(prefix);
			if (isVisible) {
				return false;
			}
			node = node.previousSibling || node.parentNode;
		}
		var stop = nearest(node, Html.isListItem);
		if (!Html.isListItem(stop)) {
			return false;
		}
		var start = Boundaries.fromFrontOfNode(stop);
		var visible = 0;
		Html.walkBetween(start, boundary, function (nodes) {
			visible += nodes.filter(Html.isRendered).length;
		});
		return 0 === visible;
	}

	function removeNext(loc, num) {
		var records = [];
		while (num--) {
			records.push(Zip.after(loc));
			loc = Zip.remove(loc);
		}
		return {
			loc     : loc,
			records : records
		};
	}

	function isRenderedRecord(record) {
		return Html.isRendered(record.domNode());
	}

	function prevVisible(loc) {
		var index = Arrays.someIndex(loc.lefts.concat().reverse(), isRenderedRecord);
		return -1 === index ? null : Zip.prev(loc, index + 1);
	}

	function nextVisible(loc) {
		var index = Arrays.someIndex(loc.rights, isRenderedRecord);
		return -1 === index ? null : Zip.next(loc, index);
	}

	function bottomJoiningLoc(loc) {
		loc = nextVisible(loc);
		if (!loc || !Html.isListItem(Zip.dom(loc))) {
			return null;
		}
		loc = nextVisible(Zip.down(loc));
		if (!loc || !Html.isListContainer(Zip.dom(loc))) {
			return null;
		}
		return loc;
	}

	function topJoiningLoc(loc) {
		loc = prevVisible(loc);
		if (!loc || !Html.isListItem(Zip.dom(loc))) {
			return null;
		}
		var atLiEnd = Zip.next(Zip.down(loc), loc.rights.length);
		loc = prevVisible(atLiEnd);
		if (!loc || !Html.isListContainer(Zip.dom(loc))) {
			return atLiEnd;
		}
		loc = Zip.down(loc);
		return Zip.next(loc, loc.rights.length);
	}

	function insertAt(loc, records) {
		if (!Html.isListContainer(Zip.dom(Zip.up(loc)))) {
			loc = Zip.down(Zip.insert(loc, Boromir(document.createElement('UL'))));
		}
		return Zip.insert(loc, records);
	}

	function indent(start, end) {
		var startLi = nearest(Boundaries.prevNode(start), Html.isListItem);
		var endLi = nearest(Boundaries.nextNode(end), Html.isListItem);
		// Because otherwise the range between `start` and `end` is not within a
		// list
		if (!Html.isListItem(startLi) || !Html.isListItem(endLi)) {
			return [start, end];
		}
		start = Boundaries.fromFrontOfNode(startLi);
		end = Boundaries.fromBehindOfNode(endLi);
		var cac = Boundaries.commonContainer(start, end);
		var zip = Zip.zipper(Dom.editingHost(startLi), {
			start : start,
			end   : end
		});
		var isBelowCac = function (loc) { return Zip.dom(Zip.up(loc)) === cac; };
		var loc = zip.loc;
		loc = Zip.splitAt(loc, zip.markers.start, isBelowCac);
		loc = Zip.splitAt(loc, zip.markers.end, isBelowCac);
		var bottom = bottomJoiningLoc(Zip.next(Zip.go(loc, zip.markers.end)));
		var records = [];
		var removed;
		if (bottom) {
			loc = Zip.down(bottom);
			removed = removeNext(loc, loc.rights.length);
			records = records.concat(removed.records);
			loc = Zip.remove(Zip.up(Zip.up(removed.loc)));
		}
		loc = Zip.go(loc, zip.markers.start);
		removed = removeNext(loc, Arrays.someIndex(loc.rights.slice(1), Zip.isMarker) + 2);
		records = removed.records.concat(records);
		loc = topJoiningLoc(removed.loc) || Zip.down(Zip.insert(
			removed.loc,
			Boromir(document.createElement('LI'))
		));
		var markers = Zip.update(Zip.root(insertAt(loc, records)));
		return [Boundaries.next(markers.start), Boundaries.prev(markers.end)];
	}

	function isIndentationRange(start, end) {
		var startLi = nearest(Boundaries.prevNode(start), Html.isListItem);
		if (!Html.isListItem(startLi)) {
			return false;
		}
		var endLi = nearest(Boundaries.nextNode(end), Html.isListItem);
		if (!Html.isListItem(endLi)) {
			return false;
		}
		// ✘ <li><b>fo[o</b><u>b]ar</u></li>
		// ✔ <li><b>{foo</b><u>b]ar</u></li>
		// ✔ <li><b>fo[o</b></li><li><u>b]ar</u></li>
		return startLi !== endLi || isAtStartOfListItem(start);
	}

	return {
		indent              : indent,
		format              : format,
		unformat            : unformat,
		toggle              : toggle,
		isIndentationRange  : isIndentationRange
	};
});

/**
 * events.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @see
 * http://www.w3.org/TR/DOM-Level-3-Events/#idl-interface-MouseEvent-initializers
 * @namespace events
 */
define('events',[
	'functions',
	'misc',
	'assert'
], function (
	Fn,
	Misc,
	Assert
) {
	

	/**
	 * Registers an event listener to fire the given callback when a specified
	 * event is triggered on the given object.
	 *
	 * @param {Element|Document|Window} obj
	 *        Object which supports events.  This includes DOM elements, the
	 *        Document itself, and the Window object for example.
	 * @param {string} event
	 *        Name of the event for which to register the given callback.
	 * @param {function} handler
	 *        Function to be invoked when event is triggered on the given
	 *        object.
	 * @param {boolean=} opt_useCapture
	 *        Optional.  Whether to add the handler in the capturing phase.
	 * @memberOf events
	 */
	function add(obj, event, handler, opt_useCapture) {
		var useCapture = !!opt_useCapture;
		if (obj.addEventListener) {
			obj.addEventListener(event, handler, useCapture);
		} else if (obj.attachEvent) {
			obj.attachEvent('on' + event, handler);
		} else {
			Assert.error();
		}
	}

	/**
	 * Detaches the specified event callback from the given event.
	 *
	 * @param {Element|Document|Window} obj
	 *        Object which supports events.  This includes DOM elements, the
	 *        Document itself, and the Window object for example.
	 * @param {string} event
	 *        Name of the event to detach.
	 * @param {function} handler
	 *        Function to be de-registered.
	 * @param {boolean=} opt_useCapture
	 *        Optional.  Must be true if the handler was registered with a true
	 *        useCapture argument.
	 * @memberOf events
	 */
	function remove(obj, event, handler, opt_useCapture) {
		var useCapture = !!opt_useCapture;
		if (obj.removeEventListener) {
			obj.removeEventListener(event, handler, useCapture);
		} else if (obj.detachEvent) {
			obj.detachEvent('on' + event, handler);
		} else {
			Assert.error();
		}
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf events
	 */
	function dispatch(doc, obj, event) {
		var eventObj;
		if (obj.dispatchEvent) {
			// NB This method is to create events is deprecated:
			// https://developer.mozilla.org/en-US/docs/Web/Guide/API/DOM/Events/Creating_and_triggering_events
			// But the new way doesn't work on IE9.
			// var eventObj = new window['Event'](event);
			eventObj = doc.createEvent('Event');
			eventObj.initEvent(event, true, true);
			obj.dispatchEvent(eventObj);
		} else if (obj.fireEvent) {
			eventObj = doc.createEventObject();
			eventObj['type'] = event;
			obj.fireEvent('on' + event, eventObj);
		} else {
			Assert.error();
		}
	}

	/**
	 * Given an event object, checks whether the ctrl key is depressed.
	 *
	 * @param  {Event} event
	 * @return {boolean}
	 * @memberOf events
	 */
	function hasKeyModifier(event, modifier) {
		return event.meta.indexOf(modifier) > -1;
	}

	/**
	 * Runs the given function after the current event handler returns
	 * to the browser.
	 *
	 * Currently implemented just with setTimeout() which is specced to
	 * have a minimum timeout value of 4 milliseconds. Alternate
	 * implementations are possible that are faster, for example:
	 * https://github.com/NobleJS/setImmediate
	 *
	 * @param fn {function} a function to call
	 * @memberOf events
	 */
	function nextTick(fn) {
		setTimeout(fn, 4);
	}

	/**
	 * Sets up all editing browser events to call `editor` on the given
	 * document.
	 *
	 * @see
	 * https://en.wikipedia.org/wiki/DOM_Events
	 * http://www.w3.org/TR/DOM-Level-3-Events
	 *
	 * @param {function} editor
	 * @param {Document} doc
	 * @memberOf events
	 */
	function setup(doc, editor) {
		add(doc, 'resize',     editor);

		add(doc, 'keyup',      editor);
		add(doc, 'keydown',    editor);
		add(doc, 'keypress',   editor);

		add(doc, 'click',      editor);
		add(doc, 'mouseup',    editor);
		add(doc, 'mousedown',  editor);
		add(doc, 'mousemove',  editor);
		add(doc, 'dblclick',   editor);

		add(doc, 'dragstart',  editor);
		add(doc, 'drag',       editor);
		add(doc, 'dragenter',  editor);
		add(doc, 'dragexit',   editor);
		add(doc, 'dragleave',  editor);
		add(doc, 'dragover',   editor);
		add(doc, 'drop',       editor);
		add(doc, 'dragend',    editor);
		add(doc, 'paste',      editor);
	}

	/**
	 * Flags the given event to prevent native handlers from performing default
	 * behavior for it.
	 *
	 * @param {Event} event
	 * @memberOf events
	 */
	function preventDefault(event) {
		if (event.preventDefault) {
			event.preventDefault();
		} else {
			event['returnValue'] = false;
		}
	}

	/**
	 * Stops this event from bubbling any further up the DOM tree.
	 *
	 * @param {Event} event
	 * @memberOf events
	 */
	function stopPropagation(event) {
		if (event.stopPropagation) {
			event.stopPropagation();
		} else {
			event['cancelBubble'] = true;
		}
	}

	/**
	 * "Suppresses" the given event such that it will not trigger default
	 * behavior and nor propagate.  This will prevent any parent handlers up of
	 * the DOM tree from being notified of this event.
	 *
	 * @param {Event} event
	 * @memberOf events
	 */
	function suppress(event) {
		stopPropagation(event);
		preventDefault(event);
	}

	/**
	 * Returns true if the given value is a native browser event object.
	 *
	 * @param  {*} obj
	 * @return {boolean}
	 * @memberOf events
	 */
	function is(obj) {
		return obj
		    && obj.hasOwnProperty
		    && obj.hasOwnProperty('type')
		    && !Fn.isNou(obj.stopPropagation)
		    && !Fn.isNou(obj.preventDefault);
	}

	return {
		is              : is,
		add             : add,
		remove          : remove,
		setup           : setup,
		hasKeyModifier  : hasKeyModifier,
		dispatch        : dispatch,
		nextTick        : nextTick,
		preventDefault  : preventDefault,
		stopPropagation : stopPropagation,
		suppress        : suppress
	};
});

/**
 * link-util.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('links/link-util',[
	'dom',
	'html',
	'boundaries'
], function(
	Dom,
    Html,
	Boundaries
) {
	

	/**
	 * Checks if `node` is an anchor node.
	 *
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function isAnchorNode(node) {
		return 'A' === node.nodeName;
	}

	/**
	 * Checks if `node` could be inserted inside a Anchor element.
	 *
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function isLinkable(node) {
		return !Html.isGroupContainer(node) && !Html.isGroupedElement(node);
	}

	/**
	 * Gets first parent element which is an anchor.
	 *
	 * @private
	 * @param   {!Node} node
	 * @returns {?Element}
	 */
	function nearestAnchorParent(node) {
		var parent = Dom.upWhile(node, function (node) {
			return !isAnchorNode(node) && isLinkable(node) && !Dom.isEditingHost(node);	
		});
		return isAnchorNode(parent) ? parent : null;
	}

	/**
	 * Gets next rendered node of `node`.
	 *
	 * @param   {!Node} node
	 * @returns {?Node}
	 */
	function nextRenderedNode(node) {
		return Dom.nextWhile(node, Html.isUnrendered);
	}

	/**
	 * Gets previous rendered node of `node`.
	 *
	 * @param   {!Node} node
	 * @returns {?Node}
	 */
	function prevRenderedNode(node) {
		return Dom.prevWhile(node, Html.isUnrendered);
	}

	/**
	 * Returns a boundary in a linkable element.
	 *
	 * @param  {!Boundary} boundary
	 * @return {Boundary}
	 */
	function linkableBoundary(boundary) {
		var node = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		if (Boundaries.isNodeBoundary(boundary)) {
			node = node.childNodes[offset];
			offset = 0;
		}
		var next = nextRenderedNode(node);
		return Boundaries.create(nearestAnchorParent(next) || next, offset);
	}

	return {
		isLinkable       : isLinkable,
		linkableBoundary : linkableBoundary,
		nextRenderedNode : nextRenderedNode,
		prevRenderedNode : prevRenderedNode
	};
});

/**
 * link-create.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('links/link-selection',[
	'dom',
	'html',
	'arrays',
	'mutation',
	'boundaries',
	'./link-util'
], function(
	Dom,
    Html,
	Arrays,
    Mutation,
	Boundaries,
    LinkUtil
) {
	

	/**
	 * Clones `node` and append `child`.
	 *
	 * @private
	 * @param  {!Node} node
	 * @param  {!Node} child
	 * @return {Node}
	 */
	function cloneAndAppend(node, child) {
		var clone = Dom.cloneShallow(node);
		node.insertBefore(clone, child);
		clone.appendChild(child);
		return clone;
	}

	/**
	 * Splits `node` by 'offset' and wraps to match unbalance node.
	 *
	 * @private
	 * @param  {!Node}    node
	 * @param  {string}   offset
	 * @param  {!Element} reachParent
	 * @return {Object.<string, Node>} {after: Node, before: Node}
	 */
	function splitTextNode(node, offset, reachParent) {
		if (isTextNodeSelectionAtEnd(node, offset)) {
			return {after: node.nextSibling, before: node};
		}

		if (isTextNodeSelectionAtStart(node, offset)) {
			return {after: node, before: node.previousSibling};
		}

		var before = Mutation.splitTextNode(node, offset);
		var after = before.nextSibling;

		if (after
				&& !after.nextSibling
				&& LinkUtil.isLinkable(after.parentNode)
				&& !Dom.isSameNode(node, reachParent)) {
			var parentNode = before.parentNode;
			do {
				after = cloneAndAppend(parentNode, after);
				before = cloneAndAppend(parentNode, before);

				Dom.removeShallow(parentNode);

				parentNode = before.parentNode;
			} while (parentNode
					&& !Dom.isSameNode(parentNode, reachParent)
					&& LinkUtil.isLinkable(parentNode));
		} else if (!after) {
			after = before;
			while (after.parentNode && !after.nextSibling && !Dom.isSameNode(after.parentNode, reachParent)) {
				after = after.parentNode;
			}
			after = after.nextSibling;
		}

		return {after: after, before: before};
	}

	/**
	 * Splits node, append `child` and insert `node` before `ref`.
	 *
	 * @private
	 * @param   {!Node} node
	 * @param   {!Node} child
	 * @param   {!Node} ref
	 * @returns {Node}
	 */
	function splitElement(node, child, ref) {
		var clone = Dom.cloneShallow(node);
		node.parentNode.insertBefore(clone, ref);
		clone.appendChild(child);
		return clone;
	}

	/**
	 * Gets first node and splits is necessary.
	 *
	 * @private
	 * @param  {!Node} container
	 * @param  {!Node} reachParent
	 * @return {Node}
	 */
	function firstNodeAndSplit(container, reachParent) {
		if (Html.hasBlockStyle(container.parentNode)) {
			return container;
		}
		container = container.parentNode;
		var clone = container;
		var parent = container.parentNode;
		while (parent && !Dom.isSameNode(parent, reachParent) && LinkUtil.isLinkable(parent)) {
			clone = splitElement(parent, container, parent.nextSibling);
			container = parent;
			parent = parent.parentNode;
		}
		return clone;
	}


	/**
	 * Gets previous linkable node.
	 * @param {node} node
	 * @return {Node}
	 */
	function prevLinkableNode(node) {
		while (!LinkUtil.isLinkable(node)) {
			node = node.lastChild;
		}
		return node;
	}

	/**
	 * Gets the end element of container, splitting if necessary.
	 * @param {Node} container
	 * @param {Node} reachParent
	 * @return {Node}
	 */
	function endNodeAndSplit(container, reachParent) {
		container = container.parentNode;

		var clonedNode = container;
		var parentNode = container.parentNode;

		while (parentNode && !Dom.isSameNode(parentNode, reachParent) && LinkUtil.isLinkable(parentNode)) {
			clonedNode = splitElement(parentNode, container, parentNode);

			container = parentNode;
			parentNode = parentNode.parentNode;
		}

		if (!isRendered(clonedNode)) {
			var previousSibling = LinkUtil.prevRenderedNode(clonedNode);
			Dom.remove(clonedNode);
			clonedNode = prevLinkableNode(previousSibling);
		}

		return clonedNode;
	}

	/**
	 * Gets the first linkable node
	 * @param {Boundary} boundary
	 * @param {Node} commonContainer
	 * @return {Node}
	 */
	function firstLinkableNode(boundary, commonContainer) {
		var container = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);
		if (Dom.isTextNode(container)) {
			return splitTextNode(container, offset, commonContainer).after;
		}
		if (Dom.isSameNode(container.parentNode, commonContainer)
			|| !LinkUtil.isLinkable(container.parentNode)) {
			return container;
		}

		return firstNodeAndSplit(container, commonContainer);
	}

	/**
	 * Checks if text node `node` has some text selected.
	 * @param {Node} node
	 * @param {integer} offset
	 * @return {boolean}
	 */
	function isTextNodeSelectionAtMiddle(node, offset) {
		return Dom.isTextNode(node) && offset > 0;
	}

	/**
	 * Checks if the text node `node` NOT has some text selected inside.
	 * @param {Node} node
	 * @param {integer} offset
	 * @return {boolean}
	 */
	function isTextNodeSelectionAtStart(node, offset) {
		return Dom.isTextNode(node) && offset === 0;
	}

	/**
	 * Checks if the text node `node` NOT has some text selected inside.
	 * @param {Node} node
	 * @param {integer} offset
	 * @return {boolean}
	 */
	function isTextNodeSelectionAtEnd(node, offset) {
		return Dom.isTextNode(node) && node.length === offset;
	}

	/**
	 * Gets last linkable node.
	 * @param boundary
	 * @param commonContainer
	 * @return {Node}
	 */
	function lastLinkableNode(boundary, commonContainer) {
		var container = Boundaries.container(boundary);
		var offset = Boundaries.offset(boundary);

		if (isTextNodeSelectionAtMiddle(container, offset)) {
			return splitTextNode(container, offset, commonContainer).before;
		}
		if (isTextNodeSelectionAtStart(container, offset) && container.previousSibling) {
			return prevLinkableNode(container.previousSibling);
		}
		if (!LinkUtil.isLinkable(container.parentNode)) {
			return container;
		}

		return endNodeAndSplit(container, commonContainer);
	}

	var LINE_BREAKING_NODES_TAGS = ['LI', 'TD', 'TR', 'TBODY', 'DD', 'DT'];

	/**
	 * Checks if `node` is a line breaking node.
	 * @param {Node} node
	 * @returns {boolean}
	 */
	function isLineBreakingNode(node) {
		return LINE_BREAKING_NODES_TAGS.indexOf(node.nodeName) >= 0;
	}

	/**
	 * Checks for spaces between line-breaking nodes <li>one</li>  <li>two</li>
	 *
	 * @param   {!Node} node
	 * @returns {boolean}
	 */
	function isWhitSpaceBetweenLineBreakingNodes(node) {
		if (!Dom.isTextNode(node) || node.textContent.trim().length > 0) {
			return false;
		}
		if (node.previousElementSibling && (isLineBreakingNode(node.previousElementSibling)) &&
				node.nextElementSibling && (isLineBreakingNode(node.nextElementSibling))) {
			return true;
		}
		if (node.previousElementSibling && (isLineBreakingNode(node.previousElementSibling)) &&
				!node.nextElementSibling) {
			return true;
		}
		if (!node.previousElementSibling &&
				node.nextElementSibling && (isLineBreakingNode(node.nextElementSibling))) {
			return true;
		}
		return false;
	}

	function isRendered(node) {
		return Html.isRendered(node) && !isWhitSpaceBetweenLineBreakingNodes(node);
	}

	/**
	 * Removes `anchorNode` is this is an anchor element, and put its
	 * children in `linkable` array.
	 * @param {Node} anchorNode
	 * @param {Array.<Element>} linkable
	 * @return {Node}
	 */
	function removeAnchorElement(anchorNode, linkable) {
		if (!isRendered(anchorNode)) {
			return anchorNode;
		}

		if ('A' === anchorNode.nodeName) {
			var firstChild = anchorNode.firstChild;

			Dom.children(anchorNode).forEach(function (item) {
					linkable.push(item);
				});

			Dom.removeShallow(anchorNode);

			anchorNode = firstChild;
		} else {
			linkable.push(anchorNode);
		}

		return anchorNode;
	}

	/**
	 * Saves `linkable` inside `linkableNodes` and create a new one.
	 * @param {Array.<Node>} linkable
	 * @param {Array.<Array<Node>>} linkableNodes
	 * @returns {*}
	 */
	function saveAndCreateLinkable(linkable, linkableNodes) {
		if (linkable.length > 0) {
			linkableNodes.push(linkable);
		}
		// create new linkable
		return [];
	}

	/**
	 * Gets next
	 * @param first
	 * @returns {{first: *, createLinkable: boolean}}
	 */
	function nextLinkable(first) {
		var createLinkable = false;
		while (first && !first.nextSibling && first.parentNode) {
			first = first.parentNode;
			if (!LinkUtil.isLinkable(first)) {
				createLinkable = true;
			}
		}
		return {next: first, createLinkable: createLinkable};
	}

	/**
	 * Gets linkable nodes between node `first` and node `last`.
	 * @param {Node} first
	 * @param {Node} last
	 * @return {Array.<Array<Element>>}
	 */
	function linkableNodesBetween(first, last) {
		var linkableNodes = [];
		var linkable = [];            // Array of consecutive nodes that belong to a single link
		while (first && !Dom.isSameNode(first, last)) {
			if (LinkUtil.isLinkable(first)) {
				first = removeAnchorElement(first, linkable);

				var nextLinkableRet = nextLinkable(first);

				first = nextLinkableRet.next.nextSibling;

				if (nextLinkableRet.createLinkable) {
					linkable = saveAndCreateLinkable(linkable, linkableNodes);
				}
			} else {
				linkable = saveAndCreateLinkable(linkable, linkableNodes);
				first = first.firstChild;
			}
		}

		if (LinkUtil.isLinkable(last)) {
			removeAnchorElement(last, linkable);
		}

		if (linkable.length > 0) {
			linkableNodes.push(linkable);
		}

		return linkableNodes;
	}

	/**
	 * Gets the start and end element contained in the selection, splitting the text
	 * if necessary.
	 * @param {Boundary} startBoundary
	 * @param {Boundary} endBoundary
	 * @param {Element} commonContainer
	 * @return {{startElement: Element, endElement: Element}}
	 */
	function firstAndLastNode(start, end, cac) {
		var sc = Boundaries.container(start);
		var ec = Boundaries.container(end);
		var so = Boundaries.offset(start);
		var eo = Boundaries.offset(end);
		var isSelectionInSameTextNode = Dom.isTextNode(sc) && Dom.isSameNode(sc, ec);
		var first = firstLinkableNode(start, cac);
		if (isSelectionInSameTextNode) {
			// If the first elements was split, we have to take precaution.
			end = Boundaries.raw(first, eo - so);
			cac = first;
		}
		return {
			startNode: first,
			endNode: lastLinkableNode(end, cac)
		};
	}

	/**
	 * Checks if the range is in the same Text Node.
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {boolean}
	 */
	function rangeInSameTextNode (start, end) {
		var sc = Boundaries.container(start);
		var ec = Boundaries.container(end);
		return Dom.isTextNode(sc) && Dom.isSameNode(sc, ec);
	}

	/**
	 * Checks if the selection completely wrap a Text Node.
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {boolean}
	 */
	function isSelectionInWholeTextNode(start, end) {
		var sc = Boundaries.container(start);
		var so = Boundaries.offset(start);
		var eo = Boundaries.offset(end);
		return rangeInSameTextNode(start, end) && so === 0 && sc.length === eo;
	}

	/**
	 * Collects a list of groups of nodes that can be wrapped into anchor tags.
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Array.<Element>>}
	 */
	function collectLinkableNodeGroups(start, end) {
		var startBoundary = LinkUtil.linkableBoundary(start);
		var endBoundary = LinkUtil.linkableBoundary(end);
		if (isSelectionInWholeTextNode(startBoundary, endBoundary)) {
			// The selection is in the whole Text Node
			// <b>[one]</b>
			return [[Boundaries.container(startBoundary)]];
		}
		var limitNodes = firstAndLastNode(
			startBoundary,
			endBoundary,
			Boundaries.commonContainer(start, end)
		);
		if (rangeInSameTextNode(startBoundary, endBoundary)) {
			// endElement is the element selected.
			return [[limitNodes.endNode]];
		}
		return linkableNodesBetween(limitNodes.startNode, limitNodes.endNode);
	}

	function collectGroups(first, last) {
	}

	function collectLinkable(start, end) {
		var startSplit = Mutation.splitBoundaryUntil(start, Boundaries.isNodeBoundary);
		var endSplit = Mutation.splitBoundaryUntil(end, Boundaries.isNodeBoundary);
		var first = Boundaries.nextNode(startSplit);
		var last = Boundaries.prevNode(endSplit);
		var groups = collectGroups(first, last);
		console.log(groups);
		return [startSplit, endSplit];
	}

	window.collectLinkable = collectLinkable;

	return {
		collectLinkableNodeGroups : collectLinkableNodeGroups
	};
});

/**
 * links.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace links
 */
define('links',[
	'dom',
	'html',
	'events',
	'ranges',
	'arrays',
	'mutation',
	'boundaries',
	'links/link-util',
	'links/link-selection'
], function (
	Dom,
	Html,
	Events,
	Ranges,
	Arrays,
	Mutation,
	Boundaries,
	LinkUtil,
	LinkSelection
) {
	

	/**
	 * Checks if the range is valid for create a link.
	 *
	 * @param  {!Range} range
	 * @return {boolean}
	 */
	function isValidRangeForCreateLink(range) {
		return !range.collapsed && (!range.textContent || range.textContent.trim().length === 0);
	}

	/**
	 * Creates anchor elements between the given boundaries.
	 *
	 * @private
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Element>}
	 */
	function createAnchors(start, end) {
		var doc = Boundaries.document(start);
		var groups = LinkSelection.collectLinkableNodeGroups(start, end);
		return groups.reduce(function (anchors, group) {
			var anchor = doc.createElement('a');
			Dom.insert(anchor, group[0]);
			Dom.move(group, anchor);
			return anchors.concat(anchor);
		}, []);
	}

	/**
	 * Creates links from the content between the given boundaries.
	 *
	 * If the boundaries represent a collapsed selection (visually equal), then
	 * the a link will be created at the boundary position with href as both the
	 * anchor text and the value of the href attribute.
	 *
	 * Will pass newly created anchor elements to optional `created` array.
	 *
	 * @todo
	 * - This function should return a list of newly created anchor elements.
	 * - This function also needs to return the modified boundaries.
	 *
	 * @param  {string}           href
	 * @param  {!Boundary}        start
	 * @param  {!Boundary}        end
	 * @param  {Array.<Element>=} created
	 * @return {Array.<Boundary>}
	 * @memberOf links
	 */
	function create(href, start, end, created) {
		var anchors;
		if (Html.isBoundariesEqual(start, end)) {
			var a = Boundaries.document(start).createElement('a');
			a.innerHTML = href;
			Mutation.insertNodeAtBoundary(a, start, true);
			anchors = [a];
		} else {
			anchors = createAnchors(start, end);
		}
		anchors.forEach(function (anchor) {Dom.setAttr(anchor, 'href', href);});
		if (created) {
			anchors.reduce(function (list, a) {list.push(a); return list;}, created);
		}
		return [
			Boundaries.fromFrontOfNode(anchors[0]),
			Boundaries.fromBehindOfNode(Arrays.last(anchors))
		];
	}

	/**
	 * Checks whether the given nodes are equal according to their type and
	 * attributes.
	 *
	 * @private
	 * @see    http://www.w3.org/TR/DOM-Level-3-Core/core.html#Node3-isEqualNode
	 * @param  {!Node} src
	 * @param  {!Node} dst
	 * @return {boolean}
	 */
	function isEqualNodeShallow(src, dst) {
		return Dom.cloneShallow(src).isEqualNode(Dom.cloneShallow(dst));
	}

	/**
	 * Checks two nodes are are compatible to be joined.
	 *
	 * @private
	 * @param  {!Node} src
	 * @param  {!Node} dst
	 * @return {boolean}
	 */
	function areJoinable(src, dst) {
		return !Dom.isTextNode(src) && isEqualNodeShallow(src, dst);
	}

	/**
	 * Joins two nodes if they are compatible.
	 *
	 * @private
	 * @param {!Node} src
	 * @param {!Node} dst
	 */
	function joinNodes(src, dst) {
		var last;
		while (src && dst && areJoinable(src, dst)) {
			last = LinkUtil.nextRenderedNode(src.firstChild);
			dst.appendChild(last);
			Dom.remove(src);
			src = last;
			dst = dst.firstChild;
		}
	}

	/**
	 * Removes link anchor.
	 *
	 * @private
	 * @param {!Node} anchor
	 */
	function removeIfLink(anchor) {
		if (!LinkUtil.isAnchorNode(anchor)) {
			return;
		}
		var firstChild = LinkUtil.nextRenderedNode(anchor.firstChild);
		var prevAnchorSibling = LinkUtil.prevRenderedNode(anchor.previousSibling);
		joinNodes(firstChild, prevAnchorSibling);
		var lastChild = LinkUtil.prevRenderedNode(anchor.lastChild);
		var nextAnchorSibling = LinkUtil.nextRenderedNode(anchor.nextSibling);
		joinNodes(nextAnchorSibling, lastChild);
		Dom.removeShallow(anchor);
	}

	/**
	 * Removes children links if exists inside `node`.
	 *
	 * @private
	 * @param {!Node} node
	 */
	function removeChildrenLinks(node) {
		if (Dom.isElementNode(node)) {
			Arrays.coerce(node.querySelectorAll('a')).forEach(removeIfLink);
		}
	}

	/**
	 * Removes parent links if exists and returns the next node which should be
	 * analyze.
	 *
	 * @private
	 * @param  {Node} next
	 * @return {Node}
	 */
	function removeParentLinksAndGetNext(next) {
		var parent;
		while (!next.nextSibling && next.parentNode) {
			parent = next.parentNode;
			removeIfLink(next);
			next = parent;
		}
		var nextSibling = next.nextSibling;
		removeIfLink(next);
		return nextSibling;
	}

	/**
	 * Removes any links in the content between the given boundaries.
	 *
	 * @param {!Boundary} start
	 * @param {!Boundary} end
	 * @memberOf links
	 */
	function remove(start, end) {
		var startBoundary = LinkUtil.boundaryLinkable(
			Boundaries.container(start),
			Boundaries.offset(start)
		);
		var endBoundary = LinkUtil.boundaryLinkable(
			Boundaries.container(end),
			Boundaries.offset(end)
		);
		var sc = Boundaries.container(startBoundary);
		var ec = Boundaries.container(endBoundary);
		removeChildrenLinks(sc);
		removeChildrenLinks(ec);
		var next = sc;
		while (next && !Dom.isSameNode(next, ec)) {
			next = removeParentLinksAndGetNext(next);
			if (next) {
				removeChildrenLinks(next);
			}
		}
		while (ec && ec.parentNode && LinkUtil.isLinkable(ec)) {
			removeIfLink(ec);
			ec = ec.parentNode;
		}
	}

	function notAnchor(node) {
		return 'A' !== node.nodeName;
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf links
	 */
	function middleware(event) {
		if ('click' !== event.type) {
			return event;
		}
		var cac = Boundaries.commonContainer(
			event.selection.boundaries[0],
			event.selection.boundaries[1]
		);
		var anchor = Dom.upWhile(cac, notAnchor);
		if (anchor) {
			Events.preventDefault(event.nativeEvent);
		}
		return event;
	}

	return {
		middleware : middleware,
		create     : create,
		remove     : remove
	};
});

/**
 * overrides.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * Refernces:
 * http://www.w3.org/TR/CSS2/propidx.html
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#value
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#state
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#value-override
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#state-override
 *
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#specified-command-value
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#inline-command-activated-values
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#equivalent-values
 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#effective-command-value
 *
 * http://www.w3.org/TR/CSS2/propidx.html
 * @namespace overrides
 */
define('overrides',[
	'dom',
	'misc',
	'maps',
	'html',
	'arrays',
	'mutation',
	'boundaries'
], function (
	Dom,
	Misc,
	Maps,
	Html,
	Arrays,
	Mutation,
	Boundaries
) {
	

	/**
	 * A table of node names that correlate to override commands.
	 *
	 * @type {Object.<string, string>}
	 * @see  stateToNode
	 * @memberOf overrides
	 */
	var nodeToState = {
		'A'      : 'link',
		'U'      : 'underline',
		'B'      : 'bold',
		'STRONG' : 'bold',
		'I'      : 'italic',
		'EM'     : 'italic',
		'STRIKE' : 'strikethrough',
		'SUB'    : 'subscript',
		'SUP'    : 'superscript'
	};

	/**
	 * A table of overrides an node names that correlate to them.
	 *
	 * @type {Object.<string, string>}
	 * @see  nodeToState
	 * @memberOf overrides
	 */
	var stateToNode = {
		'link'          : 'A',
		'underline'     : 'U',
		'bold'          : 'B',
		'italic'        : 'I',
		'strikethrough' : 'STRIKE',
		'subscript'     : 'SUB',
		'superscript'   : 'SUP'
	};

	/**
	 * Any element whose node name corresponds with the given command states
	 * ("bold", "italic", "underline", "strikethrough"), must also have the
	 * associated style property match the expected value, otherwise that
	 * element's state is considered nullified by the CSS styles that has been
	 * applied to it.
	 *
	 * @private
	 * @type {Object.<string, Array.<string|null>>}
	 */
	var stateToStyle = {
		'bold'          : ['fontWeight', 'bold', null],
		'italic'        : ['fontStyle', 'italic', null],
		'underline'     : ['textDecoration', 'underline', 'none'],
		'strikethrough' : ['textDecoration', 'line-through', 'none']
	};

	/**
	 * Translation of override values to styles.
	 *
	 * @private
	 * @type {Object.<string, string>}
	 */
	var valueToStyle = {
		'hilitecolor' : 'background-color',
		'backcolor'   : 'background-color',
		'fontname'    : 'font-family',
		'fontsize'    : 'font-size',
		'fontcolor'   : 'color'
	};

	/**
	 * List of styles that can be affected through overrides.
	 *
	 * @private
	 * @type {Object.<string, string>}
	 */
	var styles = [
		'textTransform',

		'backgroundColor',

		'color',
		'fontSize',
		'fontFamily',

		'border',
		'borderColor',
		'borderStyle',
		'borderWidth',

		'borderTop',
		'borderTopColor',
		'borderTopStyle',
		'borderTopWidth',

		'borderBottom',
		'borderBottomColor',
		'borderBottomStyle',
		'borderBottomWidth',

		'borderLeft',
		'borderLeftColor',
		'borderLeftStyle',
		'borderLeftWidth',

		'borderRight',
		'borderRightColor',
		'borderRightStyle',
		'borderRightWidth'
	];

	/**
	 * Creates a list of overrides from the given element node.
	 *
	 * @private
	 * @param  {Element} elem
	 * @return {Array.<Override>}
	 */
	function fromStyles(elem) {
		var overrides = [];
		Maps.forEach(stateToStyle, function (style, state) {
			var value = Dom.getStyle(elem, style[0]);
			if (value) {
				if (style[2]) {
					if (value === style[2]) {
						overrides.push([state, false]);
					} else if (value === style[1]) {
						overrides.push([state, true]);
					}
				} else {
					overrides.push([state, value === style[1]]);
				}
			}
		});
		return overrides;
	}

	/**
	 * Creates a list of overrides from the given node.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {Array.<Override>}
	 */
	function fromNode(node) {
		if (Dom.isTextNode(node)) {
			return [];
		}
		var state = nodeToState[node.nodeName];
		return (state ? [[state, true]] : []).concat(fromStyles(node));
	}

	/**
	 * Creates a list of overrides
	 *
	 * @private
	 * @param  {Node} node
	 * @return {Array.<Override>}
	 */
	function valuesFromNode(node) {
		if (Dom.isTextNode(node)) {
			return [];
		}
		return styles.reduce(function (values, style) {
			var value = Dom.getStyle(node, style);
			return value ? values.concat([[style, value]]) : values;
		}, []);
	}

	/**
	 * Creates a list of overrides from the given node and all ancestors until
	 * the given predicate or the editing host.
	 *
	 * @param  {Node}                   node
	 * @param  {function(Node):boolean} until
	 * @return {Array.<Override>}
	 * @memberOf overrides
	 */
	function harvest(node, until) {
		var nodes = Dom.childAndParentsUntil(node, until || Dom.isEditingHost);
		var stack = [];
		var map = {};
		var i = nodes.length;
		var j;
		var len;
		var states;
		var state;
		var index;
		while (i--) {
			states = fromNode(nodes[i]);
			for (j = 0, len = states.length; j < len; j++) {
				state = states[j];
				index = map[state[0]];
				if (Misc.defined(index)) {
					stack.splice(index - 1, 1, null);
				}
				map[state[0]] = stack.push(state);
			}
			stack = stack.concat(valuesFromNode(nodes[i]));
		}
		return stack.reduce(function (overrides, override) {
			return override ? overrides.concat([override]) : overrides;
		}, []);
	}

	/**
	 * Removes any node/formatting that corresponds to `state` at the given
	 * boundary.
	 *
	 * @private
	 * @param  {string}   state
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function purgeFormat(state, boundary) {
		var container = Boundaries.container(boundary);
		var nodes = Dom.childAndParentsUntil(container, Dom.isEditingHost);
		var nodeName = stateToNode[state];
		var style = stateToStyle[state];
		var styleName = style[0];
		var styleValue = style[1];
		var count = nodes.length;
		var limit;
		var node;
		while (count--) {
			node = nodes[count];
			if (Dom.isElementNode(node)
					&& (nodeName === node.nodeName
						|| Dom.getStyle(node, styleName) === styleValue)) {
				limit = node.parentNode;
				break;
			}
		}
		if (!limit) {
			return boundary;
		}
		var overrides = harvest(container, function (node) {
			return node == limit;
		}).reduce(function (list, override) {
			return state === override[0] ? list : list.concat([override]);
		}, []);
		boundary = Mutation.splitBoundaryUntil(boundary, function (boundary) {
			return Boundaries.container(boundary) === limit;
		});
		var boundaries = [boundary];
		var prevNode = Boundaries.prevNode(boundaries[0]);
		if (Html.isUnrendered(prevNode)) {
			boundaries = Mutation.removeNode(prevNode, boundaries);
		}
		var nextNode = Boundaries.nextNode(boundaries[0]);
		if (Html.isUnrendered(nextNode)) {
			boundaries = Mutation.removeNode(nextNode, boundaries);
		}
		return consume(boundaries[0], overrides);
	}

	/**
	 * Inserts a DOM nodes at the given boundary to reflect the list of
	 * overrides.
	 *
	 * @param  {Boundary}         boundary
	 * @param  {Array.<Override>} overrides
	 * @return {Boundary}
	 * @memberOf overrides
	 */
	function consume(boundary, overrides) {
		var doc = Boundaries.document(boundary);
		var node;
		Maps.forEach(Maps.mapTuples(overrides), function (value, state) {
			if (stateToNode[state]) {
				if (value) {
					var wrapper = doc.createElement(stateToNode[state]);
					if (node) {
						Dom.wrap(node, wrapper);
					} else {
						Mutation.insertNodeAtBoundary(wrapper, boundary);
						boundary = Boundaries.create(wrapper, 0);
					}
					node = wrapper;
				} else {
					boundary = purgeFormat(state, boundary);
				}
				return;
			}
			if (!node) {
				node = doc.createElement('span');
				Mutation.insertNodeAtBoundary(node, boundary);
				boundary = Boundaries.create(node, 0);
			}
			Dom.setStyle(node, state, value);
		});
		return boundary;
	}

	/**
	 * Returns the index of an override with the given command or state name in
	 * the given list of overrides.
	 *
	 * Returns -1 if override is not found.
	 *
	 * @param  {Array.<Override>} overrides
	 * @param  {string}           name
	 * @return {number}
	 * @memberOf overrides
	 */
	function indexOf(overrides, name) {
		for (var i = 0; i < overrides.length; i++) {
			if (name === overrides[i][0]) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Toggles the value of the override matching the given name from among the
	 * list of overrides.
	 *
	 * Returns a copy of overrides that represents the new toggle state/value.
	 *
	 * @param  {Array.<Override>} overrides
	 * @param  {string}           name
	 * @param  {string|boolean}   value
	 * @return {Array.<Override>}
	 * @memberOf overrides
	 */
	function toggle(overrides, name, value) {
		var index = indexOf(overrides, name);
		if (-1 === index) {
			return overrides.concat([[name, value]]);
		}
		var copy = overrides.concat();
		copy[index][1] = ('boolean' === typeof copy[index][1])
		               ? !copy[index][1]
		               : value;
		return copy;
	}

	function map(overrides) {
		var table = Maps.create();
		overrides.forEach(function (override) {
			table[override[0]] = override[1];
		});
		return table;
	}

	/**
	 * Returns a unique set from the given list of overrides.
	 *
	 * The last override of any given key (first element of tuple) in the list
	 * will be the value that is included in the resultant set.
	 *
	 * @param  {Array.<Override>} overrides
	 * @return {Array.<Override>}
	 * @memberOf overrides
	 */
	function unique(overrides) {
		var tuple;
		var set = [];
		var map = Maps.create();
		var count = overrides.length;
		while (count--) {
			tuple = overrides[count];
			if (!map[tuple[0]]) {
				map[tuple[0]] = true;
				set.push(tuple);
			}
		}
		return set.reverse();
	}

	/**
	 * Joins a variable list of overrides-lists into a single unique set.
	 *
	 * @param  {...Array.<Override>}
	 * @return {Array.<Override>}
	 * @memberOf overrides
	 */
	function joinToSet() {
		return unique(
			Array.prototype.concat.apply([], Arrays.coerce(arguments))
		);
	}

	return {
		map         : map,
		indexOf     : indexOf,
		unique      : unique,
		toggle      : toggle,
		consume     : consume,
		harvest     : harvest,
		joinToSet   : joinToSet,
		nodeToState : nodeToState,
		stateToNode : stateToNode
	};
});

/** editing.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @TODO formatStyle: in the following case the outer "font-family:
 * arial" span should be removed.  Can be done similar to how
 * findReusableAncestor() works.
 *
 * <pre>
 *	<span style="font-family: arial">
 *	   <span style="font-family: times">one</span>
 *	   <span style="font-family: helvetica">two<span>
 *	</span>
 * </pre>
 *
 * @TODO better handling of the last <br/> in a block and generally of
 *      unrendered whitespace.
 *      For example:
 *      formatting
 *      <p>{some<br/>text<br/>}</p>
 *      will result in
 *      <p>{<b>some<br/>text<br/></b>}</p>
 *      while it should probably be
 *      <p>{<b>some</br>text</b>}<br/></p>
 *
 * @namespace editing
 */
define('editing',[
	'dom',
	'mutation',
	'boundaries',
	'arrays',
	'maps',
	'strings',
	'functions',
	'html',
	'html/elements',
	'stable-range',
	'cursors',
	'content',
	'lists',
	'links',
	'overrides'
], function (
	Dom,
	Mutation,
	Boundaries,
	Arrays,
	Maps,
	Strings,
	Fn,
	Html,
	HtmlElements,
	StableRange,
	Cursors,
	Content,
	Lists,
	Links,
	Overrides
) {
	

	/**
	 * Walks the siblings of the given child, calling before for siblings before
	 * the given child, after for siblings after the given child, and at for the
	 * given child.
	 *
	 * @private
	 */
	function walkSiblings(parent, beforeAtAfterChild, before, at, after, arg) {
		var func = before;
		Dom.walk(parent.firstChild, function (child) {
			if (child !== beforeAtAfterChild) {
				func(child, arg);
			} else {
				func = after;
				at(child, arg);
			}
		});
	}

	/**
	 * Walks the siblings of each node in the given array (see
	 * walkSiblings()).
	 *
	 * @private
	 *
	 * @param ascendNodes from lowest descendant to topmost parent. The
	 * topmost parent and its siblings will not be walked over.
	 *
	 * @param atEnd indicates that the position to ascend from is not at
	 * ascendNodes[0], but at the end of ascendNodes[0] (meaning that
	 * all of ascendNodes[0]'s children will be walked over as well).
	 *
	 * @param carryDown is invoked on each node in the given array,
	 * allowing the carrying down of a context value. May return null
	 * to return the carryDown value from above.
	 */
	function ascendWalkSiblings(ascendNodes, atEnd, carryDown, before, at, after, arg) {
		var i;
		var args = [];
		i = ascendNodes.length;
		while (i--) {
			var cd = carryDown(ascendNodes[i], arg);
			if (null != cd) {
				arg = cd;
			}
			args.push(arg);
		}
		args.reverse();
		// Because with end positions like
		// <elem>text{</elem> or <elem>text}</elem>
		// ascendecending would start at <elem> ignoring "text".
		if (ascendNodes.length && atEnd) {
			Dom.walk(ascendNodes[0].firstChild, before, args[0]);
		}
		for (i = 0; i < ascendNodes.length - 1; i++) {
			var child = ascendNodes[i];
			var parent = ascendNodes[i + 1];
			walkSiblings(parent, child, before, at, after, args[i + 1]);
		}
	}

	function makePointNodeStep(pointNode, atEnd, stepOutsideInside, stepPartial) {
		// Because the start node is inside the range, the end node is
		// outside, and all ancestors of start and end are partially
		// inside/outside (for startAtEnd/endAtEnd positions the nodes are
		// also ancestors of the position).
		return function (node, arg) {
			if (node === pointNode && !atEnd) {
				stepOutsideInside(node, arg);
			} else {
				stepPartial(node, arg);
			}
		};
	}

	/**
	 * Walks the boundary of the range.
	 *
	 * The range's boundary starts at startContainer/startOffset, goes
	 * up to to the commonAncestorContainer's child above or equal
	 * startContainer/startOffset, continues to the
	 * commonAnestorContainer's child above or equal to
	 * endContainer/endOffset, and goes down again to
	 * endContainer/endOffset.
	 *
	 * Requires range's boundary points to be between nodes
	 * (Mutation.splitTextContainers).
	 *
	 * @private
	 */
	function walkBoundaryLeftRightInbetween(liveRange,
	                                        carryDown,
	                                        stepLeftStart,
	                                        stepRightStart,
	                                        stepLeftEnd,
	                                        stepRightEnd,
	                                        stepPartial,
	                                        stepInbetween,
	                                        arg) {
		// Because range may be mutated during traversal, we must only
		// refer to it before traversal.
		var cac = liveRange.commonAncestorContainer;
		if (Dom.isTextNode(cac)) {
			cac = cac.parentNode;
		}
		var sc         = liveRange.startContainer;
		var ec         = liveRange.endContainer;
		var so         = liveRange.startOffset;
		var eo         = liveRange.endOffset;
		var collapsed  = liveRange.collapsed;
		var start      = Dom.nodeAtOffset(sc, so);
		var end        = Dom.nodeAtOffset(ec, eo);
		var startAtEnd = Boundaries.isAtEnd(Boundaries.raw(sc, so));
		var endAtEnd   = Boundaries.isAtEnd(Boundaries.raw(ec, eo));

		var ascStart    = Dom.childAndParentsUntilNode(start, cac);
		var ascEnd      = Dom.childAndParentsUntilNode(end,   cac);
		var stepAtStart = makePointNodeStep(start, startAtEnd, stepRightStart, stepPartial);
		var stepAtEnd   = makePointNodeStep(end, endAtEnd, stepRightEnd, stepPartial);
		ascendWalkSiblings(ascStart, startAtEnd, carryDown, stepLeftStart, stepAtStart, stepRightStart, arg);
		ascendWalkSiblings(ascEnd, endAtEnd, carryDown, stepLeftEnd, stepAtEnd, stepRightEnd, arg);
		var cacChildStart = Arrays.last(ascStart);
		var cacChildEnd   = Arrays.last(ascEnd);
		stepAtStart = makePointNodeStep(start, startAtEnd, stepInbetween, stepPartial);
		Dom.walkUntilNode(cac.firstChild, stepLeftStart, cacChildStart, arg);
		if (cacChildStart) {
			var next = cacChildStart.nextSibling;
			if (cacChildStart === cacChildEnd) {
				if (!collapsed) {
					stepPartial(cacChildStart, arg);
				}
			} else {
				stepAtStart(cacChildStart, arg);
				Dom.walkUntilNode(next, stepInbetween, cacChildEnd, arg);
				if (cacChildEnd) {
					next = cacChildEnd.nextSibling;
					stepAtEnd(cacChildEnd, arg);
				}
			}
			if (cacChildEnd) {
				Dom.walk(next, stepRightEnd, arg);
			}
		}
	}

	/**
	 * Simplifies walkBoundaryLeftRightInbetween from left/right/inbetween to just inside/outside.
	 *
	 * Requires range's boundary points to be between nodes
	 * (Mutation.splitTextContainers).
	 */
	function walkBoundaryInsideOutside(liveRange,
	                                   carryDown,
	                                   stepOutside,
	                                   stepPartial,
	                                   stepInside,
	                                   arg) {
		walkBoundaryLeftRightInbetween(
			liveRange,
			carryDown,
			stepOutside,
			stepInside,
			stepInside,
			stepOutside,
			stepPartial,
			stepInside,
			arg
		);
	}

	/**
	 * Pushes down an implied context above or at pushDownFrom to the
	 * given range by clearing all overrides from pushDownFrom
	 * (inclusive) to range.commonAncestorContainer, and clearing all
	 * overrides inside and along the range's boundary (see
	 * walkBoundaryInsideOutside()), invoking pushDownOverride on all
	 * siblings of the range boundary that are not contained in it.
	 *
	 * Requires range's boundary points to be between nodes
	 * (Mutation.splitTextContainers).
	 */
	function pushDownContext(liveRange,
	                         pushDownFrom,
	                         cacOverride,
	                         getOverride,
	                         clearOverride,
	                         clearOverrideRec,
	                         pushDownOverride) {
		// Because range may be mutated during traversal, we must only
		// refer to it before traversal.
		var cac = liveRange.commonAncestorContainer;
		walkBoundaryInsideOutside(
			liveRange,
			getOverride,
			pushDownOverride,
			clearOverride,
			clearOverrideRec,
			cacOverride
		);
		var fromCacToTop = Dom.childAndParentsUntilInclNode(
			cac,
			pushDownFrom
		);
		ascendWalkSiblings(
			fromCacToTop,
			false,
			getOverride,
			pushDownOverride,
			clearOverride,
			pushDownOverride,
			null
		);
		clearOverride(pushDownFrom);
	}

	function findReusableAncestor(range,
	                              hasContext,
	                              getOverride,
	                              isUpperBoundary,
	                              isReusable,
	                              isObstruction) {
		var obstruction = null;
		function beforeAfter(node) {
			obstruction = (obstruction
						   || (!Html.isUnrenderedWhitespace(node)
							   && !hasContext(node)));
		}
		walkBoundaryInsideOutside(range, Fn.noop, beforeAfter, Fn.noop, Fn.noop);
		if (obstruction) {
			return null;
		}
		var cac = range.commonAncestorContainer;
		if (Dom.isTextNode(cac)) {
			cac = cac.parentNode;
		}
		function untilIncl(node) {
			// Because we prefer a node above the cac if possible.
			return (cac !== node && isReusable(node)) || isUpperBoundary(node) || isObstruction(node);
		}
		var cacToReusable = Dom.childAndParentsUntilIncl(cac, untilIncl);
		var reusable = Arrays.last(cacToReusable);
		if (!isReusable(reusable)) {
			// Because, although we preferred a node above the cac, we
			// fall back to the cac.
			return isReusable(cac) ? cac : null;
		}
		ascendWalkSiblings(cacToReusable, false, Fn.noop, beforeAfter, Fn.noop, beforeAfter);
		if (obstruction) {
			return isReusable(cac) ? cac : null;
		}
		return reusable;
	}

	/**
	 * Walks around the boundary of range and invokes the given
	 * functions with the nodes it encounters.
	 *
	 * clearOverride    - invoked for partially contained nodes.
	 * clearOverrideRec - invoked for top-level contained nodes.
	 * pushDownOverride - invoked for left siblings of ancestors
	 *   of startContainer[startOffset], and for right siblings of
	 *   ancestors of endContainer[endOffset].
	 * setContext       - invoked for top-level contained nodes.
	 *
	 * The purpose of the walk is to either push-down or set a context
	 * on all nodes within the range, and push-down any overrides that
	 * exist along the bounderies of the range.
	 *
	 * An override is a context that overrides the context to set.
	 *
	 * Pushing-down a context means that an existing context-giving
	 * ancestor element will be reused, if available, and setContext()
	 * will not be invoked.
	 *
	 * Pushing-down an override means that ancestors of the range's
	 * start or end containers will have their overrides cleared and the
	 * subset of the ancestors' children that is not contained by the
	 * range will have the override applied via pushDownOverride().
	 *
	 * This algorithm will not by itself mutate anything, or depend on
	 * any mutations by the given functions.
	 *
	 * clearOverride, clearOverideRec, setContext, pushDownContext may
	 * mutate the given node and it's previous siblings, and may insert
	 * nextSiblings, but must not mutate the next sibling of the given
	 * node, and must return the nextSibling of the given node (the
	 * nextSibling before any mutations).
	 *
	 * When setContext is invoked with hasOverrideAncestor, it is for
	 * example when a bold element is at the same time the upper
	 * boundary (for example when the bold element itself is the editing
	 * host) and an attempt is made to set a non-bold context inside the
	 * bold element. To work around this, setContext() could force a
	 * non-bold context by wrapping the node with a <span
	 * style="font-weight: normal">. See hasOverrideAncestor below.
	 *
	 * @param liveRange range's boundary points should be between nodes
	 * (Mutation.splitTextContainers).
	 *
	 * @param formatter a map with the following properties
	 *   isUpperBoundary(node) - identifies exclusive upper
	 *   boundary element, only elements below which will be modified.
	 *
	 *   getOverride(node) - returns a node's override, or null/undefined
	 *   if the node does not provide an override. The topmost node for
	 *   which getOverride returns a non-null value is the topmost
	 *   override. If there is a topmost override, and it is below the
	 *   upper boundary element, it will be cleared and pushed down.
	 *   Should return a non-null value for any node for which
	 *   hasContext(node) returns true.
	 *
	 *   clearOverride(node) - should clear the given node of an
	 *   override. The given node may or may not have an override
	 *   set. Will be invoked shallowly for all ancestors of start and end
	 *   containers (up to isUpperBoundary or hasContext). May perform
	 *   mutations as explained above.
	 *
	 *   clearOverrideRec(node) - like clearOverride but should clear
	 *   the override recursively. If not provided, clearOverride will
	 *   be applied recursively.
	 *
	 *   pushDownOverride(node, override) - applies the given
	 *   override to node. Should check whether the given node doesn't
	 *   already provide its own override, in which case the given
	 *   override should not be applied. May perform mutations as
	 *   explained above.
	 *
	 *   hasContext(node) - returns true if the given node
	 *   already provides the context to set.
	 *
	 *   setContext(node, override, hasOverrideAncestor) - applies the context
	 *   to the given node. Should clear overrides recursively. Should
	 *   also clear context recursively to avoid unnecessarily nested
	 *   contexts. hasOverrideAncestor is true if an override is in effect
	 *   above the given node (see explanation above). May perform
	 *   mutations as explained above.
	 */
	function mutate(liveRange, formatter) {
		// Because range may be mutated during traversal, we must only
		// refer to it before traversal.
		var cac = liveRange.commonAncestorContainer;
		var isUpperBoundary = formatter.isUpperBoundary;
		var getOverride = formatter.getOverride;
		var getInheritableOverride = formatter.getInheritableOverride;
		var pushDownOverride = formatter.pushDownOverride;
		var hasContext = formatter.hasContext;
		var hasInheritableContext = formatter.hasInheritableContext;
		var setContext = formatter.setContext;
		var clearOverride = formatter.clearOverride;
		var isObstruction = formatter.isObstruction;
		var isReusable = formatter.isReusable;
		var isContextOverride = formatter.isContextOverride;
		var isClearable = formatter.isClearable;
		var clearOverrideRec = formatter.clearOverrideRec  || function (node) {
			Dom.walkRec(node, clearOverride);
		};
		var topmostOverrideNode = null;
		var cacOverride = null;
		var isNonClearableOverride = false;
		var upperBoundaryAndAbove = false;
		var fromCacToContext = Dom.childAndParentsUntilIncl(
			cac,
			function (node) {
				// Because we shouldn't expect hasContext to handle the document
				// element (which has nodeType 9).
				return (
					!node.parentNode
						|| Dom.Nodes.DOCUMENT === node.parentNode.nodeType
							|| hasInheritableContext(node)
				);
			}
		);
		fromCacToContext.forEach(function (node) {
			upperBoundaryAndAbove = upperBoundaryAndAbove || isUpperBoundary(node);
			// Because we are only interested in non-context overrides.
			var override = getInheritableOverride(node);
			if (null != override && !isContextOverride(override)) {
				topmostOverrideNode = node;
				isNonClearableOverride = isNonClearableOverride
				                      || upperBoundaryAndAbove
				                      || !isClearable(node);
				if (null == cacOverride) {
					cacOverride = override;
				}
			}
		});
		if (null == cacOverride) {
			cacOverride = getInheritableOverride(cac);
		}

		if (hasInheritableContext(Arrays.last(fromCacToContext)) && !isNonClearableOverride) {
			if (!topmostOverrideNode) {
				// Because, if there is no override in the way, we only
				// need to clear the overrides contained in the range.
				walkBoundaryInsideOutside(
					liveRange,
					getOverride,
					pushDownOverride,
					clearOverride,
					clearOverrideRec
				);
			} else {
				var pushDownFrom = topmostOverrideNode;
				pushDownContext(
					liveRange,
					pushDownFrom,
					cacOverride,
					getOverride,
					clearOverride,
					clearOverrideRec,
					pushDownOverride
				);
			}
		} else {
			var mySetContext = function (node, override) {
				setContext(node, override, isNonClearableOverride);
			};
			var reusableAncestor = findReusableAncestor(
				liveRange,
				hasContext,
				getOverride,
				isUpperBoundary,
				isReusable,
				isObstruction
			);
			if (reusableAncestor) {
				mySetContext(reusableAncestor);
			} else {
				walkBoundaryInsideOutside(
					liveRange,
					getOverride,
					pushDownOverride,
					clearOverride,
					mySetContext
				);
			}
		}
	}

	function adjustPointWrap(point, node, wrapper) {
		// Because we prefer the range to be outside the wrapper (no
		// particular reason though).
		if (point.node === node && !point.atEnd) {
			point.node = wrapper;
		}
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf editing
	 */
	function wrap(node, wrapper, leftPoint, rightPoint) {
		if (!Content.allowsNesting(wrapper.nodeName, node.nodeName)) {
			return false;
		}
		if (wrapper.parentNode) {
			Mutation.removeShallowPreservingCursors(wrapper, [leftPoint, rightPoint]);
		}
		adjustPointWrap(leftPoint, node, wrapper);
		adjustPointWrap(rightPoint, node, wrapper);
		Dom.wrap(node, wrapper);
		return true;
	}

	// NB: depends on fixupRange to use trimClosingOpening() to move the
	// leftPoint out of an cursor.atEnd position to the first node that is to be
	// moved.
	function moveBackIntoWrapper(node, ref, atEnd, leftPoint, rightPoint) {
		// Because the points will just be moved with the node, we don't need to
		// do any special preservation.
		Dom.insert(node, ref, atEnd);
	}

	/**
	 * TODO documentation
	 *
	 * @param  {!Range}   liveRange
	 * @param  {function} mutate
	 * @param  {function} trim
	 * @return {Array.<Boundary>}
	 */
	function fixupRange(liveRange, mutate, trim) {
		// Because we are mutating the range several times and don't want the
		// caller to see the in-between updates, and because we are using
		// Ranges.trim() below to adjust the range's boundary points, which we
		// don't want the browser to re-adjust (which some browsers do).
		var range = StableRange(liveRange);

		// Because making the assumption that boundary points are between nodes
		// makes the algorithms generally a bit simpler.
		Mutation.splitTextContainers(range);

		var splitStart = Cursors.cursorFromBoundaryPoint(
			range.startContainer,
			range.startOffset
		);

		var splitEnd = Cursors.cursorFromBoundaryPoint(
			range.endContainer,
			range.endOffset
		);

		// Because we want unbolding
		// <b>one<i>two{</i>three}</b>
		// to result in
		// <b>one<i>two</i></b>three
		// and not in
		// <b>one</b><i><b>two</b></i>three
		// even though that would be cleaned up in the restacking pass
		// afterwards.
		// Also, because moveBackIntoWrapper() requires the
		// left boundary point to be next to a non-ignorable node.
		if (false !== trim) {
			trimClosingOpening(
				range,
				Html.isUnrenderedWhitespace,
				Html.isUnrenderedWhitespace
			);
		}

		// Because mutation needs to keep track and adjust boundary points so we
		// can preserve the range.
		var leftPoint = Cursors.cursorFromBoundaryPoint(
			range.startContainer,
			range.startOffset
		);

		var rightPoint = Cursors.cursorFromBoundaryPoint(
			range.endContainer,
			range.endOffset
		);

		var formatter = mutate(range, leftPoint, rightPoint);
		if (formatter) {
			formatter.postprocess();
		}

		Cursors.setToRange(range, leftPoint, rightPoint);

		// Because we want to ensure that this algorithm doesn't
		// introduce any additional splits between text nodes.
		Mutation.joinTextNodeAdjustRange(splitStart.node, range);
		Mutation.joinTextNodeAdjustRange(splitEnd.node, range);

		if (formatter) {
			formatter.postprocessTextNodes(range);
		}

		var boundaries = Boundaries.fromRange(range);
		Boundaries.setRange(liveRange, boundaries[0], boundaries[1]);
		return boundaries;
	}

	function restackRec(node, hasContext, ignoreHorizontal, ignoreVertical) {
		if (!Dom.isElementNode(node) || !ignoreVertical(node)) {
			return null;
		}
		var maybeContext = Dom.nextWhile(node.firstChild, ignoreHorizontal);
		if (!maybeContext) {
			return null;
		}
		var notIgnorable = Dom.nextWhile(maybeContext.nextSibling, ignoreHorizontal);
		if (notIgnorable) {
			return null;
		}
		if (hasContext(maybeContext)) {
			return maybeContext;
		}
		return restackRec(maybeContext, hasContext, ignoreHorizontal, ignoreVertical);
	}

	function restack(node, hasContext, ignoreHorizontal, ignoreVertical, leftPoint, rightPoint) {
		function myIgnoreHorizontal(node) {
			return !hasContext(node) && ignoreHorizontal(node);
		}
		if (hasContext(node)) {
			return node;
		}
		var context = restackRec(node, hasContext, myIgnoreHorizontal, ignoreVertical);
		if (!context) {
			return null;
		}
		if (!wrap(node, context, leftPoint, rightPoint)) {
			return null;
		}
		return context;
	}

	function ensureWrapper(node,
	                       createWrapper,
	                       isWrapper,
	                       isMergable,
	                       pruneContext,
	                       addContextValue,
	                       leftPoint,
	                       rightPoint) {
		var sibling = node.previousSibling;
		if (sibling && isMergable(sibling) && isMergable(node)) {
			moveBackIntoWrapper(node, sibling, true, leftPoint, rightPoint);
			// Because the node itself may be a wrapper.
			pruneContext(node);
		} else if (!isWrapper(node)) {
			var wrapper = createWrapper(node.ownerDocument);
			if (wrap(node, wrapper, leftPoint, rightPoint)) {
				// Because we are just making sure (probably not
				// necessary since the node isn't a wrapper).
				pruneContext(node);
			} else {
				// Because if wrapping is not successful, we try again
				// one level down.
				Dom.walk(node.firstChild, function (node) {
					ensureWrapper(
						node,
						createWrapper,
						isWrapper,
						isMergable,
						pruneContext,
						addContextValue,
						leftPoint,
						rightPoint
					);
				});
			}
		} else {
			// Because the node itself is a wrapper, but possibly not
			// with the given context value.
			addContextValue(node);
		}
	}

	function makeFormatter(contextValue, leftPoint, rightPoint, impl) {
		var hasContext = impl.hasContext;
		var isContextOverride = impl.isContextOverride;
		var hasSomeContextValue = impl.hasSomeContextValue;
		var hasContextValue = impl.hasContextValue;
		var addContextValue = impl.addContextValue;
		var removeContext = impl.removeContext;
		var createWrapper = impl.createWrapper;
		var isReusable = impl.isReusable;
		var isPrunable = impl.isPrunable;

		// Because we want to optimize reuse, we remembering any wrappers we created.
		var wrappersByContextValue = {};
		var wrappersWithContextValue = [];
		var removedNodeSiblings = [];

		function pruneContext(node) {
			if (!hasSomeContextValue(node)) {
				return;
			}
			removeContext(node);
			// TODO if the node is not prunable but overrides the
			// context (for example <b class="..."></b> may not be
			// prunable), we should descend into the node and set the
			// unformatting-context inside.
			if (!isPrunable(node)) {
				return;
			}
			if (node.previousSibling) {
				removedNodeSiblings.push(node.previousSibling);
			}
			if (node.nextSibling) {
				removedNodeSiblings.push(node.nextSibling);
			}
			Mutation.removeShallowPreservingCursors(node, [leftPoint, rightPoint]);
		}

		function createContextWrapper(value, doc) {
			var wrapper = createWrapper(value, doc);
			var key = ':' + value;
			var wrappers = wrappersByContextValue[key] = wrappersByContextValue[key] || [];
			wrappers.push(wrapper);
			wrappersWithContextValue.push([wrapper, value]);
			return wrapper;
		}

		function isClearable(node) {
			var clone = node.cloneNode(false);
			removeContext(clone);
			return isPrunable(clone);
		}

		function isMergableWrapper(value, node) {
			if (!isReusable(node)) {
				return false;
			}
			var key =  ':' + value;
			var wrappers = wrappersByContextValue[key] || [];
			if (Arrays.contains(wrappers, node)) {
				return true;
			}
			if (hasSomeContextValue(node) && !hasContextValue(node, value)) {
				return false;
			}
			// Because we assume something is mergeable if it doesn't
			// provide any context value besides the one we are
			// applying, and something doesn't provide any context value
			// at all if it is prunable.
			return isClearable(node);
		}

		function wrapContextValue(node, value) {
			ensureWrapper(
				node,
				Fn.partial(createContextWrapper, value),
				isReusable,
				Fn.partial(isMergableWrapper, value),
				pruneContext,
				Fn.partial(addContextValue, value),
				leftPoint,
				rightPoint
			);
		}

		function clearOverride(node) {
			// Because we don't want to remove any existing context if
			// not necessary (See pushDownOverride and setContext).
			if (!hasContext(node)) {
				pruneContext(node);
			}
		}

		function clearOverrideRecStep(node) {
			// Different from clearOverride because clearOverride() only
			// clears context overrides, while during a recursive
			// clearing we want to clear the override always regardless
			// of whether it is equal to context.
			pruneContext(node);
		}

		function clearOverrideRec(node) {
			Dom.walkRec(node, clearOverrideRecStep);
		}

		function pushDownOverride(node, override) {
			// Because we don't clear any context overrides, we don't
			// need to push them down either.
			if (null == override || hasSomeContextValue(node) || isContextOverride(override)) {
				return;
			}
			wrapContextValue(node, override);
		}

		function setContext(node, override, isNonClearableOverride) {
			// Because we don't clear any context overrides, we don't
			// need to set them either.
			if (isContextOverride(override)) {
				return;
			}
			Dom.walk(node.firstChild, clearOverrideRec);
			wrapContextValue(node, contextValue);
		}

		function restackMergeWrapper(wrapper, contextValue, mergeNext) {
			var sibling = mergeNext ? wrapper.nextSibling : wrapper.previousSibling;
			if (!sibling) {
				return;
			}
			function isGivenContextValue(node) {
				return hasContextValue(node, contextValue);
			}
			sibling = restack(
				sibling,
				isGivenContextValue,
				Html.isUnrenderedWhitespace,
				Html.hasInlineStyle,
				leftPoint,
				rightPoint
			);
			if (!sibling) {
				return;
			}
			var isMergable = Fn.partial(isMergableWrapper, contextValue);
			var createWrapper = Fn.partial(createContextWrapper, contextValue);
			var addValue = Fn.partial(addContextValue, contextValue);
			var mergeNode = mergeNext ? sibling : wrapper;
			ensureWrapper(
				mergeNode,
				createWrapper,
				isReusable,
				isMergable,
				pruneContext,
				addValue,
				leftPoint,
				rightPoint
			);
		}

		function mergeWrapper(wrapper, contextValue) {
			restackMergeWrapper(wrapper, contextValue, true);
			restackMergeWrapper(wrapper, contextValue, false);
		}

		function postprocess() {
			wrappersWithContextValue.forEach(function (wrapperAndContextValue) {
				mergeWrapper(wrapperAndContextValue[0], wrapperAndContextValue[1]);
			});
		}

		function postprocessTextNodes(range) {
			removedNodeSiblings.forEach(function (node) {
				Mutation.joinTextNodeAdjustRange(node, range);
			});
		}

		return {
			hasContext: hasContext,
			isReusable: isReusable,
			clearOverride: clearOverride,
			isClearable: isClearable,
			pushDownOverride: pushDownOverride,
			setContext: setContext,
			isContextOverride: isContextOverride,
			postprocess: postprocess,
			postprocessTextNodes: postprocessTextNodes,
			hasInheritableContext: impl.hasInheritableContext,
			isObstruction: impl.isObstruction,
			getOverride: impl.getOverride,
			getInheritableOverride: impl.getInheritableOverride,
			isUpperBoundary: impl.isUpperBoundary
		};
	}

	function isUpperBoundary_default(node) {
		// Because the body element is an obvious upper boundary, and
		// because, if we are inside a block element, we shouldn't touch
		// it as that causes changes in the layout, and because, when we
		// are inside an editable, we shouldn't make modifications
		// outside of it (if we are not inside an editable, we don't
		// care).
		return 'BODY' === node.nodeName || Html.hasBlockStyle(node) || Dom.isEditingHost(node);
	}

	function isStyleEqual_default(styleValueA, styleValueB) {
		return styleValueA === styleValueB;
	}

	var inlineWrapperProperties = {
		underline: {
			name: 'U',
			nodes: ['U'],
			style: 'text-decoration',
			value: 'underline',
			normal: 'none',
			normalize: {}
		},
		bold: {
			name: 'B',
			nodes: ['B', 'STRONG'],
			style: 'font-weight',
			value: 'bold',
			normal: 'normal',
			normalize: {
				/* ie7/ie8 only */
				'700': 'bold',
				'400': 'normal'
			}
		},
		italic: {
			name: 'I',
			nodes: ['I', 'EM'],
			style: 'font-style',
			value: 'italic',
			normal: 'normal',
			normalize: {}
		}
	};
	inlineWrapperProperties['emphasis']  = Maps.merge(inlineWrapperProperties.italic, {name: 'EM'});
	inlineWrapperProperties['strong']    = Maps.merge(inlineWrapperProperties.bold, {name: 'STRONG'});
	inlineWrapperProperties['bold']      = inlineWrapperProperties.bold;
	inlineWrapperProperties['italic']    = inlineWrapperProperties.italic;
	inlineWrapperProperties['underline'] = inlineWrapperProperties.underline;

	function getStyleSafely(node, name) {
		return (Dom.isElementNode(node)
		        ? Dom.getStyle(node, name)
		        : null);
	}

	function makeStyleFormatter(styleName, styleValue, leftPoint, rightPoint, opts) {
		var isStyleEqual = opts.isStyleEqual || isStyleEqual_default;
		var nodeNames = [];
		var unformat = false;
		var wrapperProps = inlineWrapperProperties[styleName];
		if (wrapperProps) {
			nodeNames = wrapperProps.nodes;
			styleName = wrapperProps.style;
			unformat = !styleValue;
			styleValue = unformat ? wrapperProps.normal : wrapperProps.value;
		}
		function normalizeStyleValue(value) {
			if (wrapperProps && wrapperProps.normalize[value]) {
				value = wrapperProps.normalize[value];
			}
			return value;
		}
		function getOverride(node) {
			if (Arrays.contains(nodeNames, node.nodeName)) {
				return wrapperProps.value;
			}
			var override = getStyleSafely(node, styleName);
			return !Strings.isEmpty(override) ? override : null;
		}
		function getInheritableOverride(node) {
			if (Arrays.contains(nodeNames, node.nodeName)) {
				return wrapperProps.value;
			}
			var override = Dom.getComputedStyle(node, styleName);
			return !Strings.isEmpty(override) ? override : null;
		}
		function isContextStyle(value) {
			return isStyleEqual(normalizeStyleValue(value), styleValue);
		}
		function isContextOverride(value) {
			return isContextStyle(value);
		}
		function hasSomeContextValue(node) {
			if (Arrays.contains(nodeNames, node.nodeName)) {
				return true;
			}
			return !Strings.isEmpty(getStyleSafely(node, styleName));
		}
		function hasContextValue(node, value) {
			value = normalizeStyleValue(value);
			if (Arrays.contains(nodeNames, node.nodeName) && isStyleEqual(wrapperProps.value, value)) {
				return true;
			}
			return isStyleEqual(getStyleSafely(node, styleName), value);
		}
		function hasContext(node) {
			if (!unformat && Arrays.contains(nodeNames, node.nodeName)) {
				return true;
			}
			return isContextStyle(getStyleSafely(node, styleName));
		}
		function hasInheritableContext(node) {
			if (!unformat && Arrays.contains(nodeNames, node.nodeName)) {
				return true;
			}
			if (unformat && 'BODY' === node.nodeName) {
				return true;
			}
			// Because default values of not-inherited styles don't
			// provide any context.
			// TODO This causes any classes that set a non-inherited
			// style to the default value, for example
			// "text-decoration: none" to be ignored.
			if (unformat && Html.isStyleInherited(styleName)) {
				return isContextStyle(getStyleSafely(node, styleName));
			}
			return isContextStyle(Dom.getComputedStyle(node, styleName));
		}
		function addContextValue(value, node) {
			value = normalizeStyleValue(value);
			if (Arrays.contains(nodeNames, node.nodeName) && isStyleEqual(wrapperProps.value, value)) {
				return;
			}
			// Because we don't want to add an explicit style if for
			// example the element already has a class set on it. For
			// example: <span class="bold"></span>.
			if (isStyleEqual(normalizeStyleValue(Dom.getComputedStyle(node, styleName)), value)) {
				return;
			}
			Dom.setStyle(node, styleName, value);
		}
		function removeContext(node) {
			Dom.removeStyle(node, styleName);
		}
		function isReusable(node) {
			if (Arrays.contains(nodeNames, node.nodeName)) {
				return true;
			}
			return 'SPAN' === node.nodeName;
		}
		function isPrunable(node) {
			return isReusable(node) && !Dom.hasAttrs(node);
		}
		function createWrapper(value, doc) {
			value = normalizeStyleValue(value);
			if (wrapperProps && isStyleEqual(wrapperProps.value, value)) {
				return doc.createElement(wrapperProps.name);
			}
			var wrapper = doc.createElement('SPAN');
			Dom.setStyle(wrapper, styleName, value);
			return wrapper;
		}
		var impl = Maps.merge({
			getOverride: getOverride,
			getInheritableOverride: getInheritableOverride,
			hasContext: hasContext,
			hasInheritableContext: hasInheritableContext,
			isContextOverride: isContextOverride,
			hasSomeContextValue: hasSomeContextValue,
			hasContextValue: hasContextValue,
			addContextValue: addContextValue,
			removeContext: removeContext,
			isPrunable: isPrunable,
			isStyleEqual: isStyleEqual,
			createWrapper: createWrapper,
			isReusable: isReusable,
			isObstruction: Fn.complement(Html.hasInlineStyle),
			isUpperBoundary: isUpperBoundary_default
		}, opts);
		return makeFormatter(styleValue, leftPoint, rightPoint, impl);
	}

	function makeElemFormatter(nodeName, unformat, leftPoint, rightPoint, opts) {
		// Because we assume nodeNames are always uppercase, but don't
		// want the user to remember this detail.
		nodeName = nodeName.toUpperCase();
		function createWrapper(wrapper, doc) {
			return doc.createElement(nodeName);
		}
		function getOverride(node) {
			return nodeName === node.nodeName || null;
		}
		function hasContext(node) {
			if (unformat) {
				// Because unformatting has no context value.
				return false;
			}
			return nodeName === node.nodeName;
		}
		function hasInheritableContext(node) {
			// Because there can be no nodes above the body element that
			// can provide a context.
			if (unformat && 'BODY' === node.nodeName) {
				return true;
			}
			return hasContext(node);
		}
		function isContextOverride(value) {
			if (unformat) {
				// Because unformatting has no context value.
				return false;
			}
			return null != value;
		}
		function isReusable(node) {
			// Because we don't want to merge with a context node that
			// does more than just provide a context (for example a <b>
			// node may have a class which shouldn't also being wrapped
			// around the merged-with node).
			return node.nodeName === nodeName && !Dom.hasAttrs(node);
		}
		function isPrunable(node) {
			return isReusable(node);
		}
		function hasSomeContextValue(node) {
			return node.nodeName === nodeName;
		}
		var impl = Maps.merge({
			getOverride: getOverride,
			// Because inheritable overrides are only useful for
			// formatters that consider the CSS style.
			getInheritableOverride: getOverride,
			hasContext: hasContext,
			hasInheritableContext: hasInheritableContext,
			isContextOverride: isContextOverride,
			hasSomeContextValue: hasSomeContextValue,
			// Because hasContextValue and hasSomeContextValue makes no
			// difference for an element formatter, since there is only one
			// context value.
			hasContextValue: hasSomeContextValue,
			addContextValue: Fn.noop,
			removeContext: Fn.noop,
			createWrapper: createWrapper,
			isReusable: isReusable,
			isPrunable: isPrunable,
			isObstruction: Fn.complement(Html.hasInlineStyle),
			isUpperBoundary: isUpperBoundary_default
		}, opts);
		return makeFormatter(nodeName, leftPoint, rightPoint, impl);
	}

	/**
	 * Ensures the given range is wrapped by elements with a given nodeName.
	 *
	 * @param {string}    nodeName
	 * @param {!Boundary} start
	 * @param {!Boundary} end
	 * @param {boolean} remove Optional flag, which when set to false will cause
	 *                         the given markup to be removed (unwrapped) rather
	 *                         then set.
	 * @param {?Object} opts A map of options (all optional):
	 *        createWrapper - a function that returns a new empty
	 *        wrapper node to use.
	 *
	 *        isReusable - a function that returns true if a given node,
	 *        already in the DOM at the correct place, can be reused
	 *        instead of creating a new wrapper node. May be merged with
	 *        other reusable or newly created wrapper nodes.
	 * @return {Array.<Boundary>} updated boundaries
	 */
	function wrapElem(nodeName, start, end, remove, opts) {
		opts = opts || {};

		var liveRange = Boundaries.range(start, end);

		// Because we should avoid splitTextContainers() if this call is a noop.
		if (liveRange.collapsed) {
			return [start, end];
		}

		return fixupRange(liveRange, function (range, leftPoint, rightPoint) {
			var formatter = makeElemFormatter(nodeName, remove, leftPoint, rightPoint, opts);
			mutate(range, formatter);
			return formatter;
		});
	}

	/**
	 * Ensures the contents between start and end are wrapped by elements 
	 * that have a given CSS style set. Returns the updated boundaries.
	 *
	 * @param styleName a CSS style name
	 *        Please note that not-inherited styles currently may (or
	 *        may not) cause undesirable results.  See also
	 *        Html.isStyleInherited().
	 *
	 *        The underline style can't be unformatted inside a
	 *        non-clearable ancestor ("text-decoration: none" doesn't do
	 *        anything as the underline will be drawn by the ancestor).
	 *
	 * @param opts all options supported by wrapElem() as well as the following:
	 *        createWrapper - a function that takes a style value and
	 *        returns a new empty wrapper node that has the style value
	 *        applied.
	 *
	 *        isPrunable - a function that returns true if a given node,
	 *        after some style was removed from it, can be removed
	 *        entirely. That's usually the case if the given node is
	 *        equivalent to an empty wrapper.
	 *
	 *        isStyleEqual - a function that returns true if two given
	 *        style values are equal.
	 *        TODO currently we just use strict equals by default, but
	 *             we should implement for each supported style it's own
	 *             equals function.
	 * @return {Array.<Boundary>}
	 * @memberOf editing
	 */
	function style(start, end, name, value, opts) {
		var range = Boundaries.range(start, end);
		// Because we should avoid splitTextContainers() if this call is a noop.
		if (range.collapsed) {
			return [start, end];
		}
		return fixupRange(range, function (range, leftPoint, rightPoint) {
			var formatter = makeStyleFormatter(name, value, leftPoint, rightPoint, opts || {});
			mutate(range, formatter);
			return formatter;
		});
	}

	/**
	 * Applies inline formatting to contents enclosed by start and end boundary.
	 * Will return updated array of boundaries after the operation.
	 *
	 * @private
	 * @param  {string}    node
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @param  {boolean}   isWrapping
	 * @return {Array.<Boundary>}
	 */
	function formatInline(node, start, end, isWrapping) {
		var styleName = resolveStyleName(node);
		var boundaries = (styleName === false)
		               ? [start, end]
		               : style(start, end, styleName, isWrapping);
		if (Boundaries.equals(boundaries[0], boundaries[1])) {
			return boundaries;
		}
		var next = Boundaries.nodeAfter(boundaries[0]);
		var prev = Boundaries.nodeBefore(boundaries[1]);
		start = next
		      ? Boundaries.normalize(Boundaries.fromStartOfNode(next))
		      : boundaries[0];
		end = prev
		    ? Boundaries.normalize(Boundaries.fromEndOfNode(prev))
		    : boundaries[1];
		return [start, end];
	}

	/**
	 * Resolves the according CSS style name for an uppercase (!) node name
	 * passed in styleNode. Will return the CSS name of the style (eg. 'bold') 
	 * or false.
	 * So 'B' will eg. be resolved to 'bold'
	 *
	 * @param  {string} styleNode
	 * @return {string|false}
	 */
	function resolveStyleName(styleNode) {
		for (var styleName in inlineWrapperProperties) {
			if (inlineWrapperProperties[styleName].nodes.indexOf(styleNode) !== -1) {
				return styleName;
			}
		}
		return false;
	}

	/**
	 * Ensures that the given start point Cursor is not at a "start position"
	 * and the given end point Cursor is not at an "end position" by moving the
	 * points to the left and right respectively.  This is effectively the
	 * opposite of trimBoundaries().
	 *
	 * @param {Cusor} start
	 * @param {Cusor} end
	 * @param {function():boolean} until
	 *        Optional predicate.  May be used to stop the trimming process from
	 *        moving the Cursor from within an element outside of it.
	 * @param {function():boolean} ignore
	 *        Optional predicate.  May be used to ignore (skip)
	 *        following/preceding siblings which otherwise would stop the
	 *        trimming process, like for example underendered whitespace.
	 */
	function expandBoundaries(start, end, until, ignore) {
		until = until || Fn.returnFalse;
		ignore = ignore || Fn.returnFalse;
		start.prevWhile(function (start) {
			var prevSibling = start.prevSibling();
			return prevSibling ? ignore(prevSibling) : !until(start.parent());
		});
		end.nextWhile(function (end) {
			return !end.atEnd ? ignore(end.node) : !until(end.parent());
		});
	}

	/**
	 * Ensures that the given start point Cursor is not at an "start position"
	 * and the given end point Cursor is not at an "end position" by moving the
	 * points to the left and right respectively.  This is effectively the
	 * opposite of expandBoundaries().
	 *
	 * If the boundaries are equal (collapsed), or become equal during this
	 * operation, or if until() returns true for either point, they may remain
	 * in start and end position respectively.
	 *
	 * @param {Cusor} start
	 * @param {Cusor} end
	 * @param {function():boolean} until
	 *        Optional predicate.  May be used to stop the trimming process from
	 *        moving the Cursor from within an element outside of it.
	 * @param {function():boolean} ignore
	 *        Optional predicate.  May be used to ignore (skip)
	 *        following/preceding siblings which otherwise would stop the
	 *        trimming process, like for example underendered whitespace.
	 */
	function trimBoundaries(start, end, until, ignore) {
		until = until || Fn.returnFalse;
		ignore = ignore || Fn.returnFalse;
		start.nextWhile(function (start) {
			return (
				!start.equals(end)
					&& (
						!start.atEnd
							? ignore(start.node)
							: !until(start.parent())
					)
			);
		});
		end.prevWhile(function (end) {
			var prevSibling = end.prevSibling();
			return (
				!start.equals(end)
					&& (
						prevSibling
							? ignore(prevSibling)
							: !until(end.parent())
					)
			);
		});
	}

	/**
	 * Ensures that the given boundaries are neither in start nor end positions.
	 * In other words, after this operation, both will have preceding and
	 * following siblings.
	 *
	 * Expansion/trimming can be controlled via expandUntil and trimUntil, but
	 * may cause one or both of the boundaries to remain in start or end
	 * position.
	 */
	function trimExpandBoundaries(startPoint, endPoint, trimUntil, expandUntil, ignore) {
		var collapsed = startPoint.equals(endPoint);
		trimBoundaries(startPoint, endPoint, trimUntil, ignore);
		expandBoundaries(startPoint, endPoint, expandUntil, ignore);
		if (collapsed) {
			endPoint.setFrom(startPoint);
		}
	}

	/**
	 * Seekts a boundary point.
	 *
	 * @private
	 */
	function seekBoundaryPoint(range, container, offset, oppositeContainer,
	                           oppositeOffset, setFn, ignore, backwards) {
		var cursor = Cursors.cursorFromBoundaryPoint(container, offset);

		// Because when seeking backwards, if the boundary point is inside a
		// text node, trimming starts after it. When seeking forwards, the
		// cursor starts before the node, which is what
		// cursorFromBoundaryPoint() does automatically.
		if (backwards
				&& Dom.isTextNode(container)
					&& offset > 0
						&& offset < container.length) {
			if (cursor.next()) {
				if (!ignore(cursor)) {
					return range;
				}
				// Bacause the text node can be ignored, we go back to the
				// initial position.
				cursor.prev();
			}
		}
		var opposite = Cursors.cursorFromBoundaryPoint(
			oppositeContainer,
			oppositeOffset
		);
		var changed = false;
		while (!cursor.equals(opposite)
		           && ignore(cursor)
		           && (backwards ? cursor.prev() : cursor.next())) {
			changed = true;
		}
		if (changed) {
			setFn(range, cursor);
		}
		return range;
	}


	/**
	 * Starting with the given range's start and end boundary points, seek
	 * inward using a cursor, passing the cursor to ignoreLeft and ignoreRight,
	 * stopping when either of these returns true, adjusting the given range to
	 * the end positions of both cursors.
	 *
	 * The dom cursor passed to ignoreLeft and ignoreRight does not traverse
	 * positions inside text nodes. The exact rules for when text node
	 * containers are passed are as follows: If the left boundary point is
	 * inside a text node, trimming will start before it. If the right boundary
	 * point is inside a text node, trimming will start after it.
	 * ignoreLeft/ignoreRight() are invoked with the cursor before/after the
	 * text node that contains the boundary point.
	 *
	 * @todo: Implement in terms of boundaries
	 *
	 * @param  {Range}     range
	 * @param  {function=} ignoreLeft
	 * @param  {function=} ignoreRight
	 * @return {Range}
	 */
	function trim(range, ignoreLeft, ignoreRight) {
		ignoreLeft = ignoreLeft || Fn.returnFalse;
		ignoreRight = ignoreRight || Fn.returnFalse;
		if (range.collapsed) {
			return range;
		}
		// Because range may be mutated, we must store its properties before
		// doing anything else.
		var sc = range.startContainer;
		var so = range.startOffset;
		var ec = range.endContainer;
		var eo = range.endOffset;
		seekBoundaryPoint(
			range,
			sc,
			so,
			ec,
			eo,
			Cursors.setRangeStart,
			ignoreLeft,
			false
		);
		sc = range.startContainer;
		so = range.startOffset;
		seekBoundaryPoint(
			range,
			ec,
			eo,
			sc,
			so,
			Cursors.setRangeEnd,
			ignoreRight,
			true
		);
		return range;
	}

	/**
	 * Like trim() but ignores closing (to the left) and opening positions (to
	 * the right).
	 *
	 * @param  {Range}     range
	 * @param  {function=} ignoreLeft
	 * @param  {function=} ignoreRight
	 * @return {Range}
	 */
	function trimClosingOpening(range, ignoreLeft, ignoreRight) {
		ignoreLeft = ignoreLeft || Fn.returnFalse;
		ignoreRight = ignoreRight || Fn.returnFalse;
		trim(range, function (cursor) {
			return cursor.atEnd || ignoreLeft(cursor.node);
		}, function (cursor) {
			return !cursor.prevSibling() || ignoreRight(cursor.prevSibling());
		});
		return range;
	}

	function splitBoundaryPoint(node, atEnd, leftPoint, rightPoint, removeEmpty, opts) {
		var wrapper = null;

		function carryDown(elem, stop) {
			return stop || opts.until(elem);
		}

		function intoWrapper(node, stop) {
			if (stop) {
				return;
			}
			var parent = node.parentNode;
			if (!wrapper || parent.previousSibling !== wrapper) {
				wrapper = opts.clone(parent);
				removeEmpty.push(parent);
				Dom.insert(wrapper, parent, false);
				if (leftPoint.node === parent && !leftPoint.atEnd) {
					leftPoint.node = wrapper;
				}
				if (rightPoint.node === parent) {
					rightPoint.node = wrapper;
				}
			}
			moveBackIntoWrapper(node, wrapper, true, leftPoint, rightPoint);
		}

		var ascend = Dom.childAndParentsUntilIncl(node, opts.below);
		var unsplitParent = ascend.pop();
		if (unsplitParent && opts.below(unsplitParent)) {
			ascendWalkSiblings(ascend, atEnd, carryDown, intoWrapper, Fn.noop, Fn.noop);
		}

		return unsplitParent;
	}

	/**
	 * Tries to move the given boundary to the start of line, skipping over any
	 * unrendered nodes, or if that fails to the end of line (after a br
	 * element if present), and for the last line in a block, to the very end
	 * of the block.
	 *
	 * If the selection is inside a block with only a single empty line (empty
	 * except for unrendered nodes), and both boundary points are normalized,
	 * the selection will be collapsed to the start of the block.
	 *
	 * For some operations it's useful to think of a block as a number of
	 * lines, each including its respective br and any preceding unrendered
	 * whitespace and in case of the last line, also any following unrendered
	 * whitespace.
	 *
	 * @param  {!Cursor} point
	 * @return {boolean} True if the cursor is moved.
	 */
	function normalizeBoundary(point) {
		if (HtmlElements.skipUnrenderedToStartOfLine(point)) {
			return true;
		}
		if (!HtmlElements.skipUnrenderedToEndOfLine(point)) {
			return false;
		}
		if ('BR' === point.node.nodeName) {
			point.skipNext();
			// Because, if this is the last line in a block, any unrendered
			// whitespace after the last br will not constitute an independent
			// line, and as such we must include it in the last line.
			var endOfBlock = point.clone();
			if (HtmlElements.skipUnrenderedToEndOfLine(endOfBlock) && endOfBlock.atEnd) {
				point.setFrom(endOfBlock);
			}
		}
		return true;
	}

	function splitRangeAtBoundaries(range, left, right, opts) {
		var normalizeLeft = opts.normalizeRange ? left : left.clone();
		var normalizeRight = opts.normalizeRange ? right : right.clone();
		normalizeBoundary(normalizeLeft);
		normalizeBoundary(normalizeRight);

		Cursors.setToRange(range, normalizeLeft, normalizeRight);

		var removeEmpty = [];
		var start = Dom.nodeAtOffset(range.startContainer, range.startOffset);
		var end = Dom.nodeAtOffset(range.endContainer, range.endOffset);
		var startAtEnd = Boundaries.isAtEnd(Boundaries.raw(range.startContainer, range.startOffset));
		var endAtEnd = Boundaries.isAtEnd(Boundaries.raw(range.endContainer, range.endOffset));
		var unsplitParentStart = splitBoundaryPoint(start, startAtEnd, left, right, removeEmpty, opts);
		var unsplitParentEnd = splitBoundaryPoint(end, endAtEnd, left, right, removeEmpty, opts);

		removeEmpty.forEach(function (elem) {
			// Because we may end up cloning the same node twice (by splitting
			// both start and end points)
			if (!elem.firstChild && elem.parentNode) {
				Mutation.removeShallowPreservingCursors(elem, [left, right]);
			}
		});

		if (opts.normalizeRange) {
			trimExpandBoundaries(left, right, null, function (node) {
				return node === unsplitParentStart || node === unsplitParentEnd;
			});
		}
	}

	/**
	 * Splits the ancestors above the given range's start and end points.
	 *
	 * @param opts a map of options (all optional):
	 *
	 *        clone - a function that clones a given element node
	 *        shallowly and returns the cloned node.
	 *
	 *        until - a function that returns true if splitting
	 *        should stop at a given node (exclusive) below the topmost
	 *        node for which below() returns true. By default all
	 *        nodes are split.
	 *
	 *        below - a function that returns true if descendants
	 *        of a given node can be split. Used to determine the
	 *        topmost node at which to end the splitting process. If
	 *        false is returned for all ancestors of the start and end
	 *        points of the range, nothing will be split. By default,
	 *        returns true for an editing host.
	 *
	 *        normalizeRange - a boolean, defaults to true.
	 *        After splitting the selection may still be inside the split
	 *        nodes, for example after splitting the DOM may look like
	 *
	 *        <b>1</b><b>\{2</b><i>3</i><i>\}4</i>
	 *
	 *	      If normalizeRange is true, the selection is trimmed to
	 *	      correct <i>\}4</i> and expanded to correct <b>\{2</b>, such
	 *        that it will look like
	 *
	 *	      <b>1</b>\{<b>2</b><i>3</i>\}<i>4</i>
	 *
	 *	      This should make both start and end points children of the
	 *        same cac which is going to be the topmost unsplit node. This
	 *        is usually what one expects the range to look like after a
	 *        split.
	 *        NB: if splitUntil() returns true, start and end points
	 *        may not become children of the topmost unsplit node. Also,
	 *        if splitUntil() returns true, the selection may be moved
	 *        out of an unsplit node which may be unexpected.
	 * @return {Array.<Boundary>}
	 */
	function split(liveRange, opts) {
		opts = opts || {};

		opts = Maps.merge({
			clone: Dom.cloneShallow,
			until: Fn.returnFalse,
			below: Dom.isEditingHost,
			normalizeRange: true
		}, opts);

		return fixupRange(liveRange, function (range, left, right) {
			splitRangeAtBoundaries(range, left, right, opts);
			return null;
		});
	}

	/**
	 * Removes the content inside the given range.
	 *
	 * “If you delete a paragraph-boundary, the result seems to be consistent:
	 * The leftmost block 'wins', and the content of the rightmost block is
	 * included in the leftmost:
	 *
	 * <h1>Overskrift</h1><p>[]Text</p>
	 *
	 * “If delete is pressed, this is the result:
	 *
	 * <h1>Overskrift[]Text</h1>”
	 * -- http://dev.opera.com/articles/view/rich-html-editing-in-the-browser-part-1
	 *
	 * TODO:
	 * put &nbsp; at beginning and end position in order to preserve spaces at
	 * these locations when deleting.
	 *
	 * @see https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#deleting-the-selection
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Boundary>}
	 * @memberOf editing
	 */
	function remove(start, end) {
		var range = Boundaries.range(start, end);
		return fixupRange(range, function (range, left, right) {
			var remove = function (node) {
				Mutation.removePreservingRange(node, range);
			};
			walkBoundaryLeftRightInbetween(
				range,
				//carryDown
				Fn.noop,
				// stepLeftStart
				Fn.noop,
				// remove
				//   |
				//   v
				// {<b>...
				remove,
				//   remove
				//     |
				//     v
				// ...<b>}
				remove,
				// stepRightEnd
				Fn.noop,
				// stepPartial
				Fn.noop,
				//      remove
				//        |
				//        v
				// {...<b></b>...}
				remove,
				null
			);
			return {
				postprocessTextNodes: Fn.noop,
				postprocess: function () {
					var split = Html.removeBreak(
						Boundaries.fromRangeStart(range),
						Boundaries.fromRangeEnd(range)
					)[0];
					var cursor = Cursors.createFromBoundary(
						Boundaries.container(split),
						Boundaries.offset(split)
					);
					left.setFrom(cursor);
					right.setFrom(cursor);
				}
			};
		}, false);
	}

	/**
	 * Creates a visual line break at the given boundary position.
	 *
	 * @see
	 * https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#splitting-a-node-list's-parent
	 * http://lists.whatwg.org/htdig.cgi/whatwg-whatwg.org/2011-May/031700.html
	 *
	 * @param  {!Boundary} boundary
	 * @param  {string}    breaker
	 * @return {Array.<Boundary>}
	 * @memberOf editing
	 */
	function breakline(boundary, breaker) {
		var op = 'BR' === breaker ? Html.insertLineBreak : Html.insertBreak;
		boundary = op(boundary, breaker);
		return [boundary, boundary];
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf editing
	 */
	function insert(start, end, insertion) {
		var range = Boundaries.range(start, end);
		split(range, {
			below: function (node) {
				return Content.allowsNesting(node.nodeName, insertion.nodeName);
			}
		});
		var boundary = Mutation.insertNodeAtBoundary(
			insertion,
			Boundaries.fromRangeStart(range)
		);
		Boundaries.setRange(range, boundary, Boundaries.create(
			Boundaries.container(boundary),
			Boundaries.offset(boundary) + 1
		));
		return Boundaries.fromRangeStart(range);
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf editing
	 */
	function className(start, end, name, value, boundaries) {
		throw 'Not implemented';
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf editing
	 */
	function attribute(start, end, name, value, boundaries) {
		throw 'Not implemented';
	}

	/**
	 * This function is not yet implemented.
	 * @TODO to be implemented
	 * @memberOf editing
	 */
	function cut(start, end, boundaries) {
		throw 'Not implemented';
	}

	/**
	 * This function is not yet implemented.
	 * @TODO to be implemented
	 * @memberOf editing
	 */
	function copy(start, end, boundaries) {
		throw 'Not implemented';
	}

	/**
	 * Starting with the given, returns the first node that matches the given
	 * predicate.
	 *
	 * @private
	 * @param  {!Node}                  node
	 * @param  {function(Node):boolean} pred
	 * @return {Node}
	 */
	function nearest(node, pred) {
		return Dom.upWhile(node, function (node) {
			return !pred(node)
			    && !(node.parentNode && Dom.isEditingHost(node.parentNode));
		});
	}

	/**
	 * Expands the given start and end boundaires until the nearst containers
	 * that match the given predicate.
	 *
	 * @private
	 * @param  {Boundary}               start
	 * @param  {Boundary}               end
	 * @param  {function(Node):boolean} pred
	 * @return {Array.<Boundary>}
	 */
	function expandUntil(start, end, pred) {
		var node, startNode, endNode;
		if (Html.isBoundariesEqual(start, end)) {
			//       node ----------.
			//        |             |
			//        v             v
			// </p>{}<u> or </b>{}</p>
			node = Boundaries.nextNode(end);
			if (Dom.isEditingHost(node)) {
				node = Boundaries.prevNode(start);
			}
			if (Dom.isEditingHost(node)) {
				return [start, end];
			}
			startNode = endNode = pred(node) ? node : nearest(node, pred);
		} else {
			startNode = nearest(Boundaries.nextNode(start), pred);
			endNode = nearest(Boundaries.prevNode(end), pred);
		}
		return [
			Boundaries.fromFrontOfNode(startNode),
			Boundaries.fromBehindOfNode(endNode)
		];
	}

	/**
	 * Given a list of sibling nodes and a formatting, will apply the formatting
	 * across the list of nodes.
	 *
	 * @private
	 * @param  {string}       formatting
	 * @param  {Array.<Node>} siblings
	 */
	function formatSiblings(formatting, siblings) {
		var wrapper = null;
		siblings.forEach(function (node) {
			if (Html.isUnrendered(node) && !wrapper) {
				return;
			}
			if (Content.allowsNesting(formatting, node.nodeName)) {
				if (!wrapper) {
					wrapper = node.ownerDocument.createElement(formatting);
					Dom.insert(wrapper, node);
				}
				return Dom.move([node], wrapper);
			}
			wrapper = null;
			if (Html.isVoidType(node)) {
				return;
			}
			var children = Dom.children(node);
			var childNames = children.map(function (child) { return child.nodeName; });
			var canWrapChildren = childNames.length === childNames.filter(
				Fn.partial(Content.allowsNesting, formatting)
			).length;
			var allowedInParent = Content.allowsNesting(
				node.parentNode.nodeName,
				formatting
			);
			if (
				canWrapChildren              &&
				allowedInParent              &&
				!Html.isGroupContainer(node) &&
				!Html.isGroupedElement(node)
			) {
				return Dom.replaceShallow(
					node,
					node.ownerDocument.createElement(formatting)
				);
			}
			var i = Arrays.someIndex(children, Html.isRendered);
			if (i > -1) {
				formatSiblings(formatting, children.slice(i));
			}
		});
	}

	/**
	 * Applies block formatting to contents enclosed by start and end boundary.
	 * Will return updated array of boundaries after the operation.
	 *
	 * @private
	 * @param  {!string}   formatting
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Boundary>}
	 */
	function formatBlock(formatting, start, end, preserve) {
		var boundaries = expandUntil(start, end, Html.hasLinebreakingStyle);
		boundaries = Html.walkBetween(
			boundaries[0],
			boundaries[1],
			Fn.partial(formatSiblings, formatting)
		);
		start = Boundaries.fromStartOfNode(Boundaries.nextNode(boundaries[0]));
		end = Boundaries.fromEndOfNode(Boundaries.prevNode(boundaries[1]));
		return [Html.expandForward(start), Html.expandBackward(end)];
	}

	/**
	 * Applies block and inline formattings (eg. 'B', 'I', 'H2' - be sure to use
	 * UPPERCASE node names here) to contents enclosed by start and end
	 * boundary.
	 *
	 * Will return updated array of boundaries after the operation.
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @param  {!string}   nodeName
	 * @param  {Array.<Boundary>}
	 * @return {Array.<Boundary>}
	 * @memberOf editing
	 */
	function format(start, end, nodeName, boundaries) {
		var range;
		var node = {nodeName: nodeName};
		if (nodeName.toLowerCase() === 'a') {
			range = Links.create('', start, end);
		} else if (Html.isTextLevelSemanticNode(node)) {
			range = formatInline(nodeName, start, end, true);
		} else if (Html.isListContainer(node)) {
			range = Lists.toggle(nodeName, start, end);
		} else if (Html.isBlockNode(node)) {
			range = formatBlock(nodeName, start, end);
		}
		return range;
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 * 
	 * @memberOf editing
	 */
	function unformat(start, end, nodeName, boundaries) {
	   return formatInline(nodeName, start, end, false);
	}

	/**
	 * Toggles inline style round the given selection.
	 *
	 * @private
	 * @param  {string}    nodeName
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Boundary>}
	 */
	function toggleInline(nodeName, start, end) {
		var override = Overrides.nodeToState[nodeName];
		if (!override) {
			return [start, end];
		}
		var next = Boundaries.nextNode(Html.expandForward(start));
		var prev = Boundaries.prevNode(Html.expandBackward(end));
		var overrides = Overrides.harvest(next).concat(Overrides.harvest(prev));
		var hasStyle = -1 < Overrides.indexOf(overrides, override);
		var op = hasStyle ? unformat : format;
		return op(start, end, nodeName);
	}

	/**
	 * Toggles formatting round the given selection.
	 *
	 * @todo   Support block formatting
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @param  {string}    nodeName
	 * @param  {Array.<Boundary>}
	 * @return {Array.<Boundary>}
	 */
	function toggle(start, end, nodeName, boundaries) {
		var node = {nodeName: nodeName};
		if (Html.isTextLevelSemanticNode(node)) {
			return toggleInline(nodeName, start, end);
		}
		return [start, end];
	}

	return {
		format     : format,
		unformat   : unformat,
		toggle     : toggle,
		style      : style,
		className  : className,
		attribute  : attribute,
		cut        : cut,
		copy       : copy,
		breakline  : breakline,
		insert     : insert,
		wrap      : wrapElem,

		// obsolete
		split     : split,
		remove    : remove,
		trimClosingOpening: trimClosingOpening
	};
});

/**
 * searching.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @overview
 * Module for searching for strings of token in markup
 * @namespace searching
 */
define('searching',[
	'dom',
	'boundaries',
	'functions'
], function (
	Dom,
	Boundaries,
	Fn
) {
	

	/**
	 * Joins the given list of text nodes' text strings into a single string.
	 *
	 * @private
	 * @param  {Array.<Node>} nodes
	 * @return {string}
	 */
	function joinText(nodes) {
		return nodes.reduce(function (list, node) {
			return list.concat(node.data);
		}, []).join('');
	}

	/**
	 * Given a list of text nodes, will return a boundary of the position that
	 * is `index` offsets into the cumulative node lengths.
	 *
	 * @private
	 * @param  {Array.<Node>} List of text nodes
	 * @param  {number}       index
	 * @return {?Boundary}
	 */
	function boundaryInNodeList(nodes, index) {
		var cumulative = 0;
		var node;
		for (var i = 0; i < nodes.length; i++) {
			node = nodes[i];
			if (cumulative + Dom.nodeLength(node) >= index) {
				return Boundaries.create(node, index - cumulative);
			}
			cumulative += node.length;
		}
		return null;
	}

	/**
	 * Collects all preceeding text node along with the given in document order.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {Array.<Node>}
	 */
	function collectContiguiousTextNodes(node, collect) {
		return collect(node, function (node) {
			return !Dom.isTextNode(node) || Dom.isEditingHost(node);
		});
	}

	function searchBackward(boundary, regex) {
		var offset;
		var start = Boundaries.nodeBefore(boundary);
		if (start) {
			offset = Dom.nodeLength(start);
		} else {
			start = Boundaries.container(boundary);
			offset = Boundaries.offset(boundary);
		}
		var node = start;
		do {
			if (Dom.isTextNode(node)) {
				var nodes = collectContiguiousTextNodes(node, Dom.nodeAndPrevSiblings).reverse();
				var text = joinText(nodes);
				var index = text.search(regex);
				if (index > -1) {
					if (start !== node || index < offset) {
						return boundaryInNodeList(nodes, index);
					}
					index = text.substr(0, offset).search(regex);
					if (index > -1) {
						return boundaryInNodeList(nodes, index);
					}
				}
			}
			node = Dom.backward(node);
		} while (node && !Dom.isEditingHost(node));
		return null;
	}

	function searchForward(boundary, regex) {
		var offset;
		var start = Boundaries.nodeAfter(boundary);
		if (start) {
			offset = 0;
		} else {
			start = Boundaries.container(boundary);
			offset = Boundaries.offset(boundary);
		}
		var node = start;
		do {
			if (Dom.isTextNode(node)) {
				var nodes = collectContiguiousTextNodes(node, Dom.nodeAndNextSiblings);
				var text = joinText(nodes);
				if (node === start) {
					text = text.substr(offset);
				}
				var index = text.search(regex);
				if (index > -1) {
					return boundaryInNodeList(nodes, offset + index);
				}
			}
			node = Dom.forward(node);
		} while (node && !Dom.isEditingHost(node));
		return null;
	}

	/**
	 * Collects all preceeding text node along with the given in document order.
	 *
	 * @param  {!Boundary} node
	 * @param  {!RexExp}   regex
	 * @param  {string}    direction "forward" or "backward"
	 * @return {?Boundary}
	 * @memberOf searching
	 */
	function search(boundary, regex, direction) {
		return ('backward' === direction)
		     ? searchBackward(boundary, regex)
		     : searchForward(boundary, regex);
	}

	/**
	 * Find the given string backward of the given boundary.
	 *
	 * @param  {!Boundary} boundary
	 * @param  {string}    str
	 * @return {?Boundary}
	 * @memberOf searching
	 */
	function backward(boundary, str) {
		return searchBackward(boundary, new RegExp(str + '(?!.*' + str + ')'));
	}
	/**
	 * Find the given string forward of the given boundary.
	 *
	 * @param  {!Boundary} boundary
	 * @param  {string}    str
	 * @return {?Boundary}
	 * @memberOf searching
	 */
	function forward(boundary, str) {
		return searchForward(boundary, new RegExp(str));
	}

	return {
		search   : search,
		forward  : forward,
		backward : backward
	};
});

/**
 * traversing.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace traversing
 */
define('traversing',[
	'dom',
	'html',
	'arrays',
	'strings',
	'boundaries'
], function (
	Dom,
	Html,
	Arrays,
	Strings,
	Boundaries
) {
	

	/**
	 * Moves the given boundary forward (if needed) to encapsulate all adjacent
	 * unrendered characters.
	 *
	 * This operation should therefore never cause the visual representation of
	 * the boundary to change.
	 *
	 * Since it is impossible to place a boundary immediately behind an
	 * invisible character, this function will only ever need to expand a
	 * range's end position.
	 *
	 * @param  {!Boundary} boundary
	 * @return {Boundary}
	 */
	function envelopeInvisibleCharacters(boundary) {
		if (Boundaries.isNodeBoundary(boundary)) {
			return boundary;
		}
		var offset = Html.nextSignificantOffset(boundary);
		var container = Boundaries.container(boundary);
		return (-1 === offset)
		     ? Boundaries.fromEndOfNode(container)
		     : Boundaries.create(container, offset);
	}

	/**
	 * Return the character immediately following the given boundary.
	 * If not character exists, an empty string is returned.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {string}
	 */
	function nextCharacter(boundary) {
		var ahead = Html.next(boundary, 'char');
		if (ahead && Boundaries.isTextBoundary(ahead)) {
			var node = Boundaries.container(ahead);
			var offset = Boundaries.offset(ahead);
			return node.data.substr(offset - 1, 1);
		}
		return '';
	}

	/**
	 * Moves the boundary backwards by a unit measure.
	 *
	 * The second parameter `unit` specifies the unit with which to move the
	 * boundary. This value may be one of the following strings:
	 *
	 * "char" -- Move behind the previous visible character.
	 *
	 * "word" -- Move behind the previous word.
	 *
	 *		A word is the smallest semantic unit. It is a contigious sequence of
	 *		visible characters terminated by a space or puncuation character or
	 *		a word-breaker (in languages that do not use space to delimit word
	 *		boundaries).
	 *
	 * "boundary" -- Move in behind of the previous boundary and skip over void
	 *               elements.
	 *
	 * "offset" -- Move behind the previous visual offset.
	 *
	 *		A visual offset is the smallest unit of consumed space. This can be
	 *		a line break, or a visible character.
	 *
	 * "node" -- Move in front of the previous visible node.
	 *
	 * @param  {Boundary} boundary
	 * @param  {string=}  unit Defaults to "offset"
	 * @return {?Boundary}
	 * @memberOf traversing
	 */
	function prev(boundary, unit) {
		var behind = Html.prev(boundary, unit);
		if ('word' === unit) {
			if (Strings.WORD_BOUNDARY.test(nextCharacter(behind))) {
				return prev(behind, unit);
			}
		}
		return behind;
	}

	/**
	 * Moves the boundary forward by a unit measure.
	 *
	 * The second parameter `unit` specifies the unit with which to move the
	 * boundary. This value may be one of the following strings:
	 *
	 * "char" -- Move in front of the next visible character.
	 *
	 * "word" -- Move in front of the next word.
	 *
	 *		A word is the smallest semantic unit. It is a contigious sequence of
	 *		visible characters terminated by a space or puncuation character or
	 *		a word-breaker (in languages that do not use space to delimit word
	 *		boundaries).
	 *
	 * "boundary" -- Move in front of the next boundary and skip over void
	 *               elements.
	 *
	 * "offset" -- Move in front of the next visual offset.
	 *
	 *		A visual offset is the smallest unit of consumed space. This can be
	 *		a line break, or a visible character.
	 *
	 * "node" -- Move in front of the next visible node.
	 *
	 * @param  {Boundary} boundary
	 * @param  {string=}  unit Defaults to "offset"
	 * @return {?Boundary}
	 * @memberOf traversing
	 */
	function next(boundary, unit) {
		if ('word' === unit) {
			if (Strings.WORD_BOUNDARY.test(nextCharacter(boundary))) {
				return next(Html.next(boundary, 'char'), unit);
			}
		}
		return Html.next(boundary, unit);
	}

	/**
	 * Expands two boundaries to contain a word.
	 *
	 * The boundaries represent the start and end containers of a range.
	 *
	 * A word is a collection of visible characters terminated by a space or
	 * punctuation character or a word-breaker (in languages that do not use
	 * space to delimit word boundaries).
	 *
	 * foo b[a]r baz → foo [bar] baz
	 *
	 * @private
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @return {Array.<Boundary>}
	 */
	function expandToWord(start, end) {
		return [
			prev(start, 'word') || start,
			next(end,   'word') || end
		];
	}

	/**
	 * Expands two boundaries to contain a block.
	 *
	 * The boundaries represent the start and end containers of a range.
	 *
	 * [,] = start,end boundary
	 *
	 *  +-------+     [ +-------+
	 *  | block |       | block |
	 *  |       |   →   |       |
	 *  | [ ]   |       |       |
	 *  +-------+       +-------+ ]
	 *
	 * @private
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @return {Array.<Boundary>}
	 */
	function expandToBlock(start, end) {
		var cac = Boundaries.commonContainer(start, end);
		var ancestors = Dom.childAndParentsUntilIncl(cac, function (node) {
			return Html.hasLinebreakingStyle(node) || Dom.isEditingHost(node);
		});
		var node = Arrays.last(ancestors);
		var len = Dom.nodeLength(node);
		return [Boundaries.create(node, 0), next(Boundaries.create(node, len))];
	}

	/**
	 * Expands the given boundaries to contain the given unit.
	 *
	 * The second parameter `unit` specifies the unit with which to expand.
	 * This value may be one of the following strings:
	 *
	 * "word" -- Expand to completely contain a word.
	 *
	 *		A word is the smallest semantic unit.  It is a contigious sequence
	 *		of characters terminated by a space or puncuation character or a
	 *		word-breaker (in languages that do not use space to delimit word
	 *		boundaries).
	 *
	 * "block" -- Expand to completely contain a block.
	 *
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @param  {unit}     unit
	 * @return {Array.<Boundary>}
	 * @memberOf traversing
	 */
	function expand(start, end, unit) {
		switch (unit) {
		case 'word':
			return expandToWord(start, end);
		case 'block':
			return expandToBlock(start, end);
		default:
			throw '"' + unit + '"? what\'s that?';
		}
	}

	return {
		expand                      : expand,
		next                        : next,
		prev                        : prev,
		backward                    : Html.stepBackward,
		forward                     : Html.stepForward,
		isAtStart                   : Html.isAtStart,
		isAtEnd                     : Html.isAtEnd,
		isBoundaryEqual             : Html.isBoundaryEqual,
		envelopeInvisibleCharacters : envelopeInvisibleCharacters
	};
});

/**
 * autoformat.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('autoformat',[
	'functions',
	'dom',
	'maps',
	'keys',
	'editing',
	'mutation',
	'searching',
	'traversing',
	'boundaries'
], function (
	Fn,
	Dom,
	Maps,
	Keys,
	Editing,
	Mutation,
	Searching,
	Traversing,
	Boundaries
) {
	

	/**
	 * Transforms ```foo("bar")``` into <code>foo("bar")</code>.
	 *
	 * @private
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @return {!Boundary}
	 */
	function code(start, end) {
		//  marker / front
		//    |
		//    |   ,--- pos1
		//    |   |
		//    |   |   ,--- start / pos2
		//    |   |   |
		//    |   |   |   ,--- end
		//    |   |   |   |
		//    v   v   v   v
		//     ``` foo ```
		//    .   .   .  .
		var marker = Searching.backward(start, '```');
		if (!marker) {
			return end;
		}
		var pos2 = Editing.remove(start, end)[1];
		var front = Searching.backward(pos2, '```');
		if (!front) {
			return pos2;
		}
		var behind = Searching.search(front, /[^`]|$/);
		if (!behind) {
			return pos2;
		}
		var pos1 = Editing.remove(front, behind)[0];
		return Editing.wrap('code', pos1, pos2)[1];
	}

	function ascii(symbol, start, end) {
		end = Traversing.envelopeInvisibleCharacters(end);
		var boundary = Editing.remove(start, end)[0];
		return Mutation.insertTextAtBoundary(symbol, boundary, true);
	}

	var triggers = {
		'```' : code,

		'(:'  : Fn.partial(ascii, '☺'),
		':)'  : Fn.partial(ascii, '☺'),

		':('  : Fn.partial(ascii, '☹'),
		'):'  : Fn.partial(ascii, '☹'),

		'<3'  : Fn.partial(ascii, '♥'),

		'--'  : Fn.partial(ascii, '—'),

		'-->' : Fn.partial(ascii, '→'),
		'<--' : Fn.partial(ascii, '←'),

		'==>' : Fn.partial(ascii, '⇒'),
		'<==' : Fn.partial(ascii, '⇐'),

		'|>'  : Fn.partial(ascii, '►'),
		'<|'  : Fn.partial(ascii, '◄')
	};

	/**
	 * A trie constructde from the trigger keys.
	 *
	 * @private
	 * @type {Object.<String, Object|function>}
	 */
	var dictionary = {};

	Maps.forEach(triggers, function(handler, trigger) {
		var level = dictionary;
		var tokens = trigger.split('').reverse();
		var count = tokens.length - 1;
		tokens.forEach(function (token, index, collection) {
			if (index === count) {
				level[token] = handler;
			} else if (!level[token]) {
				level = level[token] = {};
			} else {
				level = level[token];
			}
		});
	});

	function nextChar(boundary) {
		var node;
		if (Boundaries.isTextBoundary(boundary)) {
			node = Boundaries.container(boundary);
			return node.data.substr(Boundaries.offset(boundary), 1);
		}
		node = Boundaries.nextNode(boundary);
		if (Dom.isTextNode(node)) {
			return node.data.substr(0, 1);
		}
		return '';
	}

	function middleware(event) {
		if ('keydown' !== event.type) {
			return event;
		}
		if (
			Keys.CODES['tab'  ] !== event.keycode &&
			Keys.CODES['space'] !== event.keycode &&
			Keys.CODES['enter'] !== event.keycode
		) {
			return event;
		}
		var boundary = event.selection.boundaries[0];
		var prev = Traversing.prev(boundary, 'visual');
		var token = prev && nextChar(prev);
		var level = dictionary;
		var handler;
		var start;
		while (token && level[token]) {
			level = level[token];
			if ('function' === typeof level) {
				handler = level;
				start = prev;
			}
			prev = Traversing.prev(prev, 'visual');
			token = prev && nextChar(prev);
		}
		if (handler) {
			boundary = handler(start, boundary);
			event.selection.boundaries = [boundary, boundary];
		}
		return event;
	}

	return {
		middleware: middleware
	};
});

/**
 * mouse.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace mouse
 */
define('mouse',['boundaries'], function (Boundaries) {
	

	/**
	 * Native mouse events.
	 *
	 * @type {Object.<string, boolean>}
	 * @memberOf mouse
	 */
	var EVENTS = {
		'mouseup'   : true,
		'mousedown' : true,
		'mousemove' : true,
		'dblclick'  : true,
		'dragstart' : true,
		'dragover'  : true,
		'dragend'   : true
	};

	/**
	 * Updates selection
	 *
	 * @param  {AlohaEvent} event
	 * @return {AlohaEVent}
	 * @memberOf mouse
	 */
	function middleware(event) {
		if ('mousedown' === event.type) {
			event.selection.formatting = [];
			event.selection.overrides = [];
		}
		return event;
	}

	return {
		middleware : middleware,
		EVENTS     : EVENTS
	};
});

/**
 * carets.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace carets
 */
define('carets',[
	'dom',
	'html',
	'maps',
	'browsers',
	'traversing',
	'boundaries'
], function (
	Dom,
	Html,
	Maps,
	Browsers,
	Traversing,
	Boundaries
) {
	

	/**
	 * Adds a style tag to the head of the given document if one does not
	 * already exist.
	 *
	 * Each document in which box() is called requires some special br css
	 * styling in order for box() to return calculate correct range bounding
	 * offsets near br elements.
	 *
	 * @see https://github.com/alohaeditor/Aloha-Editor/issues/1138
	 * @private
	 * @param {Document} doc
	 */
	function ensureBrStyleFix(doc) {
		if (doc['!aloha-br-style-fix']) {
			return;
		}
		var style = doc.createElement('style');
		var text = doc.createTextNode(
			'.aloha-editable br,.aloha-editable br:after{content:"\\A";white-space:pre-line;}'
		);
		Dom.append(text, style);
		Dom.append(style, doc.head);
		doc['!aloha-br-style-fix'] = true;
	}

	/**
	 * Removes an unrendered (empty text-) node infront of the given boundary.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function trimPreceedingNode(boundary) {
		if (Boundaries.isTextBoundary(boundary)) {
			return boundary;
		}
		if (Boundaries.isAtStart(boundary)) {
			return boundary;
		}
		if (Html.isRendered(Boundaries.nodeBefore(boundary))) {
			return boundary;
		}
		var clone = Dom.clone(Boundaries.container(boundary), true);
		var offset = Boundaries.offset(boundary) - 1;
		Dom.remove(clone.childNodes[offset]);
		return Boundaries.create(clone, offset);
	}

	/**
	 * Expands the range one visual step to the left if possible, returns null
	 * otherwise.
	 *
	 * @private
	 * @param  {Range} range
	 * @return {?Range}
	 */
	function expandLeft(range) {
		var clone = range.cloneRange();
		var start = trimPreceedingNode(Boundaries.fromRangeStart(clone));
		var end = Boundaries.fromRangeEnd(clone);
		if (Boundaries.isAtStart(start)) {
			return null;
		}
		if (Html.hasLinebreakingStyle(Boundaries.prevNode(start))) {
			return null;
		}
		return Boundaries.range(stepLeft(start), end);
	}

	/**
	 * Expands the range one visual step to the right if possible, returns null
	 * otherwise.
	 *
	 * @private
	 * @param  {Range} range
	 * @return {?Range}
	 */
	function expandRight(range) {
		var start = Boundaries.fromRangeStart(range);
		var end = Boundaries.fromRangeEnd(range);
		if (Boundaries.isAtEnd(end)) {
			return null;
		}
		if (Html.hasLinebreakingStyle(Boundaries.nextNode(end))) {
			return null;
		}
		// Because this means that we cannot expand any further right inside the
		// container
		if (Html.isAtEnd(start)) {
			return null;
		}
		return Boundaries.range(start, stepRight(end));
	}

	/**
	 * Steps the given boundary one visual step left or until in behind of a
	 * line break position.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function stepLeft(boundary) {
		var prev = Traversing.prev(boundary, 'char');
		if (prev) {
			return prev;
		}
		if (Html.hasLinebreakingStyle(Boundaries.prevNode(boundary))) {
			return boundary;
		}
		return stepLeft(Traversing.prev(boundary, 'boundary'));
	}

	/**
	 * Steps the given boundary one visual step right or until in front of a
	 * line break position.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @return {Boundary}
	 */
	function stepRight(boundary) {
		var next = Traversing.next(boundary, 'char');
		if (next) {
			return next;
		}
		if (Html.hasLinebreakingStyle(Boundaries.nextNode(boundary))) {
			return boundary;
		}
		return stepRight(Traversing.next(boundary, 'boundary'));
	}

	/**
	 * Returns a mutable bounding client rectangle from the reference range or
	 * element.
	 *
	 * @private
	 * @param  {Element|Range} reference
	 * @return {Object.<string, number>}
	 */
	function boundingRect(reference) {
		var rect = reference.getClientRects()[0] || reference.getBoundingClientRect();
		return {
			top    : rect.top,
			left   : rect.left,
			width  : rect.width,
			height : rect.height
		};
	}

	/**
	 * Shows a box element according to the dimensions and orientation of `box`.
	 *
	 * @param  {Object.<string, number>} box
	 * @param  {Document}                doc
	 * @return {Element}
	 * @memberOf carets
	 */
	function showHint(box, doc) {
		var elem = doc.querySelector('.aloha-caret-box-hint');
		if (!elem) {
			elem = doc.createElement('div');
			Dom.addClass(elem, 'aloha-caret-box-hint', 'aloha-ephemera');
		}
		Maps.extend(elem.style, {
			'top'        : box.top + 'px',
			'left'       : box.left + 'px',
			'height'     : box.height + 'px',
			'width'      : box.width + 'px',
			'position'   : 'absolute',
			'background' : 'red',
			'opacity'    : 0.2
		});
		Dom.append(elem, doc.body);
		return elem;
	}

	/**
	 * Removes any ".aloha-caret-box-hint" elements in the body of the given
	 * document and returns it.
	 *
	 * @param  {Document} doc
	 * @return {?Element}
	 * @memberOf carets
	 */
	function hideHint(doc) {
		var box = doc.querySelector('.aloha-caret-box-hint');
		if (box) {
			Dom.remove(box);
		}
		return box || null;
	}

	/**
	 * Checks whether or not we find ourselves in a situation in Chrome where it
	 * reports incorrect values when calling `boundingRect` with a collapsed
	 * range that is at a soft visual break.
	 *
	 * @private
	 * @param  {!Object.<string, int>} rect
	 * @param  {!Range}                range
	 * @return {boolean}
	 */
	function isChromeBug(rect, range) {
		if (!Browsers.chrome || !range.collapsed) {
			return false;
		}
		var element = Dom.upWhile(range.startContainer, Dom.isTextNode);
		var size = parseInt(Dom.getComputedStyle(element, 'font-size'), 10);
		return rect.width > size;
	}

	/**
	 * Attempts to calculates the bounding rectangle offsets for the given
	 * range.
	 *
	 * This function is a hack to work around the problems that user agents have
	 * in determining the bounding client rect for collapsed ranges.
	 *
	 * @private
	 * @param  {Range} range
	 * @return {Object.<string, number>}
	 */
	function bounds(range) {
		var rect;
		var expanded = expandRight(range);
		if (expanded) {
			rect = boundingRect(expanded);
			if (rect.width && !isChromeBug(rect, range)) {
				return rect;
			}
		}
		expanded = expandLeft(range);
		if (expanded) {
			rect = boundingRect(expanded);
			rect.left += rect.width;
			return rect;
		}
		return boundingRect(range);
	}

	/**
	 * Gets the bounding box of offets for the given range.
	 *
	 * This function requires the following css:
	 * .aloha-editable br, .aloha-editable br:after { content: "\A"; white-space: pre-line; }
	 *
	 * @param  {!Boundary} start
	 * @param  {Boundary=} end
	 * @return {Object.<string, number>}
	 * @memberOf carets
	 */
	function box(start, end) {
		if (!end) {
			end = start;
		}

		var range = Boundaries.range(start, end);
		var rect = bounds(range);
		var doc = range.commonAncestorContainer.ownerDocument;

		ensureBrStyleFix(doc);

		// Because `rect` should be the box of an expanded range and must
		// therefore have a non-zero width if valid
		if (rect.width > 0) {
			return {
				top    : rect.top + Dom.scrollTop(doc),
				left   : rect.left + Dom.scrollLeft(doc),
				width  : rect.width,
				height : rect.height
			};
		}

		var node = Boundaries.nodeAfter(start)
		        || Boundaries.nodeBefore(start);

		if (node && !Dom.isTextNode(node)) {
			rect = boundingRect(node);
			if (rect) {
				return {
					top    : rect.top + Dom.scrollTop(doc),
					left   : rect.left + Dom.scrollLeft(doc),
					width  : rect.width,
					height : rect.height
				};
			}
		}

		// <li>{}</li>
		node = Boundaries.container(start);

		return {
			top    : node.offsetTop + Dom.scrollTop(doc),
			left   : node.offsetLeft + Dom.scrollLeft(doc),
			width  : node.offsetWidth,
			height : parseInt(Dom.getComputedStyle(node, 'line-height'), 10)
		};
	}

	return {
		box      : box,
		showHint : showHint,
		hideHint : hideHint
	};
});

/**
 * animation.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('animation',['functions'], function (Fn) {
	

	var requestAnimationFrame = window.requestAnimationFrame
	                         || window.webkitRequestAnimationFrame
	                         || window.mozRequestAnimationFrame
	                         || window.oRequestAnimationFrame
	                         || window.msRequestAnimationFrame
	                         || function (fn) {window.setTimeout(fn, 1000/60);};

	function easeLinear(percent, elapsed, start, end, total) {
		return start + (end - start) * percent;
	}

	function easeOutQuint(x, t, b, c, d) {
		return c * ((t = t / d - 1) * t * t * t * t + 1) + b;
	}

	function easeInOutQuint(x, t, b, c, d) {
		return ((t /= d / 2) < 1)
			 ? (c / 2 * t * t * t * t * t + b)
			 : (c / 2 * ((t -= 2) * t * t * t * t + 2) + b);
	}

	function step(state) {
		var now = new Date().getTime();
		if (!state.starttime) {
			state.starttime = now;
		}
		var elapsed = now - state.starttime;
		var percent = Math.min(1, elapsed / state.duration);
		var position = state.easing(percent, elapsed, 0, 1, state.duration);
		var abort = state.interval(state.start + (state.delta * position), percent);
		return abort ? 1 : percent;
	}

	function animate(start, end, easing, duration, interval) {
		var state = {
			start    : start,
			delta    : end - start,
			duration : duration || 1,
			interval : interval || Fn.noop,
			easing   : easing || easeOutQuint
		};
		(function tick() {
			if (step(state) < 1) {
				requestAnimationFrame(tick);
			}
		}());
		return state;
	}

	return {
		step                  : step,
		animate               : animate,
		easeLinear            : easeLinear,
		easeOutQuint          : easeOutQuint,
		easeInOutQuint        : easeInOutQuint,
		requestAnimationFrame : requestAnimationFrame
	};
});

/** undo.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace undo
 */
define('undo',[
	'arrays',
	'maps',
	'dom',
	'mutation',
	'boundaries',
	'functions',
	'ranges',
	'content', // Hack for require-proto
	'traversing' // Hack for require-proto
], function (
	Arrays,
	Maps,
	Dom,
	Mutation,
	Boundaries,
	Fn,
	Ranges,
	__hack1__,
	__hack2__
) {
	

	// Deprecated functions from assert.js

	function assertEqual(a, b) {
		if (a !== b) {
			throw Error('assertion error ' + a + ' !== ' + b);
		}
	}

	function assertNotEqual(a, b) {
		if (a === b) {
			throw Error('assertion error ' + a + ' === ' + b);
		}
	}

	function assertFalse(value) {
		assertEqual(value, false);
	}

	function assertTrue(value) {
		assertEqual(value, true);
	}

	function assertError() {
		throw Error();
	}

	// Deprecated functions from boundaries.js

	function beforeNodeBoundary(node) {
		return [node.parentNode, Dom.nodeIndex(node)];
	}

	function nodeAtBoundary(boundary) {
		return Dom.nodeAtOffset(boundary[0], boundary[1]);
	}

	function nodeBeforeBoundary(boundary) {
		boundary = Boundaries.normalize(boundary);
		if (!Boundaries.isNodeBoundary(boundary)) {
			return boundary[0];
		}
		return Boundaries.isAtStart(boundary) ? null : Dom.nthChild(boundary[0], boundary[1] - 1);
	}

	function nodeAfterBoundary(boundary) {
		boundary = Boundaries.normalize(boundary);
		if (!Boundaries.isNodeBoundary(boundary)) {
			return boundary[0].nextSibling;
		}
		return Boundaries.isAtEnd(boundary) ? null : Dom.nthChild(boundary[0], boundary[1]);
	}

	function precedingTextLength(boundary) {
		boundary = Boundaries.normalize(boundary);
		var node = nodeBeforeBoundary(boundary);
		var len = 0;
		if (!Boundaries.isNodeBoundary(boundary)) {
			len += boundary[1];
			node = node.previousSibling;
		}
		while (node && Dom.isTextNode(node)) {
			len += Dom.nodeLength(node);
			node = node.previousSibling;
		}
		return len;
	}

	/**
	 * Creates a new undo context.
	 *
	 * The undo context holds an assortment of data items used across
	 * many of the undo functions.
	 *
	 * Should be treated as a black box.
	 *
	 * @param elem {Element}
	 *        The element whose mutations are to be observed and made
	 *        undoable/redoable.
	 * @param opts {Object.<string,*>}
	 *        A map of options:
	 *        noMutationObserver - whether or not to use the MutationObserver
	 *          API to observe changes,
	 *        maxCombineChars - how many character to combine to a
	 *          single change (default 20).
	 *        maxHistory - how many items to keep in the history
	 *          (default 1000).
	 * @return {Undo}
	 * @memberOf undo
	 */
	function Context(elem, opts) {
		opts = Maps.merge({
			maxCombineChars: 20,
			maxHistory: 1000
		}, opts);
		var context = {
			elem: elem,
			observer: null,
			stack: [],
			frame: null,
			opts: opts,
			history: [],
			historyIndex: 0
		};
		context.observer = (!opts.noMutationObserver && window.MutationObserver
		                    ? ChangeObserverUsingMutationObserver()
		                    : ChangeObserverUsingSnapshots());
		return context;
	}

	/**
	 * Creates a changeSet.
	 *
	 * @param meta {*} the metadat of the changeSet
	 * @param changes {Array.<Change>} an array of changes
	 * @param selection {RangeUpdateChange} reflects the change of the
	 *        range from before to after all changes in this changeSet.
	 * @return {ChangeSet}
	 */
	function makeChangeSet(meta, changes, selection) {
		return {
			changes: changes,
			meta: meta,
			selection: selection
		};
	}

	/**
	 * Whether two paths are equal.
	 *
	 * @param pathA {Path}
	 * @param pathB {Path}
	 * @return {boolean}
	 */
	function pathEquals(pathA, pathB) {
		return Arrays.equal(pathA, pathB, Arrays.equal);
	}

	function stepDownPath(path, containerName, off) {
		path.push([off, containerName]);
	}

	/**
	 * Creates a path from the given container down to the given node.
	 *
	 * @param container {Element}
	 * @param container {Node}
	 * @return {Path}
	 */
	function nodePath(container, node) {
		var path = [];
		while (node && container !== node) {
			var parent = node.parentNode;
			if (!parent) {
				return [];
			}
			stepDownPath(path, parent.nodeName, Dom.normalizedNodeIndex(node));
			node = parent;
		}
		path.reverse();
		return path;
	}

	/**
	 * Creates a boundary from the given path in the given container.
	 *
	 * @param container {Element} at which the path begins.
	 * @param path {Path} which goes down from the given container to the boundary.
	 * @return {Boundary} the boundary at the given path.
	 */
	function boundaryFromPath(container, path) {
		for (var i = 0; i < path.length - 1; i++) {
			var step = path[i];
			assertEqual(step[1], container.nodeName);
			container = Dom.normalizedNthChild(container, step[0]);
		}
		var lastStep = Arrays.last(path);
		var off = lastStep[0];
		container = Dom.nextWhile(container, Dom.isEmptyTextNode);
		// NB: container must be non-null at this point.
		assertEqual(lastStep[1], container.nodeName);
		if (Dom.isTextNode(container)) {
			// Because text offset paths with value 0 are invalid.
			assertNotEqual(off, 0);
			while (off > Dom.nodeLength(container)) {
				assertTrue(Dom.isTextNode(container));
				off -= Dom.nodeLength(container);
				container = container.nextSibling;
			}
			// Because we may have stepped out of a text node.
			if (!Dom.isTextNode(container)) {
				assertEqual(off, 0);
				container = container.parentNode;
				off = Dom.nodeIndex(container);
			}
		} else {
			off = Dom.realFromNormalizedIndex(container, off);
		}
		return Boundaries.normalize([container, off]);
	}

	function endOfNodePath(container, node) {
		var path = nodePath(container, node);
		var numChildren = Dom.normalizedNumChildren(node);
		stepDownPath(path, node.nodeName, numChildren);
		return path;
	}

	/**
	 * Creates a path from a boundary.
	 *
	 * A path is an array of arrays where each member represents the
	 * offset of a child in a parent. The empty array represents the
	 * path of the top-most container from which the path was
	 * calculated.
	 *
	 * Only the last step in a path may be the offset in a text node.
	 *
	 * If the nodes before a boundary are text nodes, the last step will
	 * always be the offset in a text node, and the combined length of
	 * the text nodes before the boundary will be used as the
	 * offset. This is true even if the node following the boundary is
	 * not a text node, and the path could theoretically be represented
	 * by the next node's offset in the element parent. That's because
	 * the path represents the path in the DOM based on the normalized
	 * number of previous siblings, and doesn't depend on any next
	 * siblings, and if we didn't always include the text offset before
	 * the path, the path would look different if constructed from a DOM
	 * that is structurally equal before the boundary, but contains text
	 * nodes directly after the boundary.
	 *
	 * Paths with textOff = 0 are invalid because empty text nodes
	 * should be treated as if they are not present and if a path in an
	 * empty text node is taken, the same path would become invalid when
	 * the empty text node is removed. This is true even when the text
	 * node is not empty because we can't depend on what occurs after
	 * the boundary (see previous paragraph).
	 *
	 * Paths reflect the normalized DOM - offsets will be calculated
	 * assuming that empty text nodes don't exist and that subsequent
	 * text nodes are counted as one.
	 *
	 * @param container {Element}
	 *        The container from which to start calculating the path.
	 *        Must contain the given boundary.
	 * @param boundary {Boundary}
	 *        Must be contained by the given container
	 * @return {Path}
	 *        The path from the given container to the given boundary.
	 */
	function pathFromBoundary(container, boundary) {
		boundary = Boundaries.normalize(boundary);
		var path;
		var textOff = precedingTextLength(boundary);
		if (textOff) {
			var node = nodeBeforeBoundary(boundary);
			// Because nodePath() would use the normalizedNodeIndex
			// which would translate an empty text node after a
			// non-empty text node to the normalized offset after the
			// non-empty text node.
			node = Dom.prevWhile(node, Dom.isEmptyTextNode);
			path = nodePath(container, node);
			stepDownPath(path, '#text', textOff);
		} else if (Boundaries.isAtEnd(boundary)) {
			path = endOfNodePath(container, boundary[0]);
		} else {
			path = nodePath(container, nodeAfterBoundary(boundary));
		}
		return path;
	}

	/**
	 * Useful for when the path to be generated should only represent a
	 * fragment of a complete path, and mustn't include the last step,
	 * which may otherwise be a text container (which must only occur as
	 * the last step of a path and can't therefore be composed).
	 */
	function incompletePathFromBoundary(container, boundary) {
		boundary = Boundaries.normalize(boundary);
		var node = nodeAfterBoundary(boundary);
		// Because if the boundary is between two text nodes, index
		// normalization performed by nodePath() will use the offset of
		// the previous text node, while an incomplete path must point
		// to the normalized index of the next element node.
		if (precedingTextLength(boundary)) {
			node = Dom.nextWhile(node, Dom.isTextNode);
		}
		var path;
		if (node) {
			path = nodePath(container, node);
		} else {
			path = endOfNodePath(container, boundary[0]);
		}
		return path;
	}

	/**
	 * Create a path from the given container to immediately before the
	 * given node.
	 */
	function pathBeforeNode(container, node) {
		return pathFromBoundary(container, beforeNodeBoundary(node));
	}

	function recordRange(container, range) {
		if (!range) {
			return null;
		}
		var start = pathFromBoundary(container, Boundaries.fromRangeStart(range));
		var end = pathFromBoundary(container, Boundaries.fromRangeEnd(range));
		return start && end ? {start: start, end: end} : null;
	}

	function takeRecords(context, frame) {
		if (frame.opts.noObserve) {
			context.observer.discardChanges();
		} else {
			var changes = context.observer.takeChanges();
			if (changes.length) {
				frame.records.push({changes: changes});
			}
		}
	}

	function partitionRecords(context, leavingFrame, lowerFrame, upperFrame) {
		if ((upperFrame.opts.partitionRecords && !upperFrame.opts.noObserve)
		    || (!!lowerFrame.opts.noObserve !== !!upperFrame.opts.noObserve)) {
			takeRecords(context, leavingFrame);
		}
	}

	/**
	 * This function is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf undo
	 */
	function close(context) {
		if (context.frame) {
			context.observer.disconnect();
			context.frame = null;
		}
	}

	/**
	 * Enters a new frame in the given undo context.
	 * 
	 * @param context {Undo}
	 * @param opts {Object.<string,*>}
	 *        A map of options:
	 *        noObserve - whether to observe changes. If true, changes
	 *          must be supplied via the result argument of leave().
	 *          Applies recursively to all nested frames.
	 *        partitionRecords - whether to split up changes happening
	 *          inside this frame and frames direcly below this frame (but
	 *          not deeper).
	 *        oldRange - a range to record that reflects the range
	 *          before any changes in this frame happen.
	 * @return {void}
	 * @memberOf undo
	 */
	function enter(context, opts) {
		opts = opts || {};
		var upperFrame = context.frame;
		var observer = context.observer;
		var elem = context.elem;
		var noObserve = opts.noObserve || (upperFrame && upperFrame.opts.noObserve);
		var frame = {
			opts: Maps.merge(opts, {noObserve: noObserve}),
			records: [],
			oldRange: recordRange(elem, opts.oldRange),
			newRange: null
		};
		if (upperFrame) {
			partitionRecords(context, upperFrame, frame, upperFrame);
			context.stack.push(upperFrame);
		} else {
			observer.observeAll(elem);
		}
		context.frame = frame;
	}

	/**
	 * Leave a frame in the given undo context.
	 *
	 * @param context {Undo}
	 * @param result {Object.<...string>}
	 * @return {Frame}
	 * @memberOf undo
	 */
	function leave(context, result) {
		var frame = context.frame;
		var upperFrame = context.stack.pop();
		if (upperFrame) {
			partitionRecords(context, frame, frame, upperFrame);
		} else {
			takeRecords(context, frame);
			close(context);
		}
		var noObserve = frame.opts.noObserve;
		// Because we expect either a result to be returned by the
		// capture function, or observed by the observer, but not both.
		assertFalse(!!(!noObserve && result && result.changes));
		if (noObserve && result && result.changes && result.changes.length) {
			frame.records.push({changes: result.changes});
		}
		frame.newRange = recordRange(context.elem, result && result.newRange);
		if (upperFrame) {
			upperFrame.records.push({frame: frame});
			context.frame = upperFrame;
		}
		return frame;
	}

	/**
	 * Enter/leave a frame before/after calling the given function.
	 *
	 * @param context {Undo}
	 * @param opts {Object.<string,*>} given as the opts argument to enter()
	 * @param {function(void):{Object.<string,*>}} given as the result argument to leave()
	 * @return {Frame} the captured frame
	 * @memberOf undo
	 */
	function capture(context, opts, fn) {
		enter(context, opts);
		var result;
//		try {
			result = fn();
//		} catch (e) {
			// TODO for some reason, whether I rethrow here or if I
			// remove the catch (but not the try{}finally{}) completely,
			// my version of Chrome just ignores the exception. Maybe
			// it's a bug that just happens in the version of Chrome I'm
			// using?
//			window.console && window.console.log(e);
//			throw e;
//		} finally {
			return leave(context, result);
//		}
	}

	function captureOffTheRecord(context, opts, fn) {
		var frame = capture(context, Maps.merge(opts, {noObserve: true}), fn);
		// Because leave() will push the captured frame onto the
		// upperFrame.
		var upperFrame = context.frame;
		if (upperFrame) {
			upperFrame.records.pop();
		}
		return frame;
	}

	function makeInsertDeleteChange(type, path, content) {
		return {
			type: type,
			path: path,
			content: content
		};
	}

	function makeInsertChange(path, content) {
		return makeInsertDeleteChange('insert', path, content);
	}

	function makeDeleteChange(path, content) {
		return makeInsertDeleteChange('delete', path, content);
	}

	function makeUpdateAttrChange(path, node, recordAttrs) {
		var attrs = [];
		Maps.forEach(recordAttrs, function (attr) {
			var name = attr.name;
			var ns = attr.ns;
			attrs.push({
				name: name,
				ns: ns,
				oldValue: attr.oldValue,
				newValue: Dom.getAttrNS(node, ns, name)
			});
		});
		return {
			type: 'update-attr',
			path: path,
			attrs: attrs
		};
	}

	function makeRangeUpdateChange(oldRange, newRange) {
		return {
			type: 'update-range',
			oldRange: oldRange,
			newRange: newRange
		};
	}

	var INSERT = 0;
	var UPDATE_ATTR = 1;
	var UPDATE_TEXT = 2;
	var DELETE_FLAG = 4;
	var DELETE = DELETE_FLAG;
	var COMPOUND_DELETE = DELETE_FLAG + 1;

	function makeDelete(node, target, prevSibling) {
		return {
			type: DELETE,
			node: node,
			target: target,
			prevSibling: prevSibling,
			contained: [],
			updateAttr: null,
			updateText: null
		};
	}

	function makeMultiDelete(delRecords, target, prevSibling) {
		return {
			type: COMPOUND_DELETE,
			records: delRecords,
			target: target,
			prevSibling: prevSibling
		};
	}

	function makeInsert(node) {
		return {type: INSERT, node: node, contained: []};
	}

	function makeUpdateAttr(node, attrs) {
		return {type: UPDATE_ATTR, node: node, attrs: {}};
	}

	function makeUpdateText(node, oldValue) {
		return {type: UPDATE_TEXT, node: node, oldValue: oldValue};
	}

	// NB: All insert-delete sequences in this table are no-ops:
	// insert-delete               => no-op
	// insert-delete-insert        => insert (in inserted)
	// insert-delete-insert-delete => no-op
	// delete-insert               => move   (in delsBy*, inserted)
	// delete-insert-delete        => delete (in delsBy*)
	// delete-insert-delete-insert => move   (in delsBy*, inserted)
	function normalizeInsertDeletePreserveAnchors(moves, inserted, delsByPrevSibling, delsByTarget) {
		moves.forEach(function (move) {
			var node = move.node;
			var id = Dom.ensureExpandoId(node);
			var type = move.type;
			if (DELETE === type) {
				var prevSibling = move.prevSibling;
				var target = move.target;
				var ref = prevSibling ? prevSibling : target;
				var map = prevSibling ? delsByPrevSibling : delsByTarget;
				var refId = Dom.ensureExpandoId(ref);
				var dels = map[refId] = map[refId] || [];

				if (inserted[id]) {
					// Because an insert-delete sequence will become a
					// no-op, and we just pretend that it didn't happen.
					delete inserted[id];
				} else {
					dels.push(move);
				}

				// Because it may be that the deleted node is the
				// prevSibling reference of a previous delete.
				var delsHavingRefs = delsByPrevSibling[id];
				if (delsHavingRefs) {
					delete delsByPrevSibling[id];
					// Because by eliminating delete-inserts above we
					// may have eliminated the first delete in the
					// delete sequence that must have a valid anchor.
					if (!dels.length && delsHavingRefs.length) {
						var refDel = delsHavingRefs[0];
						refDel.prevSibling = prevSibling;
						refDel.target = target;
					}
					map[refId] = dels.concat(delsHavingRefs);
				}
			} else if (INSERT === type) {
				assertFalse(!!inserted[id]);
				inserted[id] =  move;
			} else {
				// NB: moves should only contains INSERTs and DELETEs
				// (not COMPOUND_DELETEs).
				assertError();
			}
		});
	}

	function records(record) {
		return (COMPOUND_DELETE === record.type) ? record.records : [record];
	}

	function insertFollowedByDelete(recordA, recordB) {
		var prevB = recordB.prevSibling;
		var targetB = recordB.target;
		var node = recordA.node;
		if (prevB) {
			if (prevB === node || Dom.contains(prevB, node)) {
				return true;
			}
			// TODO Dom.contains(node, prevB) probably not needed
			return !Dom.followedBy(prevB, node) && !Dom.contains(node, prevB);
		} else {
			if (targetB === node || Dom.contains(targetB, node)) {
				return false;
			}
			// TODO Dom.contains(node, prevB) probably not needed
			return !Dom.followedBy(targetB, node) && !Dom.contains(node, targetB);
		}
	}

	function insertFollowedByInsert(recordA, recordB) {
		return Dom.followedBy(recordA.node, recordB.node);
	}
	
	function prevSiblingFollowedByDelete(prevA, recordB) {
		var prevB = recordB.prevSibling;
		var targetB = recordB.target;
		if (prevB) {
			if (Dom.contains(prevB, prevA)) {
				return true;
			}
			if (Dom.contains(prevA, prevB)) {
				return false;
			}
			return Dom.followedBy(prevA, prevB);
		} else {
			if (prevA === targetB) {
				return false;
			}
			if (Dom.contains(targetB, prevA) || Dom.contains(prevA, targetB)) {
				return false;
			}
			return Dom.followedBy(prevA, targetB);
		}
	}

	function deleteFollowedByDelete(recordA, recordB) {
		var prevA = recordA.prevSibling;
		var prevB = recordB.prevSibling;
		var targetA = recordA.target;
		var targetB = recordB.target;
		if (prevA) {
			return prevSiblingFollowedByDelete(prevA, recordB);
		} else if (prevB) {
			return !prevSiblingFollowedByDelete(prevB, recordA);
		} else {
			return Dom.followedBy(targetA, targetB);
		}
	}

	function compareRecords(recordA, recordB) {
		var deleteA = (DELETE_FLAG & recordA.type);
		var deleteB = (DELETE_FLAG & recordB.type);
		var follows;
		if (deleteA && deleteB) {
			follows = deleteFollowedByDelete(recordA, recordB);
		} else if (!deleteA && !deleteB) {
			follows = insertFollowedByInsert(recordA, recordB);
		} else if (!deleteA && deleteB) {
			follows = insertFollowedByDelete(recordA, recordB);
		} else if (deleteA && !deleteB) {
			follows = !insertFollowedByDelete(recordB, recordA);
		}
		return follows ? -1 : 1;
	}

	function sortRecordTree(tree) {
		tree.sort(compareRecords);
		tree.forEach(function (record) {
			records(record).forEach(function (record) {
				if (record.contained && (DELETE_FLAG & record.type)) {
					sortRecordTree(record.contained);
				}
			});
		});
	}

	function fillOutContained(container, recs) {
		var index = {};
		recs.forEach(function (record) {
			records(record).forEach(function (record) {
				var type = record.type;
				if (!(type & DELETE_FLAG) && type !== INSERT) {
					return;
				}
				var id = Dom.ensureExpandoId(record.node);
				// NB The same node may have one insert and one or more
				// deletes. It may have more than one delete because it
				// may have been inserted in a not-observed element, and
				// then removed again from it after the not-observed
				// element was inserted itself.
				var containerRecords = index[id] || [];
				containerRecords.push(record);
				index[id] = containerRecords;
			});
		});
		var containerId = Dom.ensureExpandoId(container);
		assertFalse(!!index[containerId]);
		var containerInsert = makeInsert(container);
		index[containerId] = [containerInsert];
		recs.forEach(function (record) {
			var target = ((DELETE & record.type)
			              ? record.target
			              : record.node.parentNode);
			var ancestor = Dom.upWhile(target, function (ancestor) {
				return !index[Dom.ensureExpandoId(ancestor)];
			});
			if (!ancestor) {
				return;
			}
			var containerRecords = index[Dom.ensureExpandoId(ancestor)];
			containerRecords.forEach(function (containerRecord) {
				containerRecord.contained.push(record);
			});
		});
		return containerInsert.contained;
	}

	function makeRecordTree(container, moves, updateAttr, updateText) {
		var delsByPrevSibling = {};
		var delsByTarget = {};
		var inserted = {};
		normalizeInsertDeletePreserveAnchors(moves, inserted, delsByPrevSibling, delsByTarget);
		var delss = Maps.vals(delsByPrevSibling).concat(Maps.vals(delsByTarget));
		// Because normalizeInsertDeletePreserveAnchors may cause empty
		// del arrays.
		delss = delss.filter(function (dels) {
			return dels.length;
		});
		function consumeUpdates(record) {
			var id = Dom.ensureExpandoId(record.node);
			if (DELETE === record.type) {
				record.updateAttr = updateAttr[id];
				record.updateText = updateText[id];
			}
			delete updateAttr[id];
			delete updateText[id];
		}
		var dels = delss.map(function (dels){
			var refDel = dels[0];
			dels.forEach(consumeUpdates);
			return makeMultiDelete(dels, refDel.target, refDel.prevSibling);
		});
		var inss = Maps.vals(inserted);
		inss.forEach(consumeUpdates);
		var tree = fillOutContained(
			container,
			dels.concat(inss)
				.concat(Maps.vals(updateAttr))
				.concat(Maps.vals(updateText))
		);
		sortRecordTree(tree);
		return tree;
	}

	function delPath(container, delRecord, incomplete) {
		var prevSibling = delRecord.prevSibling;
		var path;
		var boundary;
		if (prevSibling) {
			var off = Dom.nodeIndex(prevSibling) + 1;
			boundary = [prevSibling.parentNode, off];
			path = (incomplete
			        ? incompletePathFromBoundary(container, boundary)
			        : pathFromBoundary(container, boundary));
		} else {
			var target = delRecord.target;
			boundary = [target, 0];
			path = (incomplete
			        ? incompletePathFromBoundary(container, boundary)
			        : pathFromBoundary(container, boundary));
		}
		return path;
	}

	function reconstructNodeFromDelRecord(delRecord) {
		var node = delRecord.node;
		var reconstructedNode;
		if (Dom.isTextNode(node)) {
			var updateText = delRecord.updateText;
			if (updateText) {
				reconstructedNode = node.ownerDocument.createTextNode(updateText.oldValue);
			} else {
				reconstructedNode = Dom.clone(node);
			}
		} else {
			reconstructedNode = Dom.clone(node);
			var updateAttr = delRecord.updateAttr;
			if (updateAttr) {
				Maps.forEach(updateAttr.attrs, function (attr) {
					Dom.setAttrNS(reconstructedNode, attr.ns, attr.name, attr.oldValue);
				});
			}
		}
		return reconstructedNode;
	}

	function generateChanges(containerPath, container, changes, recordTree) {
		var lastInsertContent = null;
		var lastInsertNode = null;
		recordTree.forEach(function (record) {
			var type = record.type;
			var path;
			var node;
			if (COMPOUND_DELETE === type) {
				lastInsertNode = null;
				path = containerPath.concat(delPath(container, record));
				var parentPath = containerPath.concat(delPath(container, record, true));
				var lastDeleteContent = null;
				record.records.forEach(function (record) {
					var contained = record.contained;
					if (contained.length) {
						generateChanges(parentPath, record.node, changes, contained);
						lastDeleteContent = null;
					}
					var delNode = reconstructNodeFromDelRecord(record);
					if (lastDeleteContent) {
						lastDeleteContent.push(delNode);
					} else {
						lastDeleteContent = [delNode];
						changes.push(makeDeleteChange(path, lastDeleteContent));
					}
				});
			} else if (INSERT === type) {
				node = record.node;
				path = containerPath.concat(pathBeforeNode(container, node));
				if (lastInsertNode && lastInsertNode === node.previousSibling) {
					lastInsertContent.push(Dom.clone(node));
				} else {
					lastInsertContent = [Dom.clone(node)];
					changes.push(makeInsertChange(path, lastInsertContent));
				}
				lastInsertNode = node;
			} else if (UPDATE_ATTR === type) {
				lastInsertNode = null;
				node = record.node;
				path = containerPath.concat(pathBeforeNode(container, node));
				changes.push(makeUpdateAttrChange(path, node, record.attrs));
			} else if (UPDATE_TEXT === type) {
				lastInsertNode = null;
				node = record.node;
				path = containerPath.concat(pathBeforeNode(container, node));
				changes.push(makeDeleteChange(path, [node.ownerDocument.createTextNode(record.oldValue)]));
				changes.push(makeInsertChange(path, [Dom.clone(node)]));
			} else {
				// NB: only COMPOUND_DELETEs should occur in a recordTree,
				// DELETEs should not except as part of a COMPOUND_DELETE.
				assertError();
			}
		});
	}

	function changesFromMutationRecords(container, records) {
		var updateAttr = {};
		var updateText = {};
		var moves = [];
		records.forEach(function (record) {
			var target = record.target;
			var oldValue = record.oldValue;
			var type = record.type;
			var id;
			if ('attributes' === type) {
				var name = record.attributeName;
				var ns = record.attributeNamespace;
				id = Dom.ensureExpandoId(target);
				var updateAttrRecord = updateAttr[id] = updateAttr[id] || makeUpdateAttr(target, {});
				var attrs = updateAttrRecord.attrs;
				var attr = {oldValue: oldValue, name: name, ns: ns};
				var key = name + ' ' + ns;
				attrs[key] = attrs[key] || attr;
			} else if ('characterData' === type) {
				id = Dom.ensureExpandoId(target);
				updateText[id] = updateText[id] || makeUpdateText(target, oldValue);
			} else if ('childList' === type) {
				var prevSibling = record.previousSibling;
				Arrays.coerce(record.removedNodes).forEach(function (node) {
					moves.push(makeDelete(node, target, prevSibling));
				});
				Arrays.coerce(record.addedNodes).forEach(function (node) {
					moves.push(makeInsert(node));
				});
			} else {
				assertError();
			}
		});
		var recordTree = makeRecordTree(container, moves, updateAttr, updateText);
		var changes = [];
		var rootPath = [];
		generateChanges(rootPath, container, changes, recordTree);
		return changes;
	}

	function changesFromSnapshots(before, after) {
		var path = [];
		stepDownPath(path, after.nodeName, 0);
		var changes = [];
		// NB: We don't clone the children because a snapshot is
		// already a copy of the actual content and is supposed to
		// be immutable.
		changes.push(makeDeleteChange(path, Dom.children(before)));
		changes.push(makeInsertChange(path, Dom.children(after)));
		return changes;
	}

	function ChangeObserverUsingMutationObserver() {
		var observedElem = null;
		var pushedRecords = [];
		var observer = new MutationObserver(function (records) {
			pushedRecords = pushedRecords.concat(records);
		});

		function observeAll(elem) {
			var observeAllFlags = {
				'childList': true,
				'attributes': true,
				'characterData': true,
				'subtree': true,
				'attributeOldValue': true,
				'characterDataOldValue': true
			};
			observer.observe(elem, observeAllFlags);
			observedElem = elem;
		}

		function takeChanges() {
			var records =  pushedRecords.concat(observer.takeRecords());
			pushedRecords.length = 0;
			return changesFromMutationRecords(observedElem, records);
		}

		function disconnect() {
 			observedElem = null;
			pushedRecords.length = 0;
			observer.disconnect();
			observer = null;
		}

		return {
			observeAll: observeAll,
			takeChanges: takeChanges,
			discardChanges: takeChanges,
			disconnect: disconnect
		};
	}

	function ChangeObserverUsingSnapshots() {
		var observedElem = null;
		var beforeSnapshot = null;

		function observeAll(elem) {
			observedElem = elem;
			beforeSnapshot = Dom.clone(elem);
		}

		function takeChanges() {
			if (Dom.isEqualNode(beforeSnapshot, observedElem)) {
				return [];
			}
			var before = beforeSnapshot;
			var after = Dom.clone(observedElem);
			beforeSnapshot = after;
			return changesFromSnapshots(before, after);
		}

		// TODO instead of discarding the snapshot and making a new one,
		// we could accept the changes that were generated instead and
		// apply them to the snapshot, which would be faster for big
		// documents.
		function discardChanges() {
			beforeSnapshot = Dom.clone(observedElem);
		}

		function disconnect() {
			observedElem = null;
			beforeSnapshot = null;
		}

		return {
			observeAll: observeAll,
			takeChanges: takeChanges,
			discardChanges: discardChanges,
			disconnect: disconnect
		};
	}

	function applyChange(container, change, range, ranges, textNodes) {
		var type = change.type;
		var boundary;
		var node;
		var parent;
		if ('update-attr' === type) {
			boundary = boundaryFromPath(container, change.path);
			node = nodeAfterBoundary(boundary);
			change.attrs.forEach(function (attr) {
				Dom.setAttrNS(node, attr.ns, attr.name, attr.newValue);
			});
		} else if ('update-range' === type) {
			var newRange = change.newRange;
			if (range && newRange) {
				var startBoundary = boundaryFromPath(container, newRange.start);
				var endBoundary = boundaryFromPath(container, newRange.end);
				Boundaries.setRange(range, startBoundary, endBoundary);
			}
		} else if ('insert' === type) {
			boundary = boundaryFromPath(container, change.path);
			change.content.forEach(function (node) {
				var insertNode = Dom.clone(node);
				if (Dom.isTextNode(insertNode)) {
					textNodes.push(insertNode);
				}
				boundary = Mutation.insertNodeAtBoundary(insertNode, boundary, true, ranges);
			});
		} else if ('delete' === type) {
			boundary = boundaryFromPath(container, change.path);
			boundary = Mutation.splitBoundary(boundary, ranges);
			node = nodeAtBoundary(boundary);
			parent = node.parentNode;
			change.content.forEach(function (removedNode) {
				var next;
				if (Dom.isTextNode(removedNode)) {
					var removedLen = Dom.nodeLength(removedNode);
					while (removedLen) {
						assertEqual(node.nodeName, removedNode.nodeName);
						var len = Dom.nodeLength(node);
						if (removedLen >= len) {
							next = node.nextSibling;
							Mutation.removePreservingRanges(node, ranges);
							removedLen -= len;
							node = next;
						} else {
							boundary = Mutation.splitBoundary([node, removedLen], ranges);
							var nodeBeforeSplit = nodeBeforeBoundary(boundary);
							var nodeAfterSplit = nodeAfterBoundary(boundary);
							Mutation.removePreservingRanges(nodeBeforeSplit, ranges);
							removedLen = 0;
							textNodes.push(nodeAfterSplit);
							node = nodeAfterSplit;
						}
					}
				} else {
					next = node.nextSibling;
					assertEqual(node.nodeName, removedNode.nodeName);
					Mutation.removePreservingRanges(node, ranges);
					node = next;
				}
			});
		} else {
			assertError();
		}
	}

	function applyChanges(container, changes, ranges) {
		var textNodes = [];
		changes.forEach(function (change) {
			applyChange(container, change, null, ranges, textNodes);
		});
		textNodes.forEach(function (node) {
			Mutation.joinTextNode(node, ranges);
		});
	}

	function applyChangeSet(container, changeSet, range, ranges) {
		applyChanges(container, changeSet.changes, ranges);
		if (range && changeSet.selection) {
			applyChange(container, changeSet.selection, range, ranges, []);
		}
	}

	function inverseChange(change) {
		var type = change.type;
		var inverse;
		if ('update-attr' === type) {
			inverse = Maps.merge(change, {
				attrs: change.attrs.map(function (attr) {
					return Maps.merge(attr, {oldValue: attr.newValue, newValue: attr.oldValue});
				})
			});
		} else if ('update-range' === type) {
			inverse = Maps.merge(change, {
				oldRange: change.newRange,
				newRange: change.oldRange
			});
		} else if ('insert' === type) {
			inverse = Maps.merge(change, {type: 'delete'});
		} else if ('delete' === type) {
			inverse = Maps.merge(change, {type: 'insert'});
		} else {
			assertError();
		}
		return inverse;
	}

	function inverseChangeSet(changeSet) {
		var changes = changeSet.changes.slice(0).reverse().map(inverseChange);
		return makeChangeSet(changeSet.meta, changes, inverseChange(changeSet.selection));
	}

	function collectChanges(context, frame) {
		var collectedChanges = [];
		frame.records.forEach(function (record) {
			var changes;
			var nestedFrame = record.frame;
			if (nestedFrame) {
				changes = collectChanges(context, nestedFrame);
			} else {
				changes = record.changes;
			}
			collectedChanges = collectedChanges.concat(changes);
		});
		return collectedChanges;
	}

	function changeSetFromFrameHavingChanges(context, frame, changes) {
		var rangeUpdateChange = makeRangeUpdateChange(frame.oldRange, frame.newRange);
		return makeChangeSet(frame.opts.meta, changes, rangeUpdateChange);
	}

	/**
	 * Given a frame, creates a changeSet from it.
	 *
	 * @param context {Undo}
	 * @param frame {Frame}
	 * @return {ChangeSet}
	 */
	function changeSetFromFrame(context, frame) {
		var changes = collectChanges(context, frame);
		return changeSetFromFrameHavingChanges(context, frame, changes);
	}

	function partitionedChangeSetsFromFrame(context, frame) {
		var changeSets = [];
		frame.records.forEach(function (record) {
			var changeSet;
			var nestedFrame = record.frame;
			if (nestedFrame) {
				var changes = collectChanges(context, nestedFrame);
				changeSet = changeSetFromFrameHavingChanges(context, nestedFrame, changes);
			} else {
				changeSet = changeSetFromFrameHavingChanges(context, frame, record.changes);
			}
			changeSets.push(changeSet);
		});
		return changeSets;
	}

	function combineChanges(oldChangeSet, newChangeSet, opts) {
		var oldChanges = oldChangeSet.changes;
		var newChanges = newChangeSet.changes;
		if (!oldChanges.length || !newChanges.length) {
			return null;
		}
		var oldType = oldChangeSet.meta && oldChangeSet.meta.type;
		var newType = newChangeSet.meta && newChangeSet.meta.type;
		// TODO combine enter as the first character of a sequence of
		// text inserts (currently will return null below because we
		// only handle text boundaries).
		if (!(('typing' === oldType || 'enter' === oldType)
		      && 'typing' === newType)) {
			return null;
		}
		var oldChange = oldChanges[0];
		var newChange = newChanges[0];
		var oldPath = oldChange.path;
		var newPath = newChange.path;
		var oldStep = Arrays.last(oldPath);
		var newStep = Arrays.last(newPath);
		// Because the text inserts may have started at a node boundary
		// but we expect text steps below, we'll just pretend they
		// started at the start of a text node.
		if (oldStep && '#text' !== oldStep[1]) {
			oldStep = ['#text', 0];
			oldPath = oldPath.concat([oldStep]);
		}
		if (oldChange.type !== 'insert'
		    || oldChange.type !== newChange.type
		    || oldStep[1] !== '#text'
		    || oldStep[1] !== newStep[1]
		    || 1 !== oldChange.content.length
		    || 1 !== newChange.content.length
		    || !Dom.isTextNode(oldChange.content[0])
		    || !Dom.isTextNode(newChange.content[0])
		    || opts.maxCombineChars <= Dom.nodeLength(oldChange.content[0])
		    || oldStep[0] + Dom.nodeLength(oldChange.content[0]) !== newStep[0]
		    || !pathEquals(oldPath.slice(0, oldPath.length - 1),
		                   newPath.slice(0, newPath.length - 1))) {
			return null;
		}
		var combinedNode = Dom.clone(oldChange.content[0]);
		combinedNode.insertData(Dom.nodeLength(combinedNode), newChange.content[0].data);
		var insertChange = makeInsertChange(oldPath, [combinedNode]);
		var oldRange = oldChangeSet.selection.oldRange;
		var newRange = newChangeSet.selection.newRange;
		var rangeUpdateChange = makeRangeUpdateChange(oldRange, newRange);
		return makeChangeSet(oldChangeSet.meta, [insertChange], rangeUpdateChange);
	}

	/**
	 * Generates changeSets from the records in the current frame in the
	 * given context, empties the frame's records, and adds the
	 * changeSets to the history.
	 *
	 * The current frame should have the partitionRecords option set to
	 * true and must be a top-level frame (not a nested frame).
	 *
	 * If the current history index is not at the end of the current
	 * history, for example due to an undo, all changes after the
	 * current index will be dropped.
	 *
	 * @param context {Undo}
	 * @return {void}
	 */
	function advanceHistory(context) {
		assertFalse(!!context.stack.length);
		var history = context.history;
		var historyIndex = context.historyIndex;
		var frame = context.frame;
		takeRecords(context, frame);
		var newChangeSets = partitionedChangeSetsFromFrame(context, frame);
		if (!newChangeSets.length) {
			return;
		}
		history.length = historyIndex;
		var lastChangeSet = Arrays.last(history);
		if (1 === newChangeSets.length && lastChangeSet && !context.interrupted) {
			var combinedChangeSet = combineChanges(lastChangeSet, newChangeSets[0], context.opts);
			if (combinedChangeSet) {
				history.pop();
				newChangeSets = [combinedChangeSet];
			}
		}
		context.interrupted = false;
		history = history.concat(newChangeSets);
		var maxHistory = context.opts.maxHistory;
		if (history.length > maxHistory) {
			history = history.slice(history.length - maxHistory, history.length);
		}
		frame.records = [];
		context.history = history;
		context.historyIndex = history.length;
	}

	/**
	 * Undoes the last changeSet in the history and decreases the
	 * history index.
	 *
	 * @param context {Undo}
	 * @param range {Range} will be set to the recorded range before the
	 *        changes in the changeSet occurred.
	 * @param ranges {Array.<Range>} will be preserved.
	 * @return {void}
	 * @memberOf undo
	 */
	function undo(context, range, ranges) {
		advanceHistory(context);
		var history = context.history;
		var historyIndex = context.historyIndex;
		if (!historyIndex) {
			return;
		}
		historyIndex -= 1;
		var changeSet = history[historyIndex];
		var undoChangeSet = inverseChangeSet(changeSet);
		captureOffTheRecord(context, {meta: {type: 'undo'}}, function () {
			applyChangeSet(context.elem, undoChangeSet, range, ranges);
		});
		context.historyIndex = historyIndex;
	}

	/**
	 * Redoes a previously undone changeSet in the history and
	 * increments the history index.
	 *
	 * @param context {Undo}
	 * @param range {Range} will be set to the recorded range after the
	 *        changes in the changeSet occurred.
	 * @param ranges {Array.<Range>} will be preserved.
	 * @return {void}
	 * @memberOf undo
	 */
	function redo(context, range, ranges) {
		advanceHistory(context);
		var history = context.history;
		var historyIndex = context.historyIndex;
		if (historyIndex === history.length) {
			return;
		}
		var changeSet = history[historyIndex];
		historyIndex += 1;
		captureOffTheRecord(context, {meta: {type: 'redo'}}, function () {
			applyChangeSet(context.elem, changeSet, range, ranges);
		});
		context.historyIndex = historyIndex;
	}

	return {
		Context: Context,
		enter: enter,
		close: close,
		leave: leave,
		capture: capture,
		pathFromBoundary: pathFromBoundary,
		changeSetFromFrame: changeSetFromFrame,
		inverseChangeSet: inverseChangeSet,
		applyChangeSet: applyChangeSet,
		advanceHistory: advanceHistory,
		makeInsertChange: makeInsertChange,
		undo: undo,
		redo: redo
	};
});

/**
 * editables.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace editables
 */
define('editables',[
	'dom',
	'maps',
	'undo',
	'content',
	'boundaries'
], function (
	Dom,
	Maps,
	Undo,
	Content,
	Boundaries
) {
	

	/**
	 * Returns an editable object for the given editable DOM element.
	 *
	 * @param  {Editor}  editor
	 * @param  {Element} elem
	 * @return {?Editable}
	 * @memberOf editables
	 */
	function fromElem(editor, elem) {
		return editor.editables[Dom.ensureExpandoId(elem)];
	}

	/**
	 * Returns an editable object for the given boundary.
	 *
	 * @param    {Editor}   editor
	 * @param    {Boundary} boundary
	 * @return   {?Editable}
	 * @memberOf editables
	 */
	function fromBoundary(editor, boundary) {
		var container = Boundaries.container(boundary);
		var elem = Dom.upWhile(container, function (node) {
			return !editor.editables[Dom.ensureExpandoId(node)];
		});
		return elem && fromElem(editor, elem);
	}

	/**
	 * Prepares the given element to be an editing host.
	 *
	 * @param    {!Element} elem
	 * @return   {Editable}
	 * @memberOf editables
	 */
	function Editable(elem) {
		if (!Dom.getStyle(elem, 'min-height')) {
			Dom.setStyle(elem, 'min-height', '1em');
		}
		Dom.setStyle(elem, 'cursor', 'text');
		Dom.addClass(elem, 'aloha-editable');
		var undoContext = Undo.Context(elem);
		var id = Dom.ensureExpandoId(elem);
		var editable = {
			id: id,
			elem: elem,
			undoContext: undoContext
		};
		return editable;
	}

	function dissocFromEditor(editor, editable) {
		delete editor.editables[editable.id];
	}

	function assocIntoEditor(editor, editable) {
		editor.editables[editable.id] = editable;
		editable.editor = editor;
	}

	function close(editable) {
		Undo.close(editable['undoContext']);
	}

	var DEFAULTS = {
		defaultBlock      : 'p',
		allowedStyles     : Content.allowedStyles(),
		allowedAttributes : Content.allowedAttributes(),
		disallowedNodes   : Content.disallowedNodes(),
		nodeTranslations  : Content.nodeTranslations()
	};

	/**
	 * Initializes an editable.
	 *
	 * @param  {function(AlohaEvent)} editor
	 * @param  {Element}              element
	 * @param  {Object}               options
	 * @return {Editable}
	 * @memberOf editables
	 */
	function create(editor, element, options) {
		var editable = Editable(element);
		editable.settings = Maps.merge({}, DEFAULTS, options);
		assocIntoEditor(editor, editable);
		Undo.enter(editable.undoContext, {
			meta             : {type: 'external'},
			partitionRecords : true
		});
		return editable;
	}

	/**
	 * Undos the scaffolding that was placed around the given element if the
	 * given element is an editable.
	 *
	 * @param    {!Editor}  editor
	 * @param    {!Element} element
	 * @return   {?Editable}
	 * @memberOf editables
	 */
	function destroy(editor, element)  {
		var editable = fromElem(editor, element);
		if (!editable) {
			return null;
		}
		close(editable);
		dissocFromEditor(editor, editable);
		if ('1em' === Dom.getStyle(element, 'min-height')) {
			Dom.setStyle(element, 'min-height', '');
		}
		Dom.setStyle(element, 'cursor', '');
		Dom.removeClass(element, 'aloha-editable');
		return editable;
	}

	/**
	 * Returns true if the given value is an editable.
	 *
	 * @param  {*} obj
	 * @return {boolean}
	 * @memberOf editables
	 */
	function is(obj) {
		return obj
		    && obj['elem']
		    && obj['elem'].hasOwnProperty
		    && obj['elem'].hasOwnProperty('!aloha-expando-node-id');
	}

	return {
		fromElem         : fromElem,
		fromBoundary     : fromBoundary,
		assocIntoEditor  : assocIntoEditor,
		dissocFromEditor : dissocFromEditor,
		is               : is,
		close            : close,
		create           : create,
		destroy          : destroy
	};
});

/**
 * selections.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @namespace selections
 */
define('selections',[
	'functions',
	'dom',
	'keys',
	'maps',
	'html',
	'mouse',
	'events',
	'arrays',
	'ranges',
	'carets',
	'browsers',
	'overrides',
	'animation',
	'boundaries',
	'traversing',
	'editables'
], function (
	Fn,
	Dom,
	Keys,
	Maps,
	Html,
	Mouse,
	Events,
	Arrays,
	Ranges,
	Carets,
	Browsers,
	Overrides,
	Animation,
	Boundaries,
	Traversing,
	Editables
) {
	

	/**
	 * Hides all visible caret elements and returns all those that were hidden
	 * in this operation.
	 *
	 * @param  {Document} doc
	 * @return {Array.<Element>}
	 */
	function hideCarets(doc) {
		var carets = doc.querySelectorAll('div.aloha-caret');
		var visible = [];
		[].forEach.call(carets, function (caret) {
			if ('block' === Dom.getStyle(caret, 'display')) {
				visible.push(caret);
				Dom.setStyle(caret, 'display', 'none');
			}
		});
		return visible;
	}

	/**
	 * Unhides the given list of caret elements.
	 *
	 * @param {Array.<Element>} carets
	 */
	function unhideCarets(carets) {
		carets.forEach(function (caret) {
			Dom.setStyle(caret, 'display', 'block');
		});
	}

	/**
	 * Renders the given element at the specified boundary to represent the
	 * caret position.
	 *
	 * @param {Element}  caret
	 * @param {Boundary} boundary
	 * @memberOf selections
	 */
	function show(caret, boundary) {
		var box = Carets.box(boundary);
		Maps.extend(caret.style, {
			'top'     : box.top + 'px',
			'left'    : box.left + 'px',
			'height'  : box.height + 'px',
			'width'   : '2px',
			'display' : 'block'
		});
	}

	/**
	 * Determines how to style a caret element based on the given overrides.
	 *
	 * @private
	 * @param  {Object} overrides
	 * @return {Object} A map of style properties and their values
	 */
	function stylesFromOverrides(overrides) {
		var style = {};
		style['padding'] = overrides['bold'] ? '1.5px' : '0px';
		style[Browsers.VENDOR_PREFIX + 'transform']
				= overrides['italic'] ? 'rotate(16deg)' : '';
		style['background'] = overrides['color'] || 'black';
		return style;
	}

	/**
	 * Given the boundaries checks whether the end boundary preceeds the start
	 * boundary in document order.
	 *
	 * @private
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @return {boolean}
	 */
	function isReversed(start, end) {
		var sc = Boundaries.container(start);
		var ec = Boundaries.container(end);
		var so = Boundaries.offset(start);
		var eo = Boundaries.offset(end);
		return (sc === ec && so > eo) || Dom.followedBy(ec, sc);
	}

	/**
	 * Creates a range that is `stride` pixels above the given offset bounds.
	 *
	 * @private
	 * @param  {Object.<string, number>} box
	 * @param  {number}                  stride
	 * @param  {Document}                doc
	 * @return {Range}
	 */
	function up(box, stride, doc) {
		var boundary = Boundaries.fromPosition(
			box.left,
			box.top - stride,
			doc
		);
		return boundary && Boundaries.range(boundary, boundary);
	}

	/**
	 * Creates a range that is `stride` pixels below the given offset bounds.
	 *
	 * @private
	 * @param  {Object.<string, number>} box
	 * @param  {number}                  stride
	 * @return {Range}
	 */
	function down(box, stride, doc) {
		var boundary = Boundaries.fromPosition(
			box.left,
			box.top + box.height + stride,
			doc
		);
		return boundary && Boundaries.range(boundary, boundary);
	}

	/**
	 * Given two ranges (represented in boundary tuples), creates a range that
	 * is between the two.
	 *
	 * @private
	 * @param  {Array.<Boundary>} a
	 * @param  {Array.<Boundary>} b
	 * @param  {string} focus Either "start" or "end"
	 * @return {Object}
	 */
	function mergeRanges(a, b, focus) {
		var start, end;
		if ('start' === focus) {
			start = a[0];
			end = b[1];
		} else {
			start = b[0];
			end = a[1];
		}
		if (isReversed(start, end)) {
			return {
				boundaries : [end, start],
				focus      : ('start' === focus) ? 'end' : 'start'
			};
		}
		return {
			boundaries : [start, end],
			focus      : focus
		};
	}

	/**
	 * Jumps the front or end position of the given editable.
	 *
	 * @private
	 * @param  {string}           direction "up" or "down"
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {string}           focus
	 * @return {Object}
	 */
	function jump(direction, event, boundaries, focus) {
		var boundary;
		if ('up' === direction) {
			boundary = Boundaries.create(event.editable.elem, 0);
			boundary = Html.expandForward(boundary);
		} else {
			boundary = Boundaries.fromEndOfNode(event.editable.elem);
			boundary = Html.expandBackward(boundary);
		}
		var next = [boundary, boundary];
		if (!Events.hasKeyModifier(event, 'shift')) {
			return {
				boundaries : next,
				focus      : focus
			};
		}
		return mergeRanges(next, boundaries, focus);
	}

	/**
	 * Finds the closest linebreaking element from the given node.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {?Element};
	 */
	function closestLine(node) {
		return Dom.upWhile(node, Fn.complement(Html.hasLinebreakingStyle));
	}

	/**
	 * Computes the visual boundary positoin above/below the given.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @param  {!function} step
	 * @return {?Boundary}
	 */
	function climbStep(boundary, step) {
		var range = Boundaries.range(boundary, boundary);
		var doc = Boundaries.document(boundary);
		var box = Carets.box(boundary);
		var half = box.height / 4;
		var stride = 0;
		var next;
		do {
			stride += half;
			next = step(box, stride, doc);
		} while (next && Ranges.equals(next, range));
		return next && Boundaries.fromRangeStart(next);
	}

	/**
	 * Computes a lists of box dimensions for the a given range boundaries.
	 *
	 * @private
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Object>}
	 */
	function selectionBoxes(start, end) {
		var doc = Boundaries.document(start);
		var endBox = Carets.box(end);
		var endTop = endBox.top;
		var endLeft = endBox.left;
		var box, top, left, right, width, line, atEnd;
		var leftBoundary, rightBoundary;
		var boxes = [];
		var boundary = start;
		while (boundary) {
			box = Carets.box(boundary);
			top = box.top;
			line = closestLine(Boundaries.container(boundary));
			if (!line) {
				break;
			}
			atEnd = endTop < top + box.height;
			if (atEnd) {
				if (0 === boxes.length) {
					left = box.left;
				} else {
					left = Dom.offset(line).left;
				}
				width = endLeft - left;
			} else {
				if (0 === boxes.length) {
					left = box.left;
					width = elementWidth(line) - (left - Dom.offset(line).left);
				} else {
					left = Dom.offset(line).left;
					width = elementWidth(line);
				}
			}
			leftBoundary = Boundaries.fromPosition(left, top, doc);
			rightBoundary = Boundaries.fromPosition(left + width, top, doc);
			if (!leftBoundary || !rightBoundary) {
				break;
			}
			left = Carets.box(leftBoundary).left;
			right = Carets.box(rightBoundary).left;
			boxes.push({
				top    : top,
				left   : left,
				width  : right - left,
				height : box.height
			});
			if (atEnd) {
				break;
			}
			boundary = moveDown(boundary);
		}
		return boxes;
	}

	/**
	 * Renders divs to represent the given range
	 *
	 * @private
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @return {Array.<Element>}
	 */
	function highlight(start, end) {
		var doc = Boundaries.document(start);
		Dom.query('.aloha-selection-box', doc).forEach(Dom.remove);
		return selectionBoxes(start, end).map(function (box) {
			return drawBox(box, doc);
		});
	}

	/**
	 * Calculates the width of the given element as best as possible.
	 *
	 * We need to do this because clientWidth/clientHeight sometimes return 0
	 * erroneously.
	 *
	 * The difference between clientWidth and offsetWidth is that offsetWidth
	 * includes scrollbar size, but since we will almost certainly not have
	 * scrollbar within editing elements, this should not be a problem:
	 * http://stackoverflow.com/questions/4106538/difference-between-offsetheight-and-clientheight
	 *
	 * @private
	 * @param  {!Element} elem
	 * @return {number}
	 */
	function elementWidth(elem) {
		return elem.clientWidth || elem.offsetWidth;
	}

	/**
	 * Calculates the height of the given element as best as possible.
	 *
	 * We need to do this because clientWidth/clientHeight sometimes return 0
	 * erroneously.
	 *
	 * @private
	 * @see elementWidth
	 * @param  {!Element} elem
	 * @return {number}
	 */
	function elementHeight(elem) {
		return elem.clientHeight || elem.offsetHeight;
	}

	/**
	 * Checks whether the given node is a visible linebreaking non-void element.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function isVisibleBreakingContainer(node) {
		return !Html.isVoidType(node)
		    && Html.isRendered(node)
		    && Html.hasLinebreakingStyle(node);
	}

	/**
	 * Checks whether the given node is a visible non-void element.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function isVisibleContainer(node) {
		return !Html.isVoidType(node) && Html.isRendered(node);
	}

	/**
	 * Finds the breaking element above/below the given boundary.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @param  {!function} next
	 * @param  {!function} forwards
	 * @return {?Object}
	 */
	function findBreakpoint(boundary, next, forwards) {
		var node = next(boundary);
		var breaker = isVisibleBreakingContainer(node)
		            ? node
		            : forwards(node, isVisibleBreakingContainer);
		if (!breaker) {
			return null;
		}
		var isInsideBreaker = !!Dom.upWhile(node, function (node) {
			return node !== breaker;
		});
		return {
			breaker: breaker,

			// Because if the breaking node is an ancestor of the boundary
			// container, then the breakpoint ought to be calculated from the
			// top of the breaker, otherwise we ought to calculate it from the
			// bottom
			isInsideBreaker: isInsideBreaker
		};
	}

	/**
	 * Finds the visual boundary position above the given.
	 *
	 * Cases:
	 * foo<br>
	 * ba░r
	 *
	 * <p>foo</p>ba░r
	 *
	 * <p>foo</p><p>ba░r</p>
	 *
	 * foo<ul><li>ba░r</li></ul>
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {Boundary}
	 */
	function moveUp(boundary) {
		var next;
		var box = Carets.box(boundary);
		var above = paragraphAbove(boundary);
		if (above) {
			var aboveBoundary = Html.expandBackward(
				Boundaries.fromEndOfNode(above)
			);
			above = Boundaries.prevNode(aboveBoundary);
			var aboveBox = Carets.box(aboveBoundary);
			var top;
			if (Dom.isTextNode(above)) {
				top = aboveBox.top + (aboveBox.height / 2);
			} else {
				top = Dom.absoluteTop(above)
				    + elementHeight(above)
				    - (aboveBox.height / 2);
			}
			next = Boundaries.fromPosition(
				box.left,
				top,
				above.ownerDocument
			);
		}
		next = next || climbStep(boundary, up);
		return (!next || box.top === Carets.box(next).top) ? boundary : next;
	}

	/**
	 * Finds the visual boundary position below the given.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {Boundary}
	 */
	function moveDown(boundary) {
		var next;
		var box = Carets.box(boundary);
		var below = paragraphBelow(boundary);
		if (below) {
			var belowBoundary = Html.expandForward(
				Boundaries.fromStartOfNode(below)
			);
			below = Boundaries.nodeAfter(belowBoundary);
			var belowBox = Carets.box(belowBoundary);
			var top = Dom.isTextNode(below)
					? belowBox.top
					: Dom.absoluteTop(below);
			top += belowBox.height / 2;
			next = Boundaries.fromPosition(
				box.left,
				top,
				below.ownerDocument
			);
		}
		next = next || climbStep(boundary, down);
		return (!next || box.top === Carets.box(next).top) ? boundary : next;
	}

	/**
	 * Locates the next paragraphing element that is visually above the given
	 * aboundary.
	 *
	 * If a soft break is causing there to be a line of text inside before a
	 * visual paragraph-dictated linebreak, then `null` is returned.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {?Boundary}
	 */
	function paragraphAbove(boundary) {
		var breakpoint = findBreakpoint(
			boundary,
			Boundaries.prevNode,
			Dom.backwardPreorderBacktraceUntil
		);
		if (!breakpoint) {
			return null;
		}
		var box = Carets.box(boundary);
		var breaker = breakpoint.breaker;
		var offset = box.top - box.height;
		var breakOffset = Dom.absoluteTop(breaker);
		if (!breakpoint.isInsideBreaker) {
			breakOffset += elementHeight(breaker);
		}
		if (offset >= breakOffset) {
			return null;
		}
		var above;
		if (breakpoint.isInsideBreaker) {
			above = Dom.nextNonAncestor(
				breaker,
				true,
				isVisibleContainer,
				Dom.isEditingHost
			);
		} else {
			above = breaker;
		}
		if (!above) {
			return null;
		}
		if (Html.isGroupContainer(above)) {
			return Dom.backwardPreorderBacktraceUntil(
				above.nextSibling,
				Html.isGroupedElement
			);
		}
		return above;
	}

	/**
	 * Locates the next paragraphing element that is visually below the given
	 * aboundary.
	 *
	 * If a soft break is causing there to be a line of text inside before a
	 * visual paragraph-dictated linebreak, then `null` is returned.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {?Boundary}
	 */
	function paragraphBelow(boundary) {
		var breakpoint = findBreakpoint(
			boundary,
			Boundaries.nextNode,
			Dom.forwardPreorderBacktraceUntil
		);
		if (!breakpoint) {
			return null;
		}
		var breaker = breakpoint.breaker;
		var box = Carets.box(boundary);
		var offset = box.top + box.height + box.height;
		var breakOffset = Dom.absoluteTop(breaker);
		if (breakpoint.isInsideBreaker) {
			breakOffset += elementHeight(breaker);
		}
		if (offset <= breakOffset) {
			return null;
		}
		var below;
		if (breakpoint.isInsideBreaker) {
			below = Dom.nextNonAncestor(
				breaker,
				false,
				isVisibleContainer,
				Dom.isEditingHost
			);
		} else {
			below = breaker;
		}
		if (!below) {
			return null;
		}
		if (Html.isGroupContainer(below)) {
			return Dom.forwardPreorderBacktraceUntil(
				below.previousSibling,
				Html.isGroupedElement
			);
		}
		return below;
	}

	/**
	 * Ensures that there is enough space above/below the given boundary from
	 * which to calculate the next boundary position for moveUp/moveDown.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @param  {string}    direction
	 */
	function ensureVerticalMovingRoom(boundary, direction) {
		var doc = Boundaries.document(boundary);
		var win = Dom.documentWindow(doc);
		var top = Dom.scrollTop(doc);
		var height = win.innerHeight;
		var box = Carets.box(boundary);

		var above = paragraphAbove(boundary);
		var below = paragraphBelow(boundary);

		var belowCaret = (below && !Dom.isTextNode(below))
		               ? Dom.absoluteTop(below) + box.height
		               : box.top + box.height + box.height;

		var aboveCaret = (above && !Dom.isTextNode(above))
		               ? Dom.absoluteTop(above) + elementHeight(above) - box.height
		               : box.top - box.height;

		var buffer = box.height;
		var correctTop = 0;
		if (aboveCaret <= top) {
			if ('up' === direction) {
				correctTop = aboveCaret - buffer;
			}
		} else if (belowCaret >= top + height) {
			if ('down' === direction) {
				correctTop = belowCaret - height + buffer + buffer;
			}
		}
		if (correctTop) {
			win.scrollTo(Dom.scrollLeft(doc), correctTop);
		}
	}

	/**
	 * Determines the closest visual caret position above or below the given
	 * range.
	 *
	 * @private
	 * @param  {string}           direction "up" or "down"
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundary
	 * @param  {string}           focus
	 * @return {Object}
	 */
	function climb(direction, event, boundaries, focus) {
		var boundary = boundaries['start' === focus ? 0 : 1];
		ensureVerticalMovingRoom(boundary, direction);
		var next = 'up' === direction ? moveUp(boundary) : moveDown(boundary);
		if (!next) {
			return {
				boundaries : boundaries,
				focus      : focus
			};
		}
		if (Events.hasKeyModifier(event, 'shift')) {
			return mergeRanges([next, next], boundaries, focus);
		}
		return {
			boundaries : [next, next],
			focus      : focus
		};
	}

	/**
	 * Determines the next visual caret position before or after the given
	 * boundaries.
	 *
	 * @private
	 * @param  {string}           direction "left" or "right"
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {string}           focus
	 * @return {Object}
	 */
	function step(direction, event, boundaries, focus) {
		var shift = Events.hasKeyModifier(event, 'shift');
		var start = boundaries[0];
		var end = boundaries[1];
		var collapsed = Boundaries.equals(start, end);
		if (collapsed || !shift) {
			focus = ('left' === direction) ? 'start' : 'end';
		}
		var boundary = ('start' === focus)
		             ? start
		             : Traversing.envelopeInvisibleCharacters(end);
		if (collapsed || shift) {
			var stride = (Events.hasKeyModifier(event, 'ctrl')
			          || Events.hasKeyModifier(event, 'alt'))
			           ? 'word'
			           : 'visual';
			var next = ('left' === direction)
			         ? Traversing.prev(boundary, stride)
			         : Traversing.next(boundary, stride);
			if (Dom.isEditingHost(Boundaries.container(next))) {
				if (Boundaries.isAtStart(boundary)) {
					next = Html.expandForward(boundary);
				} else if (Boundaries.isAtEnd(boundary)) {
					next = Html.expandBackward(boundary);
				}
			}
			if (next) {
				boundary = next;
			}
		}
		if (shift) {
			return {
				boundaries : ('start' === focus) ? [boundary, end] : [start, boundary],
				focus      : focus
			};
		}
		return {
			boundaries : [boundary, boundary],
			focus      : focus
		};
	}

	/**
	 * Determines the dimensions of the vertical line in the editable at the
	 * given boundary position.
	 *
	 * @private
	 * @param  {Boundary} boundary
	 * @param  {Element}  editable
	 * @return {Object.<string, number>}
	 */
	function lineBox(boundary, editable) {
		var box = Carets.box(boundary);
		var node = Boundaries.container(boundary);
		if (Dom.isTextNode(node)) {
			node = node.parentNode;
		}
		var fontSize = parseInt(Dom.getComputedStyle(node, 'font-size'));
		var top = box ? box.top : Dom.absoluteTop(node);
		top += (fontSize ? fontSize / 2 : 0);
		var left = Dom.offset(editable).left;
		return {
			top   : top,
			left  : left,
			right : left + elementWidth(editable)
		};
	}

	function end(event, boundaries, focus) {
		var box = lineBox(boundaries[1], event.editable.elem);
		var boundary = Boundaries.fromPosition(
			// Because -1 ensures that the position is within the viewport
			box.right - 1,
			box.top,
			Boundaries.document(boundaries[0])
		);
		if (boundary) {
			var start = boundaries['start' === focus ? 1 : 0];
			boundaries = Events.hasKeyModifier(event, 'shift')
			           ? [start, boundary]
			           : [boundary, boundary];
			focus = 'end';
		}
		return {
			boundaries : boundaries,
			focus      : focus
		};
	}

	function home(event, boundaries, focus) {
		var box = lineBox(boundaries[0], event.editable.elem);
		var boundary = Boundaries.fromPosition(
			box.left,
			box.top,
			Boundaries.document(boundaries[0])
		);
		if (boundary) {
			var end = boundaries['end' === focus ? 0 : 1];
			boundaries = Events.hasKeyModifier(event, 'shift')
			           ? [boundary, end]
			           : [boundary, boundary];
			focus = 'start';
		}
		return {
			boundaries : boundaries,
			focus      : focus
		};
	}

	/**
	 * Caret movement operations mapped against cursor key keycodes.
	 *
	 * @private
	 * @type {Object.<string, function(Event, Array.<Boundary>, string):Object>}
	 */
	var movements = {};

	movements['left'] =
	movements['*+left'] = Fn.partial(step, 'left');

	movements['right'] =
	movements['*+right'] = Fn.partial(step, 'right');

	movements['up'] =
	movements['*+up'] = Fn.partial(climb, 'up');

	movements['down'] =
	movements['*+down'] = Fn.partial(climb, 'down');

	movements['pageUp'] =
	movements['meta+up'] = Fn.partial(jump, 'up');

	movements['pageDown'] =
	movements['meta+down'] = Fn.partial(jump, 'down');

	movements['home'] =
	movements['meta+left'] =
	movements['meta+shift+left'] = home;

	movements['end'] =
	movements['meta+right'] =
	movements['meta+shift+right'] = end;

	/**
	 * Processes a keydown event.
	 *
	 * @private
	 * @param  {AlohaEvent}       event
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {string}           focus
	 * @return {Object}
	 */
	function keydown(event, boundaries, focus) {
		var handler = Keys.shortcutHandler(event.meta, event.keycode, movements);
		if (handler) {
			Events.preventDefault(event.nativeEvent);
			return handler(event, boundaries, focus);
		}
		return {
			boundaries : boundaries,
			focus      : focus
		};
	}

	/**
	 * Processes a double-click event.
	 *
	 * @private
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundaries
	 * @return {Object}
	 */
	function dblclick(event, boundaries) {
		return {
			boundaries : Traversing.expand(boundaries[0], boundaries[1], 'word'),
			focus      : 'end'
		};
	}

	/**
	 * Processes a triple-click event.
	 *
	 * @private
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundaries
	 * @return {Object}
	 */
	function tplclick(event, boundaries) {
		return {
			boundaries : Traversing.expand(boundaries[0], boundaries[1], 'block'),
			focus      : 'end'
		};
	}

	/**
	 * Processes a mouseup event.
	 *
	 * @private
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {string}           focus
	 * @return {Object}
	 */
	function mouseup(event, boundaries, focus, previous, expanding) {
		return mergeRanges(boundaries, previous, focus);
	}

	/**
	 * Processes a mousedown event.
	 *
	 * @private
	 * @param  {Event}            event
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {string}           focus
	 * @param  {Array.<Boundary>} previous
	 * @param  {boolean}          expanding
	 * @return {Object}
	 */
	function mousedown(event, boundaries, focus, previous, expanding) {
		if (!expanding) {
			return {
				boundaries : boundaries,
				focus      : focus
			};
		}
		var start = boundaries[0];
		var end = previous['start' === focus ? 1 : 0];
		if (isReversed(start, end)) {
			return {
				boundaries : [end, start],
				focus      : 'end'
			};
		}
		return {
			boundaries : [start, end],
			focus      : 'start'
		};
	}

	function dragndrop(event, boundaries) {
		return {
			boundaries : boundaries,
			focus      : 'end'
		};
	}

	function resize(event, boundaries, focus) {
		return {
			boundaries : boundaries,
			focus      : focus
		};
	}

	function paste(event, boundaries) {
		return {
			boundaries : boundaries,
			focus      : 'end'
		};
	}

	/**
	 * Event handlers.
	 *
	 * @private
	 * @type {Object.<string, function>}
	 */
	var handlers = {
		'keydown'        : keydown,
		'aloha.dblclick' : dblclick,
		'aloha.tplclick' : tplclick,
		'aloha.mouseup'  : mouseup,
		'mouseup'        : mouseup,
		'mousedown'      : mousedown,
		'dragover'       : dragndrop,
		'drop'           : dragndrop,
		'resize'         : resize,
		'paste'          : paste
	};

	/**
	 * Initialize blinking using the given element.
	 *
	 * @private
	 * @param  {Element} caret
	 * @return {Object}
	 */
	function blinking(caret) {
		var timers = [];
		var isBlinking = true;
		function fade(start, end, duration) {
			Animation.animate(
				start,
				end,
				Animation.easeLinear,
				duration,
				function (value, percent, state) {
					if (!isBlinking) {
						return true;
					}
					Dom.setStyle(caret, 'opacity', value);
					if (percent < 1) {
						return;
					}
					if (0 === value) {
						timers.push(setTimeout(function () {
							fade(0, 1, 100);
						}, 300));
					} else if (1 === value){
						timers.push(setTimeout(function () {
							fade(1, 0, 100);
						}, 500));
					}
				}
			);
		}
		function stop() {
			isBlinking = false;
			Dom.setStyle(caret, 'opacity', 1);
			timers.forEach(clearTimeout);
			timers = [];
		}
		function blink() {
			stop();
			isBlinking = true;
			timers.push(setTimeout(function () {
				fade(1, 0, 100);
			}, 500));
		}
		function start() {
			stop();
			timers.push(setTimeout(blink, 50));
		}
		return {
			start : start,
			stop  : stop
		};
	}

	/**
	 * Creates a new selection context.
	 *
	 * Will create a DOM element at the end of the document body to be used to
	 * represent the caret position.
	 *
	 * @param  {Document} doc
	 * @return {Object}
	 * @memberOf selections
	 */
	function Context(doc) {
		var hidden = doc.createElement('textarea');
		Maps.extend(hidden.style, {
			'overflow'  : 'hidden',
			'width'     : '1px',
			'height'    : '1px',
			'outline'   : '0',
			'opacity'   : '0.01'
		});
		var caret = doc.createElement('div');
		Maps.extend(caret.style, {
			'overflow' : 'hidden',
			'cursor'   : 'text',
			'color'    : '#000',
			'zIndex'   : '9999',
			'display'  : 'none',
			'position' : 'absolute'
		});
		Dom.addClass(caret, 'aloha-caret', 'aloha-ephemera');
		Dom.append(hidden, caret);
		Dom.append(caret, doc.body);
		return {
			blinking       : blinking(caret),
			focus          : 'end',
			boundaries     : null,
			event          : null,
			dragging       : null,
			multiclick     : null,
			clickTimer     : 0,
			lastMouseEvent : '',
			caret          : caret,
			formatting     : [],
			overrides      : []
		};
	}

	/**
	 * Ensures that the given boundary is visible inside of the viewport by
	 * scolling the view port if necessary.
	 *
	 * @param {!Boundary} boundary
	 * @memberOf selections
	 */
	function focus(boundary) {
		var box = Carets.box(boundary);
		var doc = Boundaries.document(boundary);
		var win = Dom.documentWindow(doc);
		var top = Dom.scrollTop(doc);
		var left = Dom.scrollLeft(doc);
		var height = win.innerHeight;
		var width = win.innerWidth;
		var buffer = box.height;
		var caretTop = box.top;
		var caretLeft = box.left;
		var correctTop = 0;
		var correctLeft = 0;
		if (caretTop < top) {
			// Because we want to caret to be near the top
			correctTop = caretTop - buffer;
		} else if (caretTop > top + height) {
			// Because we want to caret to be near the bottom
			correctTop = caretTop - height + buffer + buffer;
		}
		if (caretLeft < left) {
			// Because we want to caret to be near the left
			correctLeft = caretLeft - buffer;
		} else if (caretLeft > left + width) {
			// Because we want to caret to be near the right
			correctLeft = caretLeft - width + buffer + buffer;
		}
		if (correctTop || correctLeft) {
			win.scrollTo(correctLeft || left, correctTop || top);
		}
	}

	/**
	 * Computes a table of the given override and those collected at the given
	 * node.
	 *
	 * An object with overrides mapped against their names.
	 *
	 * @private
	 * @param  {!Node}      node
	 * @param  {!Selection} selection
	 * @return {Object}
	 */
	function mapOverrides(node, selection) {
		var overrides = Overrides.joinToSet(
			selection.formatting,
			Overrides.harvest(node),
			selection.overrides
		);
		var map = Maps.merge(Maps.mapTuples(overrides));
		if (!map['color']) {
			map['color'] = Dom.getComputedStyle(
				Dom.isTextNode(node) ? node.parentNode : node,
				'color'
			);
		}
		return map;
	}

	function drawBox(box, doc) {
		var elem = doc.createElement('div');
		Maps.extend(elem.style, {
			'top'        : box.top + 'px',
			'left'       : box.left + 'px',
			'height'     : box.height + 'px',
			'width'      : box.width + 'px',
			'position'   : 'absolute',
			'background' : 'red',
			'opacity'    : 0.4
		});
		Dom.addClass(elem, 'aloha-selection-box', 'aloha-ephemera');
		Dom.append(elem, doc.body);
		return elem;
	}

	/**
	 * Updates selection
	 *
	 * @param  {AlohaEvent} event
	 * @return {AlohaEvent}
	 * @memberOf selections
	 */
	function middleware(event) {
		if (!handlers[event.type]) {
			return event;
		}
		var selection = event.selection;
		var change = handlers[event.type](
			event,
			selection.boundaries,
			selection.focus,
			selection.previousBoundaries,
			Events.hasKeyModifier(event, 'shift')
		);
		selection.focus = change.focus;
		selection.boundaries = change.boundaries;
		/*
		// This check should not be necessary
		if (selection.boundaries[0]) {
			highlight(selection.boundaries[0], selection.boundaries[1]).forEach(function (box) {
				Dom.setStyle(box, 'background', '#fce05e'); // or blue #a6c7f7
			});
		}
		*/
		return event;
	}

	/**
	 * Whether the given event will cause the position of the selection to move.
	 *
	 * @private
	 * @param  {Event} event
	 * @return {boolean}
	 */
	function isCaretMovingEvent(event) {
		if ('keypress' === event.type) {
			return true;
		}
		if ('paste' === event.type) {
			return true;
		}
		if (Keys.ARROWS[event.keycode]) {
			return true;
		}
		if (Keys.CODES['pageDown'] === event.keycode || Keys.CODES['pageUp'] === event.keycode) {
			return true;
		}
		if (Keys.CODES['undo'] === event.keycode) {
			if ('meta' === event.meta || 'ctrl' === event.meta || 'shift' === event.meta) {
				return true;
			}
		}
		if (Keys.CODES['enter'] === event.keycode) {
			return true;
		}
		return false;
	}

	var MOBILE_EVENT_TYPE = /^mobile\./;

	/**
	 * Causes the selection for the given event to be set to the browser and the
	 * caret position to be visualized.
	 *
	 * @param  {!Event} event
	 * @return {?Selection}
	 */
	function update(event) {
		var selection = event.selection;
		if (MOBILE_EVENT_TYPE.test(event.type)) {
			return;
		}
		if (event.preventSelection || (selection.dragging && 'dragover' !== event.type)) {
			return;
		}
		if ('leave' === event.type) {
			Dom.setStyle(selection.caret, 'display', 'none');
			return;
		}
		var type = event.type;
		if ('mouseup' === type || 'click' === type || 'dblclick' === type) {
			Dom.setStyle(selection.caret, 'display', 'block');
			return;
		}
		selection = select(
			selection,
			selection.boundaries[0],
			selection.boundaries[1],
			selection.focus
		);
		var boundary = selection.focus === 'start' ? selection.boundaries[0] : selection.boundaries[1];
		// Because we don't want the screen to jump when the editor hits "shift"
		if (isCaretMovingEvent(event)) {
			focus(boundary);
		}
		return selection;
	}

	/**
	 * Selects the given boundaries and visualizes the caret position.
	 *
	 * Returns the updated Selection object, that can be reassigned to
	 * aloha.editor.selection
	 *
	 * @param  {Selection} selection
	 * @param  {Boundary}  start
	 * @param  {Boundary}  end
	 * @param  {string=}   focus optional. "start" or "end". Defaults to "end"
	 * @return {Selection}
	 * @memberOf selections
	 */
	function select(selection, start, end, focus) {
		var boundary = 'start' === focus ? start : end;
		var node = Boundaries.container(boundary);
		if (!Dom.isEditableNode(node)) {
			Dom.setStyle(selection.caret, 'display', 'none');
			return selection;
		}
		show(selection.caret, boundary);
		Maps.extend(
			selection.caret.style,
			stylesFromOverrides(mapOverrides(node, selection))
		);
		Boundaries.select(start, end);
		selection.blinking.start();
		return Maps.merge(selection, { 
			boundaries : [start, end],
			focus      : focus
		});
	}

	/**
	 * Returns true if obj is a selection as returned by a selection context
	 * object.
	 *
	 * @param  {*} obj
	 * @return {boolean}
	 * @memberOf selections
	 */
	function is(obj) {
		if (obj &&
			obj.hasOwnPropery &&
			obj.hasOwnProperty('focus') &&
			obj.hasOwnProperty('caret') &&
			obj.hasOwnProperty('boundaries')) {
			return true;
		}
		return false;
	}

	var MOUSE_EVENT = {
		'mousemove'      : true,
		'mousedown'      : true,
		'mouseup'        : true,
		'click'          : true,
		'dblclick'       : true,
		'aloha.dblclick' : true,
		'aloha.tplclick' : true
	};

	var CLICKING_EVENT = {
		'mousedown'      : true,
		'mouseup'        : true,
		'click'          : true,
		'dblclick'       : true,
		'aloha.dblclick' : true,
		'aloha.tplclick' : true
	};

	var MUTLICLICK_EVENT = {
		'dblclick'       : true,
		'aloha.dblclick' : true,
		'aloha.tplclick' : true
	};

	/**
	 * Returns the appropriate event type in the click cycle.
	 *
	 * <pre>
	 * Event cycle:
	 *     mousedown
	 *     mouseup
	 *     click
	 *     mousedown -> aloha.dblclick
	 *     mouseup
	 *     click
	 *     dblclick
	 *     mousedown -> aloha.tplclick
	 *     mouseup
	 *     click
	 * </pre>
	 *
	 * @private
	 * @param  {!Event}     event
	 * @param  {!Selection} selection
	 * @return {?string}
	 */
	function processClicking(event, selection) {
		if ('mousedown'      !== event.type &&
		    'dbclick'        !== event.type &&
		    'aloha.dblclick' !== event.type) {
			return null;
		}
		var time = new Date();
		var elapsed = time - selection.clickTimer;
		var multiclick = selection.multiclick;
		selection.multiclick = null;
		selection.clickTimer = time;
		if (elapsed > 500) {
			return null;
		}
		if (!selection.event) {
			return null;
		}
		if (selection.event.clientX !== event.clientX) {
			return null;
		}
		if (selection.event.clientY !== event.clientY) {
			return null;
		}
		return MUTLICLICK_EVENT[multiclick] ? 'aloha.tplclick' : 'aloha.dblclick';
	}

	/**
	 * Gets the dragging state.
	 *
	 * @private
	 * @param  {!Event}     event
	 * @param  {!Selection} selection
	 * @return {?string}
	 */
	function getDragging(event, selection) {
		if (selection.dragging) {
			return selection.dragging;
		}
		if ('mousemove' !== event.type) {
			return null;
		}
		var last = selection.lastMouseEvent;
		if ('mousedown'       === last ||
		    'aloha.dblclick'  === last ||
		    'aloha.tplclick'  === last) {
			return last;
		}
		return null;
	}

	function isMobileField(node, selection) {
		if (node === selection.caret) {
			return true;
		}
		var parent = node.parentNode;
		return (parent.parentNode === selection.caret)
		    && ('true' === Dom.getAttr(parent, 'contentEditable'));
	}

	/**
	 * Creates an event object that will contain the following properties:
	 *
	 * <pre>
	 *     type
	 *     nativeEvent
	 *     editable
	 *     selection
	 *     dnd
	 *     preventSelection
	 * </pre>
	 *
	 * @param  {!Editor} editor
	 * @param  {!Event}  event
	 * @return {?AlohaEvent}
	 * @memberOf selections
	 */
	function selectionEvent(editor, event) {
		var type = event.type;
		var doc = event.target.document || event.target.ownerDocument;
		var selection = editor.selection || Context(doc);
		var isClicking = CLICKING_EVENT[type] || false;
		var dragging = getDragging(event, selection);
		var isDragStart = dragging && dragging !== selection.dragging;
		var caretDisplay = Dom.getStyle(selection.caret, 'display');
		if (isClicking || isDragStart) {
			// Because otherwise if the mouse position is over the caret element
			// Boundaries.fromPosition() will compute the boundaries to be
			// inside the absolutely positioned caret element, which is not what
			// we want
			Dom.setStyle(selection.caret, 'display', 'none');
		}
		if (isDragStart) {
			selection.dragging = dragging;
		}
		if ('mousemove' === type) {
			return null;
		}
		if ('mouseup' === type && selection.dragging) {
			type = 'aloha.mouseup';
			selection.dragging = null;
		}
		if (isClicking) {
			type = processClicking(event, selection) || type;
		}
		if (MUTLICLICK_EVENT[type]) {
			selection.multiclick = type;
			Events.preventDefault(event);
		}
		if (MOUSE_EVENT[type]) {
			selection.lastMouseEvent = type;
		}
		var boundaries;
		if (isClicking) {
			var boundary = Boundaries.fromPosition(
				event.clientX + Dom.scrollLeft(doc),
				event.clientY + Dom.scrollTop(doc),
				doc
			);
			boundaries = boundary && [boundary, boundary];
		} else {
			boundaries = Boundaries.get(doc);
		}
		Dom.setStyle(selection.caret, 'display', caretDisplay);
		var editable;
		if (!boundaries) {
			if ('click' !== type && selection.boundaries) {
				editable = Editables.fromBoundary(editor, selection.boundaries[0]);
				return {
					preventSelection : false,
					type             : 'leave',
					nativeEvent      : event,
					editable         : editable,
					selection        : selection,
					dnd              : editor.dnd
				};
			}
			return null;
		}
		var cac = Boundaries.commonContainer(boundaries[0], boundaries[1]);
		var start = Boundaries.container(boundaries[0]);
		var end = Boundaries.container(boundaries[1]);
		var isPartial = !Dom.isEditableNode(cac)
		             && (Dom.isEditableNode(start) || Dom.isEditableNode(end));
		if ('keydown' === type && isPartial) {
			Events.preventDefault(event);
			return null;
		}
		editable = Editables.fromBoundary(editor, boundaries[0]);
		if (!editable) {
			if (isMobileField(Boundaries.container(boundaries[0]), selection)) {
				type = 'mobile.' + type;
			} else {
				return null;
			}
		}
		selection.overrides = editor.selection ? editor.selection.overrides : [];
		selection.previousBoundaries = selection.boundaries || boundaries;
		selection.boundaries = boundaries;
		selection.event = event;
		return {
			// TODO: Reconsider this
			// Because sometimes an interaction going through the editor pipe
			// should not result in an updated selection. eg: When inserting a
			// link you want to focus on an input field in the ui.
			preventSelection : false,
			type             : type,
			nativeEvent      : event,
			editable         : editable,
			selection        : selection,
			dnd              : editor.dnd
		};
	}

	/**
	 * Returns true if the given value is a Selection Event object as created by
	 * aloha.selections.selectionEvent.
	 *
	 * @param  {*} obj
	 * @return {boolean}
	 * @memberOf events
	 */
	function isSelectionEvent(obj) {
		return obj
		    && obj.hasOwnProperty
		    && obj.hasOwnProperty('dnd')
		    && obj.hasOwnProperty('editable')
		    && obj.hasOwnProperty('selection')
		    && obj.hasOwnProperty('nativeEvent');
	}

	return {
		is               : is,
		isSelectionEvent : isSelectionEvent,
		show             : show,
		select           : select,
		focus            : focus,
		update           : update,
		middleware       : middleware,
		Context          : Context,
		hideCarets       : hideCarets,
		unhideCarets     : unhideCarets,
		highlight        : highlight,
		selectionBoxes   : selectionBoxes,
		selectionEvent   : selectionEvent
	};
});

/**
 * dragdrop.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @see
 * http://www.whatwg.org/specs/web-apps/current-work/#dnd
 * http://www.html5rocks.com/en/tutorials/dnd/basics/
 * https://developer.mozilla.org/en-US/docs/Drag_and_drop_events
 * https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer
 * @namespace dragdrop
 */
define('dragdrop',[
	'dom',
	'maps',
	'events',
	'editing',
	'boundaries',
	'selections'
], function (
	Dom,
	Maps,
	Events,
	Editing,
	Boundaries,
	Selections
) {
	

	/**
	 * The pixel distance between the mouse pointer and where the caret should
	 * be rendered when dragging.
	 *
	 * @const
	 * @type {number}
	 */
	var DRAGGING_CARET_OFFSET = -10;

	/**
	 * Default drag and drop context properites.
	 *
	 * These are the default attributes from which drag and drop contexts will
	 * be created.
	 *
	 * @const
	 * @type {Object.<string, *>}
	 */
	var DEFAULTS = {
		'dropEffect' : 'none',
		'element'    : null,
		'target'     : null,
		'data'       : ['text/plain', '']
	};

	/**
	 * Creates a new drag and drop context.
	 *
	 * The following attributes are supported in the options object that is
	 * passed to this function:
	 *
	 *	`dropEffect`
	 *		The dropEffect attribute controls the drag-and-drop feedback that
	 *		the user is given during a drag-and-drop operation. If the
	 *		`dropEffect` value is set to "copy", for example, the user agent may
	 *		rendered the drag icon with a "+" (plus) sign. The supported values
	 *		are "none", "copy", "link", or "move". All other values are ignored.
	 *
	 *	`element`
	 *		The element on which dragging was initiated on. If the drag and drop
	 *		operation is a moving operation, this element will be relocated into
	 *		the boundary at the point at which the drop event is fired. If the
	 *		drag and drop operation is a copying operation, then this attribute
	 *		should a reference to a deep clone of the element on which dragging
	 *		was initiated.
	 *
	 *	`data`
	 *		A tuple describing the data that will be set to the drag data store.
	 *		See:
	 *		http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html#drag-data-store
	 *
	 * @param  {Object} options
	 * @return {Object}
	 * @memberOf dragdrop
	 */
	function Context(options) {
		return Maps.merge({}, DEFAULTS, options);
	}

	/**
	 * Whether or not the given node is draggable.
	 *
	 * In an attempt to follow the implementation on most browsers, text
	 * selections, IMG elements, and anchor elements with an href attribute are
	 * draggable by default.
	 *
	 * @param  {Element} node
	 * @return {boolean}
	 * @memberOf dragdrop
	 */
	function isDraggable(node) {
		if (!Dom.isElementNode(node)) {
			return false;
		}
		var attr = node.getAttribute('draggable');
		if ('false' === attr) {
			return false;
		}
		if ('true' === attr) {
			return true;
		}
		if ('IMG' === node.nodeName) {
			return true;
		}
		return ('A' === node.nodeName) && node.getAttribute('href');
	}

	/**
	 * Moves the given node into the given boundary positions.
	 *
	 * @private
	 * @param  {Boundary} start
	 * @param  {Boundary} end
	 * @param  {Node}     node
	 * @return {Array.<Boundary>}
	 */
	function moveNode(start, end, node) {
		var prev = node.previousSibling;
		var boundary = Editing.insert(start, end, node);
		if (prev && prev.nextSibling) {
			Dom.merge(prev, prev.nextSibling);
		}
		return [boundary, boundary];
	}

	function handleDragStart(event) {
		// Because this is required in Firefox for dragging to start on elements
		// other than IMG elements or anchor elements with href values
		event.nativeEvent.dataTransfer.setData(
			event.dnd.data[0],
			event.dnd.data[1]
		);
		event.dnd.element = event.nativeEvent.target;
		event.dnd.target = event.nativeEvent.target;
	}

	function calculateBoundaries(x, y, doc) {
		var carets = Selections.hideCarets(doc);
		var boundary = Boundaries.fromPosition(
			Dom.scrollLeft(doc) + x,
			Dom.scrollTop(doc) + y,
			doc
		);
		Selections.unhideCarets(carets);
		return [boundary, boundary];
	}

	function handleDragOver(event) {
		var nativeEvent = event.nativeEvent;
		event.selection.boundaries = calculateBoundaries(
			nativeEvent.clientX + DRAGGING_CARET_OFFSET,
			nativeEvent.clientY + DRAGGING_CARET_OFFSET,
			nativeEvent.target.ownerDocument
		);
		// Because this is necessary for dropping to work
		Events.preventDefault(nativeEvent);
	}

	function handleDrop(event) {
		var nativeEvent = event.nativeEvent;
		event.selection.boundaries = calculateBoundaries(
			// +8 because, for some reason the boundaries are always calculated
			// a character behind of where it should be...
			nativeEvent.clientX + DRAGGING_CARET_OFFSET + 8,
			nativeEvent.clientY + DRAGGING_CARET_OFFSET,
			nativeEvent.target.ownerDocument
		);
		if (event.selection.boundaries) {
			event.selection.boundaries = moveNode(
				event.selection.boundaries[0],
				event.selection.boundaries[1],
				event.dnd.element
			);
		}
		Events.stopPropagation(nativeEvent);
		// Because some browsers will otherwise redirect
		Events.preventDefault(nativeEvent);
	}

	var handlers = {
		'dragstart' : handleDragStart,
		'dragover'  : handleDragOver,
		'drop'      : handleDrop
	};

	/**
	 * Processes drag and drop events.
	 *
	 * Updates dnd and nativeEvent
	 *
	 * @param  {AlohaEvent} event
	 * @return {AlohaEvent}
	 * @memberOf dragdrop
	 */
	function middleware(event) {
		if (event.dnd && handlers[event.type]) {
			handlers[event.type](event);
		}
		return event;
	}

	return {
		middleware  : middleware,
		Context     : Context,
		isDraggable : isDraggable
	};
});

/**
 * blocks.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace blocks
 */
define('blocks',[
	'dom',
	'events',
	'dragdrop',
	'browsers',
	'editables'
], function (
	Dom,
	Events,
	DragDrop,
	Browsers,
	Editables
) {
	

	/**
	 * List of style property/value pairs.
	 *
	 * @private
	 * @type {Object.<string, string>}
	 */
	var draggingStyles = [
		[
			Browsers.VENDOR_PREFIX + 'transition',
			Browsers.VENDOR_PREFIX + 'transform 0.2s ease-out'
		],
		[Browsers.VENDOR_PREFIX + 'transform', 'scale(0.9)'],
		['opacity', '0.5']
	];

	/**
	 * Creates a drag and drop context for copying.
	 *
	 * @private
	 * @param  {Element} block
	 * @return {Context}
	 */
	function copyContext(block) {
		return DragDrop.Context({
			'dropEffect' : 'copy',
			'element'    : block.cloneNode(true),
			'target'     : block,
			'data'       : ['text/html', block.outerHTML]
		});
	}

	/**
	 * Creates a drag and drop context for moving.
	 *
	 * @private
	 * @param  {Element} block
	 * @return {Context}
	 */
	function moveContext(block) {
		return DragDrop.Context({
			'dropEffect' : 'move',
			'element'    : block,
			'target'     : block,
			'data'       : ['text/html', block.outerHTML]
		});
	}

	/**
	 * Whether or not the given event is an event targeting an Aloha Block
	 * element.
	 *
	 * @param  {AlohaEvent}   event
	 * @return {boolean}
	 */
	function isBlockEvent(event) {
		return 'IMG' === event.nativeEvent.target.nodeName
		    || Dom.hasClass(event.nativeEvent.target, 'aloha-block');
	}

	function initializeBlocks(editable) {
		var blocks = Dom.query('.aloha-block img', editable.ownerDocument);
		blocks.forEach(function (block) {
			block.setAttribute('contentEditable', 'false');
			Dom.setStyle(block, 'cursor', Browsers.VENDOR_PREFIX + 'grab');
		});
		return blocks;
	}

	function handleMouseDown(event) {
		var block = event.nativeEvent.target;
		if (isBlockEvent(event) && DragDrop.isDraggable(block)) {
			event.dnd = Events.hasKeyModifier(event, 'ctrl')
			          ? copyContext(block)
			          : moveContext(block);
		}
	}

	function handleDragStart(event) {
		if (event.dnd && isBlockEvent(event)) {
			draggingStyles.forEach(function (style) {
				if (event.dnd.target) {
					Dom.setStyle(event.dnd.target, style[0], style[1]);
				}
				Dom.setStyle(event.dnd.element, style[0], style[1]);
			});
		}
	}

	function handleDragEnd(event) {
		if (event.dnd && isBlockEvent(event)) {
			draggingStyles.forEach(function (style) {
				if (event.dnd.target) {
					Dom.setStyle(event.dnd.target, style[0], '');
				}
				Dom.setStyle(event.dnd.element, style[0], '');
			});
		}
	}

	function handleDragOver(event) {}

	var handlers = {
		'mousedown' : handleMouseDown,
		'dragstart' : handleDragStart,
		'dragend'   : handleDragEnd,
		'dragover'  : handleDragOver
	};

	/**
	 * Updates editable
	 *
	 * @param  {AlohaEvent} event
	 * @return {AlohaEvent}
	 * @memberOf blocks
	 */
	function middleware(event) {
		if (handlers[event.type]) {
			handlers[event.type](event);
		}
		return event;
	}

	return {
		middleware       : middleware,
		initializeBlocks : initializeBlocks
	};
});

/**
 * colors.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace colors
 */
define('colors',[], function () {
	

	var COLOR_PREFIX = /^(#|rgba?|hsl)\(?([^\(\)]+)/i;
	var COMMA = /\s*,\s*/;

	/**
	 * Returns a human readable representation of the given color.
	 *
	 * @param  {Array.<number|string>} color
	 * @return {string}
	 * @memberOf colors
	 */
	function serialize(color) {
		if ('string' === typeof color[0]) {
			return '#' + color.join('');
		}
		return (4 === color.length)
		     ? 'rgba(' + color.join(',') + ')'
		     : 'rgb('  + color.join(',') + ')';
	}

	/**
	 * Checks whether the two given colors are equal in value (if not in
	 * representation).
	 *
	 * equals('#f00', 'rgb(255,0,0)') == true
	 *
	 * @param  {string} a
	 * @param  {string} b
	 * @return {boolean}
	 * @memberOf colors
	 */
	function equals(a, b) {
		return hex(a) === hex(b);
	}

	/**
	 * Normalizes hexidecimal colors from #f34 to #ff3344.
	 *
	 * @private
	 * @param  {string} hex
	 * @return {Array.<string>} Long version of hexidecimal color value
	 */
	function normalizeHex(hex) {
		var r, g, b;
		if (4 === hex.length) {
			r = hex.substr(1, 1);
			g = hex.substr(2, 1);
			b = hex.substr(3, 1);
			r += r;
			g += g;
			b += b;
		} else {
			r = hex.substr(1, 2);
			g = hex.substr(3, 2);
			b = hex.substr(5, 2);
		}
		return [r, g, b];
	}

	/**
	 * Converts the RGB color representation into hexidecimal.
	 *
	 * @private
	 * @param  {Array.<string>} rgb
	 * @return {Array.<string>}
	 */
	function rgb2hex(rgb) {
		return rgb.reduce(function (values, value) {
			var color = parseInt(value, 10).toString(16);
			return values.concat(1 === color.length ? color + color : color);
		}, []);
	}

	/**
	 * Converts the hexidecimal color representation into RGB.
	 *
	 * @private
	 * @param  {Array.<string>} hex
	 * @return {Array.<string>}
	 */
	function hex2rgb(hex) {
		return normalizeHex(hex).reduce(function (values, value) {
			return values.concat(parseInt(value, 16));
		}, []);
	}

	/**
	 * Given a color string, will normalize it to a hexidecimal color string.
	 *
	 * @param  {string} value
	 * @return {string}
	 * @memberOf colors
	 */
	function hex(value) {
		var color = value.trim().match(COLOR_PREFIX);
		switch (color && color[1]) {
		case '#':
			return '#' + normalizeHex(color[0]).join('');
		case 'rgb':
		case 'rgba':
			return '#' + rgb2hex(color[2].split(COMMA)).join('');
		}
	}

	/**
	 * Given a color string, will normalize it to a RGB color string.
	 *
	 * @param  {string} value
	 * @return {Array.<number>}
	 * @memberOf colors
	 */
	function rgb(value) {
		var color = value.trim().match(COLOR_PREFIX);
		switch (color && color[1]) {
		case '#':
			return hex2rgb(color[0]);
		case 'rgb':
		case 'rgba':
			return color[2].split(COMMA).reduce(function (values, value) {
				return values.concat(parseInt(value, 10));
			}, []);
		}
	}

	/**
	 * Cross fades RGBA color `from` to RBG color `to` by a given percent.
	 *
	 * @param  {Array.<number>} from
	 * @param  {Array.<number>} to
	 * @param  {number}         percent Range from 0 - 1
	 * @return {Array.<number>}
	 * @memberOf colors
	 */
	function cross(from, to, percent) {
		var r = to[0] - from[0];
		var g = to[1] - from[1];
		var b = to[2] - from[2];
		return [
			from[0] + Math.round(r * percent),
			from[1] + Math.round(g * percent),
			from[2] + Math.round(b * percent)
		];
	}

	return {
		hex       : hex,
		rgb       : rgb,
		cross     : cross,
		equals    : equals,
		serialize : serialize
	};
});

/* image.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */

define('image/image-selection',[
	'boundaries'
], function(
	Boundaries
) {
	

	/**
	 * Checks if `node` is an ImageElement.
	 * @param {Node} node
	 * @returns {boolean}
	 */
	function isImage(node) {
		return node.nodeName === 'IMG';
	}

	/**
	 * Retrieves elements from `boundaries`.
	 * @param {Array.<Boundary>} boundaries
	 * @return {Array.<Element>}
	 */
	function imagesFromBoundaries(boundaries) {
		var elements = [];

		var node = Boundaries.nextNode(boundaries[0]);
		var lastElement = Boundaries.prevNode(boundaries[1]);
		var boundary = boundaries[0];

		while (node && (node !== lastElement)) {
			if (isImage(node)) {
				elements.push(node);
			}

			boundary = Boundaries.next(boundary);
			node = Boundaries.container(boundary);
		}

		if (isImage(lastElement)) {
			elements.push(lastElement);
		}

		return elements;
	}


	return {
		imagesFromBoundaries: imagesFromBoundaries
	};
});

/**
 * transform/utils.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('transform/utils',[
	'dom',
	'functions',
	'html',
	'arrays',
	'content'
], function (
	Dom,
	Fn,
	Html,
	Arrays,
	Content
) {
	

	function isBlacklisted(blacklist, node) {
		return blacklist[node.nodeName];
	}

	/**
	 * Given a list of nodes, will wrap consecutive nodes that return true for
	 * `pred` into a `wrapper` nodes.
	 *
	 * @private
	 * @param  {Array.<Nodes>}          nodes
	 * @param  {function(Node):boolean} pred
	 * @param  {string}                 wrapper
	 * @return {Array.<Nodes>}
	 */
	function wrapSublists(nodes, pred, wrapper) {
		var elements = [];
		var wrapplings;
		var l = nodes.length;
		var i;
		for (i = 0; i < l; i++) {
			wrapplings = [];
			while (i < l && pred(nodes[i])) {
				wrapplings.push(nodes[i]);
				i++;
			}
			if (wrapplings.length > 0) {
				elements.push(wrapplings[0].ownerDocument.createElement(wrapper));
				Dom.move(wrapplings, Arrays.last(elements));
			}
			if (i < l) {
				elements.push(nodes[i]);
			}
		}
		return elements;
	}

	/**
	 * Checks whether this node should is visible.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function isRendered(node) {
		if (Html.isRendered(node)) {
			return !Dom.isElementNode(node)
			    || Dom.getStyle(node, 'display') !== 'none';
		}
		return false;
	}

	/**
	 * Reduces the given list of nodes into a list of cleaned nodes.
	 *
	 * @private
	 * @param  {Array.<Node>}                nodes
	 * @param  {function(Node):Array.<Node>} clean
	 * @param  {function():Node}             normalize
	 * @return {Array.<Node>}
	 */
	function cleanNodes(rules, nodes, clean, normalize) {
		var allowed = nodes.filter(Fn.partial(
			Fn.complement(isBlacklisted),
			rules.disallowedNodes
		));
		var rendered = allowed.filter(isRendered);
		return rendered.reduce(function (nodes, node) {
			clean(rules, node).forEach(function (node) {
				nodes = nodes.concat(normalize(rules, node, clean));
			});
			return nodes;
		}, []);
	}

	/**
	 * Removes redundant nested blocks in the given list of nodes.
	 *
	 * Example:
	 * <div><div><p>The 2 outer divs are reduntant</p></div></div>
	 * <div><p>This outer div is also reduntant</p></div>
	 *
	 * @private
	 * @param  {Array.<Node>} nodes
	 * @return {Array.<Node>}
	 */
	function removeRedundantNesting(nodes) {
		return nodes.reduce(function (nodes, node) {
			var kids = Dom.children(node);
			if (1 === kids.length && Html.hasLinebreakingStyle(kids[0])) {
				if (Html.isGroupedElement(node)) {
					if (!Html.isGroupContainer(kids[0])) {
						Dom.removeShallow(kids[0]);
					}
				} else if (!Html.isGroupContainer(node) && Html.hasLinebreakingStyle(node)) {
					node = kids[0];
				}
			}
			var copy = Dom.cloneShallow(node);
			Dom.move(removeRedundantNesting(Dom.children(node)), copy);
			return nodes.concat(copy);
		}, []);
	}

	/**
	 * Recursively cleans the given node and it's children according to the
	 * given clean function.
	 *
	 * @private
	 * @param  {!Node}                       node
	 * @param  {function(Node):Array.<Node>} clean
	 * @return {Array.<Node>}
	 */
	function cleanNode(rules, node, clean) {
		return clean(rules, node).reduce(function (nodes, node) {
			if (isBlacklisted(rules.disallowedNodes, node) || !isRendered(node)) {
				return nodes;
			}
			var children = cleanNodes(rules, Dom.children(node), clean, cleanNode);
			if ('DIV' === node.nodeName) {
				children = wrapSublists(
					children,
					Html.isInlineNode,
					rules.defaultBlock
				);
			}
			var copy = Dom.cloneShallow(node);
			Dom.move(removeRedundantNesting(children), copy);
			return nodes.concat(copy);
		}, []);
	}

	/**
	 * Creates a rewrapped copy of `element`. Will create a an element based on
	 * `nodeName`, and copies the content of the given element into it.
	 *
	 * @param  {!Element} element
	 * @param  {string}   nodeName
	 * @return {Element}
	 */
	function rewrap(element, nodeName) {
		var wrapper = element.ownerDocument.createElement(nodeName);
		Dom.move(Dom.children(Dom.clone(element)), wrapper);
		return wrapper;
	}

	/**
	 * Normalizes the given node tree and returns a fragment.
	 *
	 * @param  {!Element}            element
	 * @param  {function(Node):Node} clean
	 * @return {Fragment}
	 */
	function normalize(rules, element, clean) {
		var fragment = element.ownerDocument.createDocumentFragment();
		Dom.move(cleanNode(rules, element, clean), fragment);
		return fragment;
	}

	/**
	 * Extracts body content if the content is an HTML page. Otherwise it
	 * returns the content itself.
	 *
	 * @fixme
	 * What if `content` contains a comment like this:
	 * <html><!-- <body>gotcha!</body> --><title>woops</title><body>hello, world!</body></html>
	 *
	 * @param  {string} markup
	 * @return {string}
	 */
	function extract(markup) {
		markup = markup.replace(/\n/g, ' ');
		markup = markup.replace(/<iframe.*?<\/iframe>/g, '');
		var start = /<body.*?>/i.exec(markup);
		var end = /<\/body.*?>/i.exec(markup);
		if (start && end) {
			var index = markup.indexOf(start[0]) + start[0].length;
			var lastIndex = markup.indexOf(end[0]);
			return markup.slice(index, lastIndex);
		}
		return markup;
	}

	var DEFAULT_RULES = {
		defaultBlock      : 'p',
		allowedStyles     : Content.allowedStyles(),
		allowedAttributes : Content.allowedAttributes(),
		disallowedNodes   : Content.disallowedNodes(),
		nodeTranslations  : Content.nodeTranslations()
	};

	return {
		DEFAULT_RULES : DEFAULT_RULES,
		normalize     : normalize,
		extract       : extract,
		rewrap        : rewrap
	};
});

/**
 * transform/html.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('transform/html',[
	'dom',
	'arrays',
	'maps',
	'functions',
	'html',
	'content',
	'./utils'
], function (
	Dom,
	Arrays,
	Maps,
	Fn,
	Html,
	Content,
	Utils
) {
	

	/**
	 * Conversion of font size number into point size unit of width values (em).
	 * Font size numbers range from 1 to 7.
	 *
	 * @const
	 * @private
	 * @type {Object.<string, string>}
	 */
	var FONT_SIZES = {
		'1': '0.63em',
		'2': '0.82em',
		'3': '1em',
		'4': '1.13em',
		'5': '1.5em',
		'6': '2em',
		'7': '3em'
	};

	/**
	 * Unwraps or replaces the given font element while preserving the styles it
	 * effected.
	 *
	 * @private
	 * @param  {Element} font Must be a font element
	 * @return {Element}
	 */
	function normalizeFont(font) {
		var children = Dom.children(font);
		var color = Dom.getStyle(font, 'color')       || Dom.getAttr(font, 'color');
		var size  = Dom.getStyle(font, 'font-size')   || FONT_SIZES[Dom.getAttr(font, 'size')];
		var face  = Dom.getStyle(font, 'font-family') || Dom.getAttr(font, 'face');
		var child;
		if (1 === children.length && Dom.isElementNode(children[0])) {
			child = children[0];
		} else {
			child = font.ownerDocument.createElement('span');
			Dom.move(children, child);
		}
		if (color) {
			Dom.setStyle(child, 'color', color);
		}
		if (size) {
			Dom.setStyle(child, 'font-size', size);
		}
		if (face) {
			Dom.setStyle(child, 'font-family', face);
		}
		return child;
	}

	/**
	 * Strategy:
	 * 1) Check if the parent of the center allows for paragraph children.
	 *    - If it doesn't, split the element down to the first ancestor that
	 *      does allow for a paragraph, then insert the center at the split.
	 * 2) replace the center node with a paragraph
	 * 3) add alignment styling to new paragraph
	 *
	 * @todo implement this function
	 * @private
	 * @param  {Element} node
	 * @return {Element}
	 */
	function normalizeCenter(node) {
		return node;
	}

	/**
	 * Extracts width and height attributes from the given element, and applies
	 * them as styles instead.
	 *
	 * @private
	 * @param  {Element} img Must be an image
	 * @return {Element}
	 */
	function normalizeImage(img) {
		var width = Dom.getAttr(img, 'width');
		var height = Dom.getAttr(img, 'height');
		if (width) {
			Dom.setStyle(img, 'width', width);
		}
		if (height) {
			Dom.setStyle(img, 'height', height);
		}
		return img;
	}

	function generateWhitelist(whitelist, nodeName) {
		return (whitelist['*'] || []).concat(whitelist[nodeName] || []);
	}

	/**
	 * Removes all disallowed attributes from the given node.
	 *
	 * @private
	 * @param  {Editable} editable
	 * @param  {Node}     node
	 * @return {Node}
	 */
	function normalizeAttributes(allowedAttributes, node) {
		var permitted = generateWhitelist(allowedAttributes, node.nodeName);
		var attrs = Maps.keys(Dom.attrs(node));
		var allowed = Arrays.intersect(permitted, attrs);
		var disallowed = Arrays.difference(attrs, allowed);
		disallowed.forEach(Fn.partial(Dom.removeAttr, node));
	}

	/**
	 * Removes all disallowed styles from the given node.
	 *
	 * @private
	 * @param  {Editable} editable
	 * @param  {Node} node
	 */
	function normalizeStyles(allowedStyles, node) {
		var permitted = generateWhitelist(allowedStyles, node.nodeName);
		// Because '*' means that all styles are permitted
		if (Arrays.contains(permitted, '*')) {
			return;
		}
		var styles = permitted.reduce(function (map, name) {
			map[name] = Dom.getStyle(node, name);
			return map;
		}, {});
		Dom.removeAttr(node, 'style');
		Maps.forEach(styles, function (value, key) {
			if (value) {
				Dom.setStyle(node, key, value);
			}
		});
	}

	/**
	 * Unwrap spans that have not attributes.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {Node|Fragment}
	 */
	function normalizeSpan(node) {
		if (Dom.hasAttrs(node)) {
			return node;
		}
		var fragment = node.ownerDocument.createDocumentFragment();
		Dom.move(Dom.children(node), fragment);
		return fragment;
	}

	/**
	 * Runs the appropriate cleaning processes on the given node based on its
	 * type. The returned node will not necessarily be of the same type as that
	 * of the given (eg: <font> => <span>).
	 *
	 * @private
	 * @param  {Editable} editable
	 * @param  {Node}     node
	 * @return {Array.<Node>}
	 */
	function clean(rules, node) {
		node = Dom.clone(node);
		if (Dom.isTextNode(node)) {
			return [node];
		}
		var cleaned;
		switch (node.nodeName) {
		case 'IMG':
			cleaned = normalizeImage(node);
			break;
		case 'FONT':
			cleaned = node;
			// Because <font> elements may be nested
			do {
				cleaned = normalizeFont(cleaned);
			} while ('FONT' === cleaned.nodeName);
			break;
		case 'CENTER':
			cleaned = normalizeCenter(node);
			break;
		default:
			cleaned = node;
		}
		if (Dom.isFragmentNode(cleaned)) {
			return [cleaned];
		}
		normalizeAttributes(rules.allowedAttributes, cleaned);
		normalizeStyles(rules.allowedStyles, cleaned);
		if ('SPAN' === cleaned.nodeName) {
			cleaned = normalizeSpan(cleaned);
		}
		var kids = Dom.children(cleaned);
		var i;
		for (i = 0; i < kids.length; i++) {
			if (!Content.allowsNesting(cleaned.nodeName, kids[i].nodeName)) {
				return kids;
			}
		}
		return [cleaned];
	}

	/**
	 * Transforms html markup to normalized HTML.
	 *
	 * @param  {string}   markup
	 * @param  {Document} document
	 * @param  {Object}   rules
	 * @return {string}
	 * @alias html
	 * @memberOf transform
	 */
	function transform(markup, doc, rules) {
		if (!rules) {
			rules = Utils.DEFAULT_RULES;
		}
		var fragment = doc.createDocumentFragment();
		Dom.move(Html.parse(Utils.extract(markup), doc), fragment);
		return Dom.outerHtml(Utils.normalize(rules, fragment, clean));
	}

	return {
		transform : transform
	};
});

/**
 * transform/plain.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('transform/plain',[], function () {
	

	/**
	 * Transforms plain text into visually comparable HTML.
	 *
	 * @param  {string} text
	 * @return {string}
	 * @alias plain
	 * @memberOf transform
	 */
	function transform(text) {
		var markup = text.split(/\n/).reduce(function (paragraphs, snippet) {
			return paragraphs.concat('<p>', snippet.trim() || '<br>', '</p>');
		}, []);
		return markup.join('');
	}

	return {
		transform: transform
	};
});

/**
 * transform/ms-word/list.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('transform/ms-word/lists',[
	'dom',
	'misc',
	'arrays',
	'functions'
], function (
	Dom,
	Misc,
	Arrays,
	Fn
) {
	

	/**
	 * Matches list numbers:
	 *
	 * 1)
	 * iv)
	 * a)
	 * 1.
	 * 3.1.6.
	 *
	 * @private
	 * @type {RegExp}
	 */
	var LIST_NUMBERS = new RegExp('^\\s*'
	                 + '('
	                 + '(?:[0-9]{1,3}|[a-zA-Z]{1,5})+' // 123 or xiii
	                 + '[\\.\\)]'                      // .   or )
	                 + ')+'
	                 + '\\s*$');

	/**
	 * Matches mso-list ignore style.
	 *
	 * @private
	 * @type {RegExp}
	 */
	var LIST_IGNORE_STYLE = /mso-list:\s*Ignore/i;

	/**
	 * Matches mso-list level.
	 *
	 * @private
	 * @type {RegExp}
	 */
	var MSO_LIST_LEVEL = /mso-list:.*?level(\d+)/i;

	/**
	 * Extracts the number from an ordered list item.
	 *
	 * @private
	 * @param  {Element} element
	 * @return {?String}
	 */
	function extractNumber(element) {
		if (!element.firstChild) {
			return null;
		}
		var match = LIST_NUMBERS.exec(Dom.text(element.firstChild));
		if (!match) {
			return null;
		}
		match = /(\w+)/i.exec(match[0]);
		return match ? match[1] : null;
	}

	/**
	 * Gets the numbering if the list for an ordered list.
	 *
	 * Returns an object containing a property "type" that denotes what the
	 * numbering type for the list is, and a property "start" that indicates
	 * the start value of the numbering.
	 *
	 * @private
	 * @param  {Element} p
	 * @return {Object}
	 */
	function getNumbering(p) {
		var number = extractNumber(p);
		if (!number) {
			return {};
		}
		var start;
		var type;
		if (/\d+/.test(number)) {
			start = number;
			type = '1';
		} else if (/i/i.test(number)) {
			type = (/I/.test(number)) ? 'I' : 'i';
		} else {
			type = (/[A-Z]/.test(number)) ? 'A' : 'a';
		}
		return {
			start : start,
			type  : type
		};
	}

	/**
	 * Checks whether the given list-paragraph contains a leading span that
	 * denotes it as an ordered list.
	 *
	 * @private
	 * @param  {Element} p
	 * @return {boolean}
	 */
	function isOrderedList(p) {
		if (!p.firstChild) {
			return false;
		}
		var font = Dom.getStyle(p.firstChild, 'fontFamily');
		if (font === 'Wingdings' || font === 'Symbol') {
			return false;
		}
		return null !== extractNumber(p);
	}

	/**
	 * Checks whether the given node is a leading span that msword uses to as a
	 * list bullet point or list number.
	 *
	 * <p class="MsoListParagraphCxSp...">
	 *
	 *     <span style="font-family:Symbol...">
	 *                        (Bullet/Number + Indentation) => isIgnorableSpan
	 *                                       |
	 *                                       |-----------.
	 *                                       |           |
	 *                                       v           v
	 *         <span style="mso-list:Ignore">·<span>&nbsp;&nbsp;</span></span>
	 *     </span>
	 *
	 *     <span>List item</span>
	 * </p>
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isIgnorableSpan(node) {
		if ('SPAN' === node.nodeName
				&& LIST_IGNORE_STYLE.test(Dom.getAttr(node, 'style'))) {
			return true;
		}
		return !Dom.isTextNode(node) && isIgnorableSpan(node.firstChild);
	}

	/**
	 * Checks whether the given node is a msword list-paragraph.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isListParagraph(node) {
		if ('P' !== node.nodeName) {
			return false;
		}
		return Dom.hasClass(node, 'MsoListParagraph')
		    || Dom.hasClass(node, 'MsoListParagraphCxSpFirst')
		    || Dom.hasClass(node, 'MsoListParagraphCxSpMiddle')
		    || Dom.hasClass(node, 'MsoListParagraphCxSpLast');
	}

	/**
	 * Checks whether the given node is a list-paragraph that denotes the start
	 * of a new list.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isFirstListParagraph(node) {
		return ('P' === node.nodeName)
			&& Dom.hasClass(node, 'MsoListParagraphCxSpFirst');
	}

	/**
	 * Checks whether the given node is a list-paragraph that denotes a one
	 * item list.
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isSingleListParagraph(node) {
		return ('P' === node.nodeName)
		    && Dom.hasClass(node, 'MsoListParagraph');
	}

	/**
	 * Checks whether the given element is a paragraph the demarks last item in
	 * a list.
	 *
	 * @private
	 * @param  {Element} node
	 * @return {boolean}
	 */
	function isLastListParagraph(node) {
		return ('P' === node.nodeName)
		    && Dom.hasClass(node, 'MsoListParagraphCxSpLast');
	}

	/**
	 * Checks whether the given node is a list-paragraph that denotes the start
	 * of a list item (as opposed to a continuation of a list element).
	 *
	 * @private
	 * @param  {Node} node
	 * @return {boolean}
	 */
	function isStartOfListItem(node) {
		if (!node.firstChild) {
			return false;
		}
		return isIgnorableSpan(node.firstChild);
	}

	/**
	 * Creates an a list container from the given list-paragraph.
	 *
	 * @private
	 * @param  {Element} p
	 * @return {Element}
	 */
	function createContainer(p) {
		var type = isOrderedList(p) ? 'ol' : 'ul';
		var list = p.ownerDocument.createElement(type);
		if ('ul' === type) {
			return list;
		}
		var numbering = getNumbering(p);
		if (Misc.defined(numbering.start)) {
			Dom.setAttr(list, 'start', numbering.start);
		}
		if (Misc.defined(numbering.type)) {
			Dom.setAttr(list, 'type', numbering.type);
		}
		return list;
	}

	/**
	 * Extracts the list item level from the given list-paragraph.
	 *
	 * @private
	 * @param  {Element} p
	 * @return {number}
	 */
	function extractLevel(p) {
		var match = MSO_LIST_LEVEL.exec(Dom.getAttr(p, 'style'));
		return (match && match[1]) ? parseInt(match[1], 10) : 1;
	}

	/**
	 * Creates a list DOM structure based on the given `list` data structure
	 * (created from createList()).
	 *
	 * @private
	 * @param  {Object} list
	 * @param  {string} marker
	 * @return {Element}
	 */
	function constructList(list, marker) {
		var container = createContainer(list.node);
		var items = list.items.reduce(function (items, item) {
			var children = item.reduce(function (children, contents) {
				return children.concat(
					contents[marker] ? constructList(contents, marker)
					                 : contents
				);
			}, []);
			var li = list.node.ownerDocument.createElement('li');
			Dom.copy(children, li);
			return items.concat(li);
		}, []);
		Dom.move(items, container);
		return container;
	}

	/**
	 * Takes a flat list of nodes, which consitutes a (multi-level) list in
	 * MS-Word and generates a standard HTML list DOM structure from it.
	 *
	 * This function requires that the given list of nodes must begin with a
	 * list-paragraph, and must end with a list-paragraph since this is the only
	 * valid way that lists are represented in MS-Word.
	 *
	 * @private
	 * @param  {Array.<Node>}              nodes
	 * @param  {function(Element):Element} transform
	 * @return {?Element}
	 */
	function createList(nodes, transform) {
		var i, j, l, node, list, first, last, level;
		var marker = '_aloha' + (new Date().getTime());

		for (i = 0; i < nodes.length; i++) {
			node = transform(nodes[i]);
			level = extractLevel(node);

			if (!list) {
				first = list = {
					parent : null,
					level  : 1,
					node   : node,
					items  : []
				};
				list[marker] = true;
			}

			if (level > list.level) {
				for (j = list.level; j < level; j++) {
					list = {
						parent : list,
						level  : j + 1,
						node   : node,
						items  : []
					};
					list[marker] = true;
					last = Arrays.last(list.parent.items);
					if (!last) {
						last = [];
						list.parent.items.push(last);
					}
					last.push(list);
				}
			}

			if (level < list.level) {
				for (j = level, l = list.level; j < l && list.parent; j++) {
					list = list.parent;
				}
			}

			if (!isListParagraph(node) || !isStartOfListItem(node)) {
				// Because `node` is line-breaking content that continues inside
				// of the previous list item
				last = Arrays.last(list.items);
				if (!last) {
					last = [];
					list.items.push(last);
				}
				last.push(node);
			} else {
				// Because `node` is a new list item
				list.items.push(
					Dom.children(node).filter(Fn.complement(isIgnorableSpan))
				);
			}
		}

		return first && constructList(first, marker);
	}

	/**
	 * Transforms list-paragraphs in the given DOM structure to normalized HTML
	 * lists.
	 *
	 * Note that decimal-pointer counters are a styling issue and not a
	 * structural issue  This mean that the list numbering may look different,
	 * even when the normalized structure matches MS-Word's, until you apply the
	 * correct css styling.
	 * (see: https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/Counters). 
	 *
	 * @param  {Element} element
	 * @return {Element} A normalized copy of `element`
	 */
	function transform(element) {
		var children = Dom.children(element);
		var processed = [];
		var i;
		var l = children.length;
		var list;
		var last;
		var node;
		var nodes;
		for (i = 0; i < l; i++) {
			node = children[i];
			if (isSingleListParagraph(node)) {
				processed.push(createList([node], transform));
			} else if (!isFirstListParagraph(node)) {
				processed.push(transform(node));
			} else {
				nodes = Dom.nodeAndNextSiblings(node, isLastListParagraph);
				// Becuase Dom.nextSibling() excludes the predicative node
				last = Arrays.last(nodes).nextSibling;
				if (last) {
					nodes.push(last);
				}
				list = createList(nodes, transform);
				if (list) {
					processed.push(list);
					i += nodes.length - 1;
				}
			}
		}
		var clone = Dom.clone(element, false);
		Dom.move(processed, clone);
		return clone;
	}

	return {
		transform: transform
	};
});

/**
 * transform/ms-word/tables.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('transform/ms-word/tables',['dom'], function (Dom) {
	

	/**
	 * Converts all TD elements into TH elements in the given list of nodes.
	 *
	 * @private
	 * @param  {Array.<Node>} nodes
	 * @return {Array.<Node>}
	 */
	function createTableHeadings(nodes) {
		var list = [];
		nodes.forEach(function (node) {
			if ('TD' === node.nodeName) {
				var children = Dom.children(node);
				node = node.ownerDocument.createElement('th');
				Dom.copy(children, node);
			}
			return list.push(node);
		});
		return list;
	}

	/**
	 * Matches MS-WORD styling that demarks table header rows.
	 *
	 * @private
	 * @type {RegExp}
	 */
	var HEADER_ROW_INDEX = /mso-yfti-irow:\-1;/;

	function isTableHeading(node) {
		return 'TR' === node.nodeName
		    && HEADER_ROW_INDEX.test(Dom.getAttr(node, 'style'));
	}

	/**
	 * Normalizes tables in the given DOM structure.
	 *
	 * @param  {node}     element
	 * @return {Element} A normalized copy of `element`
	 */
	function transform(element) {
		var children = Dom.children(element);
		var processed = [];
		var node;
		var tds;
		var i;
		for (i = 0; i < children.length; i++) {
			node = transform(children[i]);
			if (isTableHeading(node)) {
				node = Dom.clone(node);
				tds = Dom.children(node);
				tds.forEach(Dom.remove);
				Dom.move(createTableHeadings(tds), node);
			}
			processed.push(node);
		}
		var clone = Dom.clone(element, false);
		Dom.move(processed, clone);
		return clone;
	}

	return {
		transform: transform
	};
});

/**
 * transform/ms-word/toc.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 */
define('transform/ms-word/toc',[
	'functions',
	'dom',
	'arrays',
	'../utils'
], function (
	Fn,
	Dom,
	Arrays,
	Utils
) {
	

	/**
	 * Match MsoToc.
	 *
	 * @private
	 * @type {RegExp}
	 */
	var TOC_CLASS_NAME = /MsoToc(\d+)/;

	/**
	 * Extracts the TOC element level number, otherwise returns null.
	 *
	 * @private
	 * @param  {Element} element
	 * @return {?number}
	 */
	function extractLevel(element) {
		var match = TOC_CLASS_NAME.exec(Dom.getAttr(element, 'class'));
		return (match && match[1]) ? parseInt(match[1], 10) : null;
	}

	/**
	 * Checks whether the given node is a TOC header paragraph.
	 *
	 * @private
	 * @param  {Node}
	 * @return {boolean}
	 */
	function isTocHeading(node) {
		return 'P' === node.nodeName && Dom.hasClass(node, 'MsoTocHeading');
	}

	/**
	 * Checks whether the given node is a TOC list item.
	 *
	 * @private
	 * @param  {Node}
	 * @return {boolean}
	 */
	function isTocItem(node) {
		if (!Dom.isElementNode(node)) {
			return false;
		}
		var match = TOC_CLASS_NAME.exec(Dom.getAttr(node, 'class'));
		return match ? match.length > 0 : false;
	}

	/**
	 * Creates a list DOM structure based on the given `list` data structure
	 * (created from createList()).
	 *
	 * @private
	 * @param  {Object} list
	 * @param  {string} marker
	 * @return {Element}
	 */
	function constructList(list, marker) {
		var container = list.node.ownerDocument.createElement('ul');
		var items = list.items.reduce(function (items, item) {
			var children = item.reduce(function (children, contents) {
				return children.concat(
					contents[marker] ? constructList(contents, marker)
					                 : contents
				);
			}, []);
			var li = list.node.ownerDocument.createElement('li');
			Dom.copy(children, li);
			return items.concat(li);
		}, []);
		Dom.move(items, container);
		return container;
	}

	/**
	 * Takes a flat list of nodes, which consitutes a (multi-level) list in
	 * MS-Word and generates a standard HTML list DOM structure from it.
	 *
	 * This function requires that the given list of nodes must begin with a
	 * list-paragraph, and must end with a list-paragraph since this is the only
	 * valid way that lists are represented in MS-Word.
	 *
	 * @private
	 * @param  {Array.<Node>}              nodes
	 * @param  {function(Element):Element} transform
	 * @return {?Element}
	 */
	function createList(nodes, transform) {
		var i, j, l, node, list, first, last, level;
		var marker = '_aloha' + (new Date().getTime());

		for (i = 0; i < nodes.length; i++) {
			node = transform(nodes[i]);
			level = extractLevel(node);

			if (!list) {
				first = list = {
					parent : null,
					level  : 1,
					node   : node,
					items  : []
				};
				list[marker] = true;
			} else if (level > list.level) {
				for (j = list.level; j < level; j++) {
					list = {
						parent : list,
						level  : j + 1,
						node   : node,
						items  : []
					};
					list[marker] = true;
					last = Arrays.last(list.parent.items);
					if (!last) {
						last = [];
						list.parent.items.push(last);
					}
					last.push(list);
				}
			} else if (level < list.level) {
				for (j = level, l = list.level; j < l && list.parent; j++) {
					list = list.parent;
				}
			}

			list.items.push(Dom.children(node));
		}

		return first && constructList(first, marker);
	}

	/**
	 * Transforms MS Office table of contents into a normalized HTML list.
	 *
	 * @param  {Element} element
	 * @return {Element} A normalized copy of `element`
	 */
	function transform(element) {
		var notTocItem = Fn.complement(isTocItem);
		var children = Dom.children(element);
		var processed = [];
		var l = children.length;
		var i;
		var list;
		var node;
		var nodes;
		for (i = 0; i < l; i++) {
			node = children[i];
			if (isTocHeading(node)) {
				processed.push(Utils.rewrap(node, 'h1'));
			} else if (isTocItem(node)) {
				nodes = Arrays.split(Dom.nodeAndNextSiblings(node), notTocItem)[0];
				list = createList(nodes, transform);
				if (list) {
					processed.push(list);
					i += nodes.length - 1;
				}
			} else {
				processed.push(transform(node));
			}
		}
		var clone = Dom.clone(element, false);
		Dom.move(processed, clone);
		return clone;
	}

	return {
		transform: transform
	};
});

/* transform/ms-word.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * Refernces:
 * CF_HTML:
 * http://msdn.microsoft.com/en-us/library/windows/desktop/ms649015(v=vs.85).aspx
 */
define('transform/ms-word',[
	'dom',
	'html',
	'arrays',
	'./ms-word/lists',
	'./ms-word/tables',
	'./ms-word/toc',
	'./utils'
], function (
	Dom,
	Html,
	Arrays,
	Lists,
	Tables,
	Toc,
	Utils
) {
	

	/**
	 * Matches tags in the markup that are deemed superfluous: having no effect
	 * in the representation of the content.
	 *
	 * This will be used to strip tags like "<w:data>08D0C9EA7...</w:data>" but
	 * not "<o:p></o:p>"
	 *
	 * @private
	 * @const
	 * @type {RegExp}
	 */
	var SUPERFLUOUS_TAG = /xml|v\:\w+/i;

	/**
	 * Matches namespaced tags like "<o:p></o:p>".
	 *
	 * @private
	 * @const
	 * @type {RegExp}
	 */
	var NAMESPACED_NODENAME = /o\:(\w+)/i;

	/**
	 * Checks whether the given node is considered superfluous (has not affect
	 * to the visual presentation of the content).
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {boolean}
	 */
	function isSuperfluous(node) {
		return node.nodeType === Dom.Nodes.COMMENT
		    || SUPERFLUOUS_TAG.test(node.nodeName);
	}

	/**
	 * Returns the the non-namespaced version of the given node's nodeName.
	 * If the node is not namespaced, will return null.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {string}
	 */
	function namespacedNodeName(node) {
		var match = node.nodeName.match(NAMESPACED_NODENAME);
		return match ? match[1] : null;
	}

	/**
	 * Returns a clean copy of the given node.
	 *
	 * @private
	 * @param  {!Node} node
	 * @return {Array.<Node>}
	 */
	function clean(rules, node) {
		if (isSuperfluous(node)) {
			return [];
		}
		if (Dom.isTextNode(node)) {
			return [Dom.clone(node)];
		}
		if (Dom.hasClass(node, 'MsoTitle')) {
			return [Utils.rewrap(node, 'h1')];
		}
		if (Dom.hasClass(node, 'MsoSubtitle')) {
			return [Utils.rewrap(node, 'h2')];
		}
		var nodeName = namespacedNodeName(node);
		if (nodeName) {
			return [Utils.rewrap(node, nodeName)];
		}
		return [Dom.clone(node)];
	}

	/**
	 * Checks if the given markup originates from MS Office.
	 *
	 * TODO: use <meta name="Generator" content="WORD|OPENOFFICE|ETC">
	 *       this is more formally correct
	 *
	 * @param  {string}    markup
	 * @param  {!Document} doc
	 * @return {boolean}
	 */
	function isMSWordContent(markup, doc) {
		var element = doc.createElement('div');
		Dom.move(Html.parse(markup, doc), element);
		return null !== element.querySelector('[style*="mso-"],[class^="Mso"]');
	}

	/**
	 * Transforms msword markup to normalized HTML.
	 *
	 * @param  {string}    markup
	 * @param  {!Document} doc
	 * @return {string}
	 * @alias msword
	 * @memberOf transform
	 */
	function transform(markup, doc, rules) {
		if (!rules) {
			rules = Utils.DEFAULT_RULES;
		}
		var nodes = Html.parse(Utils.extract(markup), doc);
		var raw = doc.createElement('div');
		Dom.move(nodes, raw);
		var fragment = Utils.normalize(rules, raw, clean) || raw;
		fragment = Lists.transform(fragment);
		fragment = Toc.transform(fragment);
		fragment = Tables.transform(fragment);
		var children = Dom.children(fragment);
		return 0 === children.length ? '' : children[0].innerHTML;
	}

	return {
		transform       : transform,
		isMSWordContent : isMSWordContent
	};
});

/**
 * transform.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace transform
 */
define('transform',[
	'transform/html',
	'transform/plain',
	'transform/ms-word'
], function (
	Html,
	Plain,
	MSWord
) {
	

	/**
	 * Transformation functions, mapped to their corresponding mime-subtype.
	 */
	return {
		html : Html.transform,
		plain : Plain.transform,
		msword : MSWord.transform
	};
});

/**
 * paste.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace paste
 */
define('paste',[
	'dom',
	'html',
	'undo',
	'paths',
	'arrays',
	'events',
	'boromir',
	'content',
	'editing',
	'zippers',
	'mutation',
	'boundaries',
	'functions',
	'transform',
	'transform/ms-word'
], function (
	Dom,
	Html,
	Undo,
	Paths,
	Arrays,
	Events,
	Boromir,
	Content,
	Editing,
	Zip,
	Mutation,
	Boundaries,
	Fn,
	Transform,
	WordTransform
) {
	

	/**
	 * Mime types
	 *
	 * @private
	 * @type {Object.<string, string>}
	 */
	var Mime = {
		plain : 'text/plain',
		html  : 'text/html'
	};

	/**
	 * Checks the content type of `event`.
	 *
	 * @private
	 * @param  {!Event} event
	 * @param  {string} type
	 * @return {boolean}
	 */
	function holds(event, type) {
		return Arrays.contains(event.clipboardData.types, type);
	}

	/**
	 * Gets content of the paste data that matches the given mime type.
	 *
	 * @private
	 * @param  {!Event} event
	 * @param  {string} type
	 * @return {string}
	 */
	function getData(event, type) {
		return event.clipboardData.getData(type);
	}

	/**
	 * Moves the given node before the given boundary.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @param  {!Node}     node
	 * @return {Bounbary}
	 */
	function moveBeforeBoundary(boundary, node) {
		return Mutation.insertNodeAtBoundary(node, boundary, true);
	}

	/**
	 * Pastes the markup at the given boundary range.
	 *
	 * @private
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @param  {string}    markup
	 * @return {Array.<Boundary>}
	 */
	function insert(start, end, markup) {
		var doc = Boundaries.document(start);
		var boundaries = Editing.remove(start, end);
		var nodes = Html.parse(markup, doc);

		if (0 === nodes.length) {
			return boundaries;
		}

		// Because we are only able to detect "void type" (non-content editable
		// nodes) when they are contained within a editing host
		var container = doc.createElement('div');
		Dom.setAttr(container, 'contentEditable', true);
		Dom.move(nodes, container);

		var first = nodes[0];

		// Because (unlike plain-text), pasted html will contain an unintended
		// linebreak caused by the wrapper inwhich the pasted content is placed
		// (P in most cases). We therefore unfold this wrapper whenever is valid
		// to do so (ie: we cannot unfold grouping elements like 'ul', 'table',
		// etc)
		if (!Dom.isTextNode(first) && !Html.isVoidType(first) && !Html.isGroupContainer(first)) {
			nodes = Dom.children(first).concat(nodes.slice(1));
		}

		if (0 === nodes.length) {
			return boundaries;
		}

		var editable = Dom.editingHost(Boundaries.container(boundaries[0]));
		var zip = Zip.zipper(editable, {
			start : boundaries[0],
			end   : boundaries[1]
		});
		var loc = Zip.go(zip.loc, zip.markers.start);
		nodes.forEach(function (child) {
			loc = Zip.split(loc, function (loc) {
				return Content.allowsNesting(Zip.after(loc).name(), child.nodeName);
			});
			loc = Zip.insert(loc, Boromir(child));
		});
		var markers = Zip.update(loc);

		return [markers.start, markers.end];

		var result = MutationTrees.update(tree);
		boundaries = result[1].map(Fn.partial(Paths.toBoundary, result[0].domNode()));

		var last = Arrays.last(nodes);
		var next = Boundaries.nodeAfter(boundaries[1]);

		// Because we want to remove the unintentional line added at the end of
		// the pasted content
		if (next && ('P' === last.nodeName || 'DIV' === last.nodeName)) {
			if (Html.hasInlineStyle(next)) {
				boundaries[1] = Boundaries.fromEndOfNode(last);
				// Move the next inline nodes into the last element
				Dom.move(Dom.nodeAndNextSiblings(next, Html.hasLinebreakingStyle), last);
			} else if (!Html.isVoidType(next) && !Html.isGroupContainer(next)) {
				// Move the children of the last element into the beginning of
				// the next block element
				boundaries[1] = Dom.children(last).reduce(moveBeforeBoundary, Boundaries.create(next, 0));
				Dom.remove(last);
			}
		}

		return boundaries;
	}

	/**
	 * Extracts the paste data from the event object.
	 *
	 * @private
	 * @param  {Event}    event
	 * @param  {Document} doc
	 * @return {string}
	 */
	function extractContent(event, doc, rules) {
		if (holds(event, Mime.html)) {
			var content = getData(event, Mime.html);
			return WordTransform.isMSWordContent(content, doc)
			     ? Transform.html(Transform.msword(content, doc), doc, rules)
			     : Transform.html(content, doc, rules);
		}
		if (holds(event, Mime.plain)) {
			return Transform.plain(getData(event, Mime.plain), doc);
		}
		return '';
	}

	/**
	 * Handles and processes paste events.
	 *
	 * Updates:
	 * 		range
	 * 		nativeEvent
	 *
	 * @param  {AlohaEvent} event
	 * @return {AlohaEvent}
	 * @memberOf paste
	 */
	function middleware(event) {
		if ('paste' !== event.type || 'undefined' === typeof event.nativeEvent.clipboardData) {
			return event;
		}
		Events.suppress(event.nativeEvent);
		var content = extractContent(
			event.nativeEvent,
			event.nativeEvent.target.ownerDocument,
			event.editable.settings
		);
		if (!content) {
			return event;
		}
		Undo.capture(event.editable.undoContext, {
			meta: {type: 'paste'}
		}, function () {
			event.selection.boundaries = insert(
				event.selection.boundaries[0],
				event.selection.boundaries[1],
				content
			);
		});
		return event;
	}

	return {
		middleware: middleware
	};
});

/** image.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace images
 */

define('image',[
	'boundaries',
	'dom',
	'image/image-selection',
	'paste'
], function (
	Boundaries,
	Dom,
	ImageSelection,
	Paste
) {
	

	/**
	 * Sets `attributes` in `image`, overwritten the existing ones.
	 * @param {ImageElement} image
	 * @param {Array.<Object.<string, string>>} attributes
	 */
	function setImageAttributes(image, attributes) {
		Object.keys(attributes).forEach(function (item) {
			Dom.setAttr(image, item, attributes[item]);
		});
	}

	/**
	 * Creates Image element.
	 * @param {Array.<Object.<string, string>>} attributes
	 * @param doc
	 * @return {ImageElement}
	 */
	function createImage (attributes, doc) {
		var image = doc.createElement('img');

		setImageAttributes(image, attributes);

		return image;
	}

	/**
	 * Inserts a new image with `attributes` in `range`.
	 * @param {Range} range
	 * @param {Array.<Object.<string, string>> attributes Attributes
	 *        for the new image
	 */
	function insertFromRange(range, attributes) {
		var doc = range.commonAncestorContainer.ownerDocument;
		var image = createImage(attributes, doc);
		var paragraph = doc.createElement('p');

		paragraph.appendChild(image);

		var docFragment = doc.createDocumentFragment();
		docFragment.appendChild(paragraph);

		Paste.insertIntoDomFromRange(range, docFragment, {}, doc);
	}

	/**
	 * Inserts image with `attributes` in the actual selection.
	 * @param {Array.<Object.<string, string>>} attributes Attributes
	 *        for the new image
	 * @param {Document} doc
	 * @memberOf images
	 */
	function insert(attributes, doc) {
		var boundaries = Boundaries.get(doc);
		if (!boundaries) {
			return;
		}
		insertFromRange(
			Boundaries.range(boundaries[0], boundaries[1]),
			attributes
		);
	}

	/**
	 * Sets `attributes` to all images in `range`.
	 * @param {Range} range
	 * @param {Array.<Object.<string, string>>} attributes
	 */
	function setAttributesFromRange(range, attributes) {
		var boundaries = Boundaries.fromRange(range);

		var images = ImageSelection.imagesFromBoundaries(boundaries);

		images.forEach(function (img) {
			setImageAttributes(img, attributes);
		});
	}

	/**
	 * Set `attributes` to all images in the actual selection.
	 * @param {Array.<Object.<string, string>>} attributes
	 * @param {Document} doc
	 * @memberOf images
	 */
	function setAttributes(attributes, doc) {
		var boundaries = Boundaries.get(doc);
		if (!boundaries) {
			return;
		}
		setAttributesFromRange(
			Boundaries.range(boundaries[0], boundaries[1]),
			attributes
		);
	}

	return {
		insert: insert,
		insertFromRange: insertFromRange,
		setAttributesFromRange: setAttributesFromRange,
		setAttributes: setAttributes
	};
});

/**
 * markers.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace markers
 */
define('markers',[
	'dom',
	'misc',
	'mutation',
	'arrays',
	'strings',
	'ranges',
	'paths',
	'boundaries'
], function (
	Dom,
	Misc,
	Mutation,
	Arrays,
	Strings,
	Ranges,
	Paths,
	Boundaries
) {
	

	var augmentedMarks = {
		'TEXT_LEFT'      : '▓[',
		'TEXT_RIGHT'     : ']▓',
		'ELEMENT_LEFT'   : '▓{',
		'ELEMENT_RIGHT'  : '}▓',
		'TEXT_SINGLE'    : '▓',
		'ELEMENT_SINGLE' : '█'
	};

	var marks = {
		'TEXT_LEFT'      : '[',
		'TEXT_RIGHT'     : ']',
		'ELEMENT_LEFT'   : '{',
		'ELEMENT_RIGHT'  : '}',
		'TEXT_SINGLE'    : '¦',
		'ELEMENT_SINGLE' : '|'
	};

	/**
	 * Insert boundary markers at the given boundaries.
	 *
	 * @param  {!Boundary} start
	 * @param  {!Boundary} end
	 * @param  {boolean=}  augment
	 * @return {Array.<Boundary>}
	 * @memberOf markers
	 */
	function insert(start, end, augment) {
		var markers = augment ? augmentedMarks : marks;
		var startContainer = Boundaries.container(start);
		var endContainer = Boundaries.container(end);
		var doc = startContainer.ownerDocument;
		var startMarker = doc.createTextNode(Dom.isTextNode(endContainer)
		                ? markers['TEXT_RIGHT']
		                : markers['ELEMENT_RIGHT']);
		var endMarker = doc.createTextNode(Dom.isTextNode(startContainer)
		              ? markers['TEXT_LEFT']
		              : markers['ELEMENT_LEFT']);
		var range = Boundaries.range(start, end);
		start = Mutation.splitBoundary(Boundaries.fromRangeStart(range), [range]);
		end = Mutation.splitBoundary(Boundaries.fromRangeEnd(range));
		Dom.insert(startMarker, Boundaries.nextNode(end), Boundaries.isAtEnd(end));
		Dom.insert(endMarker, Boundaries.nextNode(start), Boundaries.isAtEnd(start));
		return [start, end];
	}

	/**
	 * Insert a single boundary marker at the given boundary.
	 *
	 * @param  {!Boundary} boundary
	 * @param  {boolean=}  augment
	 * @return {Boundary}
	 */
	function insertSingle(boundary, augment) {
		var markers = augment ? augmentedMarks : marks;
		var container = Boundaries.container(boundary);
		var marker = container.ownerDocument.createTextNode(
			Boundaries.isTextBoundary(boundary)
				? markers['TEXT_SINGLE']
				: markers['ELEMENT_SINGLE']
		);
		boundary = Mutation.splitBoundary(boundary);
		Dom.insert(marker, Boundaries.nextNode(boundary), Boundaries.isAtEnd(boundary));
		return boundary;
	}

	/**
	 * Set the selection based on selection markers found in the content inside
	 * of `rootElem`.
	 *
	 * @param  {Element} rootElem
	 * @return {Array.<Boundary>}
	 * @memberOf markers
	 */
	function extract(rootElem) {
		var markers = ['[', '{', '}', ']'];
		var markersFound = 0;
		var boundaries = [];
		function setBoundaryPoint(marker, node) {
			var whichBoundary;
			if (0 === markersFound) {
				whichBoundary = 0;
				if (marker !== '[' && marker !== '{') {
					throw 'end marker before start marker';
				}
			} else if (1 === markersFound) {
				whichBoundary = 1;
				if (marker !== ']' && marker !== '}') {
					throw 'start marker before end marker';
				}
			} else {
				throw 'Too many markers';
			}
			markersFound += 1;
			if (marker === '[' || marker === ']') {
				var previousSibling = node.previousSibling;
				if (!previousSibling || !Dom.isTextNode(previousSibling)) {
					previousSibling = node.ownerDocument.createTextNode('');
					node.parentNode.insertBefore(previousSibling, node);
				}
				boundaries[whichBoundary] = [previousSibling, previousSibling.length];
				// Because we have set a text offset.
				return false;
			}
			boundaries[whichBoundary] = [node.parentNode, Dom.nodeIndex(node)];
			// Because we have set a non-text offset.
			return true;
		}
		function extractMarkers(node) {
			if (!Dom.isTextNode(node)) {
				return;
			}
			var text = node.nodeValue;
			var parts = Strings.splitIncl(text, /[\[\{\}\]]/g);
			// Because modifying every text node when there can be only two
			// markers seems like too much overhead
			if (!Arrays.contains(markers, parts[0]) && parts.length < 2) {
				return;
			}
			// Because non-text boundary positions must not be joined again
			var forceNextSplit = false;
			parts.forEach(function (part, i) {
				// Because we don't want to join text nodes we haven't split
				forceNextSplit = forceNextSplit || (i === 0);
				if (Arrays.contains(markers, part)) {
					forceNextSplit = setBoundaryPoint(part, node);
				} else if (!forceNextSplit
						&& node.previousSibling
							&& Dom.isTextNode(node.previousSibling)) {
					node.previousSibling.insertData(
						node.previousSibling.length,
						part
					);
				} else {
					node.parentNode.insertBefore(
						node.ownerDocument.createTextNode(part),
						node
					);
				}
			});
			node.parentNode.removeChild(node);
		}
		Dom.walkRec(rootElem, extractMarkers);
		if (2 !== markersFound) {
			throw 'Missing one or both markers';
		}
		return boundaries;
	}

	/**
	 * Returns a string with boundary markers inserted into the representation
	 * of the DOM to indicate the span of the given range.
	 *
	 * @private
	 * @param  {!Boundary} start
	 * @param  {Boundary=} end
	 * @param  {augment=}  augment
	 * @return {string}
	 */
	function show(start, end, augment) {
		var single = !end;

		end = end || start;

		var cac = Boundaries.commonContainer(start, end);

		var doc = Dom.Nodes.DOCUMENT === cac.nodeType
		        ? cac
		        : cac.ownerDocument;

		var startPath = Paths.fromBoundary(cac, start);
		var endPath = Paths.fromBoundary(cac, end);
		var clone;
		var root;

		if (cac.parentNode) {
			root = Paths.fromBoundary(
				cac.parentNode,
				Boundaries.fromFrontOfNode(cac)
			);
			clone = Boundaries.container(
				Paths.toBoundary(cac.parentNode.cloneNode(true), root)
			);
		} else {
			clone = cac.cloneNode(true);
			var one = doc.createDocumentFragment();
			var two = doc.createDocumentFragment();
			Dom.append(clone, two);
			Dom.append(two, one);
			root = [];
		}

		startPath = root.concat(startPath);
		endPath = root.concat(endPath);

		if (single) {
			insertSingle(Paths.toBoundary(clone, startPath), augment);
		} else {
			insert(
				Paths.toBoundary(clone, startPath),
				Paths.toBoundary(clone, endPath),
				augment
			);
		}

		if (Dom.Nodes.DOCUMENT_FRAGMENT !== clone.nodeType) {
			return clone.outerHTML;
		}

		var node = doc.createElement('div');
		Dom.append(clone, node);
		return node.innerHTML;
	}

	function rawBoundariesFromRange(range) {
		return [
			Boundaries.raw(range.startContainer, range.startOffset),
			Boundaries.raw(range.endContainer, range.endOffset)
		];
	}

	/**
	 * Returns string representation of the given boundary boundaries tuple or
	 * range.
	 *
	 * If the option argument augment is set to true, the markers will be
	 * rendered with an extra character along side it to make it easier to see
	 * it in the output.
	 *
	 * @param  {!Boundary|Array.<Boundary>|Range} selection
	 * @param  {boolean=}                         augment
	 * @return {string}
	 * @memberOf markers
	 */
	function hint(selection, augment) {
		if (Misc.defined(selection.length)) {
			return ('string' === typeof selection[0].nodeName)
			     ? show(selection, selection, augment)
			     : show(selection[0], selection[1], augment);
		}
		var boundaries = rawBoundariesFromRange(selection);
		return show(boundaries[0], boundaries[1], augment);
	}

	return {
		hint    : hint,
		insert  : insert,
		extract : extract
	};
});

/**
 * metaview.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @reference: cssminimizer.com
 * @namespace metaview
 */
define('metaview',['dom'], function (Dom) {
	

	var CSS

		// outlines
		= '.✪{outline:5px solid #fce05e;}'
		+ '.✪ td,.✪ th,.✪ b,.✪ i,.✪ u,.✪ p,.✪ ul,.✪ ol,.✪ li,.✪ h1,.✪ h2,.✪ h3,.✪ h4,.✪ h5,.✪ h6,.✪ div,.✪ span{border:1px solid rgba(0,0,0,0.1)}'
		+ '.✪ p,.✪ ul,.✪ ol,.✪ h1,.✪ h2,.✪ h3,.✪ h4,.✪ h5,.✪ h6,.✪ div{border-width:2px}'
		+ '.✪ b{border-color:#f47d43}'
		+ '.✪ i{border-color:#82b5e0}'
		+ '.✪ u{border-color:#bb94b7}'
		+ '.✪ span{border-color:#bb94b7}'
		+ '.✪ code{border-color:#999}'
		+ '.✪ pre{border-color:#999}'
		+ '.✪ ul,.✪ ol{border-color:#91c9cf}'
		+ '.✪ p{border-color:#bdd74b}'
		+ '.✪ h1,.✪ h2,.✪ h3,.✪ h4,.✪ h5,.✪ h6{border-color:#f47d43}'

		// tagnames
		+ '.✪✪ td,.✪✪ th,'
		+ '.✪✪ b,.✪✪ i,.✪✪ u,.✪✪ span,.✪✪ pre,.✪✪ code,'
		+ '.✪✪ ul,.✪✪ ol,.✪✪ li,'
		+ '.✪✪ h1,.✪✪ h2,.✪✪ h3,.✪✪ h4,.✪✪ h5,.✪✪ h6,'
		+ '.✪✪ p,.✪✪ div{position:relative}'
		+ ''
		+ '.✪✪ td::before,.✪✪ th::before,'
		+ '.✪✪ b::before,.✪✪ i::before,.✪✪ u::before,.✪✪ p::before,'
		+ '.✪✪ ul::before,.✪✪ ol::before,.✪✪ li::before,'
		+ '.✪✪ h1::before,.✪✪ h2::before,.✪✪ h3::before,.✪✪ h4::before,.✪✪ h5::before,.✪✪ h6::before,'
		+ '.✪✪ div::before,.✪✪ span::before,.✪✪ pre::before,'
		+ '.✪✪ code::before{position:absolute;top:-2px;left:-2px;line-height:8px;font-size:8px;font-weight:bold;font-style:normal;letter-spacing:0.5px;background:#fff;color:#111;opacity:0.5;}'
		+ '.✪✪ td::before{content:"TD"}'
		+ '.✪✪ th::before{content:"TH"}'
		+ '.✪✪ b::before{content:"B"}'
		+ '.✪✪ i::before{content:"I"}'
		+ '.✪✪ u::before{content:"U"}'
		+ '.✪✪ p::before{content:"P"}'
		+ '.✪✪ ul::before{content:"UL"}'
		+ '.✪✪ ol::before{content:"OL"}'
		+ '.✪✪ li::before{content:"LI"}'
		+ '.✪✪ h1::before{content:"H1"}'
		+ '.✪✪ h2::before{content:"H2"}'
		+ '.✪✪ h3::before{content:"H3"}'
		+ '.✪✪ h4::before{content:"H4"}'
		+ '.✪✪ h5::before{content:"H5"}'
		+ '.✪✪ h6::before{content:"H6"}'
		+ '.✪✪ div::before{content:"DIV"}'
		+ '.✪✪ pre::before{content:"PRE"}'
		+ '.✪✪ span::before{content:"SPAN"}'
		+ '.✪✪ code::before{content:"CODE"}'

		// padding
		+ '.✪✪✪{padding:10px}'
		+ '.✪✪✪ td,.✪✪✪ th,.✪✪✪ b,.✪✪✪ i,.✪✪✪ u,.✪✪✪ p,.✪✪✪ /*xul,.✪✪✪ ol,.✪✪✪*/ li,.✪✪✪ h1,.✪✪✪ h2,.✪✪✪ h3,.✪✪✪ h4,.✪✪✪ h5,.✪✪✪ h6,.✪✪✪ div,.✪✪✪ span{padding:2px 4px;margin:2px;}';

	/**
	 * Insertes the necessary styles into the given document head.
	 *
	 * @private
	 * @param {!Document} doc
	 */
	function insertStyle(doc) {
		var metaview = doc.createElement('style');
		Dom.setAttr(metaview, 'id', 'metaview');
		Dom.append(metaview, doc['head']);
		Dom.append(doc.createTextNode(CSS), metaview);
	}

	var OUTLINE_CLASS = '✪';
	var TAGNAME_CLASS = '✪✪';
	var PADDING_CLASS = '✪✪✪';

	/**
	 * Toggles metaview mode.
	 *
	 * @usage:
	 * aloha.metaview.toggle(editable, {
	 *		outline: true,
	 *		tagname: true,
	 *		padding: true
	 * });
	 *
	 * @param {!Element} editable
	 * @param {Object=}  opts
	 * @memberOf metaview
	 */
	function toggle(editable, opts) {
		if (!editable.ownerDocument.querySelector('style#metaview')) {
			insertStyle(editable.ownerDocument);
		}
		opts = opts || {};
		if (opts['outline']) {
			Dom.addClass(editable, OUTLINE_CLASS);
		} else {
			Dom.removeClass(editable, OUTLINE_CLASS);
		}
		if (opts['tagname']) {
			Dom.addClass(editable, TAGNAME_CLASS);
		} else {
			Dom.removeClass(editable, TAGNAME_CLASS);
		}
		if (opts['padding']) {
			Dom.addClass(editable, PADDING_CLASS);
		} else {
			Dom.removeClass(editable, PADDING_CLASS);
		}
	}

	return { toggle: toggle };
});

/**
 * mobile.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * @namespace mobile
 */
define('mobile',[
	'dom',
	'html',
	'paths',
	'arrays',
	'strings',
	'boundaries',
	'selections'
], function (
	Dom,
	Html,
	Paths,
	Arrays,
	Strings,
	Boundaries,
	Selections
) {
	

	var VOID_MARKER = '♞';
	var LEAF = 1 << 0;
	var TEXT = 1 << 1;
	var VOID = 1 << 2;
	var META = 1 << 3;

	function getBit(node) {
		if (Dom.isTextNode(node)) {
			return TEXT | LEAF;
		}
		if (Html.isVoidNode(node)) {
			return VOID | LEAF;
		}
		return META;
	}

	/**
	 * Clips the sub section of the given path that is common with `root`.
	 *
	 * @private
	 * @param  {!Path} root
	 * @param  {!Path} path
	 * @return {Path}
	 */
	function clipCommonRoot(root, path) {
		for (var i = 0; i < root.length; i++) {
			if (path[i] !== root[i]) {
				return [];
			}
		}
		return path.slice(i);
	}

	function getPaths(node, boundaries) {
		var paths = [];
		var body = node.ownerDocument.body;
		var origin = Paths.fromBoundary(body, Boundaries.fromFrontOfNode(node));
		boundaries.forEach(function (boundary) {
			paths.push(clipCommonRoot(
				origin,
				Paths.fromBoundary(body, boundary)
			));
		});
		return paths;
	}

	function parseDom(element, offset, paths) {
		offset = offset || 0;
		var ranges = [];
		var offsets = [];
		var snippets = [];
		var wasText = false;
		Dom.children(element).forEach(function (node, index) {
			var bit = getBit(node);
			var length = 0;
			var trails = paths.filter(function (path) { return path[0] === index; })
			                  .map(function (path) { return path.slice(1); });

			if (bit & TEXT) {
				if (wasText) {
					ranges.push([offset, offset, 'split']);
				}
				wasText = true;
				snippets.push(node.data);
				length = node.data.length;
				offsets = offsets.concat(trails.map(function (trail) { return offset + (trail[0] || 0); }));
			} else if (bit & VOID) {
				wasText = false;
				snippets.push(VOID_MARKER);
				length = 1;
				offsets = offsets.concat(trails.map(function (trail) { return offset + (trail[0] || 0); }));
			} else {
				wasText = false;
				var more = parseDom(node, offset, trails);
				ranges = ranges.concat(
					[[offset, offset + more.content.length, node]],
					more.ranges
				);
				snippets.push(more.content);
				length += more.content.length;
				offsets = offsets.concat(more.offsets);
			}
			offset += length;
		});
		return {
			content   : snippets.join(''),
			offsets   : offsets,
			ranges    : ranges,
			collapsed : []
		};
	}

	// `remove

	function remove(deranged, start, end) {
		function reduceRanges(list, item) {
			var a = item[0];
			var b = item[1];
			if (!a || !b || (a >= start && b <= end)) {
				return list.concat([[]]);
			}
			if (start < a) {
				a = a - (Math.min(a, end) - start);
			}
			if (start < b) {
				b = b - (Math.min(b, end) - start);
			}
			return list.concat([[a, b].concat(item.slice(2))]);
		}
		function reduceOffset(list, offset) {
			if (offset >= start && offset <= end) {
				return list;
			}
			if (start < offset) {
				offset = offset - (Math.min(offset, end) - start);
			}
			return list.concat(offset);
		}
		var content = deranged.content;
		return {
			content : content.substring(0, start) + content.substring(end),
			offsets : deranged.offsets.reduce(reduceOffset, []),
			ranges  : deranged.ranges.reduce(reduceRanges, [])
		};
	}

	// `extractCollapsed

	var zwChars = Strings.ZERO_WIDTH_CHARACTERS.join('');
	var breakingWhiteSpaces = Arrays.difference(
		Strings.WHITE_SPACE_CHARACTERS,
		Strings.NON_BREAKING_SPACE_CHARACTERS
	).join('');

	var NOT_WSP_FROM_START = new RegExp('[^' + breakingWhiteSpaces + zwChars + ']');
	var WSP_FROM_START = new RegExp('[' + breakingWhiteSpaces + ']');

	function extractCollapsed(deranged) {
		var content = deranged.content;
		var collapsed = [];
		var offset = 0;
		var guard = 99;
		var match;
		while (--guard) {
			match = content.search(NOT_WSP_FROM_START);
			// Only whitespaces
			if (-1 === match) {
				collapsed.push([offset, content.substring(0, content)]);
				break;
			}
			// No leading whitespaces
			if (0 === match) {
				match = content.search(WSP_FROM_START);
				// No more white spaces
				if (-1 === match) {
					break;
				}
				offset += match;
				content = content.substring(match);

			// Leading white space found
			// eg: " foo bar"
			// But multiple spaces should be replaced with a single space
			// *except* at the beginning and end of the string
			} else if (0 === offset || match === content.length) {
				collapsed.push([offset, content.substring(0, match)]);
				deranged = remove(deranged, offset, offset + match);
				content = deranged.content;
			} else if (1 === match) {
				offset++;
				content = content.substring(1);
			} else {
				offset++;
				collapsed.push([offset, content.substring(1, match)]);
				deranged = remove(deranged, offset, offset + match - 1);
				content = content.substring(match);
			}
		}
		deranged.collapsed = collapsed;
		return deranged;
	}

	function context(boundaries) {
		var block = Dom.upWhile(
			Boundaries.container(boundaries[0]),
			function (node) {
				return !Html.hasLinebreakingStyle(node)
					&& !Dom.isEditingHost(node);
			}
		);
		return extractCollapsed(
			parseDom(block, 0, getPaths(block, boundaries))
		);
	}

	function capture(event, mobile) {
		var ctx = context(event.selection.boundaries);
		var offset = ctx.offsets[0] || 0;
		mobile.field.focus();
		mobile.field.value = ctx.content;
		mobile.field.selectionEnd = offset;
		mobile.field.selectionStart = offset;
		return {
			'offset'     : offset,
			'field'      : mobile.field,
			'editable'   : event.editable,
			'boundaries' : event.selection.boundaries
		};
	}

	function shift(event, mobile) {
		return mobile;
	}

	return {
		shift   : shift,
		capture : capture,
		context : context
	};
});

/**
 * selection-change.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace selection-change
 */
define('selection-change',[
	'functions',
	'arrays',
	'boundaries',
	'browsers',
	'events'
], function (
	Fn,
	Arrays,
	Boundaries,
	Browsers,
	Events
) {
	

	/**
	 * Sometimes Firefox (tested with version 25.0) changes the selection only
	 * immediatly after a mouseup (both when the listener was registered with
	 * useCapture true and false). This seems to happen only in rare cases. One
	 * way to reproduce it is to have an editable like this (just a plain
	 * editable without Aloha):
	 *
	 * <div contenteditable="true">
	 *  xxxxxxxxxxxxxx<br/>
	 *  xxxxxxxxxxxxxxx<br type="_moz"/>
	 * </div>
	 *
	 * where the second line is written by just holding down the x key, and
	 * releasing it, and typing an individual x at the end of the line, and
	 * setting the selection with a mouse press after and before the last
	 * character. I think it has to do with the last character being an
	 * individual text node.
	 *
	 * The same goes for the keypress event (in both IE and Firefox and possibly
	 * others), except with the keypress event the selection seems to be never
	 * up to date, so we would always have to do it. Handling keypresses is
	 * useful to get a selection update when the user auto-repeats text-input by
	 * holding down a key. It's not a big deal however if on each keypress event
	 * the user gets the selection change caused by a previous keypress event,
	 * because the keyup event when the user releases the key will ensure a
	 * correct notification at the end of an auto-repeat sequence.
	 *
	 * Because Firefox sets the new selection immediately after the event
	 * handler returns we can use nextTick() instead of a timeout. This could
	 * lead to the handler passed to watchSelection() being called even after
	 * calling the freeing function returned by watchSelection().
	 *
	 * NB: keeping around events in a timeout in IE9 causes strange behaviour:
	 * the mouseup events are somehow played back again after they happened.
	 */
	function maybeNextTick(event, watchSelection) {
		var type = event.type;
		// Because the only browser where can confirm the problem is Firefox,
		// and doing it anyway may cause problems on IE.
		if (Browsers.mozilla && 'mouseup' === type) {
			Events.nextTick(Fn.partial(watchSelection, event));
		}
	}

	/**
	 * Creates a handler that can be used to listen to selection change events,
	 * and which will call the given handler function when the selection
	 * changes.
	 *
	 * See watchSelection().
	 *
	 * @param {function():Array.<Boundary>} getBoundaries
	 * @param {Array.<Boundary>} boundaries current selection
	 * @param fn {!function(Array.<Boundary>, Event)}
	 *        A handler function that will be called with the changed
	 *        selection, and the event that caused the selection change.
	 * @memberOf selection-change
	 */
	function handler(getBoundaries, boundaries, fn) {
		return function watchSelection(event) {
			var newBoundaries = getBoundaries();
			if (newBoundaries && !Arrays.equal(boundaries, newBoundaries, Boundaries.equals)) {
				boundaries = newBoundaries;
				fn(newBoundaries, event);
			} else {
				maybeNextTick(event, watchSelection);
			}
		};
	}

	/**
	 * Adds a handler function to events that may cause a selection-change.
	 *
	 * Our strategy
	 *
	 * Use the selectionchange event (see below) when available (Chrome, IE) and
	 * additionally hook into keyup, keypress, mouseup, touchend events (other
	 * browsers). We need keyup events even in IE to detect selection changes
	 * caused by text input. Keypress events are necessary to capture selection
	 * changes when the user is auto-repeating text-input by holding down a key,
	 * except in Chrome it's not necessary because there the selectionchange
	 * event fires on text-input (See maybeNextTick() for more information
	 * regarding auto-repeating text-input). Hooking into all events on all
	 * browsers does no harm. Touchend is probably necessary for mobile support
	 * other than webkit, although I only tested it on webkit, where it is not
	 * necessary due to selectionchange support.
	 *
	 * For programmatic selection changes we recommend programmatically firing
	 * the selectionchange event on the document element (IE7 needs the document
	 * element, but for IE9+, Chrome and Firefox triggering it on an element
	 * works too).
	 *
	 * We set useCapture to true, so that a stopPropagation call in the bubbling
	 * phase will not starve our handlers. In IE < 9 someone may still do it
	 * since useCapture is not supported.
	 *
	 * Behaviour of the 'selectionchange' event:
	 * * will be fired on every selection change, including when the user
	 *   selects something by pressing and holding the mouse button and
	 *   dragging the selection,
	 * * will not be fired when the user enters text e.g. in a content
	 *   editable in IE9 and IE10,
	 * * will be fired when the selection is set programatically in Chrome,
	 *   but not in IE9 and IE10,
	 * * works in IE as far back as IE7 and Chrome but doesn't work in Firefox or Opera.
	 * * can be feature detected with ('onselectionchange' in document).
	 *
	 * @param {!Document} doc
	 * @param {!function(Array.<Boundary>, Event)} watchSelection
	 *        A handler function like the one returned from handler().
	 * @param {boolean=} mousemove
	 *        Even with all the events above hooked, we only get up-to-date
	 *        selection change updates when the user presses the mouse and drags
	 *        the selection in Chrome and IE, but not in Firefox (and probably
	 *        others). This case can be covered by handling the mousemove event.
	 *        We don't do it by default because handling the mousemove event
	 *        could have different implications from handling up/down/press
	 *        events.
	 *
	 * @memberOf selection-change
	 */
	function addHandler(doc, watchSelection, mousemove) {
		// Chrome, IE (except IE text input)
		Events.add(doc, 'selectionchange', watchSelection, true);
		// IE and others
		Events.add(doc, 'keyup', watchSelection, true);
		// Others
		Events.add(doc, 'mouseup', watchSelection, true);
		Events.add(doc, 'touchend', watchSelection, true);
		Events.add(doc, 'keypress', watchSelection, true);
		// Because we know Chrome and IE behave acceptably we only do it for
		// Firefox and others.
		if (!Browsers.webkit && !Browsers.msie && mousemove) {
			Events.add(doc, 'mousemove', watchSelection, true);
		}
	}

	/**
	 * Removes a handler add with addHandler().
	 *
	 * All arguments including mousemove must be the same as when the handler
	 * was added.
	 *
	 * Expect the handler to be called even after it was removed.
	 *
	 * @param {!Document}                          doc
	 * @param {!function(Array.<Boundary>, Event)} watchSelection
	 * @param {boolean=}                           mousemove
	 * @memberOf selection-change
	 */
	function removeHandler(doc, watchSelection, mousemove) {
		Events.remove(doc, 'selectionchange', watchSelection, true);
		Events.remove(doc, 'keyup', watchSelection, true);
		Events.remove(doc, 'mouseup', watchSelection, true);
		Events.remove(doc, 'touchend', watchSelection, true);
		Events.remove(doc, 'keypress', watchSelection, true);
		if (!Browsers.webkit && !Browsers.msie && mousemove) {
			Events.remove(doc, 'mousemove', watchSelection, true);
		}
	}

	return {
		handler       : handler,
		addHandler    : addHandler,
		removeHandler : removeHandler
	};
});

/**
 * typing.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace typing
 */
define('typing',[
	'dom',
	'keys',
	'html',
	'undo',
	'lists',
	'events',
	'arrays',
	'editing',
	'strings',
	'metaview',
	'mutation',
	'selections',
	'traversing',
	'boundaries',
	'overrides',
	'functions'
], function (
	Dom,
	Keys,
	Html,
	Undo,
	Lists,
	Events,
	Arrays,
	Editing,
	Strings,
	Metaview,
	Mutation,
	Selections,
	Traversing,
	Boundaries,
	Overrides,
	Fn
) {
	

	function undoable(type, event, fn) {
		var range = Boundaries.range(
			event.selection.boundaries[0],
			event.selection.boundaries[1]
		);
		Undo.capture(event.editable.undoContext, {
			meta: {type: type},
			oldRange: range
		}, function () {
			range = fn();
			return {newRange: range};
		});
	}

	/**
	 * Removes unrendered containers from each of the given boundaries while
	 * preserving the correct position of all.
	 *
	 * Returns a new set of boundaries that represent the corrected positions
	 * following node-removal. The order of the returned list corresponds with
	 * the list of boundaries that was given.
	 *
	 * @private
	 * @param  {Array.<Boundary>} boundaries
	 * @return {Array.<Boundary>}
	 */
	function removeUnrenderedContainers(boundaries) {
		function remove(node) {
			boundaries = Mutation.removeNode(node, boundaries);
		}
		function isRendered(node) {
			return Html.isRendered(node) || Dom.isEditingHost(node);
		}
		for (var i = 0; i < boundaries.length; i++) {
			Dom.climbUntil(Boundaries.container(boundaries[i]), remove, isRendered);
		}
		return boundaries;
	}

	function remove(direction, event) {
		var selection = event.selection;
		var start = selection.boundaries[0];
		var end = selection.boundaries[1];
		if (Boundaries.equals(start, end)) {
			if (direction) {
				end = Traversing.next(end);
			} else {
				start = Traversing.prev(start);
			}
		}
		var boundaries = Editing.remove(
			start,
			Traversing.envelopeInvisibleCharacters(end)
		);
		selection.formatting = Overrides.joinToSet(
			selection.formatting,
			Overrides.harvest(Boundaries.container(boundaries[0]))
		);
		boundaries = removeUnrenderedContainers(boundaries);
		Html.prop(Boundaries.commonContainer(boundaries[0], boundaries[1]));
		return boundaries;
	}

	function format(style, event) {
		var selection = event.selection;
		var boundaries = selection.boundaries;
		if (!Html.isBoundariesEqual(boundaries[0], boundaries[1])) {
			return Editing.toggle(boundaries[0], boundaries[1], style);
		}
		var override = Overrides.nodeToState[style];
		if (!override) {
			return boundaries;
		}
		var overrides = Overrides.joinToSet(
			selection.formatting,
			Overrides.harvest(Boundaries.container(boundaries[0])),
			selection.overrides
		);
		selection.overrides = Overrides.toggle(overrides, override, true);
		return selection.boundaries;
	}

	function breakline(isLinebreak, event) {
		if (!isLinebreak) {
			event.selection.formatting = Overrides.joinToSet(
				event.selection.formatting,
				Overrides.harvest(Boundaries.container(event.selection.boundaries[0]))
			);
		}
		var breaker = (event.meta.indexOf('shift') > -1)
		            ? 'BR'
		            : event.editable.settings.defaultBlock;
		return Editing.breakline(event.selection.boundaries[1], breaker);
	}

	/**
	 * Checks whether the given normalized boundary is immediately behind of a
	 * whitespace character.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {boolean}
	 */
	function isBehindWhitespace(boundary) {
		var text = Boundaries.container(boundary).data;
		var offset = Boundaries.offset(boundary);
		return Strings.WHITE_SPACE.test(text.substr(offset - 1, 1));
	}

	/**
	 * Checks whether the given normalized boundary is immediately in front of a
	 * whitespace character.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {boolean}
	 */
	function isInfrontWhitespace(boundary) {
		var text = Boundaries.container(boundary).data;
		var offset = Boundaries.offset(boundary);
		return Strings.WHITE_SPACE.test(text.substr(offset, 1));
	}

	/**
	 * Given a normalized boundary, determines the appropriate white-space that
	 * should be inserted at the given normalized boundary position.
	 *
	 * Strategy:
	 *
	 * A non-breaking-white-space (0x00a0) is required if none of the two
	 * conditions below is met:
	 *
	 * Condition 1.
	 *
	 * The boundary is inside a text node with non-space characters adjacent on
	 * either side. This is often the case when seperating words with a space.
	 *
	 *
	 * Condition 2.
	 *
	 * From the boundary it is possible to locate a preceeding text node (using
	 * pre-order-backtracing traversal Dom.backwardPreorderBacktracingUntil)
	 * whose last character is a non-space character. This text node must be
	 * located before encountering a linebreaking element.
	 *
	 * ... and ...
	 *
	 * From the boundary it is possible to locate a preceeding text node (using
	 * pre-order-backtracing traversal. Dom.forwardPreorderBacktracingUntil)
	 * whose first character is a non-space character. This text node must be
	 * located before encountering a linebreaking element.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 * @return {string}
	 */
	function appropriateWhitespace(boundary) {
		if (Boundaries.isTextBoundary(boundary)) {
			return (isBehindWhitespace(boundary) || isInfrontWhitespace(boundary))
			     ? '\xa0'
			     : ' ';
		}
		var node = Boundaries.container(boundary);
		var stop = Dom.backwardPreorderBacktraceUntil(node, function (node) {
			return Dom.isTextNode(node)
			    || Dom.isEditingHost(node)
			    || Html.hasLinebreakingStyle(node);
		});
		if (Dom.isElementNode(stop)) {
			return '\xa0';
		}
		if (isBehindWhitespace(Boundaries.fromEndOfNode(stop))) {
			return '\xa0';
		}
		stop = Dom.forwardPreorderBacktraceUntil(node, function (node) {
			return Dom.isTextNode(node)
			    || Dom.isEditingHost(node)
			    || Html.hasLinebreakingStyle(node);
		});
		if (Dom.isElementNode(stop)) {
			return '\xa0';
		}
		if (isInfrontWhitespace(Boundaries.fromStartOfNode(stop))) {
			return '\xa0';
		}
		return ' ';
	}

	/**
	 * Looks to see if the boundary is immediately in front of a non-breaking
	 * whitespace and replaces it with a regular whitespace.
	 *
	 * It is necessary to do this normalization every time we insert any
	 * character that is not a whitespace character in order to not end up with
	 * a situation where all spaces between words being non-breaking
	 * whitespaces. Such a situation would otherwise arise because, when space
	 * inserting spaces at the end of a block-level element, these spaces need
	 * to be non-breaking whitespace for them to be visible.  But when a
	 * non-space character is inserted, that non-breaking whitespace is no
	 * longer needed and should converted to a collapsable whitespace.
	 *
	 * @private
	 * @param  {!Boundary} boundary
	 */
	function normalizePreceedingWhitespace(boundary) {
		var node, offset;
		if (Boundaries.isTextBoundary(boundary)) {
			node = Boundaries.container(boundary);
			offset = Boundaries.offset(boundary);
		} else {
			node = Boundaries.nodeBefore(boundary);
			if (node && Dom.isTextNode(node)) {
				offset = node.data.length;
			}
		}
		if (!node) {
			return;
		}
		var text = node.data;
		if (text && Strings.NON_BREAKING_SPACE.test(text.substr(offset - 1, 1))) {
			node.data = text.substr(0, offset - 1) + ' ' + text.substr(offset);
		}
	}

	function indent(event) {
		var boundaries = event.selection.boundaries;
		var start = boundaries[0];
		var end = boundaries[1];
		if (Lists.isIndentationRange(start, end)) {
			return Lists.indent(start, end);
		}
		if (!Boundaries.equals(start, end)) {
			event.selection.boundaries = remove(false, event);
		}
		return insertText(event);
	}

	function insertText(event) {
		var editable = event.editable;
		var selection = event.selection;
		var text = String.fromCharCode(event.keycode);
		var boundary = selection.boundaries[0];
		if ('\t' === text) {
			text = '\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0';
		} else if (' ' === text) {
			var whiteSpaceStyle = Dom.getComputedStyle(
				Dom.upWhile(Boundaries.container(boundary), Dom.isTextNode),
				'white-space'
			);
			if (!Html.isWhiteSpacePreserveStyle(whiteSpaceStyle)) {
				text = appropriateWhitespace(boundary);
			}
		} else {
			normalizePreceedingWhitespace(boundary);
		}
		boundary = Overrides.consume(boundary, Overrides.joinToSet(
			selection.formatting,
			selection.overrides
		));
		selection.overrides = [];
		selection.formatting = [];
		var range = Boundaries.range(boundary, boundary);
		var insertPath = Undo.pathFromBoundary(editable.elem, boundary);
		var insertContent = [editable.elem.ownerDocument.createTextNode(text)];
		var change = Undo.makeInsertChange(insertPath, insertContent);
		Undo.capture(editable.undoContext, {noObserve: true}, function () {
			Mutation.insertTextAtBoundary(text, boundary, true, [range]);
			return {changes: [change]};
		});
		return Boundaries.fromRange(range);
	}

	function toggleUndo(op, event) {
		var range = Boundaries.range(
			event.selection.boundaries[0],
			event.selection.boundaries[1]
		);
		op(event.editable.undoContext, range, [range]);
		return Boundaries.fromRange(range);
	}

	function selectEditable(event) {
		var editable = Dom.editingHost(Boundaries.commonContainer(
			event.selection.boundaries[0],
			event.selection.boundaries[1]
		));
		return !editable ? event.selection.boundaries : [
			Boundaries.create(editable, 0),
			Boundaries.fromEndOfNode(editable)
		];
	}

	/**
	 * Whether or not the given event represents a text input.
	 *
	 * @see
	 * https://lists.webkit.org/pipermail/webkit-dev/2007-December/002992.html
	 *
	 * @private
	 * @param  {AlohaEvent} event
	 * @return {boolean}
	 */
	function isTextInput(event) {
		return 'keypress' === event.type
		    && 'alt' !== event.meta
			&& 'ctrl' !== event.meta
		    && !Strings.isControlCharacter(String.fromCharCode(event.keycode));
	}

	var deleteBackward = {
		clearOverrides : true,
		preventDefault : true,
		undo           : 'delete',
		mutate         : Fn.partial(remove, false)
	};

	var deleteForward = {
		clearOverrides : true,
		preventDefault : true,
		undo           : 'delete',
		mutate         : Fn.partial(remove, true)
	};

	var breakBlock = {
		removeContent  : true,
		preventDefault : true,
		undo           : 'enter',
		mutate         : Fn.partial(breakline, false)
	};

	var breakLine = {
		removeContent  : true,
		preventDefault : true,
		undo           : 'enter',
		mutate         : Fn.partial(breakline, true)
	};

	var formatBold = {
		preventDefault : true,
		undo           : 'bold',
		mutate         : Fn.partial(format, 'B')
	};

	var formatItalic = {
		preventDefault : true,
		undo           : 'italic',
		mutate         : Fn.partial(format, 'I')
	};

	var formatUnderline = {
		preventDefault : true,
		undo           : 'underline',
		mutate         : Fn.partial(format, 'U')
	};

	var inputText = {
		removeContent  : true,
		preventDefault : true,
		undo           : 'typing',
		mutate         : insertText
	};

	var indentContent = {
		preventDefault : true,
		undo           : 'indent',
		mutate         : indent
	};

	var selectAll = {
		preventDefault : true,
		clearOverrides : true,
		mutate         : selectEditable
	};

	var undo = {
		clearOverrides : true,
		preventDefault : true,
		mutate         : Fn.partial(toggleUndo, Undo.undo)
	};

	var redo = {
		preventDefault : true,
		clearOverrides : true,
		mutate         : Fn.partial(toggleUndo, Undo.redo)
	};

	/**
	 * This variable is missing documentation.
	 * @TODO Complete documentation.
	 *
	 * @memberOf typing
	 */
	var actions = {
		'breakBlock'     : breakBlock,
		'breakLine'      : breakLine,
		'deleteBackward' : deleteBackward,
		'deleteForward'  : deleteForward,
		'formatBold'     : formatBold,
		'formatItalic'   : formatItalic,
		'inputText'      : inputText,
		'redo'           : redo,
		'undo'           : undo
	};

	var handlers = {
		'keydown'  : {},
		'keypress' : {},
		'keyup'    : {}
	};

	handlers['keydown']['up'] =
	handlers['keydown']['down'] =
	handlers['keydown']['left'] =
	handlers['keydown']['right'] = {clearOverrides: true};

	handlers['keydown']['delete'] = deleteForward;
	handlers['keydown']['backspace'] = deleteBackward;
	handlers['keydown']['enter'] = breakBlock;
	handlers['keydown']['shift+enter'] = breakLine;
	handlers['keydown']['ctrl+b'] =
	handlers['keydown']['meta+b'] = formatBold;
	handlers['keydown']['ctrl+i'] =
	handlers['keydown']['meta+i'] = formatItalic;
	handlers['keydown']['ctrl+u'] =
	handlers['keydown']['meta+u'] = formatUnderline;
	handlers['keydown']['ctrl+a'] =
	handlers['keydown']['meta+a'] = selectAll;
	handlers['keydown']['ctrl+z'] =
	handlers['keydown']['meta+z'] = undo;
	handlers['keydown']['ctrl+shift+z'] =
	handlers['keydown']['meta+shift+z'] = redo;
	handlers['keydown']['tab'] = indentContent;

	handlers['keypress']['input'] = inputText;

	handlers['keydown']['ctrl+0'] = {mutate : function toggleUndo(event) {
		if (event.editable) {
			Metaview.toggle(event.editable.elem);
		}
		return event.selection.boundaries;
	}};

	handlers['keydown']['ctrl+1'] = {mutate : function toggleUndo(event) {
		if (event.editable) {
			Metaview.toggle(event.editable.elem, {
				'outline': true,
				'tagname': true
			});
		}
		return event.selection.boundaries;
	}};

	handlers['keydown']['ctrl+2'] = {mutate : function toggleUndo(event) {
		if (event.editable) {
			Metaview.toggle(event.editable.elem, {
				'outline': true,
				'tagname': true,
				'padding': true
			});
		}
		return event.selection.boundaries;
	}};

	function handler(event) {
		return Keys.shortcutHandler(event.meta, event.keycode, handlers[event.type] || [])
		    || (isTextInput(event) && handlers['keypress']['input']);
	}

	/**
	 * Updates selection and nativeEvent
	 * @memberOf typing
	 */
	function middleware(event) {
		var selection = event.selection;
		var start = selection.boundaries[0];
		var end = selection.boundaries[1];
		var handling = handler(event);
		if (!handling) {
			return event;
		}
		if (handling.preventDefault) {
			Events.preventDefault(event.nativeEvent);
		}
		if (handling.clearOverrides) {
			selection.overrides = [];
			selection.formatting = [];
		}
		if (handling.mutate) {
			if (handling.undo) {
				undoable(handling.undo, event, function () {
					if (handling.removeContent && !Boundaries.equals(start, end)) {
						selection.boundaries = remove(false, event);
					}
					selection.boundaries = handling.mutate(event);
					Html.prop(Boundaries.commonContainer(
						selection.boundaries[0],
						selection.boundaries[1]
					));
				});
			} else {
				selection.boundaries = handling.mutate(event);
			}
		}
		return event;
	}

	return {
		middleware : middleware,
		actions    : actions
	};
});

/**
 * ui.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 * @namespace ui
 */
define('ui',[
	'dom',
	'maps',
	'html',
	'arrays',
	'editing',
	'editables',
	'overrides',
	'boundaries',
	'selections',
	'functions'
], function (
	Dom,
	Maps,
	Html,
	Arrays,
	Editing,
	Editables,
	Overrides,
	Boundaries,
	Selections,
	Fn
) {
	

	/**
	 * For the given command, determines if the command state is true for the
	 * specified node.
	 *
	 * @private
	 * @param  {Node}    node
	 * @param  {Command} command
	 * @return {boolean}
	 */
	function commandState(node, command) {
		if (command.state) {
			return command.state(node, command);
		}
		if (!(command.node || command.style || command.classes)) {
			return false;
		}
		if (command.node && node.nodeName !== command.node.toUpperCase()) {
			return false;
		}
		var result;
		if (command.style) {
			result = 1;
			Maps.forEach(command.style, function (value, prop) {
				result &= (value === Dom.getStyle(node, prop));
			});
			if (!result) {
				return false;
			}
		}
		if (command.classes) {
			result = 1;
			Maps.forEach(command.classes, function (value, prop) {
				result &= Dom.hasClass(node, prop);
			});
			if (!result) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines the states of the commands for the given event.
	 *
	 * @param  {Object.<string, command>} commands
	 * @param  {Event}                    event
	 * @return {Object.<string, boolean>}
	 * @memberOf ui
	 */
	function states(commands, event) {
		var values = {};
		var selection = event.selection;
		var container = Boundaries.container(
			selection.boundaries['start' === selection.focus ? 0 : 1]
		);
		var overrides = Overrides.map(Overrides.joinToSet(
			selection.formatting,
			Overrides.harvest(container),
			selection.overrides
		));
		var nodes = Dom.childAndParentsUntil(
			container,
			Dom.isEditingHost
		).filter(Dom.isElementNode);
		Maps.forEach(commands, function (command, key) {
			if (command.override && command.override in overrides) {
				values[key] = !!overrides[command.override];
			} else {
				var value = 0;
				nodes.forEach(function (node) {
					value |= commandState(node, command);
				});
				values[key] = !!value;
			}
		});
		return values;
	}

	/**
	 * Applies the given formatting command for blocks inside the given
	 * boundaries.
	 *
	 * @private
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {Selection}        selection
	 * @param  {Command}          command
	 * @return {Array.<Boundary>}
	 */
	function formatBlock(boundaries, selection, command) {
		var style = command.style;
		var formatting = command.node.toUpperCase();
		boundaries = Editing.format(boundaries[0], boundaries[1], formatting);
		if (!style) {
			return boundaries;
		}
		Dom.nodesAndSiblingsBetween(
			Boundaries.container(boundaries[0]),
			Boundaries.container(boundaries[1])
		).forEach(function (node) {
			if (formatting === node.nodeName) {
				Maps.forEach(style, function (value, prop) {
					Dom.setStyle(node, prop, value);
				});
			}
		});
		return boundaries;
	}

	/**
	 * Applies the given formatting command for inline elements inside the given
	 * boundaries.
	 *
	 * @private
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {Selection}        selection
	 * @param  {Command}          command
	 * @return {Array.<Boundary>}
	 */
	function formatInline(boundaries, selection, command) {
		var formatting = command.node.toUpperCase();
		if (!Boundaries.equals(boundaries[0], boundaries[1])) {
			return Editing.format(boundaries[0], boundaries[1], formatting);
		}
		var override = Overrides.nodeToState[formatting];
		if (!override) {
			return boundaries;
		}
		var overrides = Overrides.joinToSet(
			selection.formatting,
			Overrides.harvest(Boundaries.container(boundaries[0])),
			selection.overrides
		);
		selection.overrides = Overrides.toggle(overrides, override, true);
		return boundaries;
	}

	/**
	 * Removes formatting between the given boundaries according to the given command.
	 *
	 * @private
	 * @param  {Array.<Boundary>} boundaries
	 * @param  {Selection}        selection
	 * @param  {Command}          command
	 * @return {Array.<Boundary>}
	 */
	function removeFormatting(boundaries, selection, command) {
		command['nodes'].map(function (node) {
			return node.toUpperCase();
		}).forEach(function (formatting) {
			boundaries = Editing.wrap(
				formatting,
				boundaries[0],
				boundaries[1],
				true
			);
		});
		return boundaries;
	}

	/**
	 * Executes the given command
	 *
	 * @private
	 * @param {Command}          command
	 * @param {Array.<Boundary>} boundaries
	 * @param {Editor}           editor
	 * @param {AlohaEvent}       event
	 */
	function execute(command, boundaries, editor, event) {
		if (command.action) {
			boundaries = command.action(boundaries, editor.selection, command)
			          || boundaries;
		} else {
			var action = Html.isBlockNode({nodeName: command.node.toUpperCase()})
			           ? formatBlock
			           : formatInline;
			boundaries = action(boundaries, editor.selection, command);
		}
		editor.selection = Selections.select(
			editor.selection,
			boundaries[0],
			boundaries[1]
		);
		var editable = Editables.fromBoundary(editor, boundaries[0]);
		if (editable) {
			// TODO: Rely on nodechange event instead
			Fn.comp.apply(editor.stack, editor.stack)({
				preventSelection : false,
				type             : 'nodechange',
				nativeEvent      : event,
				editable         : editable,
				selection        : editor.selection,
				dnd              : editor.dnd
			});
		}
	}

	/**
	 * Binds the given function to only be executed if the current document
	 * selection is inside the given editable(s).
	 *
	 * @param {Editable|Array.<Editable>} editables
	 * @param {function(Boundaries, Editor, AlohaEvent)}
	 * @memberOf ui
	 */
	function bind(editables, fn) {
		var check = 0 === editables.length
		          ? Fn.returnTrue
		          : Fn.partial(Arrays.contains, editables);
		// TODO: I'm sorry. I'll fix this!
		var editor = (editables[0] || window['aloha'])['editor'];
		return function (event) {
			var selection = editor.selection;
			if (selection && selection.boundaries) {
				var boundaries = Boundaries.get(Boundaries.document(selection.boundaries[0]));
				if (boundaries && check(Editables.fromBoundary(editor, boundaries[0]))) {
					fn(boundaries, editor, event);
				}
			}
		};
	}

	/**
	 * Binds the given command.
	 *
	 * @param  {Command}           cmd
	 * @param  {Array.<Editable>=} editables
	 * @return {function(Boundaries, Editor, AlohaEvent)}
	 * @memberOf ui
	 */
	function command(editables, cmd) {
		if (Editables.is(editables)) {
			editables = [editables];
		}
		if (!Arrays.is(editables)) {
			cmd = editables;
			editables = [];
		}
		return bind(editables, Fn.partial(execute, cmd));
	}

	/**
	 * UI Commands.
	 *
	 * <pre>
	 * Commands:
	 *     p
	 *     h1
	 *     h2
	 *     h3
	 *     h4
	 *     ol
	 *     ul
	 *     pre
	 *     bold
	 *     italic
	 *     underline
	 *     unformat
	 * </pre>
	 *
	 * @type {Object}
	 * @memberOf ui
	 */
	var commands = {
		'p'         : { 'node' : 'p'                           },
		'h1'        : { 'node' : 'h1'                          },
		'h2'        : { 'node' : 'h2'                          },
		'h3'        : { 'node' : 'h3'                          },
		'h4'        : { 'node' : 'h4'                          },
		'ol'        : { 'node' : 'ol'                          },
		'ul'        : { 'node' : 'ul'                          },
		'pre'       : { 'node' : 'pre'                         },
		'bold'      : { 'node' : 'b', 'override' : 'bold'      },
		'italic'    : { 'node' : 'i', 'override' : 'italic'    },
		'underline' : { 'node' : 'u', 'override' : 'underline' },

		/**
		 * Unformat command.
		 *
		 * @type {Object}
		 * @memberOf ui.commands
		 */
		'unformat'  : {
			'state'  : Fn.returnFalse,
			'action' : removeFormatting,
			'nodes'  : ['b', 'i', 'u', 'em', 'strong', 'sub', 'sup', 'del', 'small', 'code']
		}
	};

	return {
		bind     : bind,
		states   : states,
		command  : command,
		commands : commands
	};

});

/**
 * api.js is part of Aloha Editor project http://www.alohaeditor.org
 *
 * Aloha Editor ● JavaScript Content Editing Library
 * Copyright (c) 2010-2015 Gentics Software GmbH, Vienna, Austria.
 * Contributors http://www.alohaeditor.org/docs/contributing.html
 *
 * This module exports the Aloha Editor API in a way that will be safe from
 * mungling by the Google Closure Compiler when comipling in advanced
 * compilation mode. It also wraps all API functions in API helper wrapper.
 */
define('api',[
	'arrays',
	'autoformat',
	'blocks',
	'boromir',
	'boundaries',
	'browsers',
	'carets',
	'colors',
	'content',
	'dom',
	'dragdrop',
	'editables',
	'editing',
	'events',
	'functions',
	'html',
	'image',
	'keys',
	'links',
	'lists',
	'maps',
	'markers',
	'metaview',
	'mobile',
	'mouse',
	'overrides',
	'paste',
	'paths',
	'ranges',
	'record',
	'selection-change',
	'selections',
	'searching',
	'strings',
	'transform',
	'traversing',
	'typing',
	'ui',
	'undo',
	'zippers'
], function (
	Arrays,
	AutoFormat,
	Blocks,
	Boromir,
	Boundaries,
	Browsers,
	Carets,
	Colors,
	Content,
	Dom,
	DragDrop,
	Editables,
	Editing,
	Events,
	Fn,
	Html,
	Images,
	Keys,
	Links,
	Lists,
	Maps,
	Markers,
	Metaview,
	Mobile,
	Mouse,
	Overrides,
	Paste,
	Paths,
	Ranges,
	Record,
	SelectionChange,
	Selections,
	Searching,
	Strings,
	Transform,
	Traversing,
	Typing,
	Ui,
	Undo,
	Zippers
) {
	

	var exports = {};

	exports['Boromir'] = Boromir;
	exports['Record'] = Record;

	exports['arrays'] = {};
	exports['arrays']['is']         = Arrays.is;
	exports['arrays']['contains']   = Arrays.contains;
	exports['arrays']['difference'] = Arrays.difference;
	exports['arrays']['equal']      = Arrays.equal;
	exports['arrays']['intersect']  = Arrays.intersect;
	exports['arrays']['is']         = Arrays.is;
	exports['arrays']['last']       = Arrays.last;
	exports['arrays']['coerce']     = Arrays.coerce;
	exports['arrays']['mapcat']     = Arrays.mapcat;
	exports['arrays']['partition']  = Arrays.partition;
	exports['arrays']['some']       = Arrays.some;
	exports['arrays']['someIndex']  = Arrays.someIndex;
	exports['arrays']['split']      = Arrays.split;
	exports['arrays']['unique']     = Arrays.unique;
	exports['arrays']['refill']     = Arrays.refill;

	exports['autoformat'] = {};
	exports['autoformat']['middleware'] = AutoFormat.middleware;

	exports['blocks'] = {};
	exports['blocks']['middleware'] = Blocks.middleware;

	exports['boundaries'] = {};
	exports['boundaries']['is']                  = Boundaries.is;
	exports['boundaries']['get']                 = Boundaries.get;
	exports['boundaries']['select']              = Boundaries.select;
	exports['boundaries']['raw']                 = Boundaries.raw;
	exports['boundaries']['create']              = Boundaries.create;
	exports['boundaries']['normalize']           = Boundaries.normalize;
	exports['boundaries']['equals']              = Boundaries.equals;
	exports['boundaries']['container']           = Boundaries.container;
	exports['boundaries']['offset']              = Boundaries.offset;
	exports['boundaries']['document']            = Boundaries.document;
	exports['boundaries']['fromRange']           = Boundaries.fromRange;
	exports['boundaries']['fromRanges']          = Boundaries.fromRanges;
	exports['boundaries']['fromRangeStart']      = Boundaries.fromRangeStart;
	exports['boundaries']['fromRangeEnd']        = Boundaries.fromRangeEnd;
	exports['boundaries']['fromFrontOfNode']     = Boundaries.fromFrontOfNode;
	exports['boundaries']['fromBehindOfNode']    = Boundaries.fromBehindOfNode;
	exports['boundaries']['fromStartOfNode']     = Boundaries.fromStartOfNode;
	exports['boundaries']['fromEndOfNode']       = Boundaries.fromEndOfNode;
	exports['boundaries']['fromPosition']        = Boundaries.fromPosition;
	exports['boundaries']['setRange']            = Boundaries.setRange;
	exports['boundaries']['setRanges']           = Boundaries.setRanges;
	exports['boundaries']['setRangeStart']       = Boundaries.setRangeStart;
	exports['boundaries']['setRangeEnd']         = Boundaries.setRangeEnd;
	exports['boundaries']['isAtStart']           = Boundaries.isAtStart;
	exports['boundaries']['isAtEnd']             = Boundaries.isAtEnd;
	exports['boundaries']['isTextBoundary']      = Boundaries.isTextBoundary;
	exports['boundaries']['isNodeBoundary']      = Boundaries.isNodeBoundary;
	exports['boundaries']['next']                = Boundaries.next;
	exports['boundaries']['prev']                = Boundaries.prev;
	exports['boundaries']['jumpOver']            = Boundaries.jumpOver;
	exports['boundaries']['nextWhile']           = Boundaries.nextWhile;
	exports['boundaries']['prevWhile']           = Boundaries.prevWhile;
	exports['boundaries']['stepWhile']           = Boundaries.stepWhile;
	exports['boundaries']['walkWhile']           = Boundaries.walkWhile;
	exports['boundaries']['nextNode']            = Boundaries.nextNode;
	exports['boundaries']['prevNode']            = Boundaries.prevNode;
	exports['boundaries']['nodeAfter']           = Boundaries.nodeAfter;
	exports['boundaries']['nodeBefore']          = Boundaries.nodeBefore;
	exports['boundaries']['commonContainer']     = Boundaries.commonContainer;
	exports['boundaries']['range']               = Boundaries.range;

	exports['markers'] = {};
	exports['markers']['hint']    = Markers.hint;
	exports['markers']['insert']  = Markers.insert;
	exports['markers']['extract'] = Markers.extract;

	exports['browsers'] = {};
	exports['browsers']['chrome']        = Browsers.chrome;
	exports['browsers']['webkit']        = Browsers.webkit;
	exports['browsers']['safari']        = Browsers.safari;
	exports['browsers']['vendor']        = Browsers.vendor;
	exports['browsers']['version']       = Browsers.version;
	exports['browsers']['VENDOR_PREFIX'] = Browsers.VENDOR_PREFIX;

	exports['colors'] = {};
	exports['colors']['hex']       = Colors.hex;
	exports['colors']['rgb']       = Colors.rgb;
	exports['colors']['cross']     = Colors.cross;
	exports['colors']['equals']    = Colors.equals;
	exports['colors']['serialize'] = Colors.serialize;

	exports['content'] = {};
	exports['content']['allowsNesting']     = Content.allowsNesting;
	exports['content']['allowedStyles']     = Content.allowedStyles;
	exports['content']['allowedAttributes'] = Content.allowedAttributes;
	exports['content']['disallowedNodes']   = Content.disallowedNodes;
	exports['content']['nodeTranslations']  = Content.nodeTranslations;

	exports['dom'] = {};
	exports['dom']['Nodes']                        = Dom.Nodes;
	exports['dom']['offset']                       = Dom.offset;
	exports['dom']['cloneShallow']                 = Dom.cloneShallow;
	exports['dom']['clone']                        = Dom.clone;
	exports['dom']['text']                         = Dom.text;
	exports['dom']['children']                     = Dom.children;
	exports['dom']['nthChild']                     = Dom.nthChild;
	exports['dom']['numChildren']                  = Dom.numChildren;
	exports['dom']['nodeIndex']                    = Dom.nodeIndex;
	exports['dom']['nodeLength']                   = Dom.nodeLength;
	exports['dom']['hasChildren']                  = Dom.hasChildren;
	exports['dom']['nodeAtOffset']                 = Dom.nodeAtOffset;
	exports['dom']['normalizedNthChild']           = Dom.normalizedNthChild;
	exports['dom']['normalizedNodeIndex']          = Dom.normalizedNodeIndex;
	exports['dom']['realFromNormalizedIndex']      = Dom.realFromNormalizedIndex;
	exports['dom']['normalizedNumChildren']        = Dom.normalizedNumChildren;
	exports['dom']['isNode']                       = Dom.isNode;
	exports['dom']['isTextNode']                   = Dom.isTextNode;
	exports['dom']['isElementNode']                = Dom.isElementNode;
	exports['dom']['isFragmentNode']               = Dom.isFragmentNode;
	exports['dom']['isEmptyTextNode']              = Dom.isEmptyTextNode;
	exports['dom']['isSameNode']                   = Dom.isSameNode;
	exports['dom']['equals']                       = Dom.equals;
	exports['dom']['contains']                     = Dom.contains;
	exports['dom']['followedBy']                   = Dom.followedBy;
	exports['dom']['hasText']                      = Dom.hasText;
	exports['dom']['outerHtml']                    = Dom.outerHtml;
	exports['dom']['append']                       = Dom.append;
	exports['dom']['merge']                        = Dom.merge;
	exports['dom']['moveNextAll']                  = Dom.moveNextAll;
	exports['dom']['moveBefore']                   = Dom.moveBefore;
	exports['dom']['moveAfter']                    = Dom.moveAfter;
	exports['dom']['move']                         = Dom.move;
	exports['dom']['copy']                         = Dom.copy;
	exports['dom']['wrap']                         = Dom.wrap;
	exports['dom']['wrapWith']                     = Dom.wrapWith;
	exports['dom']['insert']                       = Dom.insert;
	exports['dom']['insertAfter']                  = Dom.insertAfter;
	exports['dom']['replace']                      = Dom.replace;
	exports['dom']['replaceShallow']               = Dom.replaceShallow;
	exports['dom']['remove']                       = Dom.remove;
	exports['dom']['removeShallow']                = Dom.removeShallow;
	exports['dom']['removeChildren']               = Dom.removeChildren;
	exports['dom']['addClass']                     = Dom.addClass;
	exports['dom']['removeClass']                  = Dom.removeClass;
	exports['dom']['toggleClass']                  = Dom.toggleClass;
	exports['dom']['hasClass']                     = Dom.hasClass;
	exports['dom']['hasAttrs']                     = Dom.hasAttrs;
	exports['dom']['attrs']                        = Dom.attrs;
	exports['dom']['setAttr']                      = Dom.setAttr;
	exports['dom']['setAttrNS']                    = Dom.setAttrNS;
	exports['dom']['getAttr']                      = Dom.getAttr;
	exports['dom']['getAttrNS']                    = Dom.getAttrNS;
	exports['dom']['removeAttr']                   = Dom.removeAttr;
	exports['dom']['removeAttrNS']                 = Dom.removeAttrNS;
	exports['dom']['removeAttrs']                  = Dom.removeAttrs;
	exports['dom']['removeStyle']                  = Dom.removeStyle;
	exports['dom']['setStyle']                     = Dom.setStyle;
	exports['dom']['getStyle']                     = Dom.getStyle;
	exports['dom']['getComputedStyle']             = Dom.getComputedStyle;
	exports['dom']['getComputedStyles']            = Dom.getComputedStyles;
	exports['dom']['query']                        = Dom.query;
	exports['dom']['nextNonAncestor']              = Dom.nextNonAncestor;
	exports['dom']['nextWhile']                    = Dom.nextWhile;
	exports['dom']['nextUntil']                    = Dom.nextUntil;
	exports['dom']['nextSibling']                  = Dom.nextSibling;
	exports['dom']['nextSiblings']                 = Dom.nextSiblings;
	exports['dom']['prevWhile']                    = Dom.prevWhile;
	exports['dom']['prevUntil']                    = Dom.prevUntil;
	exports['dom']['prevSibling']                  = Dom.prevSibling;
	exports['dom']['prevSiblings']                 = Dom.prevSiblings;
	exports['dom']['nodeAndNextSiblings']          = Dom.nodeAndNextSiblings;
	exports['dom']['nodeAndPrevSiblings']          = Dom.nodeAndPrevSiblings;
	exports['dom']['nodesAndSiblingsBetween']      = Dom.nodesAndSiblingsBetween;
	exports['dom']['walk']                         = Dom.walk;
	exports['dom']['walkRec']                      = Dom.walkRec;
	exports['dom']['walkUntilNode']                = Dom.walkUntilNode;
	exports['dom']['forward']                      = Dom.forward;
	exports['dom']['backward']                     = Dom.backward;
	exports['dom']['findForward']                  = Dom.findForward;
	exports['dom']['findBackward']                 = Dom.findBackward;
	exports['dom']['upWhile']                      = Dom.upWhile;
	exports['dom']['climbUntil']                   = Dom.climbUntil;
	exports['dom']['childAndParentsUntil']         = Dom.childAndParentsUntil;
	exports['dom']['childAndParentsUntilIncl']     = Dom.childAndParentsUntilIncl;
	exports['dom']['childAndParentsUntilNode']     = Dom.childAndParentsUntilNode;
	exports['dom']['childAndParentsUntilInclNode'] = Dom.childAndParentsUntilInclNode;
	exports['dom']['parentsUntil']                 = Dom.parentsUntil;
	exports['dom']['parentsUntilIncl']             = Dom.parentsUntilIncl;
	exports['dom']['serialize']                    = Dom.serialize;
	exports['dom']['enableSelection']              = Dom.enableSelection;
	exports['dom']['disableSelection']             = Dom.disableSelection;
	exports['dom']['isEditable']                   = Dom.isEditable;
	exports['dom']['isEditableNode']               = Dom.isEditableNode;
	exports['dom']['isEditingHost']                = Dom.isEditingHost;
	exports['dom']['isContentEditable']            = Dom.isContentEditable;
	exports['dom']['documentWindow']               = Dom.documentWindow;
	exports['dom']['editingHost']                  = Dom.editingHost;
	exports['dom']['editableParent']               = Dom.editableParent;
	exports['dom']['scrollTop']                    = Dom.scrollTop;
	exports['dom']['scrollLeft']                   = Dom.scrollLeft;
	exports['dom']['absoluteTop']                  = Dom.absoluteTop;
	exports['dom']['absoluteLeft']                 = Dom.absoluteLeft;

	exports['dragdrop'] = {};
	exports['dragdrop']['middleware']  = DragDrop.middleware;
	exports['dragdrop']['Context']     = DragDrop.Context;
	exports['dragdrop']['isDraggable'] = DragDrop.isDraggable;

	exports['editables'] = {};
	exports['editables']['is']           = Editables.is;
	exports['editables']['fromElem']     = Editables.fromElem;
	exports['editables']['fromBoundary'] = Editables.fromBoundary;
	exports['editables']['create']       = Editables.create;
	exports['editables']['destroy']      = Editables.destroy;

	exports['editing'] = {};
	exports['editing']['format']    = Editing.format;
	exports['editing']['unformat']  = Editing.unformat;
	exports['editing']['style']     = Editing.style;
	exports['editing']['remove']    = Editing.remove;
	exports['editing']['breakline'] = Editing.breakline;
	exports['editing']['insert']    = Editing.insert;
	exports['editing']['className'] = Editing.className;
	exports['editing']['attribute'] = Editing.attribute;
	exports['editing']['cut']       = Editing.cut;
	exports['editing']['copy']      = Editing.copy;
	exports['editing']['wrap']      = Editing.wrap;

	exports['events'] = {};
	exports['events']['is']              = Events.is;
	exports['events']['add']             = Events.add;
	exports['events']['remove']          = Events.remove;
	exports['events']['setup']           = Events.setup;
	exports['events']['hasKeyModifier']  = Events.hasKeyModifier;
	exports['events']['dispatch']        = Events.dispatch;
	exports['events']['nextTick']        = Events.nextTick;
	exports['events']['preventDefault']  = Events.preventDefault;
	exports['events']['stopPropagation'] = Events.stopPropagation;
	exports['events']['suppress']        = Events.suppress;

	exports['fn'] = {};
	exports['fn']['identity']     = Fn.identity;
	exports['fn']['noop']         = Fn.noop;
	exports['fn']['returnTrue']   = Fn.returnTrue;
	exports['fn']['returnFalse']  = Fn.returnFalse;
	exports['fn']['complement']   = Fn.complement;
	exports['fn']['partial']      = Fn.partial;
	exports['fn']['strictEquals'] = Fn.strictEquals;
	exports['fn']['comp']         = Fn.comp;
	exports['fn']['and']          = Fn.and;
	exports['fn']['constantly']   = Fn.constantly;
	exports['fn']['is']           = Fn.is;
	exports['fn']['isNou']        = Fn.isNou;
	exports['fn']['or']           = Fn.or;
	exports['fn']['and']          = Fn.and;
	exports['fn']['asMethod']     = Fn.asMethod;
	exports['fn']['extendType']   = Fn.extendType;

	exports['html'] = {};
	exports['html']['parse']                   = Html.parse;
	exports['html']['hasBlockStyle']           = Html.hasBlockStyle;
	exports['html']['hasInlineStyle']          = Html.hasInlineStyle;
	exports['html']['hasLinebreakingStyle']    = Html.hasLinebreakingStyle;
	exports['html']['prop']                    = Html.prop;
	exports['html']['isVoidType']              = Html.isVoidType;
	exports['html']['isRendered']              = Html.isRendered;
	exports['html']['isUnrendered']            = Html.isUnrendered;
	exports['html']['isBlockNode']             = Html.isBlockNode;
	exports['html']['isInlineNode']            = Html.isInlineNode;
	exports['html']['isListContainer']         = Html.isListContainer;
	exports['html']['isTableContainer']        = Html.isTableContainer;
	exports['html']['isGroupContainer']        = Html.isGroupContainer;
	exports['html']['isGroupedElement']        = Html.isGroupedElement;
	exports['html']['isListItem']              = Html.isListItem;
	exports['html']['isHeading']               = Html.isHeading;
	exports['html']['isTextLevelSemanticNode'] = Html.isTextLevelSemanticNode;
	exports['html']['isVoidNode']              = Html.isVoidNode;

	exports['images'] = {};
	exports['images']['insert']        = Images.insert;
	exports['images']['setAttributes'] = Images.setAttributes;

	exports['keys'] = {};
	exports['keys']['middleware']      = Keys.middleware;
	exports['keys']['parseKeys']       = Keys.parseKeys;
	exports['keys']['ARROWS']          = Keys.ARROWS;
	exports['keys']['CODES']           = Keys.CODES;
	exports['keys']['shortcutHandler'] = Keys.shortcutHandler;

	exports['links'] = {};
	exports['links']['create']     = Links.create;
	exports['links']['remove']     = Links.remove;
	exports['links']['middleware'] = Links.middleware;

	exports['lists'] = {};
	exports['lists']['toggle']   = Lists.toggle;
	exports['lists']['format']   = Lists.format;
	exports['lists']['unformat'] = Lists.unformat;

	exports['maps'] = {};
	exports['maps']['isEmpty']     = Maps.isEmpty;
	exports['maps']['fillKeys']    = Maps.fillKeys;
	exports['maps']['keys']        = Maps.keys;
	exports['maps']['vals']        = Maps.vals;
	exports['maps']['selectVals']  = Maps.selectVals;
	exports['maps']['filter']      = Maps.filter;
	exports['maps']['forEach']     = Maps.forEach;
	exports['maps']['extend']      = Maps.extend;
	exports['maps']['merge']       = Maps.merge;
	exports['maps']['isMap']       = Maps.isMap;
	exports['maps']['clone']       = Maps.clone;
	exports['maps']['cloneSet']    = Maps.cloneSet;
	exports['maps']['cloneDelete'] = Maps.cloneDelete;
	exports['maps']['create']      = Maps.create;
	exports['maps']['mapTuples']   = Maps.mapTuples;

	exports['metaview'] = {};
	exports['metaview']['toggle'] = Metaview.toggle;

	exports['mobile'] = {};
	exports['mobile']['shift']   = Mobile.shift;
	exports['mobile']['capture'] = Mobile.capture;
	exports['mobile']['context'] = Mobile.context;

	exports['mouse'] = {};
	exports['mouse']['middleware'] = Mouse.middleware;
	exports['mouse']['EVENTS']     = Mouse.EVENTS;

	exports['overrides'] = {};
	exports['overrides']['map']         = Overrides.map;
	exports['overrides']['indexOf']     = Overrides.indexOf;
	exports['overrides']['unique']      = Overrides.unique;
	exports['overrides']['toggle']      = Overrides.toggle;
	exports['overrides']['harvest']     = Overrides.harvest;
	exports['overrides']['consume']     = Overrides.consume;
	exports['overrides']['nodeToState'] = Overrides.nodeToState;
	exports['overrides']['stateToNode'] = Overrides.stateToNode;
	exports['overrides']['joinToSet']   = Overrides.joinToSet;

	exports['paste'] = {};
	exports['paste']['middleware'] = Paste.middleware;

	exports['paths'] = {};
	exports['paths']['toBoundary']   = Paths.toBoundary;
	exports['paths']['fromBoundary'] = Paths.fromBoundary;

	exports['carets'] = {};
	exports['carets']['box']      = Carets.box;
	exports['carets']['showHint'] = Carets.showHint;
	exports['carets']['hideHint'] = Carets.hideHint;

	exports['selectionchange'] = {};
	exports['selectionchange']['middleware']    = SelectionChange.middleware;
	exports['selectionchange']['addHandler']    = SelectionChange.addHandler;
	exports['selectionchange']['removeHandler'] = SelectionChange.removeHandler;

	exports['selections'] = {};
	exports['selections']['is']               = Selections.is;
	exports['selections']['isSelectionEvent'] = Selections.isSelectionEvent;
	exports['selections']['isRange']          = Selections.isRange;
	exports['selections']['show']             = Selections.show;
	exports['selections']['focus']            = Selections.focus;
	exports['selections']['select']           = Selections.select;
	exports['selections']['update']           = Selections.update;
	exports['selections']['middleware']       = Selections.middleware;
	exports['selections']['Context']          = Selections.Context;
	exports['selections']['selectionEvent']   = Selections.selectionEvent;

	exports['searching'] = {};
	exports['searching']['search']   = Searching.search;
	exports['searching']['forward']  = Searching.forward;
	exports['searching']['backward'] = Searching.backward;

	exports['strings'] = {};
	exports['strings']['addToList']                     = Strings.addToList;
	exports['strings']['removeFromList']                = Strings.removeFromList;
	exports['strings']['uniqueList']                    = Strings.uniqueList;
	exports['strings']['words']                         = Strings.words;
	exports['strings']['splitIncl']                     = Strings.splitIncl;
	exports['strings']['dashesToCamelCase']             = Strings.dashesToCamelCase;
	exports['strings']['camelCaseToDashes']             = Strings.camelCaseToDashes;
	exports['strings']['isEmpty']                       = Strings.isEmpty;
	exports['strings']['isControlCharacter']            = Strings.isControlCharacter;
	exports['strings']['CONTROL_CHARACTER']             = Strings.CONTROL_CHARACTER;
	exports['strings']['SPACE']                         = Strings.SPACE;
	exports['strings']['NOT_SPACE']                     = Strings.NOT_SPACE;
	exports['strings']['WHITE_SPACE']                   = Strings.WHITE_SPACE;
	exports['strings']['WHITE_SPACES']                  = Strings.WHITE_SPACES;
	exports['strings']['ZERO_WIDTH_SPACE']              = Strings.ZERO_WIDTH_SPACE;
	exports['strings']['NON_BREAKING_SPACE']            = Strings.NON_BREAKING_SPACE;
	exports['strings']['WORD_BOUNDARY']                 = Strings.WORD_BOUNDARY;
	exports['strings']['WORD_BOUNDARY_FROM_END']        = Strings.WORD_BOUNDARY_FROM_END;
	exports['strings']['WORD_BREAKING_CHARACTER']       = Strings.WORD_BREAKING_CHARACTER;
	exports['strings']['TERMINAL_WHITE_SPACES']         = Strings.TERMINAL_WHITE_SPACES;
	exports['strings']['ZERO_WIDTH_CHARACTERS']         = Strings.ZERO_WIDTH_CHARACTERS;
	exports['strings']['WHITE_SPACE_CHARACTERS']        = Strings.WHITE_SPACE_CHARACTERS;
	exports['strings']['WORD_BREAKING_CHARACTERS']      = Strings.WORD_BREAKING_CHARACTERS;
	exports['strings']['NON_BREAKING_SPACE_CHARACTERS'] = Strings.NON_BREAKING_SPACE_CHARACTERS;

	exports['transform'] = {};
	exports['transform']['html']   = Transform.html;
	exports['transform']['plain']  = Transform.plain;
	exports['transform']['msword'] = Transform.msword;

	exports['traversing'] = {};
	exports['traversing']['next']              = Traversing.next;
	exports['traversing']['prev']              = Traversing.prev;
	exports['traversing']['expand']            = Traversing.expand;
	exports['traversing']['isAtStart']         = Traversing.isAtStart;
	exports['traversing']['isAtEnd']           = Traversing.isAtEnd;
	exports['traversing']['isBoundariesEqual'] = Traversing.isBoundariesEqual;

	exports['typing'] = {};
	exports['typing']['middleware'] = Typing.middleware;
	exports['typing']['actions']    = Typing.actions;

	exports['ui'] = {};
	exports['ui']['bind']     = Ui.bind;
	exports['ui']['states']   = Ui.states;
	exports['ui']['command']  = Ui.command;
	exports['ui']['commands'] = Ui.commands;

	exports['undo'] = {};
	exports['undo']['Context'] = Undo.Context;
	exports['undo']['enter']   = Undo.enter;
	exports['undo']['close']   = Undo.close;
	exports['undo']['leave']   = Undo.leave;
	exports['undo']['capture'] = Undo.capture;
	exports['undo']['undo']    = Undo.undo;
	exports['undo']['redo']    = Undo.redo;

	exports['zippers'] = {};
	exports['zippers']['go']           = Zippers.go;
	exports['zippers']['dom']          = Zippers.dom;
	exports['zippers']['hint']         = Zippers.hint;
	exports['zippers']['update']       = Zippers.update;
	exports['zippers']['before']       = Zippers.before;
	exports['zippers']['after']        = Zippers.after;
	exports['zippers']['prev']         = Zippers.prev;
	exports['zippers']['next']         = Zippers.next;
	exports['zippers']['up']           = Zippers.up;
	exports['zippers']['down']         = Zippers.down;
	exports['zippers']['root']         = Zippers.root;
	exports['zippers']['peek']         = Zippers.peek;
	exports['zippers']['split']        = Zippers.split;
	exports['zippers']['splice']       = Zippers.splice;
	exports['zippers']['insert']       = Zippers.insert;
	exports['zippers']['replace']      = Zippers.replace;
	exports['zippers']['remove']       = Zippers.remove;
	exports['zippers']['zipper']       = Zippers.zipper;
	exports['zippers']['isAtStart']    = Zippers.isAtStart;
	exports['zippers']['isAtEnd']      = Zippers.isAtEnd;
	exports['zippers']['splitAt']      = Zippers.splitAt;
	exports['zippers']['insertAt']     = Zippers.insertAt;
	exports['zippers']['isMarker']     = Zippers.isMarker;
	exports['zippers']['createMarker'] = Zippers.createMarker;

	/**
	 * wrap an API function to catch exceptions and provide an API link
	 *
	 * @private
	 * @param  {string} pack
	 * @param  {string} func
	 * @return {function}
	 */
	function apiErrorWrapper (pack, func) {
		return function () {
			try {
				return exports[pack][func].apply(this, arguments);
			} catch (e) {
				console.info(apiLink(Arrays.coerce(arguments), pack, func));
				throw e;
			}
		};
	}

	/**
	 * Generates a link to the api from using the list of  arguments passed to
	 * the original function the package name and the function name
	 *
	 * @private
	 * @param  {Array.<*>} args
	 * @param  {string}    pack
	 * @param  {string}    func
	 * @return {string}
	 */
	function apiLink(args, pack, func) {
		return 'See http://www.alohaeditor.org/api/' + pack + '.html'
		     + '?types=' + args.map(typeOf).join('-') + '#' + func;
	}

	/**
	 * Gets the type of the given value.
	 *
	 * @param  {*} obj
	 * @return {string}
	 */
	function typeOf(obj) {
		if (null === obj) {
			return 'null';
		}
		var type = typeof obj;
		if ('object' !== type) {
			// boolean, function, object, string, number, undefined
			return type;
		}
		if (Arrays.is(obj)) {
			return Boundaries.is(obj) ? 'Boundary' : 'Array';
		}
		if (Editables.is(obj)) {
			return 'Editable';
		}
		if (Dom.isElementNode(obj)) {
			return 'Element';
		}
		if (Dom.isNode(obj)) {
			return 'Node';
		}
		if (Selections.is(obj)) {
			return 'Selection';
		}
		if (Ranges.is(obj)) {
			return 'Range';
		}
		if (Events.is(obj)) {
			return 'Event';
		}
		if (Selections.isSelectionEvent(obj)) {
			return 'AlohaEvent';
		}
		if (obj instanceof RegExp) {
			return 'RegExp';
		}
		return type;
	}

	var api = {};
	for (var pack in exports) {
		api[pack] = {};
		if (pack === 'Boromir' || pack === 'Record') {
			api[pack] = exports[pack];
		} else {
			for (var func in exports[pack]) {
				api[pack][func] = Fn.is(exports[pack][func])
					? apiErrorWrapper(pack, func)
					: exports[pack][func];
			}
		}
	}

	return api;
});

/**
 * ┌───────────────────────────────────────────────────────────────┐
 * │ Aloha Editor 2.0.0 ● JavaScript Content Editing Library       │
 * ├───────────────────────────────────────────────────────────────┤
 * │ Copyright © 2010-2015 Gentics Software GmbH, Vienna, Austria. │
 * ├───────────────────────────────────────────────────────────────┤
 * │ alohaeditor.org | github.com/alohaeditor                      │
 * └───────────────────────────────────────────────────────────────┘
 * @preserve
 */
/**
 * Aloha Editor API root.
 * @namespace aloha
 */
define('alohaeditor',[
	'api',
	'dom',
	'links',
	'arrays',
	'blocks',
	'dragdrop',
	'editables',
	'autoformat',
	'events',
	'functions',
	'keys',
	'maps',
	'mouse',
	'paste',
	'typing',
	'selections'
], function (
	Api,
	Dom,
	Links,
	Arrays,
	Blocks,
	DragDrop,
	Editables,
	AutoFormat,
	Events,
	Fn,
	Keys,
	Maps,
	Mouse,
	Paste,
	Typing,
	Selections
) {
	

	/**
	 * Editor function/namespace.
	 *
	 * This is where state is surfaced.
	 *
	 * @param {Event}      nativeEvent
	 * @param {AlohaEvent} event
	 * @memberOf aloha
	 */
	function editor(nativeEvent, event) {
		event = event || Selections.selectionEvent(editor, nativeEvent);
		if (event) {
			event = Fn.comp.apply(editor.stack, editor.stack)(event);
			var selection = Selections.update(event);
			if (selection) {
				editor.selection = Maps.merge(selection);
			}
			if (event.dnd) {
				editor.dnd = Maps.merge(event.dnd);
			}
		}
	}

	editor.dnd       = null;

	/**
	 * Volatile selection context object.
	 *
	 * @type {Selection}
	 * @memberOf aloha
	 */
	editor.selection = null;

	/**
	 * Editables.
	 *
	 * @type {Array.<Editable>}
	 * @memberOf aloha
	 */
	editor.editables = {};
	
	/**
	 * Aloha Editor stack.
	 *
	 * Aloha Editor’s aloha.editor.stack uses the middleware pattern to thread a
	 * browser event through a series of ordered functions called handlers. Each
	 * handler receives an event (a map of properties), operates on it, and
	 * returns an potentially modified version of that object.
	 *
	 * Order matters.
	 *
	 * It is important to remember that when it comes to middleware order
	 * matters. Middlewares that depend on particular properties to be in the
	 * event object, need to be ordered after middleware that provide those
	 * required properties. e.g.: handleKeys must be ordered before
	 * handleTyping.
	 *
	 * Where’s the `next()` argument?
	 *
	 * Unlike other middleware implementations, Aloha Editor’s does not require
	 * calling a next() function to continue the execution of the middlewares
	 * function chain. This is because aloha.editor.middleware, assume
	 * synchronous execution for the purpose of processing editing events.
	 *
	 * @type {Array.<Function(AlohaEvent):AlohaEvent>}
	 * @memberOf aloha
	 */
	editor.stack     = [
		Selections.middleware,
		Links.middleware,
		Typing.middleware,
		AutoFormat.middleware,
		Blocks.middleware,
		DragDrop.middleware,
		Paste.middleware,
		Keys.middleware,
		Mouse.middleware
	];

	function documents(editor) {
		var docs = [];
		for (var expando in editor.editables) {
			docs.push(editor.editables[expando].elem.ownerDocument);
		}
		return docs;
	}

	/**
	 * Transforms the given element into an Aloha editable region.
	 *
	 * @param    {!Element} element
	 * @parma    {Object=}  options
	 * @return   {Editable}
	 * @memberOf aloha
	 */
	function aloha(element, options) {
		var doc = element.ownerDocument;
		if (!Arrays.contains(documents(editor), doc)) {
			Events.setup(doc, editor);
			Events.add(Dom.documentWindow(doc), 'resize', editor);
			editor.selection = Selections.Context(doc);
		}
		var editable = Editables.create(editor, element, options);
		Blocks.initializeBlocks(editable.elem);
		return editable;
	}

	/**
	 * Destroys an editable.
	 *
	 * @param    {!Element} element
	 * @return   {Editable}
	 * @memberOf aloha
	 */
	function mahalo(element) {
		editor.selection.blinking.stop();
		Selections.hideCarets(element.ownerDocument);
		return Editables.destroy(editor, element);
	}

	Api['aloha'] = aloha;
	Api['mahalo'] = mahalo;
	Api['editor'] = editor;
	Api['buildcommit'] = 'https://github.com/alohaeditor/Aloha-Editor/commit/1b1624aaa3fa11ea7ea2684cb6673fa62b9e3ddf';
	window['aloha'] = Maps.extend(aloha, Api);

	var a = 'color: #7ad; background: #f8f6f5; padding: 5px 0;';
	var b = 'color: #aaa; background: #f8f6f5; padding: 5px 0;';
	console.log('%c ✔%c Invoke Aloha by calling: %caloha(document.querySelector(".editable")) ', a, b, a);

	return aloha;
});

    return require('alohaeditor');
}));
