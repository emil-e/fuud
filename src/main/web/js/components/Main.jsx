"use strict";
var React = require("react/addons");
var _ = require("lodash");

module.exports = React.createClass({
  getInitialState: _.constant({
    viewMeta: { title: "" }
  }),

  _onViewMetadata: function(meta) {
    this.setState({ viewMeta: meta });
  },

  render: function() {
    var view = React.Children.only(this.props.children);

    return (
      <div id="main">
        <div className="bar-top">
          <h1 id="title">{this.state.viewMeta.title}</h1>
        </div>

        <div id="main-content">
          {
            React.addons.cloneWithProps(view, {
              onMetadata: this._onViewMetadata
            })
          }
        </div>
      </div>
    );
  }
});
