"use strict";
var React = require("react");
var Link = require("./Link.jsx");
var backend = require("../backend.js");

var RecipeNode = React.createClass({
  render: function() {
    return (
      <li>
        <Link href={"/recipes/" + this.props.data.id}>
          {this.props.data.title}
        </Link>
      </li>
    );
  }
});

var FolderNode = React.createClass({
  render: function() {
    var folders = this.props.data.subFolders.map(function(folder) {
      return (<FolderNode data={folder} />);
    });
    var recipes = this.props.data.recipes.map(function(recipe) {
      return (<RecipeNode data={recipe} key={recipe.id} />);
    });

    return (
      <li>
        {this.props.name ? this.props.name : "Recipes"}
        <ul>
          {folders.concat(recipes)}
        </ul>
      </li>
    );
  }
});

module.exports = React.createClass({
  getInitialState: function() {
    return {
      subFolders: [],
      recipes: []
    };
  },

  componentDidMount: function() {
    backend.getRecipes().done(function(data) {
      this.setState(data);
    }.bind(this));
  },

  render: function() {
    return (
      <div>
        <h1>Recipes</h1>
        <FolderNode data={this.state} />
      </div>
    );
  }
});
