"use strict";
var _ = require("lodash");

var callbacks = [];

function locationChanged(loc) {
  callbacks.forEach(function(l) { l(loc); });
}

function notify() {
  locationChanged(window.location);
}

window.onpopstate = function() {
  notify();
};

exports.to = function(obj) {
  history.pushState(undefined, "", obj.toString());
  notify();
};

exports.addListener = function(listener) {
  callbacks.push(listener);
  // Initial update
  _.defer(listener, window.location);
};

exports.removeListener = function(listener) {
  callbacks = _.without(callbacks, listener);
};
