

const int LEFT_PIN = 12;
const int RIGHT_PIN = 11;

const int LED_PIN = 5;
const int BUTTON_PIN = 4;
const int RESET_PIN = 2;
const int SEND_PIN = 3;

String inputString = "";         // a String to hold incoming data
bool stringComplete = false;  // whether the string is complete
int ledState = LOW;

const int blinkTimeout = 100;
const int delayTimeout = 270;
int mode = 0;
void setup() {
  // initialize serial:
  Serial.begin(9600);
  pinMode(LEFT_PIN, OUTPUT);
  pinMode(RIGHT_PIN, OUTPUT);
  // reserve 200 bytes for the inputString:
  inputString.reserve(200);
  pinMode(SEND_PIN, OUTPUT);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT);
  digitalWrite(LED_PIN, LOW);
  digitalWrite(SEND_PIN, ledState);

  pinMode(RESET_PIN, OUTPUT);
  digitalWrite(RESET_PIN, LOW);
  pinMode(LED_BUILTIN, INPUT);
  digitalWrite(LED_BUILTIN, LOW);
}

void clearInput() {
  inputString = "";
  stringComplete = false;
}

void loop() {
  // print the string when a newline arrives:
  if (stringComplete) {
    if(inputString.startsWith("the-led: ")) {
        inputString.remove(0, 9);
        ledState = inputString == "0" ? LOW : HIGH;
    }
    else if(inputString == "reset") {
        digitalWrite(RESET_PIN, HIGH);
    }
    else if(inputString.startsWith("spin: ")) {
      inputString.remove(0, 6);
      mode = inputString.toInt();
      switch(mode) {
        case 0:
          digitalWrite(LEFT_PIN, LOW);
          digitalWrite(RIGHT_PIN, LOW);
          break;
        case 1:
          digitalWrite(LEFT_PIN, HIGH);
          digitalWrite(RIGHT_PIN, LOW);
          break;
        case -1:
          digitalWrite(LEFT_PIN, LOW);
          digitalWrite(RIGHT_PIN, HIGH);
          break;
        default: break;
      }
    }
    clearInput();
  }

  digitalWrite(LED_PIN, ledState);
  int btn = digitalRead(BUTTON_PIN);
  String str = "the-button: ";
  if(btn == HIGH) str += "1";
  else str += "0";
  send(str);

  Serial.flush();

  delay(delayTimeout);
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
    digitalWrite(SEND_PIN, HIGH);
    Serial.print(message + ";");
    digitalWrite(SEND_PIN, LOW);
}

void log(String message) {
    send("log: " + message);
}
