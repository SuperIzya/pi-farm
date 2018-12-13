#include "dht11.h"

dht11 DHT;
const int DHT11_PIN = 4;

String inputString = "";         // a String to hold incoming data
bool stringComplete = false;  // whether the string is complete
const int LED_PIN = 6;
#define MOISTURE_PIN A0
int ledState = HIGH;
int count = 0;

void setup() {
  // initialize serial:
  Serial.begin(9600);
  // reserve 200 bytes for the inputString:
  inputString.reserve(200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, ledState);

  log("Started");
}

void blink() {
  digitalWrite(LED_PIN, 1 - ledState);
  delay(100);
  digitalWrite(LED_PIN, ledState);
}

void toggle() {
  ledState = 1 - ledState;
  digitalWrite(LED_PIN, ledState);
}


void loop() {
  // print the string when a newline arrives:
  if (stringComplete) {
    if(inputString.startsWith("cmd: ")) {
        inputString.remove(0, 5);
        String tmp = "|" + inputString;

        log(tmp + "|");
        if(inputString == "blink")
           blink();
        else if(inputString == "toggleLed") {
          toggle();
          log("Toggled to " + String(ledState));
        }
    }


    // clear the string:
    inputString = "";
    stringComplete = false;
  }

    int chk = DHT.read(DHT11_PIN);
    String str = "value: " + String(DHT.temperature);
    str += " - " + String(DHT.humidity);

    float val = analogRead(MOISTURE_PIN);

    str += " - " + String(map(val, 550, 10, 0, 100));
    str += " - " + String(ledState);
    log(str);

    delay(200);
}

/*
  SerialEvent occurs whenever a new data comes in the hardware serial RX. This
  routine is run between each time loop() runs, so using delay inside loop can
  delay response. Multiple bytes of data may be available.
*/
void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();
    switch (inChar) {
     case ';':
      stringComplete = true;
      return;
     case '\n':
      continue;
    }
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag so the main loop can
    // do something about it:
  }
}

void send(String message) {
    Serial.print(message + ";");
}

void log(String message) {
    send("log: " + message);
}