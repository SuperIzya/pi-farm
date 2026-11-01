#pragma once

#include <functional>
#include <ArduinoJson.h>
#include "utils.h"

template <typename T, typename R>
class Sensor {
public:
    using SetupFn = std::function<void(T&)>;
    using ReadDataFn = std::function<R(T&)>;

    Sensor(
        const char* name, 
        T& sensor,
        SetupFn setupFn, 
        ReadDataFn readDataFn
    ): _name(name), 
        _sensor(sensor), 
        _setupFn(setupFn), 
        _readDataFn(readDataFn)
    {}

    void setup() {
        _setupFn(_sensor);
    }

    JsonDocument readData() {
        R result = _readDataFn(_sensor);            
        JsonDocument value;
        toJson(result, value);
        JsonDocument doc;
        doc[_name] = value;
        return doc;
    }

protected:
    const char* _name;
    T& _sensor;
    SetupFn _setupFn;
    ReadDataFn _readDataFn;
};
