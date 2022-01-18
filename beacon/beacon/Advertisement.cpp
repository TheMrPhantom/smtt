#include "./Advertisement.h"

#include <iostream>
#include <stdexcept>
#include <string>


Advertisement::Advertisement() {
    flags = "";
    companyID = "";
}

void Advertisement::setFlags(int f) {
    flags += (char)0x02;  //length of data to come
    flags += (char)0x01;  //type of flags
    flags += (char)f;     //actual flags
}

void Advertisement::setCompanyID() {
    companyID += (char)0xF0;  //LSB of CompanyID
    companyID += (char)0x0D;  //MSB of CompanyID
}

std::string Advertisement::generateAdvertisementData(std::string payload) {
    //0xFF to indicate that data to come is manufacturer data.
    std::string payloadData = (char)0xFF + companyID + payload;
    int payloadLength = payloadData.length();

    std::string advData = "";

    advData += flags;
    advData += (char)payloadLength;
    advData += payloadData;

    return advData;
}
