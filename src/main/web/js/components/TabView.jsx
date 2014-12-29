"use strict";
var React = require("react");
var go = require("../go.js");

var TabBar = React.createClass({
  _handleClick: function(id) {
    this.props.onClick(id);
  },
  
  render: function() {
    var self = this;
    var index = 0;
    var buttons = this.props.metadata.map(function(item) {
      var element = (
        <div className="tab-button" onClick={self._handleClick.bind(self, index)}>
          {item.title}
        </div>);
      index++;
      return element;
    });

    return (
      <div className="bar-bottom">
        <div className="tab-bar">
          {buttons}
        </div>
      </div>
    );
  }
});

module.exports = React.createClass({
  getInitialState: function() {
    return { selectedTab: 0 };
  },
  
  _handleTabClick: function(index) {
    this.setState({ selectedTab: index });
  },
  
  render: function() {
    var self = this;
    var activeId = this.props.route.query.tab;
    var activeView;

    var metadata = [];
    var index = 0;
    React.Children.forEach(this.props.children, function(child) {
      if ((self.state.selectedTab === index) || !activeView)
        activeView = child;

      metadata.push({
        title: child.props.tabTitle
      });
      index++;
    });

    return (
      <div className="tab-view">
        <div className="tab-content">{activeView}</div>
        <TabBar metadata={metadata}
                onClick={this._handleTabClick} />
      </div>
    );
  }
});
