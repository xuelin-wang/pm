import React, { Component } from 'react';
import logo from './logo.svg';
import './graphiql.css';
import ReactDOM from 'react-dom';
import GraphiQL from 'graphiql';
import fetch from 'isomorphic-fetch';

class App extends Component {
graphQLFetcher(graphQLParams) {
  return fetch(window.location.origin + '/graphql', {
    method: 'post',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(graphQLParams),
  }).then(response => response.json());
}

  render() {
    return (
<GraphiQL fetcher={this.graphQLFetcher} />
    );
  }
}

export default App;
