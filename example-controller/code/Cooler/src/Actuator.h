#pragma once

#include <functional>
#include <ArduinoJson.h>

template <typename T, typename State>
class Actuator {
public:
    using SetupFn = std::function<void(const T&)>;
    using SetStateFn = std::function<void(const T&, const State&)>;
    using ToJsonFn = std::function<void(const State&, JsonDocument&)>;

    Actuator(const char* name, const T& actuator, const State& initialState, SetupFn setupFn,
             SetStateFn setStateFn, ToJsonFn toJson)
        : _name(name), _actuator(actuator), _state(initialState), _setupFn(setupFn),
          _setStateFn(setStateFn), _toJson(toJson) {}

    void setup() {
        _setupFn(_actuator);
    }

    void setState(const State& state) {
        _state = state;
        _setStateFn(_actuator, _state);
    }
    

    JsonDocument getState() const {
        JsonDocument value;
        _toJson(_state, value);
        JsonDocument doc;
        doc[_name] = value;
        return doc;
    }

private:
    const char* _name;
    const T& _actuator;
    State _state;
    const SetupFn _setupFn;
    const SetStateFn _setStateFn;
    const ToJsonFn _toJson;
};