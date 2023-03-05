#include <ArduinoBLE.h>

BLEService userDataService("181C");
BLEIntCharacteristic targetCharacteristic("0101", BLERead | BLENotify);
BLEIntCharacteristic laserCharacteristic("0102", BLEWrite | BLEWriteWithoutResponse);

char* temiName = "temi_4";
int light1[16];
int light2[16];
int sigPin = 0;
int s0 = 2;
int s1 = 3;
int s2 = 4;
int s3 = 5;
int laserPin = 7;

void busyDelay(BLEDevice* central, int ms) {
  long baseMillis = millis();
  
  while(central->connected()) {
    if (millis() - baseMillis >= ms) {
      break;
    }
  }
}

void dataWritten(BLEDevice central, BLECharacteristic characteristic) {
  // Serial.println(laserCharacteristic.value());
  digitalWrite(laserPin, HIGH);
  busyDelay(&central, 200);
  digitalWrite(laserPin, LOW);
}

void setup() {
  // Serial.begin(9600);

  pinMode(s0, OUTPUT);
  pinMode(s1, OUTPUT);
  pinMode(s2, OUTPUT);
  pinMode(s3, OUTPUT);
  pinMode(laserPin, OUTPUT);

  digitalWrite(s0, LOW);
  digitalWrite(s1, LOW);
  digitalWrite(s2, LOW);
  digitalWrite(s3, LOW);

  if (!BLE.begin()) {
    // Serial.println("starting Bluetooth® Low Energy module failed!");

    while (1);
  }

  BLE.setLocalName(temiName);
  BLE.setAdvertisedService(userDataService); 
  userDataService.addCharacteristic(targetCharacteristic);
  userDataService.addCharacteristic(laserCharacteristic);
  BLE.addService(userDataService);

  laserCharacteristic.setEventHandler(BLEWritten, dataWritten);
  // targetCharacteristic.writeValue(0);

  BLE.advertise();
  // Serial.println("Bluetooth Device Active, Waiting for Connections...");
  // Serial.println(BLE.address());
}

int readMux(int channel) {
  int controlPin[] = { s0, s1, s2, s3 };
  int muxChannel[16][4] = {
    { 0, 0, 0, 0 },
    { 1, 0, 0, 0 },  //channel 1
    { 0, 1, 0, 0 },  //channel 2
    { 1, 1, 0, 0 },  //channel 3
    { 0, 0, 1, 0 },  //channel 4
    { 1, 0, 1, 0 },  //channel 5
    { 0, 1, 1, 0 },  //channel 6
    { 1, 1, 1, 0 },  //channel 7
    { 0, 0, 0, 1 },  //channel 8
    { 1, 0, 0, 1 },  //channel 9
    { 0, 1, 0, 1 },  //channel 10
    { 1, 1, 0, 1 },  //channel 11
    { 0, 0, 1, 1 },  //channel 12
    { 1, 0, 1, 1 },  //channel 13
    { 0, 1, 1, 1 },  //channel 14
    { 1, 1, 1, 1 }   //channel 15
  };
  //loop through the 4 sig
  for (int i = 0; i < 4; i++) {
    digitalWrite(controlPin[i], muxChannel[channel][i]);
  }
  //read the value at the SIG pin
  int val = analogRead(sigPin);  //return the value
  return val;
}

void loop() {
  BLEDevice central = BLE.central();
  int gap;

  if (central.connected()) {
    gap = 0;

    for (int i = 0; i < 16; i++) {
      light1[i] = readMux(i);
    }

    busyDelay(&central, 50);

    for (int i = 0; i < 16; i++) {
      light2[i] = readMux(i);
    }

    for (int i = 0; i < 16; i++) {
      gap = light2[i] - light1[i];  //증가하는 경우만

      if (gap > 150) {
        targetCharacteristic.writeValue(1);
        // Serial.println("HIT");
        busyDelay(&central, 1000);
        break;
      }
    }
  }

  BLE.poll();
}