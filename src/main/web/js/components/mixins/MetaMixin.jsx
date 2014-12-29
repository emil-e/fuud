"use strict";
var _ = require("lodash");

module.exports = {
  getInitialState: function() {
    return {
      metadata: this.getInitialMetadata ? this.getInitialMetadata() : {}
    };
  },

  componentDidMount: function() {
    this._notifyMetadata();
  },

  setMetadata: function(meta) {
    var self = this;

    this.setState({
      metadata: _.assign({}, this.state.metadata, meta)
    }, this._notifyMetadata);
  },

  _notifyMetadata: function() {
    if (this.props.onMetadata)
      this.props.onMetadata(this.state.metadata);
  }
};
