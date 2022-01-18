#include "./Advertisement.h"
#include "BLEBeacon.h"
#include "BLEDevice.h"
#include "BLEUtils.h"
#include "esp_sleep.h"

#define BEACON_ID "jsjs"  //4-character id of my beacon
#define BEACON_PSK "AuSiyh8KvoTGKXYtsRRfx6nXWZubKraFS9JFhJHWW8XJItR4YiF2fwvunYqLNiNVsDbQtRf49GjtEjhHN63bE2QKOy5a1mU1YqRd0PwchW9vk9v7laYSjJZ8cltj3KwB"
#define GPIO_DEEP_SLEEP_DURATION 0.333  // sleep x seconds and then wake up


RTC_DATA_ATTR static bool initialized = false;
RTC_DATA_ATTR static uint8_t bootcount = 0;  // remember number of boots in RTC Memory; will be reset every 10 boots
RTC_DATA_ATTR static uint32_t nonce = 0;
RTC_DATA_ATTR static char lastStrAdvData[31];

BLEAdvertising* pAdvertising;
Advertisement adv;


void setup() {
    Serial.begin(115200);

    //Serial.println("[INFO] ESP wake up. Good morning.");
    //Serial.printf("[INFO] Program initalized: %d. \n", initialized);
    //Serial.printf("[INFO] Bootcount: %d \n", bootcount);
    Serial.printf("[INFO] Nonce: %d \n", nonce);

    /*
    WARNING: The creation of BLEAdvertisementData takes a lot of time, about 600ms.
    This is tolerable for a proof-of-concept, yet has to be taken into account for deep-sleep duration!
    The underlying library to set BLE Advertisement Data (https://github.com/espressif/arduino-esp32/blob/master/libraries/BLE/src/BLEAdvertising.cpp) unfortunately
    needs this object (reference).
    For runtime (and therefore energy-comsumption) improvements, consider to improve the underlying library.

    (by changing/enabling BLEAdvertising::setAdvertisementData to take a std::string plus length as input, instead of a BLEAdvertisementData object.)
    */
    BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();

    if (initialized && bootcount < 10) {
        bootcount++;

        Serial.printf("Nonce: %i\n", nonce);
        /*it is unchanged!
        do not re-calculate it for power savings! */
        std::string adv = "";
        for (char c : lastStrAdvData) adv += c;
        oAdvertisementData.addData(adv);
    } else {
        initialized = true;

        nonce++;
        bootcount = 0;

        Serial.println("[INITIALIZED!]");
        SMTTBeacon beacon = SMTTBeacon(BEACON_ID, BEACON_PSK);

        std::string tmp_sc = beacon.generateSecret(nonce);
        //Serial.printf("[SECRET LENGTH] %d \n", tmp_sc.length());

        std::string strBeacon = beacon.getBeacon();

        Advertisement adv = Advertisement();
        adv.setFlags(0x06);
        adv.setCompanyID();
        std::string strAdvData = adv.generateAdvertisementData(strBeacon);

        oAdvertisementData.addData(strAdvData);

        /*
        The beacon string will only change after 10 boots (10 rounds of specified deep sleep + ~0.7s calculation and broadcasting time.)
        We story it in RTC memory to reuse it, to avoid recalculating it every time.
        */
        std::copy(strAdvData.begin(), strAdvData.end(), lastStrAdvData);
        //for (int i : strAdvData) Serial.printf("%01x", i);
    }

    // Create the BLE Device
    BLEDevice::init("");

    pAdvertising = BLEDevice::getAdvertising();

    pAdvertising->setAdvertisementType(ADV_TYPE_NONCONN_IND);  //have to to this to indicate not connectable
    pAdvertising->setMinInterval(800);                         //1.25
    pAdvertising->setMaxInterval(1200);                         //*1.25 = 666,25
    pAdvertising->setAdvertisementData(oAdvertisementData);
    //pAdvertising->setScanResponseData(oScanResponseData);

    // Start advertising
    pAdvertising->start();
    //Serial.println("Advertizing started...");
    delay(300);
    pAdvertising->stop();
    //Serial.printf("enter deep sleep\n");
    Serial.printf("Current millis: %lu \n", millis());
    delay(50);
    esp_deep_sleep(100000LL * GPIO_DEEP_SLEEP_DURATION);
    Serial.printf("in deep sleep\n");
}

void loop() {
}
