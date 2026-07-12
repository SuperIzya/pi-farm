#pragma once

#include <functional>
#include <ArduinoJson.h>

template <typename T, typename R>
class Sensor {
public:
    Sensor(const char* name, T& sensor, std::function<void()> setupFn, std::function<R()> readDataFn, std::function<void(R&, JsonDocument&)> toJson)
        : _name(name), _sensor(sensor), _setupFn(setupFn), _readDataFn(readDataFn), _toJson(toJson) {}

    void setup() {
        _setupFn();
    }

    JsonDocument readData() {
        R reading = _readDataFn();
        JsonDocument value;
        _toJson(reading, value);
        JsonDocument doc;
        doc[_name] = value;
        return doc;
    }

private:
    const char* _name;
    T& _sensor;
    std::function<void()> _setupFn;
    std::function<R()> _readDataFn;
    std::function<void(R&, JsonDocument&)> _toJson;
};
