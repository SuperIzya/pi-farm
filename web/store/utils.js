
/**
 * A class where you can register reducers.
 * The primary class used to record ALL reducers used by redux.
 * */
class ReducerRegistry {
    constructor() {
        this._emitChange = null;
        this._reducers = {};
    }

    /**
     * Create a shallow copy of all current reducers and return it.
     * */
    getReducers() {
        return { ...this._reducers };
    }

    /**
     * Adds a new reducer to the registry, and calls the listener.
     * */
    register(name, initState, reducersObj) {
        const reducer = createReducer(initState, reducersObj);
        this._reducers = { ...this._reducers, [name]: reducer };
        if (this._emitChange)
            this._emitChange(this.getReducers());

    }
    /**
     * listener receives all reducers -- result of this.getReducers()
     * (ie. a reducer is added to the registry)
     * */
    setChangeListener(listener) {
        this._emitChange = listener;
    }
}

const reducerRegistry = new ReducerRegistry();

/**
 * Creates reducer function from reducerObj and initial state
 * The reducer function takes a state and an action, and returns the new state.
 * */
const createReducer = (initialState, reduceObj) => (state, action) => {
    state = state || initialState;
    const reducer = reduceObj[action.type];
    if(reducer)
        return reducer(state, action);
    return state;
};
export {
    reducerRegistry,
    createReducer
};

