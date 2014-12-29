"use strict";
var _ = require("lodash");

function parsePath(strPath) {
  return strPath.split("/").filter(function(c) { return c.length > 0; });
}

function parseQueryString(str) {
  if (str[0] === "?")
    str = str.slice(1);

  if (str.length === 0)
    return {};

  return _(str.split("&"))
    .map(function(x) { return x.split("=", 2); })
    .zipObject()
    .valueOf();
}

function Route(path, query, hash, context) {
  this.path = path;
  this.query = query;
  this.hash = hash;
  this.context = context ? context : {
    path: [], query: {}, hash: null
  };
}

Route.from = function(obj) {
  if (obj instanceof Route) {
    return obj;
  } else if (obj instanceof Location) {
    var hash = obj.hash.slice(1);
    return new Route(
      parsePath(obj.pathname),
      parseQueryString(obj.search),
      (hash.length > 0) ? hash : undefined);
  } else {
    throw new Error("Cannot convert " + obj + " to Route");
  }
};

Route.prototype.modify = function(props) {
  var merged = _.assign({}, this, props);
  merged.context = _.assign({}, this.context, props.context);
  return new Route(merged.path,
                  merged.query,
                  merged.hash,
                  merged.context);
};

Route.prototype.dropPath = function(n) {
  if (n === undefined)
    n = this.path.length;

  return this.modify({
    path: this.path.slice(n),
    context: { path: this.context.path.concat(this.path.slice(0, n)) }
  });
};

Route.prototype.dropQueryKeys = function(keys) {
  return this.modify({
    query: _.omit(this.query, keys),
    context: {
      path: _.assign({}, this.context.query, _.pick(this.query, keys))
    }
  });
};

Route.prototype.withQuery = function(query) {
  return this.modify({ query: _.assign({}, this.query, query )});
};

Route.prototype.rewind = function() {
  return new Route(
    this.context.path.concat(this.path),
    _.assign({}, this.context.query, this.query),
    this.context.hash || this.hash);
};

Route.prototype.toString = function() {
  var rew = this.rewind();
  console.log(JSON.stringify(rew));
  var str = "/";
  str += rew.path.join("/");

  var queryString = _.map(rew.query, function(value, key) {
    return key + "=" + value;
  }).join("&");
  if (queryString.length > 0)
    str += "?" + queryString;
  
  if (rew.hash && (rew.hash.length > 0))
    str += "#" + rew.hash;

  return str;
};

module.exports = Route;
