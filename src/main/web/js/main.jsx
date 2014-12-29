"use strict";
var React = require("react");
var _ = require("lodash");

var go = require("./go.js");
var Router = require("./Router.js");

var RecipeList = require("./components/RecipeList.jsx");
var Recipe = require("./components/Recipe.jsx");
var Main = require("./components/Main.jsx");

var router = Router.from({
  "recipes/": {
    "*id": Router.component(Recipe),
    "": Router.component(RecipeList)
  },

  "": Router.component(RecipeList)
});

go.addListener(function(path) {
  React.render(<Main>{router.route(path)}</Main>, document.body);
});
