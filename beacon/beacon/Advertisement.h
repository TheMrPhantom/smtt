#pragma once
#include <iostream>
#include <stdexcept>
#include <string>

#include "./SMTTBeacon.h"

class Advertisement {
   private:
    std::string flags;
    std::string companyID;

   public:
    Advertisement();
    void setFlags(int f);
    void setCompanyID();
    std::string generateAdvertisementData(std::string payload);
};
