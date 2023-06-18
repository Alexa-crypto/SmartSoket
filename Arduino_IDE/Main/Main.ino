//Board name --> DOIT ESP32 DEVKIT V1
#include <WiFi.h>
#include <EEPROM.h>
#include <Wire.h>
#include <SPI.h>
#include <WebServer.h>
#include <SPIFFS.h>
#include <FS.h>
#include "pzem004t.h"
#include "ErriezDS3231.h"

#define relay 4
#define INT_PIN 5
#define piezoPIN 18
// #define ionistor_read 32
// #define ionistor_key 33
// #define LED 2
#define ServerVersion "1.0"

#define time_off_hour 0
#define time_off_min 1
#define time_on_hour 2
#define time_on_min 3

#define volume_wifi_flash_bit 40
#define start_wifi_con_bit 4

Pzem004v3 pzem_mod;
ErriezDS3231 ds3231;
WebServer server(80);
// unsigned long count = 0;
bool pzemActiv = true;
// bool flag_set_WIFI;


#include "Request_File_Server.h"
#include "Request_Data_procesing.h"

TaskHandle_t handleClient;
hw_timer_t *My_timer = NULL;
volatile bool pzemInterrupt = false;

void IRAM_ATTR onTimer(){
  pzemInterrupt = true;
}

void setup() {
  Serial.begin(9600);
  Serial2.begin(9600, SERIAL_8N1, 16, 17);
  EEPROM.begin(volume_wifi_flash_bit + 4); // Ініціалізуємо EEPROM
  pinMode(relay, OUTPUT);
  pinMode(piezoPIN, OUTPUT);
  

  // pinMode(LED, OUTPUT);
  // digitalWrite(LED, HIGH);

  while(!SPIFFS.begin(true)) delay(1000);
  SPIFFS_present = true; 

  // Initialize TWI
  Wire.begin();
  Wire.setClock(400000);
  // Initialize RTC
  while (!ds3231.begin()) delay(3000);
  // Enable RTC clock
  if (!ds3231.isRunning())  ds3231.clockEnable();

  My_timer = timerBegin(0, 80, true);
  timerAttachInterrupt(My_timer, &onTimer, true);
  timerAlarmWrite(My_timer, 150000, true);
  timerAlarmEnable(My_timer);

  pinMode(INT_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(INT_PIN), alarmHandler, FALLING);
  // ds3231.setAlarm1(Alarm1MatchSeconds, 0, 0, 0, 30);
  ds3231.setAlarm1(Alarm1MatchHours, 0, 23, 59, 59);
  ds3231.alarmInterruptEnable(Alarm1, true);

  // setup_data_server();
  // setup_file_server();

  connecting_to_wifi();

  xTaskCreatePinnedToCore(
  handleClient_code,   /* Функция задачи */
  "handleClient_HTTP_server",     /* Название задачи */
  10000,       /* Размер стека задачи */
  NULL,        /* Параметр задачи */
  1,           /* Приоритет задачи */
  &handleClient,/* Идентификатор задачи,
                чтобы ее можно было отслеживать */
  0);          /* Ядро для выполнения задачи (0) */
  delay(500);

  // if (pzem_mod.update_data_all() == 0x00){
  tm dt;
  ds3231.read(&dt);
  String data = "on  &&  ";
  data += dt.tm_hour;         data += ":";
  data += dt.tm_min;          data += ":";
  data += dt.tm_sec;          data += "  &&  ";

  data += dt.tm_mday;         data += ".";
  data += dt.tm_mon;          data += ".";
  data += dt.tm_year + 1900;  data += "\n";

  file_append("/log_file.txt", data);

  
  // }
  
  // digitalWrite(LED, LOW);
  digitalWrite(relay, HIGH);
  pzem_mod.begin(0x08, &Serial2);
  // analogReadResolution(12);
  // pinMode(ionistor_read, INPUT);
  // pinMode(ionistor_key, OUTPUT);
  // while (true){
  //   Serial.println(analogRead(ionistor_read));
  //   if (analogRead(ionistor_read) > 3500)
  //     break;
  // }
  // digitalWrite(ionistor_key, HIGH);

  // ADC1_CTRL_REG = 0; // Встановлюємо значення 0 в регістрі управління ADC1
  // ADC2_CTRL_REG = 0; // Встановлюємо значення 0 в регістрі управління ADC2
}

