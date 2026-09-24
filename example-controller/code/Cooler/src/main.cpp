#include <Arduino.h>
#include <ArduinoJson.h>
#include <DHTesp.h>
#include <functional>
#include "Sensor.h"
#include "Actuator.h"
#include "wifi.h"
#include "udp.h"
#include "packet.h"

#define DHT_PIN_1 13
#define DHT_PIN_2 12
const int FAN = 40;

DHTesp internalDHT;
DHTesp externalDHT;
UdpJson udpJson(1024);
Packet packet(udpJson);

inline void toJson(const TempAndHumidity& reading, JsonDocument& doc) {
    doc["temperature"] = reading.temperature;
    doc["humidity"]    = reading.humidity;
}

Sensor<DHTesp, TempAndHumidity>::SetupFn setupDHT(int pin) {
    return [pin](DHTesp& dht) { dht.setup(pin, DHTesp::DHT22); };
}

Sensor<DHTesp, TempAndHumidity>::ReadDataFn readData = [](DHTesp& dht) { return dht.getTempAndHumidity(); };

Sensor<DHTesp, TempAndHumidity> internal("internal", internalDHT,
    setupDHT(DHT_PIN_1),
    readData,
    toJson
);
Sensor<DHTesp, TempAndHumidity> external("external", externalDHT,
    setupDHT(DHT_PIN_2),
    readData,
    toJson
);

struct FanPin { int pin; };
using Fan = Actuator<FanPin, bool>;
Fan::SetupFn setupFan = [](const FanPin& pin) { pinMode(pin.pin, OUTPUT); };
Fan::SetStateFn setFanState = [](const FanPin& pin, const bool& state) { digitalWrite(pin.pin, state ? HIGH : LOW); };
Fan::ToJsonFn fanToJson = [](const bool& state, JsonDocument& doc) { doc["fan"] = state ? "true" : "false"; };

const FanPin fanPin{FAN};

Fan fan("fan", fanPin, false,
    setupFan,
    setFanState,
    fanToJson
);

void setup()
{
    Serial.begin(SERIAL_SPEED);
    delay(2000);
    internal.setup();
    external.setup();
    fan.setup();
    log_v("AM2302/DHT22 readers started on GPIO %d and %d", DHT_PIN_1, DHT_PIN_2);
    connectWiFi();
    udpJson.begin();
    packet.begin();
}

int flag = 1;

static void printReading(int pin, const std::string& json) {
    log_v("Sensor: %d\n%s\n", pin, json.c_str());
}

void loop()
{
    JsonDocument internalData = internal.readData();
    JsonDocument externalData = external.readData();
    JsonDocument fanData = fan.getState();
    packet.sendMeasurements(internalData, externalData, fanData);
#if ARDUHAL_LOG_LEVEL >= ARDUHAL_LOG_LEVEL_VERBOSE
    printReading(DHT_PIN_1, json1);
    printReading(DHT_PIN_2, json2);
#endif
    fan.setState(flag);
    flag = 1 - flag;
    delay(2000);
}
