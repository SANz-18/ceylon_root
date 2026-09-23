import React from 'react';

export default function Toast({ msg, kind }) {
  return <div className={`toast ${kind === 'err' ? 'err' : 'ok'}`}>{msg}</div>;
}
