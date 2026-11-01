#pragma once

#include <ArduinoJson.h>
#include <DHTesp.h>
#include "utils.h"
#include "Sensor.h"
#include "SensorWithError.h"


template<>
class SensorErrorAlgebra<TempAndHumidity> {
public:
  static TempAndHumidity plus(TempAndHumidity a, TempAndHumidity b) {
    return TempAndHumidity{ a.temperature + b.temperature, a.humidity + b.humidity };
  }
  static TempAndHumidity pow2(TempAndHumidity a) {
    return TempAndHumidity{ a.temperature * a.temperature, a.humidity * a.humidity };
  }

  static TempAndHumidity divide(TempAndHumidity a, int b) {
    return TempAndHumidity{ a.temperature / b, a.humidity / b };
  }

  static TempAndHumidity sqrt(TempAndHumidity a) {
    return TempAndHumidity{ ::sqrt(a.temperature), ::sqrt(a.humidity) };
  }
  static TempAndHumidity diff(TempAndHumidity a, TempAndHumidity b) {
    return TempAndHumidity{ a.temperature - b.temperature, a.humidity - b.humidity };
  }
};

Sensor<DHTesp, TempAndHumidity>::SetupFn setupDHT(int pin) {
  return [pin](DHTesp& dht) { dht.setup(pin, DHTesp::DHT22); };
}

Sensor<DHTesp, TempAndHumidity>::ReadDataFn readData = [](DHTesp& dht) { 
  return dht.getTempAndHumidity();
};

template<>
class ToJson<TempAndHumidity> {
public:
  static void toJson(const TempAndHumidity& reading, JsonDocument& doc) {
    doc["temperature"] = reading.temperature;
    doc["humidity"]    = reading.humidity;
  }
};

template<>
class ToJson<WithError<TempAndHumidity>> {
public:
  static void toJson(const WithError<TempAndHumidity>& reading, JsonDocument& doc) {
    doc["temperature"]["value"] = reading.value.temperature;
    doc["temperature"]["sigma"] = reading.sigma.temperature;
    doc["humidity"]["value"] = reading.value.humidity;
    doc["humidity"]["sigma"] = reading.sigma.humidity;
  }
};
