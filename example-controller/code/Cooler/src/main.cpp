#include <Arduino.h>
#include <ArduinoJson.h>
#include <DHTesp.h>
#include <WiFi.h>
#include "Sensor.h"

#define DHT_PIN_1 13
#define DHT_PIN_2 12
#define FAN 40

DHTesp dht1;
DHTesp dht2;

inline void toJson(const TempAndHumidity& reading, JsonDocument& doc) {
    doc["temperature"] = reading.temperature;
    doc["humidity"]    = reading.humidity;
}

Sensor<DHTesp, TempAndHumidity> sensor1("sensor1", dht1,
    []() { dht1.setup(DHT_PIN_1, DHTesp::DHT22); },
    []() { return dht1.getTempAndHumidity(); },
    toJson
);
Sensor<DHTesp, TempAndHumidity> sensor2("sensor2", dht2,
    []() { dht2.setup(DHT_PIN_2, DHTesp::DHT22); },
    []() { return dht2.getTempAndHumidity(); },
    toJson
);

static void connectWiFi()
{
    WiFi.persistent(false);    // don't read/write NVS — stale flash creds can override what we pass
    WiFi.setSleep(false);      // disable power-save
    WiFi.mode(WIFI_STA);

    const unsigned long timeout = 10000;
    WiFi.disconnect(true);
    delay(1000);
    
    Serial.print("Connecting to '");
    Serial.print(WIFI_SSID);
    Serial.println("' ...");
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    
    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - start < timeout) {
        delay(500);
        Serial.print(".");
    }
    Serial.println();

    if (WiFi.status() != WL_CONNECTED) {
        Serial.printf("Retry. wl_status=%d  (3=wrong password, 1=AP not found, 6=disconnected)\n",
            WiFi.status());
        return;
    }
    
    Serial.printf("Connected! IP:%s  RSSI:%d dBm\n",
        WiFi.localIP().toString().c_str(), WiFi.RSSI());
}

void setup()
{
    Serial.begin(SERIAL_SPEED);
    delay(2000);
    pinMode(FAN, OUTPUT);
    sensor1.setup();
    sensor2.setup();
    Serial.println("AM2302/DHT22 readers started on GPIO " + String(DHT_PIN_1) + " and " + String(DHT_PIN_2));
    connectWiFi();
}

int flag = 1;

static void printReading(int pin, const JsonDocument& doc) {
    Serial.print("Sensor: ");
    Serial.println(pin);
    serializeJson(doc, Serial);
    Serial.println();
}

void loop()
{
    printReading(DHT_PIN_1, sensor1.readData());
    printReading(DHT_PIN_2, sensor2.readData());
    digitalWrite(FAN, flag ? HIGH : LOW);
    flag = 1 - flag;
    delay(2000);
}
