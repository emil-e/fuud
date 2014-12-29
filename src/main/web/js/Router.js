"use strict";
var _ = require("lodash");
var React = require("react");

var Route = require("./Route.js");

function Router(exec, description) {
  this.exec = exec;
  this.description = description;
}

Router.prototype.route = function(path, params) {
  return this.exec(params || {}, Route.from(path));
};

Router.const = function(value) {
  return new Router(_.constant(value), { value: value });
};

Router.fail = function() {
  return Router.const(null);
};

Router.any = function(targets) {
  if (targets.length === 0)
    return Router.const(null);
  else if (targets.length === 1)
    return targets[0];

  return new Router(
    function(params, route) {
      var result = null;
      _.forEach(targets, function(target, i) {
        result = target.exec(params, route);
        return !result;
      });

      return result;
    },
    { oneOf: _.pluck(targets, "description") });
};

Router.end = function(target) {
  return new Router(
    function(params, route) {
      return (route.path.length === 0) ? target.exec(params, route) : null;
    },
    {
      end: true,
      then: target.description
    });
};

Router.captureRest = function(target, key) {
  return new Router(
    function(params, route) {
      var newParams;
      if (key.length === 0) {
        newParams = params;
      } else {
        newParams = _.assign({}, params);
        newParams[key] = route.path.join("/");
      }
      return target.exec(newParams, route.dropPath());
    },
    {
      rest: key,
      then: target.description
    });
};

Router.capture = function(target, key) {
  return new Router(
    function(params, route) {
      if (route.path.length === 0) {
        return null;
      } else {
        var newParams;
        if (key.length === 0) {
          newParams = params;
        } else {
          newParams = _.assign({}, params);
          newParams[key] = route.path[0];
        }
        return target.exec(newParams, route.dropPath(1));
      }
    },
    {
      capture: key,
      then: target.description
    });
};

Router.exact = function(target, match) {
  return new Router(
    function(params, route) {
      if (match === route.path[0])
        return target.exec(params, route.dropPath(1));
      else
        return null;
    },
    {
      match: match,
      then: target.description
    });
};

Router.mappings = function(pairs) {
  var targets = [];
  var pathTargets = [];

  // Separate end-of-path-matches
  pairs.forEach(function(pair) {
    if (pair[0].length === 0)
      targets.push(Router.end(pair[1]));
    else
      pathTargets.push(pair);
  });

  _(pathTargets).groupBy(function(pathTarget) {
    return pathTarget[0][0];
  }).mapValues(function(pathTargets) {
    return pathTargets.map(function(pathTarget) {
      return [pathTarget[0].slice(1, pathTarget[0].length), pathTarget[1]];
    });
  }).pairs().forEach(function(groupPathTargets) {
    var group = groupPathTargets[0];
    var pathTargets = groupPathTargets[1];
    if (group === "") {
      targets.push(Router.any(_.pluck(pathTargets, "1")));
    } else {
      var target = Router.mappings(pathTargets);
      if (group[0] === "*")
        targets.push(Router.captureRest(target, group.slice(1, group.length)));
      else if (group[0] === ":")
        targets.push(Router.capture(target, group.slice(1, group.length)));
      else
        targets.unshift(Router.exact(target, group));
    }
  });

  return Router.any(targets);
};

Router.object = function(obj) {
  var pairs = _(obj).pairs().map(function(pair) {
    var pattern = pair[0].split("/");
    return [(pattern[0] === "") ? pattern.slice(1, pattern.length) : pattern,
            pair[1]];
  }).valueOf();

  return Router.mappings(pairs);
};

Router.component = function(component, childRouter) {
  childRouter = Router.from(childRouter);
  return new Router(
    function(params, route) {
      var child = childRouter.exec({}, route);
      return React.createElement(component,
                                 _.assign({}, params, {
                                   route: route
                                 }),
                                 child);
    },
    {
      component: component.toString(),
      then: childRouter.description
    });
};

Router.from = function(obj) {
  switch (typeof obj) {
  case "function":
    return new Router(obj, "<custom>");
  case "object":
    if (obj instanceof Router)
      return obj;
    else if (obj instanceof Array)
      return Router.any(_.map(obj, Router.from));
    else
      return Router.object(_.mapValues(obj, Router.from));
  case "undefined":
    return Router.fail();
  default:
    return Router.const(obj);
  }
};

module.exports = Router;
