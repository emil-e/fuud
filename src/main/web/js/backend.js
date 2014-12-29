"use strict";
var ajax = require("./ajax.js");

exports.getRecipes = function() {
  return ajax.get("/api/recipes")
      .then(JSON.parse);
};

exports.getRecipe = function(id) {
  return ajax.get("/api/recipes/" + id)
    .then(JSON.parse);
};
