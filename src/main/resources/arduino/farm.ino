#include "dht11.h"

dht11 DHT;
const int DHT11_PIN = 4;
const int LED_PIN = 6;
const int RESET_PIN = 10;
#define MOISTURE_PIN A0

String inputString = "";         // a String to hold incoming data
bool stringComplete = false;  // whether the string is complete
int ledState = HIGH;
int count = 0;

const int blinkTimeout = 100;
const int delayTimeout = 270;

void setup() {
  // initialize serial:
  Serial.begin(9600);
  // reserve 200 bytes for the inputString:
  inputString.reserve(200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, ledState);

  pinMode(RESET_PIN, OUTPUT);
  digitalWrite(RESET_PIN, LOW);

  log("Started");
}

void blink() {
  digitalWrite(LED_PIN, 1 - ledState);
  delay(blinkTimeout);
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
        if(inputString == "blink") {
           blink();
           log("Blink");
        }
        else if(inputString == "toggleLed") {
          toggle();
          log("Toggled to " + String(ledState));
        }
        else if(inputString == "reset") {
          log("Restarting");
          Serial.flush();
          digitalWrite(RESET_PIN, HIGH);
          log("Failed to restart");
        }
    }


    // clear the string:
    inputString = "";
    stringComplete = false;
  }

  sendData();

  delay(delayTimeout);
}


void sendData() {

    int chk = DHT.read(DHT11_PIN);
    String str = "value: " + String(DHT.temperature);
    str += " - " + String(DHT.humidity);

    float val = analogRead(MOISTURE_PIN);

    str += " - " + String(map(val, 550, 10, 0, 100));
    str += " - " + String(ledState);
    digitalWrite(LED_PIN, 1 - ledState);
    log(str);
    Serial.flush();
    digitalWrite(LED_PIN, ledState);

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