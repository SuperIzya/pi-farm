
const cmd = c => board => `[${board}]cmd: ${c}`;

export const Commands = {
  blink: cmd('blink'),
  toggle: cmd('toggleLed'),
  reset: cmd('reset')
};

