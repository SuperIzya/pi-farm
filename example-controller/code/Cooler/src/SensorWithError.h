#pragma once

#include <functional>
#include <ArduinoJson.h>
#include "utils.h"
#include "Sensor.h"

template <typename R> 
struct WithError{
    R value;
    R sigma;
};

template <typename R>
class SensorErrorAlgebra {
public:
    static R plus(R a, R b) = delete;
    static R pow2(R a) = delete;
    static R divide(R a, int b) = delete;
    static R sqrt(R a) = delete;
    static R diff(R a, R b) = delete;
};

template <typename R>
inline R plus(R a, R b) {
    return SensorErrorAlgebra<R>::plus(a, b);
}
template <typename R>
inline R pow2(R a) {
    return SensorErrorAlgebra<R>::pow2(a);
}
template <typename R>
inline R divide(R a, int b) {
    return SensorErrorAlgebra<R>::divide(a, b);
}
template <typename R>
inline R diff(R a, R b) {
    return SensorErrorAlgebra<R>::diff(a, b);
}

template <typename R>
inline R sqrt(R a) {
    return SensorErrorAlgebra<R>::sqrt(a);
}

template <typename T, typename R>
class SensorWithError: public Sensor<T, R> {
public:
    using SetupFn = std::function<void(T&)>;
    using ReadDataFn = std::function<R(T&)>;
    
    SensorWithError(
        const char* name, 
        T& sensor,
        SetupFn setupFn, 
        ReadDataFn readDataFn, 
        int reads = 5,
        int timeoutMs = 15
    ): Sensor<T, R>(name, sensor, setupFn, readDataFn),
        _reads(reads),
        _timeoutMs(timeoutMs)
    {}

    void setup() {
        Sensor<T, R>::setup();
    }

    JsonDocument readData() {
        WithError<R> result;
        auto coll = new R[_reads];        
        R sum;
        for (int i = 0; i < _reads; i++) {
            coll[i] = Sensor<T, R>::_readDataFn(Sensor<T, R>::_sensor);
            if(i == 0) sum = coll[i];
            else sum = plus(sum, coll[i]);
            delayTask(_timeoutMs);
        }
        result.value = divide(sum, _reads);
        R sqSum;
        for (int i = 0; i < _reads; i++) {            
            R square = pow2(diff(coll[i], result.value));
            if(i == 0) sqSum = square;
            else sqSum = plus(sqSum, square);
        }
        delete[] coll;
        result.sigma = sqrt(divide(sqSum, _reads));

        JsonDocument value;
        toJson(result, value);
        JsonDocument doc;
        doc[Sensor<T, R>::_name] = value;
        return doc;
    }

private:
    int _reads;
    int _timeoutMs;
};
