import React from 'react';
import { Subject } from 'rxjs';

export const DndContext = React.createContext();

class ContextClass {
  dropped = new Subject();
  dragged = new Subject();
}

const context = new ContextClass();

export const DndProvider = ({children}) => (
  <DndContext.Provider value={context}>
    {children}
  </DndContext.Provider>
);

