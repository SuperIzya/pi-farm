#include <Arduino.h>
#include <ArduinoJson.h>
#include <DHTesp.h>
#include <IPAddress.h>
#include <functional>
#include "Sensor.h"
#include "SensorWithError.h"
#include "HumiditySensor.h"
#include "Actuator.h"
#include "wifi.h"
#include "udp.h"
#include "packet.h"
#include "utils.h"
#include "fan.h"

#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

#define DHT_PIN_1 13
#define DHT_PIN_2 12

const int FAN = 40;

DHTesp internalDHT;
DHTesp externalDHT;
UdpJson udpJson(UDP_PORT);
Packet packet(udpJson);

Sensor<DHTesp, TempAndHumidity> *internal;
SensorWithError<DHTesp, TempAndHumidity> *external;

const FanPin fanPin{FAN};
Fan *fan;

void initPeriphery() {
    fan = createFan("fan", fanPin, false);
    fan->setup();

    internal = new Sensor<DHTesp, TempAndHumidity>(
        "internal",
        internalDHT,
        setupDHT(DHT_PIN_1),
        readData
    );
    internal->setup();

    external = new SensorWithError<DHTesp, TempAndHumidity>(
        "external",
        externalDHT,
        setupDHT(DHT_PIN_2),
        readData,
        10,
        5
    );
    external->setup();
}

void setup()
{
    Serial.begin(SERIAL_SPEED);
    while(!Serial) {
        delayTask(50);
    }
    log_i("Firmware started; connecting WiFi before initializing DHT22 sensors on GPIO %d and %d",
        DHT_PIN_1, DHT_PIN_2);
    connectWiFi();
    log_i("Will start UDP on port %d", UDP_PORT);
    while(!udpJson.begin()) {
        log_w("Failed to start UDP, retrying...");
        delayTask(100);
    }
    log_i("UDP started successfully");
    packet.begin();
    
    UdpJson::Listener listener = [](const JsonDocument& doc, const IPAddress& sender, uint16_t port) {
        bool flag = doc["command"]["data-points"]["fan"]["Fan"].as<bool>();
        fan->setState(flag);
        log_w("Fan state set to: %s", flag ? "true" : "false");
    };
    udpJson.on("command", listener);
    
    initPeriphery();
}

void loop()
{
    Serial.println("--- HARDWARE ALIVE ON V8 ---");
    udpJson.poll();
    JsonDocument internalData = internal->readData();
    JsonDocument externalData = external->readData();
    JsonDocument fanData = fan->getState();
    packet.sendMeasurements(internalData, externalData, fanData);
#if ARDUHAL_LOG_LEVEL >= ARDUHAL_LOG_LEVEL_VERBOSE
    printReading(DHT_PIN_1, internalData);
    printReading(DHT_PIN_2, externalData);
#endif
    delayTask(2000);
}
