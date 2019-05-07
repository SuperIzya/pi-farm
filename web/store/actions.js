



export const SET_BOARD_VALUE = "Set board value";
export const setBoardValue = (board, temperature, humidity, moisture, state) => ({
  type: SET_BOARD_VALUE,
  board,
  value: { temperature, humidity, moisture, state }
});


export const ADD_LOG = 'Add log';
export const addLogAction = (log) => ({ type: ADD_LOG, log });

export const CLEAR_LOG = 'Clear log';
export const clearLogAction = { type: CLEAR_LOG };

export const SET_MAX_LOGS = "Set max logs";
export const setMaxLogsAction = maxLogs => ({ type: SET_MAX_LOGS, maxLogs });

export const TOGGLE_LOG_FILTER = "Toggle log filter";
export const ToggleLogFilterAction = board => ({ type: TOGGLE_LOG_FILTER, board });
