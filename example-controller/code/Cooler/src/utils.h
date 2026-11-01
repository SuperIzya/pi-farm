#pragma once
#ifndef UTILS_H
#define UTILS_H


template <typename R>
class ToJson {
    public:
    static void toJson(R& reading, JsonDocument& doc) = delete;
};

template <typename R>
inline void toJson(R& reading, JsonDocument& doc) {
    ToJson<R>::toJson(reading, doc);
};


inline void delayTask(int ms) {
    vTaskDelay(ms / portTICK_PERIOD_MS);
}

#endif // UTILS_H