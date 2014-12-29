"use strict";
var React = require("react");
var go = require("../go.js");

module.exports = React.createClass({
  handleClick: function(event) {
    go.to(this.props.href);
    event.preventDefault();
  },

  render: function() {
    return (
      <a {...this.props} onClick={this.handleClick}>
        {this.props.children}
      </a>
    );
  }
});
