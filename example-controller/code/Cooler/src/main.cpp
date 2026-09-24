#include <Arduino.h>
#include <ArduinoJson.h>
#include <DHTesp.h>
#include "Sensor.h"
#include "wifi.h"
#include "udp.h"
#include "packet.h"

#define DHT_PIN_1 13
#define DHT_PIN_2 12
#define FAN 40

DHTesp dht1;
DHTesp dht2;
UdpJson udpJson(1024);
Packet packet(udpJson);

inline void toJson(const TempAndHumidity& reading, JsonDocument& doc) {
    doc["temperature"] = reading.temperature;
    doc["humidity"]    = reading.humidity;
}

Sensor<DHTesp, TempAndHumidity> sensor1("internal", dht1,
    []() { dht1.setup(DHT_PIN_1, DHTesp::DHT22); },
    []() { return dht1.getTempAndHumidity(); },
    toJson
);
Sensor<DHTesp, TempAndHumidity> sensor2("external", dht2,
    []() { dht2.setup(DHT_PIN_2, DHTesp::DHT22); },
    []() { return dht2.getTempAndHumidity(); },
    toJson
);

void setup()
{
    Serial.begin(SERIAL_SPEED);
    delay(2000);
    pinMode(FAN, OUTPUT);
    sensor1.setup();
    sensor2.setup();
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
    JsonDocument data1 = sensor1.readData();
    JsonDocument data2 = sensor2.readData();
    std::string json1;
    serializeJson(data1, json1);
    std::string json2;
    serializeJson(data2, json2);
#if ARDUHAL_LOG_LEVEL >= ARDUHAL_LOG_LEVEL_VERBOSE
    printReading(DHT_PIN_1, json1);
    printReading(DHT_PIN_2, json2);
#endif
    digitalWrite(FAN, flag ? HIGH : LOW);
    flag = 1 - flag;
    delay(2000);
}
