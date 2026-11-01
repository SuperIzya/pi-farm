#include <ArduinoJson.h>
#include "fan.h"

Fan::SetupFn setupFan = [](const FanPin& pin) { pinMode(pin.pin, OUTPUT); };
Fan::SetStateFn setFanState = [](const FanPin& pin, const bool& state) { digitalWrite(pin.pin, state ? HIGH : LOW); };


Fan* createFan(const char* name, const FanPin& pin, const bool& initialState) {
  return new Fan(
    name,
    pin, 
    initialState,
    setupFan,
    setFanState
  );
}