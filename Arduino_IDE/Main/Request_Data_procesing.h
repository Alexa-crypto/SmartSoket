// #include "Print.h"
// #include "HardwareSerial.h"
// Alarm interrupt flag must be volatile
volatile bool alarmInterrupt = false;

void HomePage_data();
void relay_switch();
void reset_energy();
void up_data();
void energy_chart();


void value_alarm();
void set_alarm();
void value_timer();
void timer_set();
void get_time();
void set_time();
void log_val();

void setup_data_server(){
  // Data Server Commands
  server.on("/",              HTTP_GET, HomePage_data);
  server.on("/up_data",       HTTP_GET, up_data);
  server.on("/value_alarm",   HTTP_GET, value_alarm);
  server.on("/relay_switch",  HTTP_GET, relay_switch);
  server.on("/value_timer",   HTTP_GET, value_timer);
  server.on("/log_val",       HTTP_GET, log_val);
  server.on("/get_time",      HTTP_GET, get_time);

  server.on("/set_alarm",    HTTP_POST, set_alarm);
  server.on("/timer_set",    HTTP_POST, timer_set);
  server.on("/energy_chart", HTTP_POST, energy_chart);
  server.on("/set_time",     HTTP_POST, set_time);
}

void HomePage_data(){
  String path = server.uri() + "index.html";
  File file = SPIFFS.open(path, "r");
  size_t sent = server.streamFile(file, "text/html");
  file.close();  
}

void value_alarm(){
  server.send(200, "text/plain", String(pzem_mod.get_power_alarm_threshold()));
}

void value_timer(){
  if (bitRead(ds3231.readRegister(0x0E), 1) == 1){
    String byf = "1";
    for (int i = 0; i < 4; i++)
      byf += "/" + String(EEPROM.read(i));
    server.send(200, "text/plain", byf);
  } else {
    String byf = "0";
    for (int i = 0; i < 4; i++)
      byf += "/" + String(EEPROM.read(i));
    server.send(200, "text/plain", byf);
  }
}

void timer_set(){
  String body = server.arg("plain");
  // if (body.length() != 13)
  //   return;
  if (body.startsWith("1")){
    body.remove(0, 1);
    
    char buffer[20];
    body.toCharArray(buffer,20);
    // Serial.println("stat_eeprom");
    // byte time_off_hour = (uint8_t)atoi(strtok(buffer,"/"));
    // byte time_off_min = (uint8_t)atoi(strtok(buffer,"/"));
    // byte time_on_hour = (uint8_t)atoi(strtok(buffer,"/"));
    // byte time_on_min = (uint8_t)atoi(strtok(buffer,"/"));

    EEPROM.write(time_off_hour, (uint8_t)atoi(strtok(buffer,"/")));
    EEPROM.write(time_off_min, (uint8_t)atoi(strtok(NULL,"/")));
    EEPROM.write(time_on_hour, (uint8_t)atoi(strtok(NULL,"/")));
    EEPROM.write(time_on_min, (uint8_t)atoi(strtok(NULL,"/")));
    EEPROM.commit();

    tm dt;
    ds3231.read(&dt);
    // if (dt.tm_hour == EEPROM.read(time_off_hour) && dt.tm_min >= EEPROM.read(time_off_min))
    //   ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    // else if (dt.tm_hour == EEPROM.read(time_on_hour) && dt.tm_min < EEPROM.read(time_on_min))
    //   ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    // else if (EEPROM.read(time_off_hour) < EEPROM.read(time_on_hour) && dt.tm_hour > EEPROM.read(time_off_hour) && dt.tm_hour < EEPROM.read(time_on_hour))
    //   ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    // else if (EEPROM.read(time_off_hour) > EEPROM.read(time_on_hour) && ((dt.tm_hour > EEPROM.read(time_off_hour) && dt.tm_hour > EEPROM.read(time_on_hour))
    //                                                                 || dt.tm_hour < EEPROM.read(time_on_hour)))
    //   ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    // else
    //   ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_off_hour), EEPROM.read(time_off_min));

    if (EEPROM.read(time_off_hour) < EEPROM.read(time_on_hour)){
      if (dt.tm_hour > EEPROM.read(time_off_hour))
        ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
      else if (dt.tm_hour == EEPROM.read(time_off_hour) && dt.tm_min > EEPROM.read(time_off_min))
        ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
      else
         ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_off_hour), EEPROM.read(time_off_min));
    } else if (EEPROM.read(time_off_hour) > EEPROM.read(time_on_hour)){
      if (dt.tm_hour > EEPROM.read(time_off_hour))
        ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_off_hour), EEPROM.read(time_off_min));
      else if (dt.tm_hour == EEPROM.read(time_off_hour) && dt.tm_min > EEPROM.read(time_off_min))
        ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_off_hour), EEPROM.read(time_off_min));
      else
         ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    } else if ((EEPROM.read(time_off_min) < EEPROM.read(time_on_min)) && dt.tm_min > EEPROM.read(time_off_min))
      ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    else if ((EEPROM.read(time_off_min) > EEPROM.read(time_on_min)) && dt.tm_min < EEPROM.read(time_off_min))
      ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_on_hour), EEPROM.read(time_on_min));
    else
      ds3231.setAlarm2(Alarm2MatchHours, 0, EEPROM.read(time_off_hour), EEPROM.read(time_off_min));

    ds3231.alarmInterruptEnable(Alarm2, true);
    server.send(200, "text/plain", "1");
  } else{
    ds3231.alarmInterruptEnable(Alarm2, false);
    server.send(200, "text/plain", "0");
  }  
}

