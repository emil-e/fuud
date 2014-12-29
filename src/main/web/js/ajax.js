"use strict";
var when = require("when");

function request(method, path) {
  return when.promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();

    xhr.onload = function() { resolve(this.responseText); };
    xhr.onerror = function() { resolve(new Error(this.statusText)); };
    xhr.open(method, path, true);
    xhr.send();
  });
}

exports.get = request.bind(undefined, "get");
