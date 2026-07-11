#include <Arduino.h>
#include <DHTesp.h>
#include <WiFi.h>

#define DHT_PIN_1 13
#define DHT_PIN_2 12
#define FAN 40

DHTesp dht1;
DHTesp dht2;

static void printReading(DHTesp &dht, int pin)
{
    TempAndHumidity reading = dht.getTempAndHumidity();
    if (dht.getStatus() == DHTesp::ERROR_NONE) {
        Serial.print("GPIO ");
        Serial.print(pin);
        Serial.print("  Temperature: ");
        Serial.print(reading.temperature, 1);
        Serial.print(" C  Humidity: ");
        Serial.print(reading.humidity, 1);
        Serial.println(" %");
    } else {
        Serial.println("GPIO " + String(pin) + " sensor error: " + String(dht.getStatusString()));
    }
}

static void connectWiFi()
{
    WiFi.mode(WIFI_STA);
    WiFi.disconnect(true);
    delay(200);

    Serial.print("Connecting to '");
    Serial.print(WIFI_SSID);
    Serial.println("' ...");
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    const unsigned long timeout = 20000;
    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED) {
        if (millis() - start > timeout) {
            Serial.printf("\nFailed. wl_status=%d  (3=wrong password, 1=AP not found, 6=disconnected)\n",
                WiFi.status());
            start = millis();
        }
        delay(500);
        Serial.print(".");
    }
    Serial.println();
    Serial.printf("Connected! IP:%s  RSSI:%d dBm\n",
        WiFi.localIP().toString().c_str(), WiFi.RSSI());
}

void setup()
{
    Serial.begin(SERIAL_SPEED);
    delay(2000);
    pinMode(FAN, OUTPUT);
    dht1.setup(DHT_PIN_1, DHTesp::DHT22);
    dht2.setup(DHT_PIN_2, DHTesp::DHT22);
    Serial.println("AM2302/DHT22 readers started on GPIO " + String(DHT_PIN_1) + " and " + String(DHT_PIN_2));
    connectWiFi();
}

int flag = 1;
void loop()
{
    printReading(dht1, DHT_PIN_1);
    printReading(dht2, DHT_PIN_2);
    digitalWrite(FAN, flag ? HIGH : LOW);
    flag = 1 - flag;
    delay(2000);
}
