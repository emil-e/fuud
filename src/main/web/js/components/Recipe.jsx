"use strict";
var React = require("react");
var _ = require("lodash");
var backend = require("../backend.js");
var Router = require("../Router.js");

var MetaMixin = require("./mixins/MetaMixin.jsx");
var TabView = require("./TabView.jsx");

var IngredientsList = React.createClass({
  _makeSection: function(ingredients) {
    return (
      <ul>
      {
        ingredients.map(function(spec) {
          return (<li dangerouslySetInnerHTML={{ __html: spec.ingredient }} />);
        })
      }</ul>);
  },

  render: function() {
    var self = this;
    var content;
    if (this.props.sections.length === 1) {
      content = this._makeSection(this.props.sections[0].content);
    } else {
      content = this.props.sections.map(function(section) {
        return [
          <h3>{section.title}</h3>,
          self._makeSection(section.content)
        ];
      });
    }

    return (
      <div className="text recipe-content">
        <h2>Ingredienser</h2>
        {content}
      </div>
    );
  }
});

var InstructionsList = React.createClass({
  _makeSection: function(steps) {
    return (
      <ol>{
        steps.map(function(step) {
          return (<li dangerouslySetInnerHTML={{ __html: step}} />);
        })
      }</ol>
    );
  },

  render: function() {
    var self = this;
    var content;
    if (this.props.sections.length === 1) {
      content = this._makeSection(this.props.sections[0].content);
    } else {
      content = this.props.sections.map(function(section) {
        return [
          <h3>{section.title}</h3>,
          self._makeSection(section.content)
        ];
      });
    }

    return (
      <div className="text recipe-content">
        <h2>Instruktioner</h2>
        {content}
      </div>
    );
  }
});

module.exports = React.createClass({
  mixins: [MetaMixin],

  getInitialMetadata: _.constant({
    title: "Recipe"
  }),

  getInitialState: _.constant({
    title: "",
    description: "",
    ingredients: [],
    instructions: []
  }),

  componentDidMount: function() {
    backend.getRecipe(this.props.id).done(function(recipe) {
      this.setState(recipe);
      this.setMetadata({ title: recipe.title });
    }.bind(this));
  },

  render: function() {
    return (
      <TabView route={this.props.route}>
        <IngredientsList tabTitle="Ingredienser"
                         sections={this.state.ingredients} />
        <InstructionsList tabTitle="Instruktioner"
                          sections={this.state.instructions} />
      </TabView>
    );
  }
});
