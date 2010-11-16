/*!	SWFObject v2.2 <http://code.google.com/p/swfobject/> 
	is released under the MIT License <http://www.opensource.org/licenses/mit-license.php> 
*/

var swfobject = function() {
	
	var UNDEF = "undefined",
		OBJECT = "object",
		SHOCKWAVE_FLASH = "Shockwave Flash",
		SHOCKWAVE_FLASH_AX = "ShockwaveFlash.ShockwaveFlash",
		FLASH_MIME_TYPE = "application/x-shockwave-flash",
		EXPRESS_INSTALL_ID = "SWFObjectExprInst",
		ON_READY_STATE_CHANGE = "onreadystatechange",
		
		win = window,
		doc = document,
		nav = navigator,
		
		plugin = false,
		domLoadFnArr = [main],
		regObjArr = [],
		objIdArr = [],
		listenersArr = [],
		storedAltContent,
		storedAltContentId,
		storedCallbackFn,
		storedCallbackObj,
		isDomLoaded = false,
		isExpressInstallActive = false,
		dynamicStylesheet,
		dynamicStylesheetMedia,
		autoHideShow = true,
	
	/* Centralized function for browser feature detection
		- User agent string detection is only used when no good alternative is possible
		- Is executed directly for optimal performance
	*/	
	ua = function() {
		var w3cdom = typeof doc.getElementById != UNDEF && typeof doc.getElementsByTagName != UNDEF && typeof doc.createElement != UNDEF,
			u = nav.userAgent.toLowerCase(),
			p = nav.platform.toLowerCase(),
			windows = p ? /win/.test(p) : /win/.test(u),
			mac = p ? /mac/.test(p) : /mac/.test(u),
			webkit = /webkit/.test(u) ? parseFloat(u.replace(/^.*webkit\/(\d+(\.\d+)?).*$/, "$1")) : false, // returns either the webkit version or false if not webkit
			ie = !+"\v1", // feature detection based on Andrea Giammarchi's solution: http://webreflection.blogspot.com/2009/01/32-bytes-to-know-if-your-browser-is-ie.html
			playerVersion = [0,0,0],
			d = null;
		if (typeof nav.plugins != UNDEF && typeof nav.plugins[SHOCKWAVE_FLASH] == OBJECT) {
			d = nav.plugins[SHOCKWAVE_FLASH].description;
			if (d && !(typeof nav.mimeTypes != UNDEF && nav.mimeTypes[FLASH_MIME_TYPE] && !nav.mimeTypes[FLASH_MIME_TYPE].enabledPlugin)) { // navigator.mimeTypes["application/x-shockwave-flash"].enabledPlugin indicates whether plug-ins are enabled or disabled in Safari 3+
				plugin = true;
				ie = false; // cascaded feature detection for Internet Explorer
				d = d.replace(/^.*\s+(\S+\s+\S+$)/, "$1");
				playerVersion[0] = parseInt(d.replace(/^(.*)\..*$/, "$1"), 10);
				playerVersion[1] = parseInt(d.replace(/^.*\.(.*)\s.*$/, "$1"), 10);
				playerVersion[2] = /[a-zA-Z]/.test(d) ? parseInt(d.replace(/^.*[a-zA-Z]+(.*)$/, "$1"), 10) : 0;
			}
		}
		else if (typeof win.ActiveXObject != UNDEF) {
			try {
				var a = new ActiveXObject(SHOCKWAVE_FLASH_AX);
				if (a) { // a will return null when ActiveX is disabled
					d = a.GetVariable("$version");
					if (d) {
						ie = true; // cascaded feature detection for Internet Explorer
						d = d.split(" ")[1].split(",");
						playerVersion = [parseInt(d[0], 10), parseInt(d[1], 10), parseInt(d[2], 10)];
					}
				}
			}
			catch(e) {}
		}
		return { w3:w3cdom, pv:playerVersion, wk:webkit, ie:ie, win:windows, mac:mac };
	}(),
	
	/* Cross-browser onDomLoad
		- Will fire an event as soon as the DOM of a web page is loaded
		- Internet Explorer workaround based on Diego Perini's solution: http://javascript.nwbox.com/IEContentLoaded/
		- Regular onload serves as fallback
	*/ 
	onDomLoad = function() {
		if (!ua.w3) { return; }
		if ((typeof doc.readyState != UNDEF && doc.readyState == "complete") || (typeof doc.readyState == UNDEF && (doc.getElementsByTagName("body")[0] || doc.body))) { // function is fired after onload, e.g. when script is inserted dynamically 
			callDomLoadFunctions();
		}
		if (!isDomLoaded) {
			if (typeof doc.addEventListener != UNDEF) {
				doc.addEventListener("DOMContentLoaded", callDomLoadFunctions, false);
			}		
			if (ua.ie && ua.win) {
				doc.attachEvent(ON_READY_STATE_CHANGE, function() {
					if (doc.readyState == "complete") {
						doc.detachEvent(ON_READY_STATE_CHANGE, arguments.callee);
						callDomLoadFunctions();
					}
				});
				if (win == top) { // if not inside an iframe
					(function(){
						if (isDomLoaded) { return; }
						try {
							doc.documentElement.doScroll("left");
						}
						catch(e) {
							setTimeout(arguments.callee, 0);
							return;
						}
						callDomLoadFunctions();
					})();
				}
			}
			if (ua.wk) {
				(function(){
					if (isDomLoaded) { return; }
					if (!/loaded|complete/.test(doc.readyState)) {
						setTimeout(arguments.callee, 0);
						return;
					}
					callDomLoadFunctions();
				})();
			}
			addLoadEvent(callDomLoadFunctions);
		}
	}();
	
	function callDomLoadFunctions() {
		if (isDomLoaded) { return; }
		try { // test if we can really add/remove elements to/from the DOM; we don't want to fire it too early
			var t = doc.getElementsByTagName("body")[0].appendChild(createElement("span"));
			t.parentNode.removeChild(t);
		}
		catch (e) { return; }
		isDomLoaded = true;
		var dl = domLoadFnArr.length;
		for (var i = 0; i < dl; i++) {
			domLoadFnArr[i]();
		}
	}
	
	function addDomLoadEvent(fn) {
		if (isDomLoaded) {
			fn();
		}
		else { 
			domLoadFnArr[domLoadFnArr.length] = fn; // Array.push() is only available in IE5.5+
		}
	}
	
	/* Cross-browser onload
		- Based on James Edwards' solution: http://brothercake.com/site/resources/scripts/onload/
		- Will fire an event as soon as a web page including all of its assets are loaded 
	 */
	function addLoadEvent(fn) {
		if (typeof win.addEventListener != UNDEF) {
			win.addEventListener("load", fn, false);
		}
		else if (typeof doc.addEventListener != UNDEF) {
			doc.addEventListener("load", fn, false);
		}
		else if (typeof win.attachEvent != UNDEF) {
			addListener(win, "onload", fn);
		}
		else if (typeof win.onload == "function") {
			var fnOld = win.onload;
			win.onload = function() {
				fnOld();
				fn();
			};
		}
		else {
			win.onload = fn;
		}
	}
	
	/* Main function
		- Will preferably execute onDomLoad, otherwise onload (as a fallback)
	*/
	function main() { 
		if (plugin) {
			testPlayerVersion();
		}
		else {
			matchVersions();
		}
	}
	
	/* Detect the Flash Player version for non-Internet Explorer browsers
		- Detecting the plug-in version via the object element is more precise than using the plugins collection item's description:
		  a. Both release and build numbers can be detected
		  b. Avoid wrong descriptions by corrupt installers provided by Adobe
		  c. Avoid wrong descriptions by multiple Flash Player entries in the plugin Array, caused by incorrect browser imports
		- Disadvantage of this method is that it depends on the availability of the DOM, while the plugins collection is immediately available
	*/
	function testPlayerVersion() {
		var b = doc.getElementsByTagName("body")[0];
		var o = createElement(OBJECT);
		o.setAttribute("type", FLASH_MIME_TYPE);
		var t = b.appendChild(o);
		if (t) {
			var counter = 0;
			(function(){
				if (typeof t.GetVariable != UNDEF) {
					var d = t.GetVariable("$version");
					if (d) {
						d = d.split(" ")[1].split(",");
						ua.pv = [parseInt(d[0], 10), parseInt(d[1], 10), parseInt(d[2], 10)];
					}
				}
				else if (counter < 10) {
					counter++;
					setTimeout(arguments.callee, 10);
					return;
				}
				b.removeChild(o);
				t = null;
				matchVersions();
			})();
		}
		else {
			matchVersions();
		}
	}
	
	/* Perform Flash Player and SWF version matching; static publishing only
	*/
	function matchVersions() {
		var rl = regObjArr.length;
		if (rl > 0) {
			for (var i = 0; i < rl; i++) { // for each registered object element
				var id = regObjArr[i].id;
				var cb = regObjArr[i].callbackFn;
				var cbObj = {success:false, id:id};
				if (ua.pv[0] > 0) {
					var obj = getElementById(id);
					if (obj) {
						if (hasPlayerVersion(regObjArr[i].swfVersion) && !(ua.wk && ua.wk < 312)) { // Flash Player version >= published SWF version: Houston, we have a match!
							setVisibility(id, true);
							if (cb) {
								cbObj.success = true;
								cbObj.ref = getObjectById(id);
								cb(cbObj);
							}
						}
						else if (regObjArr[i].expressInstall && canExpressInstall()) { // show the Adobe Express Install dialog if set by the web page author and if supported
							var att = {};
							att.data = regObjArr[i].expressInstall;
							att.width = obj.getAttribute("width") || "0";
							att.height = obj.getAttribute("height") || "0";
							if (obj.getAttribute("class")) { att.styleclass = obj.getAttribute("class"); }
							if (obj.getAttribute("align")) { att.align = obj.getAttribute("align"); }
							// parse HTML object param element's name-value pairs
							var par = {};
							var p = obj.getElementsByTagName("param");
							var pl = p.length;
							for (var j = 0; j < pl; j++) {
								if (p[j].getAttribute("name").toLowerCase() != "movie") {
									par[p[j].getAttribute("name")] = p[j].getAttribute("value");
								}
							}
							showExpressInstall(att, par, id, cb);
						}
						else { // Flash Player and SWF version mismatch or an older Webkit engine that ignores the HTML object element's nested param elements: display alternative content instead of SWF
							displayAltContent(obj);
							if (cb) { cb(cbObj); }
						}
					}
				}
				else {	// if no Flash Player is installed or the fp version cannot be detected we let the HTML object element do its job (either show a SWF or alternative content)
					setVisibility(id, true);
					if (cb) {
						var o = getObjectById(id); // test whether there is an HTML object element or not
						if (o && typeof o.SetVariable != UNDEF) { 
							cbObj.success = true;
							cbObj.ref = o;
						}
						cb(cbObj);
					}
				}
			}
		}
	}
	
	function getObjectById(objectIdStr) {
		var r = null;
		var o = getElementById(objectIdStr);
		if (o && o.nodeName == "OBJECT") {
			if (typeof o.SetVariable != UNDEF) {
				r = o;
			}
			else {
				var n = o.getElementsByTagName(OBJECT)[0];
				if (n) {
					r = n;
				}
			}
		}
		return r;
	}
	
	/* Requirements for Adobe Express Install
		- only one instance can be active at a time
		- fp 6.0.65 or higher
		- Win/Mac OS only
		- no Webkit engines older than version 312
	*/
	function canExpressInstall() {
		return !isExpressInstallActive && hasPlayerVersion("6.0.65") && (ua.win || ua.mac) && !(ua.wk && ua.wk < 312);
	}
	
	/* Show the Adobe Express Install dialog
		- Reference: http://www.adobe.com/cfusion/knowledgebase/index.cfm?id=6a253b75
	*/
	function showExpressInstall(att, par, replaceElemIdStr, callbackFn) {
		isExpressInstallActive = true;
		storedCallbackFn = callbackFn || null;
		storedCallbackObj = {success:false, id:replaceElemIdStr};
		var obj = getElementById(replaceElemIdStr);
		if (obj) {
			if (obj.nodeName == "OBJECT") { // static publishing
				storedAltContent = abstractAltContent(obj);
				storedAltContentId = null;
			}
			else { // dynamic publishing
				storedAltContent = obj;
				storedAltContentId = replaceElemIdStr;
			}
			att.id = EXPRESS_INSTALL_ID;
			if (typeof att.width == UNDEF || (!/%$/.test(att.width) && parseInt(att.width, 10) < 310)) { att.width = "310"; }
			if (typeof att.height == UNDEF || (!/%$/.test(att.height) && parseInt(att.height, 10) < 137)) { att.height = "137"; }
			doc.title = doc.title.slice(0, 47) + " - Flash Player Installation";
			var pt = ua.ie && ua.win ? "ActiveX" : "PlugIn",
				fv = "MMredirectURL=" + encodeURI(window.location).toString().replace(/&/g,"%26") + "&MMplayerType=" + pt + "&MMdoctitle=" + doc.title;
			if (typeof par.flashvars != UNDEF) {
				par.flashvars += "&" + fv;
			}
			else {
				par.flashvars = fv;
			}
			// IE only: when a SWF is loading (AND: not available in cache) wait for the readyState of the object element to become 4 before removing it,
			// because you cannot properly cancel a loading SWF file without breaking browser load references, also obj.onreadystatechange doesn't work
			if (ua.ie && ua.win && obj.readyState != 4) {
				var newObj = createElement("div");
				replaceElemIdStr += "SWFObjectNew";
				newObj.setAttribute("id", replaceElemIdStr);
				obj.parentNode.insertBefore(newObj, obj); // insert placeholder div that will be replaced by the object element that loads expressinstall.swf
				obj.style.display = "none";
				(function(){
					if (obj.readyState == 4) {
						obj.parentNode.removeChild(obj);
					}
					else {
						setTimeout(arguments.callee, 10);
					}
				})();
			}
			createSWF(att, par, replaceElemIdStr);
		}
	}
	
	/* Functions to abstract and display alternative content
	*/
	function displayAltContent(obj) {
		if (ua.ie && ua.win && obj.readyState != 4) {
			// IE only: when a SWF is loading (AND: not available in cache) wait for the readyState of the object element to become 4 before removing it,
			// because you cannot properly cancel a loading SWF file without breaking browser load references, also obj.onreadystatechange doesn't work
			var el = createElement("div");
			obj.parentNode.insertBefore(el, obj); // insert placeholder div that will be replaced by the alternative content
			el.parentNode.replaceChild(abstractAltContent(obj), el);
			obj.style.display = "none";
			(function(){
				if (obj.readyState == 4) {
					obj.parentNode.removeChild(obj);
				}
				else {
					setTimeout(arguments.callee, 10);
				}
			})();
		}
		else {
			obj.parentNode.replaceChild(abstractAltContent(obj), obj);
		}
	} 

	function abstractAltContent(obj) {
		var ac = createElement("div");
		if (ua.win && ua.ie) {
			ac.innerHTML = obj.innerHTML;
		}
		else {
			var nestedObj = obj.getElementsByTagName(OBJECT)[0];
			if (nestedObj) {
				var c = nestedObj.childNodes;
				if (c) {
					var cl = c.length;
					for (var i = 0; i < cl; i++) {
						if (!(c[i].nodeType == 1 && c[i].nodeName == "PARAM") && !(c[i].nodeType == 8)) {
							ac.appendChild(c[i].cloneNode(true));
						}
					}
				}
			}
		}
		return ac;
	}
	
	/* Cross-browser dynamic SWF creation
	*/
	function createSWF(attObj, parObj, id) {
		var r, el = getElementById(id);
		if (ua.wk && ua.wk < 312) { return r; }
		if (el) {
			if (typeof attObj.id == UNDEF) { // if no 'id' is defined for the object element, it will inherit the 'id' from the alternative content
				attObj.id = id;
			}
			if (ua.ie && ua.win) { // Internet Explorer + the HTML object element + W3C DOM methods do not combine: fall back to outerHTML
				var att = "";
				for (var i in attObj) {
					if (attObj[i] != Object.prototype[i]) { // filter out prototype additions from other potential libraries
						if (i.toLowerCase() == "data") {
							parObj.movie = attObj[i];
						}
						else if (i.toLowerCase() == "styleclass") { // 'class' is an ECMA4 reserved keyword
							att += ' class="' + attObj[i] + '"';
						}
						else if (i.toLowerCase() != "classid") {
							att += ' ' + i + '="' + attObj[i] + '"';
						}
					}
				}
				var par = "";
				for (var j in parObj) {
					if (parObj[j] != Object.prototype[j]) { // filter out prototype additions from other potential libraries
						par += '<param name="' + j + '" value="' + parObj[j] + '" />';
					}
				}
				el.outerHTML = '<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"' + att + '>' + par + '</object>';
				objIdArr[objIdArr.length] = attObj.id; // stored to fix object 'leaks' on unload (dynamic publishing only)
				r = getElementById(attObj.id);	
			}
			else { // well-behaving browsers
				var o = createElement(OBJECT);
				o.setAttribute("type", FLASH_MIME_TYPE);
				for (var m in attObj) {
					if (attObj[m] != Object.prototype[m]) { // filter out prototype additions from other potential libraries
						if (m.toLowerCase() == "styleclass") { // 'class' is an ECMA4 reserved keyword
							o.setAttribute("class", attObj[m]);
						}
						else if (m.toLowerCase() != "classid") { // filter out IE specific attribute
							o.setAttribute(m, attObj[m]);
						}
					}
				}
				for (var n in parObj) {
					if (parObj[n] != Object.prototype[n] && n.toLowerCase() != "movie") { // filter out prototype additions from other potential libraries and IE specific param element
						createObjParam(o, n, parObj[n]);
					}
				}
				el.parentNode.replaceChild(o, el);
				r = o;
			}
		}
		return r;
	}
	
	function createObjParam(el, pName, pValue) {
		var p = createElement("param");
		p.setAttribute("name", pName);	
		p.setAttribute("value", pValue);
		el.appendChild(p);
	}
	
	/* Cross-browser SWF removal
		- Especially needed to safely and completely remove a SWF in Internet Explorer
	*/
	function removeSWF(id) {
		var obj = getElementById(id);
		if (obj && obj.nodeName == "OBJECT") {
			if (ua.ie && ua.win) {
				obj.style.display = "none";
				(function(){
					if (obj.readyState == 4) {
						removeObjectInIE(id);
					}
					else {
						setTimeout(arguments.callee, 10);
					}
				})();
			}
			else {
				obj.parentNode.removeChild(obj);
			}
		}
	}
	
	function removeObjectInIE(id) {
		var obj = getElementById(id);
		if (obj) {
			for (var i in obj) {
				if (typeof obj[i] == "function") {
					obj[i] = null;
				}
			}
			obj.parentNode.removeChild(obj);
		}
	}
	
	/* Functions to optimize JavaScript compression
	*/
	function getElementById(id) {
		var el = null;
		try {
			el = doc.getElementById(id);
		}
		catch (e) {}
		return el;
	}
	
	function createElement(el) {
		return doc.createElement(el);
	}
	
	/* Updated attachEvent function for Internet Explorer
		- Stores attachEvent information in an Array, so on unload the detachEvent functions can be called to avoid memory leaks
	*/	
	function addListener(target, eventType, fn) {
		target.attachEvent(eventType, fn);
		listenersArr[listenersArr.length] = [target, eventType, fn];
	}
	
	/* Flash Player and SWF content version matching
	*/
	function hasPlayerVersion(rv) {
		var pv = ua.pv, v = rv.split(".");
		v[0] = parseInt(v[0], 10);
		v[1] = parseInt(v[1], 10) || 0; // supports short notation, e.g. "9" instead of "9.0.0"
		v[2] = parseInt(v[2], 10) || 0;
		return (pv[0] > v[0] || (pv[0] == v[0] && pv[1] > v[1]) || (pv[0] == v[0] && pv[1] == v[1] && pv[2] >= v[2])) ? true : false;
	}
	
	/* Cross-browser dynamic CSS creation
		- Based on Bobby van der Sluis' solution: http://www.bobbyvandersluis.com/articles/dynamicCSS.php
	*/	
	function createCSS(sel, decl, media, newStyle) {
		if (ua.ie && ua.mac) { return; }
		var h = doc.getElementsByTagName("head")[0];
		if (!h) { return; } // to also support badly authored HTML pages that lack a head element
		var m = (media && typeof media == "string") ? media : "screen";
		if (newStyle) {
			dynamicStylesheet = null;
			dynamicStylesheetMedia = null;
		}
		if (!dynamicStylesheet || dynamicStylesheetMedia != m) { 
			// create dynamic stylesheet + get a global reference to it
			var s = createElement("style");
			s.setAttribute("type", "text/css");
			s.setAttribute("media", m);
			dynamicStylesheet = h.appendChild(s);
			if (ua.ie && ua.win && typeof doc.styleSheets != UNDEF && doc.styleSheets.length > 0) {
				dynamicStylesheet = doc.styleSheets[doc.styleSheets.length - 1];
			}
			dynamicStylesheetMedia = m;
		}
		// add style rule
		if (ua.ie && ua.win) {
			if (dynamicStylesheet && typeof dynamicStylesheet.addRule == OBJECT) {
				dynamicStylesheet.addRule(sel, decl);
			}
		}
		else {
			if (dynamicStylesheet && typeof doc.createTextNode != UNDEF) {
				dynamicStylesheet.appendChild(doc.createTextNode(sel + " {" + decl + "}"));
			}
		}
	}
	
	function setVisibility(id, isVisible) {
		if (!autoHideShow) { return; }
		var v = isVisible ? "visible" : "hidden";
		if (isDomLoaded && getElementById(id)) {
			getElementById(id).style.visibility = v;
		}
		else {
			createCSS("#" + id, "visibility:" + v);
		}
	}

	/* Filter to avoid XSS attacks
	*/
	function urlEncodeIfNecessary(s) {
		var regex = /[\\\"<>\.;]/;
		var hasBadChars = regex.exec(s) != null;
		return hasBadChars && typeof encodeURIComponent != UNDEF ? encodeURIComponent(s) : s;
	}
	
	/* Release memory to avoid memory leaks caused by closures, fix hanging audio/video threads and force open sockets/NetConnections to disconnect (Internet Explorer only)
	*/
	var cleanup = function() {
		if (ua.ie && ua.win) {
			window.attachEvent("onunload", function() {
				// remove listeners to avoid memory leaks
				var ll = listenersArr.length;
				for (var i = 0; i < ll; i++) {
					listenersArr[i][0].detachEvent(listenersArr[i][1], listenersArr[i][2]);
				}
				// cleanup dynamically embedded objects to fix audio/video threads and force open sockets and NetConnections to disconnect
				var il = objIdArr.length;
				for (var j = 0; j < il; j++) {
					removeSWF(objIdArr[j]);
				}
				// cleanup library's main closures to avoid memory leaks
				for (var k in ua) {
					ua[k] = null;
				}
				ua = null;
				for (var l in swfobject) {
					swfobject[l] = null;
				}
				swfobject = null;
			});
		}
	}();
	
	return {
		/* Public API
			- Reference: http://code.google.com/p/swfobject/wiki/documentation
		*/ 
		registerObject: function(objectIdStr, swfVersionStr, xiSwfUrlStr, callbackFn) {
			if (ua.w3 && objectIdStr && swfVersionStr) {
				var regObj = {};
				regObj.id = objectIdStr;
				regObj.swfVersion = swfVersionStr;
				regObj.expressInstall = xiSwfUrlStr;
				regObj.callbackFn = callbackFn;
				regObjArr[regObjArr.length] = regObj;
				setVisibility(objectIdStr, false);
			}
			else if (callbackFn) {
				callbackFn({success:false, id:objectIdStr});
			}
		},
		
		getObjectById: function(objectIdStr) {
			if (ua.w3) {
				return getObjectById(objectIdStr);
			}
		},
		
		embedSWF: function(swfUrlStr, replaceElemIdStr, widthStr, heightStr, swfVersionStr, xiSwfUrlStr, flashvarsObj, parObj, attObj, callbackFn) {
			var callbackObj = {success:false, id:replaceElemIdStr};
			if (ua.w3 && !(ua.wk && ua.wk < 312) && swfUrlStr && replaceElemIdStr && widthStr && heightStr && swfVersionStr) {
				setVisibility(replaceElemIdStr, false);
				addDomLoadEvent(function() {
					widthStr += ""; // auto-convert to string
					heightStr += "";
					var att = {};
					if (attObj && typeof attObj === OBJECT) {
						for (var i in attObj) { // copy object to avoid the use of references, because web authors often reuse attObj for multiple SWFs
							att[i] = attObj[i];
						}
					}
					att.data = swfUrlStr;
					att.width = widthStr;
					att.height = heightStr;
					var par = {}; 
					if (parObj && typeof parObj === OBJECT) {
						for (var j in parObj) { // copy object to avoid the use of references, because web authors often reuse parObj for multiple SWFs
							par[j] = parObj[j];
						}
					}
					if (flashvarsObj && typeof flashvarsObj === OBJECT) {
						for (var k in flashvarsObj) { // copy object to avoid the use of references, because web authors often reuse flashvarsObj for multiple SWFs
							if (typeof par.flashvars != UNDEF) {
								par.flashvars += "&" + k + "=" + flashvarsObj[k];
							}
							else {
								par.flashvars = k + "=" + flashvarsObj[k];
							}
						}
					}
					if (hasPlayerVersion(swfVersionStr)) { // create SWF
						var obj = createSWF(att, par, replaceElemIdStr);
						if (att.id == replaceElemIdStr) {
							setVisibility(replaceElemIdStr, true);
						}
						callbackObj.success = true;
						callbackObj.ref = obj;
					}
					else if (xiSwfUrlStr && canExpressInstall()) { // show Adobe Express Install
						att.data = xiSwfUrlStr;
						showExpressInstall(att, par, replaceElemIdStr, callbackFn);
						return;
					}
					else { // show alternative content
						setVisibility(replaceElemIdStr, true);
					}
					if (callbackFn) { callbackFn(callbackObj); }
				});
			}
			else if (callbackFn) { callbackFn(callbackObj);	}
		},
		
		switchOffAutoHideShow: function() {
			autoHideShow = false;
		},
		
		ua: ua,
		
		getFlashPlayerVersion: function() {
			return { major:ua.pv[0], minor:ua.pv[1], release:ua.pv[2] };
		},
		
		hasFlashPlayerVersion: hasPlayerVersion,
		
		createSWF: function(attObj, parObj, replaceElemIdStr) {
			if (ua.w3) {
				return createSWF(attObj, parObj, replaceElemIdStr);
			}
			else {
				return undefined;
			}
		},
		
		showExpressInstall: function(att, par, replaceElemIdStr, callbackFn) {
			if (ua.w3 && canExpressInstall()) {
				showExpressInstall(att, par, replaceElemIdStr, callbackFn);
			}
		},
		
		removeSWF: function(objElemIdStr) {
			if (ua.w3) {
				removeSWF(objElemIdStr);
			}
		},
		
		createCSS: function(selStr, declStr, mediaStr, newStyleBoolean) {
			if (ua.w3) {
				createCSS(selStr, declStr, mediaStr, newStyleBoolean);
			}
		},
		
		addDomLoadEvent: addDomLoadEvent,
		
		addLoadEvent: addLoadEvent,
		
		getQueryParamValue: function(param) {
			var q = doc.location.search || doc.location.hash;
			if (q) {
				if (/\?/.test(q)) { q = q.split("?")[1]; } // strip question mark
				if (param == null) {
					return urlEncodeIfNecessary(q);
				}
				var pairs = q.split("&");
				for (var i = 0; i < pairs.length; i++) {
					if (pairs[i].substring(0, pairs[i].indexOf("=")) == param) {
						return urlEncodeIfNecessary(pairs[i].substring((pairs[i].indexOf("=") + 1)));
					}
				}
			}
			return "";
		},
		
		// For internal usage only
		expressInstallCallback: function() {
			if (isExpressInstallActive) {
				var obj = getElementById(EXPRESS_INSTALL_ID);
				if (obj && storedAltContent) {
					obj.parentNode.replaceChild(storedAltContent, obj);
					if (storedAltContentId) {
						setVisibility(storedAltContentId, true);
						if (ua.ie && ua.win) { storedAltContent.style.display = "block"; }
					}
					if (storedCallbackFn) { storedCallbackFn(storedCallbackObj); }
				}
				isExpressInstallActive = false;
			} 
		}
	};
}();
/*
* speechapi - Javascript frontend for use in on-line speech-to-text and text-to-speech.
*
* Copyright (C) 2010 Speechapi - http://www.speechapi.com
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*
*/

var speechapi = {
	username: null,
	password: null,
	recognizerSetup : false,
	documentReady: false,
	onResultCallback: null,
	onTtsCallback: null,
	onLoadedCallback: null,
	containerID: null,
	server: null,
	automatic: false,
	initNotCalled: true,
	ruleIndex : 1,
	grammars : {},

	currentFocus : null,
	beingRead : null,
	beingReadBC : null,
	linkables: null,
	speakables: null,
	focusables: null,
	browserControl: false,
	styleControl: false,
	formsEnabled: true,
	mashupMode: false,

	//------------
	//Low level API
	//------------


	//setup the speechapi (really justs saves some config params)
	setup: function(username, password, result, tts, onLoaded, containerID) {
		speechapi.username = username;
		speechapi.password = password;

		speechapi.recognizerSetup = false;	
		speechapi.documentReady = false;
	
		speechapi.onLoadedCallback = onLoaded;
		speechapi.onResultCallback = result;
		speechapi.initNotCalled=true;

		speechapi.onFinishTTSCallback = tts;
	
		speechapi.containerID = containerID;	
	
		speechapi.automatic=false;
	},

	// Version of the setup method that aslo embeds the flash object (hides all swfobject code from client)
	setup2: function(username, password, result, tts, onLoaded, containerID, altContainerID, server,swffile) {
		var flashvars = {speechServer : server};
        	var params = {allowscriptaccess : "always"};
		var attributes = {};
		attributes.id =containerID; 
		swfobject.embedSWF(swffile, altContainerID, "215", "138", "9.0.28", false,flashvars, params, attributes);
		speechapi.setup(username,password,onResult,onFinishTTS, onLoaded, containerID); 
	},

			

	result: function(result) {

		if (speechapi.mashupMode) {
   			for (var k in result.ruleMatches) {
	   			speechapi.processRule2(result.ruleMatches[k]._rule,result.ruleMatches[k]._tag,result.text); 
   			}
   			speechapi.setupRecognition("JSGF", speechapi.generateGrammar(),false);
		}

        	if (result == null) {
           		result =new Object();
           		result.text = "Recognition Error";
           		result.rCode="Error";
           		result.rMessage="Null result received from server";
        	} 
		//var jsonResult = eval('(' + result + ')');
		if(speechapi.onResultCallback != null)
			speechapi.onResultCallback(result);
	},

	setOnResult: function(obj) {
		if (eval("typeof " + obj + " == 'function'")) {
			speechapi.onResultCallback = obj;
		} else {
			alert('setOnResult needs to have a callback function that exists!');
		}
	},

	finishTTS: function() {
		if (speechapi.mashupMode) {
      			//alert(beingRead);
      			if (speechapi.beingRead != null) {
         			speechapi.beingRead.style.backgroundColor='white';
         			speechapi.beingRead = null;
	 			speechapi.beingReadBC = null;
      			}
		}
		if(speechapi.onFinishTTSCallback != null)
			speechapi.onFinishTTSCallback();
	},

	setOnFinishTTS: function(obj) {
		if (eval("typeof " + obj + " == 'function'")) {
			speechapi.onFinishTTSCallback = obj;
		} else {
			alert('setOnFinishTTS needs to have a callback function that exists!');
		}	
	},


	loaded: function () {
	        document.getElementById(speechapi.containerID).initFS(speechapi.username, speechapi.password, 'speechapi.result', 'speechapi.finishTTS');
		if (speechapi.mashupMode) {
			speechapi.initMashupMode();
		}
		if(speechapi.onLoadedCallback != null)
			speechapi.onLoadedCallback();
	},

	setOnLoaded: function(obj) {
		if (eval("typeof " + obj + " == 'function'")) {
			speechapi.onLoadedCallback = obj;
		} else {
			alert('setOnResult needs to have a callback function that exists!');
		}
	},

	setupRecognition: function(grammarType, grammar, automatic) {

		if (typeof automatic === 'undefined') automatic = speechapi.automatic;
	
		if(typeof grammarType == 'string' && typeof grammar == 'string') {
                	gType = grammarType.toUpperCase();
                	if ((gType == 'SIMPLE') || (gType == 'JSGF')) {
                    		if (grammar.length > 0) {
                        		speechapi.recognizerSetup = true;
		        		document.getElementById(speechapi.containerID).setupRecognition(grammarType,grammar, automatic)
                		} else {
		       			alert('Empty grammar string');
				}
                	} else {
		   		alert('Invalid grammar type '+grammarType);
                	}
		} else {
			alert('Grammar mode and/or string must be a string.');
		}	
	},

	startRecognition: function() {
        	if (speechapi.recognizerSetup) {
  	   		document.getElementById(speechapi.containerID).startRecognition();
        	} else {
			alert('Setup Grammars with setupRecognition() method before calling startRecognition()');
        	}
	},

	stopRecognition: function() {
		document.getElementById(speechapi.containerID).stopRecognition();
	},

	speak: function(text, speaker) {
		document.getElementById(speechapi.containerID).speak(text, speaker);
	},

	//------------
	// Vxml API
	//------------

	startVxmlAppUrl: function(appUrl,callback) {
		document.getElementById(speechapi.containerID).startVxmlAppUrl(appUrl,callback);
	},


	startVxmlAppText: function(app,callback) {
		document.getElementById(speechapi.containerID).startVxmlAppText(app,callback);
	},


	//------------
	// Advanced API
	//------------

	processRule: function(rulename, tag) {
		if (speechapi.grammars[rulename]) {
			speechapi.grammars[rulename].callback(speechapi.grammars[rulename].text, tag);
		} else {
			alert("unhandled rulename/tag "+rulename+"/"+tag);
		}
	},


	resetGrammar: function() {
		speechapi.grammars = {};
	},

	// similar to Array.join() but joins the keys of an
	// associative array instead of the array values
	joinKeys: function(map, separator) {
		var result = "";
		var count = 0;

		for (var x in map) {
			if (count++ > 0)
				result += separator;
			result += x;
		}

		return result;
	},

	constructGrammar: function() {
		var grammar = "#JSGF V1.0;\n";
		grammar += "grammar mashup;\n";

		//grammar += "public <command> = [<pre>] (<";
		grammar += "public <command> =  (<";
		grammar += speechapi.joinKeys(speechapi.grammars, "> | <");
		grammar += ">);\n";

		for (var rulename in speechapi.grammars) {
			grammar += ( "<" + rulename + "> = " + speechapi.grammars[rulename].text + ";\n" );
		}
		//alert(grammar);
		return grammar;
	},

	addJsgfGrammar: function(text, callback) {
		var rulename = 'id' + speechapi.ruleIndex++;
		speechapi.grammars[rulename] = {"text" : text, "callback" : callback};
		return rulename;
	},


	//--------------------
	// Page Scraping API
	//---------------------

   	setupPage: function(username,password,resultCallback,onFinishTTS,onLoaded, containerID,linkables,speakables,focusables,browserControl,formsEnabled) {
		speechapi.mashupMode = true;
   	     	speechapi.linkables = linkables;
   	     	speechapi.speakables = speakables;
 		speechapi.focusables = focusables;
		speechapi.browserControl = browserControl;
		speechapi.formsEnabled = formsEnabled;
		speechapi.setup(username,password,onResult,onFinishTTS, onLoaded, containerID); 
   	},



	initMashupMode: function() {
		speechapi.setupRecognition("JSGF", speechapi.generateGrammar(),false);
		speechapi.makeTextClickable(speakables);
  		speechapi.setupFocus();
	},



	setupFocus: function() {
   		$("input[type=text]").live("focus", function () {
    			if (speechapi.currentFocus)
       				speechapi.currentFocus.style.backgroundColor='white';
    			speechapi.currentFocus = this;
    			this.style.backgroundColor='yellow';
   		});
	},



	makeTextClickable: function(speakables) {
    		for (var i = 0; i <speakables.length; i++) {
        		$(speakables[i]).live("click", function(){
          			speechapi.beingRead = this;
          			speechapi.beingReadBC = speechapi.beingRead.style.backgroundColor;
          			speechapi.beingRead.style.backgroundColor='yellow';
          			speechapi.speak( $(this).text(),'male' );
       			});
   		}
	},


	makeSpeakLinkGrammar: function(links) {
    		var grammarSeg = "<link> = ( ";
    		for (var j = 0; j <links.length; j++) {
      			$(links[j]).each(function (i) {
        			var x = this.getAttribute("name");
				//alert(j+" "+i+ " "+links[j]+" "+x+" " +this.href);
        			if (x != null) {
	    				grammarSeg = grammarSeg +x+" {"+this.href+"}| \n";
	    				this.style.backgroundColor='ffffcc';
            				//this.style.fontWeight = 'bold';
	    				//this.style.fontStyle = 'italic';
        			} else {
	    				grammarSeg = grammarSeg +this.innerHTML+" {"+this.href+"}| \n";
            				//this.style.fontWeight = 'bold';
	    				//this.style.fontStyle = 'italic';
	    				this.style.backgroundColor='ffffcc';
        			}
			});
    		}
    		grammarSeg = grammarSeg+" speech web site  {http://www.speechapi.com}); \n";
    		return grammarSeg;
	},


	makeClickAndReadGrammar: function(readables) {
    		var grammarSeg = "<readthis> = ( ";
    		for (var j = 0; j <readables.length; j++) {
      			$(readables[j]).each(function (i) {
        			var x = this.getAttribute("name");
        			var y = this.getAttribute("id");
				//alert(j+" "+i+ " "+readables[j]+" "+x+" " +y);
				if ((x != null) && (y !=null)) {
            				grammarSeg = grammarSeg +"("+x+") {"+y+"}| ";
            				//this.style.backgroundColor='yellow';
        			}
      			});
    		}
    		grammarSeg = grammarSeg+" dummy{dummy}); \n";
    		return grammarSeg;
	},


	makeChangeFocusGrammar: function(focusables) {
    		var grammarSeg = "<changeFocus> = ( ";
    		for (var j = 0; j <focusables.length; j++) {
      			var selectThis="input[type="+focusables[j]+"]";
      			$(selectThis).each(function (i) {
        			var x = this.getAttribute("name");
        			var y = this.getAttribute("id");
				//alert(j+" "+i+ " "+focusables[j]+" "+x+" " +y);
				if ((x != null) && (y !=null)) {
            				grammarSeg = grammarSeg +"("+x+") {"+y+"}| ";
        			}
      			});
    		}
    		grammarSeg = grammarSeg+" dummy{dummy}); \n";
    		return grammarSeg;
	},

	makeFormEntryGrammar: function() {
   		var grammarSeg = "<formEntry> =  ";
   		if (speechapi.currentFocus != null) {
        		var x = speechapi.currentFocus.getAttribute("gram");
        		var y = speechapi.currentFocus.getAttribute("id");
			//alert(j+" "+i+ " "speechapi.currentFocus.name+" "+x+" " +y);
			if ((x != null) && (y !=null)) {
            			grammarSeg = grammarSeg +"("+x+") {"+y+"}; \n";
			}
   		} else {
      			grammarSeg = grammarSeg +"dummy{dummy};"
   		}
   		return grammarSeg;
	},





	processRule2: function(rule,tag,raw) {
   		//alert(rule+" : "+tag);
   		if (rule == 'link') {
      			location.href=tag;
   		} else if (rule == 'readthis') {
      			//alert(document.getElementById(tag));
      			speechapi.beingRead = document.getElementById(tag);
      			speechapi.beingReadBC = speechapi.beingRead.style.backgroundColor;
      			speechapi.beingRead.style.backgroundColor='yellow';
      			speechapi.speak(speechapi.beingRead.innerHTML,'male' );
   		//} else if (rule == 'whatread') {
			//alert("not impl");   

   		} else if (rule == 'changeFocus') {
      			document.getElementById(tag).focus();

   		} else if (rule == 'formEntry') {
      			if (speechapi.currentFocus)
	  			speechapi.currentFocus.value = raw;

   		} else if (rule == "scroll") {
      			if (tag == "up") {
         			window.scrollBy(0,-100);
      			} else if (tag =="down") {
         			window.scrollBy(0,100);
      			} else if (tag == "top") {
         			window.scrollTo(0,0);
      			} else if (tag == "bottom") {
         			if (document.body.scrollHeight) {
            				window.scrollTo(0, document.body.scrollHeight);
         			} else if (screen.height) {
            			// IE5 window.scrollTo(0, screen.height);
         			}
      			}

   		} else if (rule == "resize") {
      			if (tag  == "bigger") {
         			window.resizeBy(100,100);
      			} else if (tag  == "smaller") {
         			window.resizeBy(-100,-100);
      			} else if (tag == "tofit") {
         			window.sizeToContent();
      			}

   		} else if (rule == 'fontSize') {
      			getStyleClass('preferences').style.fontSize = tag;

   		} else if (rule == 'fontColor') {
      			getStyleClass('preferences').style.color = tag;

    		} else if (rule == 'background') {
      			getStyleClass('preferences').style.background = tag;

   		//} else if (rule == 'options') {
   			//       speakoptions();
   		//}else {
   		}
	},



	getStyleClass: function(className) {
        	for (var s = 0; s < document.styleSheets.length; s++){
                	if(document.styleSheets[s].rules) {
                        	for (var r = 0; r < document.styleSheets[s].rules.length; r++) {
                                	if (document.styleSheets[s].rules[r].selectorText == '.' + className) {
                                        	return document.styleSheets[s].rules[r];
                                	}
                        	}
                	} else if(document.styleSheets[s].cssRules) {
                        	for (var r = 0; r < document.styleSheets[s].cssRules.length; r++) {
                                	if (document.styleSheets[s].cssRules[r].selectorText == '.' + className)
                                        	return document.styleSheets[s].cssRules[r];
                        	}
                	}
        	}
        	return null;
	},




	speakoptions: function() {
      		var speakable = "Focus is not set to a speech enabled element";
      		if (currentFocus) {
         		if (currentFocus.id == 'color') {
             			speakable = "You can say: red, white, blue, green, black, yellow, orange, brown or gold"
         		} else if (currentFocus.id =='percentage') {
             			speakable = "You can say: one, two, three, four, five, six, seven, eight, nine or ten";
         		}  else {
             			speakable = "Focus is not set to a speech enabled element";
         		}
      		}
      		speechapi.speak(speakable,'female');
	},

	generateGrammar: function() {
   		var grammar1;
   		var grammar2 ="\n";
   		var firstFlag = true;
   		grammar1 = "#JSGF V1.0;\n";
   		grammar1 = grammar1 + "grammar speechapi;\n";
   		//grammar1 = grammar1 + "public <command> = [<pre>]";
   		grammar1 = grammar1 + "public <command> = ";

   		//for now no pre grammar (because bug in tags. Dont alwasy get the right tags with optional elements)
   		//grammar2 = grammar2 + "<pre> = (I would like [ to see ] ) | ( [please] get [me] ) | (go to);\n";

   		//$(document).ready(function() {
   		//hyperlinks 
   		if (speechapi.linkables.length>0) {
      			if (firstFlag) {
         			grammar1 = grammar1+"(<link>";
         			firstFlag = false;
      			} else {
         			grammar1 = grammar1+"|<link>";
      			}
      			grammar2 = grammar2 + speechapi.makeSpeakLinkGrammar(speechapi.linkables);
   		}

   		//click and read
   		if (speechapi.speakables.length>0) {
      			if (firstFlag) {
         			grammar1 = grammar1+"(<readthis>";
         			firstFlag = false;
      			} else {
         			grammar1 = grammar1+"|<readthis>";
      			}
      				grammar2 = grammar2 + speechapi.makeClickAndReadGrammar(speechapi.speakables);
   		}

   		//change focus grammar
   		if (speechapi.focusables.length>0) {
      			if (firstFlag) {
         			grammar1 = grammar1+"(<changeFocus>";
         			firstFlag = false;
      			} else {
         			grammar1 = grammar1+"|<changeFocus>";
      			}
      				grammar2 = grammar2 + speechapi.makeChangeFocusGrammar(speechapi.focusables);
   		}

   		//form fill grammar
   		if (speechapi.formsEnabled) {
      			if (firstFlag) {
         			grammar1 = grammar1+"(<formEntry>";
         			firstFlag = false;
      			} else {
         			grammar1 = grammar1+"|<formEntry>";
      			}
      			grammar2 = grammar2 + speechapi.makeFormEntryGrammar();
   		}


   		if (speechapi.browserControl)  {
      			if (firstFlag) {
         			grammar1 = grammar1+"(<scroll>|<resize>";
         			firstFlag = false;
      			} else {
         			grammar1 = grammar1+"|<scroll>|<resize>";
      			}

       			grammar2 = grammar2 + "<scroll> = (<up> {up} |<down> {down} | <top> {top} | <bottom> {bottom});\n";
       			grammar2 = grammar2 + "<up> = [scroll] up;\n";
       			grammar2 = grammar2 + "<down> = [scroll] down;\n";
       			grammar2 = grammar2 + "<top> = [scroll to] top;\n";
       			grammar2 = grammar2 + "<bottom> = [scroll to] bottom;\n";
       			grammar2 = grammar2 + "<resize> = (<bigger> {bigger} |<smaller> {smaller} | <tofit> {tofit});\n";
       			grammar2 = grammar2 + "<bigger> = [size] bigger;\n";
       			grammar2 = grammar2 + "<smaller> = [size] smaller;\n";
       			grammar2 = grammar2 + "<tofit> = [size] to fit;\n";
   		}

   		if (speechapi.styleControl)  {
      			if (firstFlag) {
         			grammar1 = grammar1+"(<fontSize>|<fontColor>|<background>";
         			firstFlag = false;
      			} else {
         			grammar1 = grammar1+"|<fontSize>|<fontColor>|<background>";
      			}

       			grammar2 = grammar2 + "<background> = ( black{black} |blue{blue} |gray{gray}| green{green}| lime{lime} |maroon{maroon}| navy{navy} |olive{olive}| purple{purple}| red{red}| silver{silver} |teal{teal}| white{white}| yellow{yellow}) background;\n";
       			grammar2 = grammar2 + "<fontColor> = (black{black} |blue{blue}|gray{gray}| green{green}| lime{lime} |maroon{maroon}| navy{navy} |olive{olive}| purple{purple}| red{red}| silver{silver} |teal{teal}| white{white}| yellow{yellow}) font;\n";
       			grammar2 = grammar2 + "<fontSize> = (extra extra small{xx-small}|extra small{x-small}|small{small}|medium{medium}|large{large}|extra large{x-large}|extra extra large{xx-large}) font [size];\n";
   		}

    		grammar1 = grammar1+");\n"+grammar2;

   		//grammar = grammar + "<options> = (([what are the]options){options} );\n";

   		//});
   		//alert("GRAMMAR: "+grammar1);
   		return grammar1;
	}

};