void handleClient_code( void * pvParameters ){
  setup_data_server();
  setup_file_server();
  server.begin();
  delay(1000);
  for(;;)
    server.handleClient(); // Listen for client connections
}

void loop() {
  if (pzemActiv){
  // put your main code here, to run repeatedly:
    // pzem_mod.update_data_all();
    byte count = 0;
    while (pzemActiv && pzem_mod.get_power() >= pzem_mod.get_power_alarm_threshold() - ((pzem_mod.get_power_alarm_threshold() >> 3) + (pzem_mod.get_power_alarm_threshold() >> 6))){
      toneESP32(piezoPIN, 1800);
      pzem_mod.update_data_all();
      if (pzem_mod.get_alarm()){
        count++;
        if (count >= 50){
          digitalWrite(relay, LOW);
          noToneESP32(piezoPIN);
          break;
        }
      } else{
        noToneESP32(piezoPIN);
        delay(100);
      }
      noToneESP32(piezoPIN);
    }

    if (pzemInterrupt){
      if (pzem_mod.update_data_all() != 0x00){
        for (byte i = 0; i < 2 && pzem_mod.update_data_all() != 0x00; i++)
          delay(250);
        if (pzem_mod.update_data_all() == 0xFF){
          tm dt;
          ds3231.read(&dt);
          String data = "off  &&  ";
          data += String(dt.tm_hour);         data += ":";
          data += String(dt.tm_min);          data += ":";
          data += String(dt.tm_sec);          data += "  &&  ";

          data += String(dt.tm_mday);         data += ".";
          data += String(dt.tm_mon);          data += ".";
          data += String(dt.tm_year + 1900);  data += "\n";

          file_append("/log_file.txt", data);
          delay(10000);
        }
      }
      pzemInterrupt = false;
    }

    if (alarmInterrupt){
      if (ds3231.getAlarmFlag(Alarm2)){
        tm dt;
        ds3231.read(&dt);
        if ((dt.tm_hour == EEPROM.read(time_off_hour)) && (dt.tm_min == EEPROM.read(time_off_min))){
          digitalWrite(relay, false);
          ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
        }
        else if ((dt.tm_hour == EEPROM.read(time_on_hour)) && (dt.tm_min == EEPROM.read(time_on_min))){
          digitalWrite(relay, true);
          ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_off_hour), EEPROM.read(time_off_min));
        }
        ds3231.clearAlarmFlag(Alarm2);
      }
    
      if (ds3231.getAlarmFlag(Alarm1)){
        tm dt;
        ds3231.read(&dt);
        String path = "/" + String(dt.tm_mon) + ".txt";
        if ((dt.tm_mon > 2) && (SPIFFS.exists("/" + String(dt.tm_mon - 3) + ".txt")))
          SPIFFS.remove("/" + String(dt.tm_mon - 3) + ".txt");
        else if ((dt.tm_mon <= 2) && (SPIFFS.exists("/" + String(11 + dt.tm_mon - 2) + ".txt")))
          SPIFFS.remove("/" + String(11 + dt.tm_mon - 2) + ".txt");
        
        String data = String(pzem_mod.get_energy());    data += "  &  ";
        data += dt.tm_mday;                             data += ".";
        data += dt.tm_mon;                              data += ".";
        data += dt.tm_year + 1900;                      data += "\n";
        pzem_mod.reset_energy();
        file_append(path, data);
        ds3231.clearAlarmFlag(Alarm1);
      }
      alarmInterrupt = false;
    }
  }
}

void alarmHandler() {
  // Set global interrupt flag
  alarmInterrupt = true;
}

