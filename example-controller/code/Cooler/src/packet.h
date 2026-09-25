#ifndef PACKET_H
#define PACKET_H


#include <ArduinoJson.h>
#include <IPAddress.h>
#include "udp.h"


class Packet {
    struct Discovered {
        int controllerId;
        IPAddress address;
        uint16_t port;
    };
public:
    Packet(UdpJson& udpJson) : udpJson_(udpJson) {
        udpJson_.on(
            [](const JsonDocument& doc) { return doc["server-discovered"].is<JsonObject>(); },
            [this](const JsonDocument& doc, const IPAddress& sender, uint16_t senderPort) {
                server = sender;
                log_i("Server discovered: %s", server.toString().c_str());
            }
        );        
    }

    void begin() {
        JsonDocument doc;
        JsonObject obj = doc["discovery"].to<JsonObject>(); 
        obj["controller-id"] = CONTROLLER_ID;
        obj["address"] = WiFi.localIP().toString() + ":" + String(udpJson_.localPort());

        while(!udpJson_.send(IPAddress(255, 255, 255, 255), UDP_PORT, doc)) {
            log_w("Failed to send discovery packet, retrying...");
            delay(100);
        }
        String json;
        serializeJsonPretty(doc, json);
    }


    template <typename... Documents>
    void sendMeasurements(const Documents&... documents) {
        if(!server) {
            log_w("Server not discovered yet, cannot send measurements");
            begin();
            return;
        }
        JsonDocument measurements;
        measurements["controller-id"] = CONTROLLER_ID;
        JsonObject dataPoints = measurements["data-points"].to<JsonObject>();
        int merged[] = {0, (mergeDocument(dataPoints, documents), 0)...};
        (void)merged;
        JsonDocument message;
        message["measurements"] = measurements;
        udpJson_.send(server, UDP_PORT, message);
    }
private:
    inline void mergeDocument(JsonObject destination, const JsonDocument& document) {
        for (JsonPairConst entry : document.as<JsonObjectConst>()) {
            destination[entry.key()] = entry.value();
        }
    }

    UdpJson& udpJson_;
    IPAddress server;
};

#endif // PACKET_H