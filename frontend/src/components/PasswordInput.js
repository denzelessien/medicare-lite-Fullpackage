import React, { useState } from 'react';
import Icon from './Icons';

/** A password field with a show/hide (eye) button. Drop-in replacement for <input type="password" />. */
export default function PasswordInput({ value, onChange, id, placeholder, autoComplete, required = true, minLength }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="pw-wrap">
      <input
        id={id}
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required={required}
        minLength={minLength}
      />
      <button
        type="button"
        className="pw-toggle"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? 'Hide password' : 'Show password'}
        title={visible ? 'Hide password' : 'Show password'}
      >
        <Icon name={visible ? 'eyeoff' : 'eye'} size={18} />
      </button>
    </div>
  );
}
