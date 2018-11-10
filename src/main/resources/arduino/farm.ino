
void setup() {
  // put your setup code here, to run once:
Serial.begin(115200);
  // Set WiFi to station mode and disconnect from an AP if it was previously connected
  Serial.println("Hello");
}

void loop() {
  // put your main code here, to run repeatedly:
Serial.println("Start");

delay(1000);
Serial.println("End");
}