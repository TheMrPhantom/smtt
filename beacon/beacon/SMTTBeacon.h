#include <Arduino.h>
#include <stdio.h>
#include <string.h>

#include <array>
#include <cstddef>
#include <stdexcept>

#include "mbedtls/md.h"

#define IDENTIFIER_LENGTH 4
#define SECRET_LENGTH 16
#define NONCE_LENGTH 4
#define PSK_LENGTH 128

class SMTTBeacon {
   private:
    std::string id;
    uint32_t nonce;
    std::string secret;

    std::string psk;

   public:
    SMTTBeacon(std::string _id, std::string _psk);

    std::string generateSecret(uint32_t _nonce);

    std::string getBeacon();

    std::string getNonceAsString();

    std::array<char, 4> getNonceAsCharArray();
};