void set_alarm(){
  if (pzemActiv)
    pzemActiv = !pzemActiv;
  delay(200);
  pzem_mod.set_power_alarm_threshold(server.arg("plain").toInt());
  server.send(200, "text/plain", String(pzem_mod.get_power_alarm_threshold()));
  if (!pzemActiv)
    pzemActiv = !pzemActiv;
}

void get_time(){
  tm dt;
  ds3231.read(&dt);
  String str = "";
  if (dt.tm_hour < 10)
    str += "0";
  str += String(dt.tm_hour) + ":";
  if (dt.tm_min < 10)
    str += "0";
  str += String(dt.tm_min);
  server.send(200, "text/plain", str); 
}

void set_time(){
  String body = server.arg("plain");
  ds3231.setEpoch(body.toInt());
  server.send(200, "text/plain"); 
}

void relay_switch() {
  bool state = !digitalRead(relay);
  digitalWrite(relay, state); 
  if (state)
    server.send(200, "text/plain", "1");
  else
    server.send(200, "text/plain", "0");
}

void reset_energy() {
  if (pzem_mod.reset_energy())
    server.send(200, "text/plain", "1");
  else
    server.send(200, "text/plain", "0");
}

void up_data() {
  String data = String(pzem_mod.get_voltage(), 1);  data += "/";
  data += String(pzem_mod.get_current(), 3);        data += "/";
  data += String(pzem_mod.get_power(), 1);          data += "/";
  data += String(pzem_mod.get_energy());            data += "/";
  data += String(pzem_mod.get_frequency(), 1);      data += "/";
  data += String(pzem_mod.get_power_factor(), 2);   data += "/";
  data += digitalRead(relay) ? "1/" : "0/";
  data += String(pzem_mod.get_alarm());
  server.send(200, "text/plain", data); 
}

void energy_chart(){
  // String data = "";
  // for (int i = 0; i <= 31; i++)
  //   data += String(random(0, 800)) + "&./";
  // server.send(200, "text/plain", data);
  // String data = server.arg("plain");
  // file_read(data);
  file_read(server.arg("plain"));
}

void log_val(){
  file_read("/log_file.txt");
}