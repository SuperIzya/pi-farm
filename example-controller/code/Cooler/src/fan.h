#pragma once

#include "utils.h"
#include "Actuator.h"

struct FanPin { int pin; };
using Fan = Actuator<FanPin, bool>;

Fan* createFan(const char* name, const FanPin& pin, const bool& initialState);

template <>
class ToJson<const bool> {
public:
  static void toJson(const bool& state, JsonDocument& doc) {
    doc["Fan"] = state ? "true" : "false";
  }
};