void connecting_to_wifi(){
  int i = start_wifi_con_bit;
  String ssid = "";
  String password = "";
  //read ssid and password
  for (; i < volume_wifi_flash_bit && EEPROM.read(i) != '\0'; i++)
    ssid += char(EEPROM.read(i));
  i++;
  for (; i < volume_wifi_flash_bit && EEPROM.read(i) != '\0'; i++)
    password += char(EEPROM.read(i));  

  // Перевіряємо, чи SSID не порожнє, та пробуєм підключитись
  if (ssid != "") {
    WiFi.mode(WIFI_STA);
    // WiFi.begin(ssid.c_str(), password.c_str());
    WiFi.begin("Redmi_AX", "REDMI_AX6S");

    for (int i = 0; i <= 10 && WiFi.status() != WL_CONNECTED; i++)
      delay(500);
  }
  //якщо за 5с не підключились, створюємо точку доступу
  if (WiFi.status() != WL_CONNECTED) {
    WiFi.mode(WIFI_AP);
    WiFi.softAP("MyESP");

    // Запускаємо веб-сервер  
    server.on("/wifi", [](){
      String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>WiFi Setup</title>"
      "<meta name='viewport' content='width=device-width, initial-scale=1'><style>"
      "*{box-sizing: border-box;}"
      "body {font-family: Arial, sans-serif;background-color: #f2f2f2;margin: 0;}"
      "h1 {text-align: center;margin-top: 50px;}"
      ".form-container {max-width: 500px;margin: 0 auto;background-color: #fff;border-radius: 5px;box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);padding: 20px;}"
      ".form-group {margin-bottom: 20px;}.form-group label {display: block;margin-bottom: 10px;color: #333;font-weight: bold;}"
      ".form-group input {width: 100%;padding: 10px;font-size: 16px;border-radius: 3px;border: none;box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);}"
      ".form-group input[type='submit'] {display: block;width: 100%;padding: 10px;font-size: 16px;color: #fff;background-color: #4CAF50;border: none;border-radius: 3px;cursor: pointer;}"
      ".form-group input[type='submit']:hover {background-color: #3e8e41;}"
      "@media only screen and (max-width: 600px) {h1 {font-size: 24px;margin-top: 20px;}.form-container {max-width: 90%;}}</style></head>"
      "<body><h1>WiFi Setup</h1><div class='form-container'><form method='post' action='/setwifi'><div class='form-group'><label for='ssid'>SSID:</label><input type='text' id='ssid' name='ssid' required></div>"
      "<div class='form-group'><label for='password'>Password:</label><input type='password' id='password' name='password' required></div>"
      "<div class='form-group'><input type='submit' value='Submit'></div></form></div></body></html>";
      server.send(200, "text/html", html); // відправка сторінки користувачеві
    });
    server.on("/setwifi", [](){
      String ssid = server.arg("ssid");
      String password = server.arg("password");
      if (ssid.length() + password.length() >= volume_wifi_flash_bit){
        server.send(200, "text/plain", "ERROR! Length ssid + password too big (>50 char)"); // відправка відповіді користувачеві
        return;        
      }
      
      int start_bit = start_wifi_con_bit;
      for (int i = start_bit; i < start_bit + ssid.length(); i++)
        EEPROM.write(i, ssid[i - start_bit]);
      start_bit += ssid.length();
      EEPROM.write(start_bit, '\0');
      start_bit++;
      for (int i = start_bit; i < start_bit + password.length(); i++)
        EEPROM.write(i, password[i - start_bit]);
      EEPROM.write(start_bit + password.length(), '\0');
      EEPROM.commit(); // збереження даних в EEPROM
      server.send(200, "text/plain", "Saved SSID: " + ssid + ", password: " + password); // відправка відповіді користувачеві
      delay(1000);
      connecting_to_wifi();
    });
  }
}
void toneESP32(uint8_t pin, unsigned int frequency) {
  ledcSetup(0, frequency, 8);
  ledcAttachPin(pin, 0);
  ledcWrite(0, 128);
}

void noToneESP32(uint8_t pin) {
  ledcDetachPin(pin);
}