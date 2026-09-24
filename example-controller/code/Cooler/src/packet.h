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
    Packet(UdpJson& udpJson) : udpJson_(udpJson) {}

    void begin() {
        udpJson_.on(
            [](const JsonDocument& doc) { return doc.containsKey("server-discovered"); },
            [this](const JsonDocument& doc, const IPAddress& sender, uint16_t senderPort) {
                server = sender;
            }
        );
        JsonDocument doc;
        doc["discovery"]["controller-id"] = CONTROLLER_ID;
        udpJson_.send(IPAddress(255, 255, 255, 255), UDP_PORT, doc);
    }

    void sendMeasurements(const JsonDocument& doc) {
        StaticJsonDocument<1024> measurements;
        measurements["data-points"] = doc;
        JsonDocument message;
        message["measurements"] = doc;
        udpJson_.send(server, UDP_PORT, message);
    }
private:
    UdpJson& udpJson_;
    IPAddress& server;
};

#endif // PACKET_H