String inputString = "";         // a String to hold incoming data
bool stringComplete = false;  // whether the string is complete
int counter = 0;

void setup() {
    Serial.begin(115200);
    // reserve 200 bytes for the inputString:
    inputString.reserve(200);
}

void loop() {
  // print the string when a newline arrives:
  if (stringComplete) {
    counter++;
    String str = inputString + " " + String(counter);
    Serial.println(str);
    // clear the string:
    inputString = "";
    stringComplete = false;
  }
}

void serialEvent() {
  while (Serial.available()) {
     // get the new byte:
     char inChar = (char)Serial.read();
     // add it to the inputString:
     inputString += inChar;
     // if the incoming character is a newline, set a flag so the main loop can
     // do something about it:
     if (inChar == '\n') stringComplete = true;
  }
}