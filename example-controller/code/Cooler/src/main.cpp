#include <Arduino.h>
#include <ArduinoJson.h>
#include <DHTesp.h>
#include <IPAddress.h>
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
UdpJson udpJson(UDP_PORT);
Packet packet(udpJson);
bool flag = true;

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
    log_i("Will start UDP on port %d", UDP_PORT);
    while(!udpJson.begin()) {
        log_w("Failed to start UDP, retrying...");
        delay(100);
    }
    log_i("UDP started successfully");
    packet.begin();

    UdpJson::Listener listener = [](const JsonDocument& doc, const IPAddress& sender, uint16_t port) {
        bool flag = doc["command"]["data-points"]["fan"]["Fan"].as<bool>();
        fan.setState(flag);
        log_w("Fan state set to: %s", flag ? "true" : "false");
    };
    udpJson.on("command", listener);
}

static void printReading(int pin, const std::string& json) {
    log_v("Sensor: %d\n%s\n", pin, json.c_str());
}

void loop()
{
    udpJson.poll();
    JsonDocument internalData = internal.readData();
    JsonDocument externalData = external.readData();
    JsonDocument fanData = fan.getState();
    packet.sendMeasurements(internalData, externalData, fanData);
#if ARDUHAL_LOG_LEVEL >= ARDUHAL_LOG_LEVEL_VERBOSE
    printReading(DHT_PIN_1, internalData);
    printReading(DHT_PIN_2, externalData);
#endif
    delay(2000);
}
