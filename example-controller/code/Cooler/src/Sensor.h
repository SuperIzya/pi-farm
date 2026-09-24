#pragma once

#include <functional>
#include <ArduinoJson.h>

template <typename T, typename R>
class Sensor {
public:
    using SetupFn = std::function<void(T&)>;
    using ReadDataFn = std::function<R(T&)>;
    using ToJsonFn = std::function<void(R&, JsonDocument&)>;
    Sensor(const char* name, T& sensor, SetupFn setupFn, ReadDataFn readDataFn, ToJsonFn toJson)
        : _name(name), _sensor(sensor), _setupFn(setupFn), _readDataFn(readDataFn), _toJson(toJson) {}

    void setup() {
        _setupFn(_sensor);
    }

    JsonDocument readData() {
        R reading = _readDataFn(_sensor);
        JsonDocument value;
        _toJson(reading, value);
        JsonDocument doc;
        doc[_name] = value;
        return doc;
    }

private:
    const char* _name;
    T& _sensor;
    SetupFn _setupFn;
    ReadDataFn _readDataFn;
    ToJsonFn _toJson;
};
