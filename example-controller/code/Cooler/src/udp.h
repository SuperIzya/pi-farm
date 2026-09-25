#pragma once

#include <Arduino.h>
#include <ArduinoJson.h>
#include <IPAddress.h>
#include <WiFiUdp.h>
#include <freertos/semphr.h>

#include <functional>
#include <utility>
#include <vector>

class UdpJson {
public:
    using Predicate = std::function<bool(const JsonDocument&)>;
    using Listener = std::function<void(const JsonDocument&, const IPAddress&, uint16_t)>;

    struct ReceivedPacket {
        IPAddress sender;
        uint16_t senderPort;
        JsonDocument document;
    };

    UdpJson(uint16_t localPort) : localPort_(localPort) {}

    bool begin()
    {
        if (receiveTaskHandle_ != nullptr) {
            return true;
        }

        if (udp_.begin(localPort_) != 1) {
            return false;
        }

        socketMutex_ = xSemaphoreCreateMutex();
        if (socketMutex_ == nullptr) {
            udp_.stop();
            return false;
        }

        queueMutex_ = xSemaphoreCreateMutex();
        if (queueMutex_ == nullptr) {
            vSemaphoreDelete(socketMutex_);
            socketMutex_ = nullptr;
            udp_.stop();
            return false;
        }

        BaseType_t result = xTaskCreatePinnedToCore(
            receiveTask,
            "udp-receive",
            4096,
            this,
            1,
            &receiveTaskHandle_,
            0
        );
        if (result != pdPASS) {
            vSemaphoreDelete(queueMutex_);
            queueMutex_ = nullptr;
            vSemaphoreDelete(socketMutex_);
            socketMutex_ = nullptr;
            udp_.stop();
            return false;
        }

        return true;
    }

    int send(const IPAddress& destination, uint16_t destinationPort,
             const JsonDocument& document)
    {
        if (socketMutex_ == nullptr ||
            xSemaphoreTake(socketMutex_, portMAX_DELAY) != pdTRUE) {
            return 0;
        }

        int result = 0;
        if (udp_.beginPacket(destination, destinationPort)) {
            serializeJson(document, udp_);
            result = udp_.endPacket();
        }
        xSemaphoreGive(socketMutex_);
        return result;
    }

    void on(Predicate predicate, Listener listener)
    {
        listeners_.push_back({std::move(predicate), std::move(listener)});
    }

    void poll()
    {
        std::vector<ReceivedPacket> packets;
        if (queueMutex_ == nullptr) {
            return;
        }
        if (xSemaphoreTake(queueMutex_, portMAX_DELAY) != pdTRUE) {
            return;
        }
        packets.swap(receivedPackets_);
        xSemaphoreGive(queueMutex_);

        for (const auto& packet : packets) {
            for (const auto& registered : listeners_) {
                if (registered.predicate(packet.document)) {
                    registered.listener(
                        packet.document, packet.sender, packet.senderPort);
                }
            }
        }
    }

    int localPort() const {
        return localPort_;
    }

private:
    static void receiveTask(void* parameter)
    {
        auto* transport = static_cast<UdpJson*>(parameter);
        for (;;) {
            transport->receivePacket();
            vTaskDelay(pdMS_TO_TICKS(10));
        }
    }

    void receivePacket()
    {
        JsonDocument document;
        DeserializationError error;
        IPAddress sender;
        uint16_t senderPort = 0;

        if (xSemaphoreTake(socketMutex_, portMAX_DELAY) != pdTRUE) {
            return;
        }

        if (udp_.parsePacket() <= 0) {
            xSemaphoreGive(socketMutex_);
            return;
        }

        error = deserializeJson(document, udp_);
        sender = udp_.remoteIP();
        senderPort = udp_.remotePort();
        xSemaphoreGive(socketMutex_);

        if (error) {
            return;
        }

        ReceivedPacket packet{sender, senderPort, std::move(document)};
        if (xSemaphoreTake(queueMutex_, portMAX_DELAY) == pdTRUE) {
            receivedPackets_.push_back(std::move(packet));
            xSemaphoreGive(queueMutex_);
        }
    }

    struct RegisteredListener {
        Predicate predicate;
        Listener listener;
    };

    WiFiUDP udp_;
    uint16_t localPort_;
    SemaphoreHandle_t socketMutex_ = nullptr;
    SemaphoreHandle_t queueMutex_ = nullptr;
    TaskHandle_t receiveTaskHandle_ = nullptr;
    std::vector<RegisteredListener> listeners_;
    std::vector<ReceivedPacket> receivedPackets_;
};