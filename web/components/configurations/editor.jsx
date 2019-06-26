import React from 'react';
import { connectConfiguration } from './store';

const EditorComponent = ({configuration}) => null;

export const Editor = connectConfiguration(EditorComponent);



