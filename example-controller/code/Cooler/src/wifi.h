#pragma once
#include <WiFi.h>

static void connectWiFi()
{
    WiFi.persistent(true);
    WiFi.setAutoReconnect(true);
    WiFi.setSleep(false);      // disable power-save
    WiFi.mode(WIFI_STA);

    WiFi.disconnect(true, true); // disconnect from any previous connection and erase old credentials
    delay(100);

    
    log_v("Connecting to '");
    log_v(WIFI_SSID);
    log_v("' %d\n", WiFi.begin(WIFI_SSID, WIFI_PASSWORD));
#ifdef ESP32
    WiFi.setTxPower(WIFI_POWER_8_5dBm); // Lower power to stabilize connection
#endif
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        log_v(".");
    }
    Serial.println();

    if (WiFi.status() != WL_CONNECTED) {
        log_v("Retry. wl_status=%d  (3=wrong password, 1=AP not found, 6=disconnected)\n",
            WiFi.status());
        return;
    }
    
    log_v("Connected! IP:%s  RSSI:%d dBm\n",
        WiFi.localIP().toString().c_str(), WiFi.RSSI());
}
