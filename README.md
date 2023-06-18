# SmartSoket
Проект розумної розетки бакалавра. Проект базується на мікроконтролері ESP32, PZEM004t, DS3231. 
В даному проекта розташований код для мікроконтролера (в серидовищі ArduinoIDE), розроблено додаток для ОС Android для управління розеткою (в серидовищі AndroidStudio на Java) 
та розроблено веб сторінку для керування через браузер. Розетка керується через локальнумережу. Вона реєструє електричні показники (за допомогою модуля Pzem004t), 
веде графік споживання та є роота за таймером (реєструє дату через DS3231), ну і звісно можна віддалено вкл/викл пристрій. Пристрій простий, мікроконтролер просто виступає в якості HTTP сервера,
та відповідає на запити. 

Схему пристрою наведено нижче:
![image](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/d8bcb5c8-c1f4-4fc3-80a2-9340aa8c6e7a)

![test](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/0d9945b0-6a24-44a4-bd23-d85ba7b07f0d)

![image](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/cfa5409a-ad7a-46d3-babe-b4fbe47fc14d)

Нижче скріни додатку та веб сторінки налаштувань:

![image](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/04dc36d1-7ac8-47ec-b5e4-99c3060a277a)
![image](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/e2c67440-b12f-4c92-905a-5e66c2b05530)
![image](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/77c12f30-3b93-4c10-910c-94d2c7160c75)
![image](https://github.com/Alexa-crypto/SmartSoket/assets/78495955/12f9a64e-23c1-4e86-bee3-c0435167f625)

