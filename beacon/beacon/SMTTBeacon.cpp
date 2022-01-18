#include "SMTTBeacon.h"

SMTTBeacon::SMTTBeacon(std::string _id, std::string _psk) {
    if (_id.length() != IDENTIFIER_LENGTH) {
        throw std::invalid_argument("bad identifier length");
    }
    id = _id;

    if (_psk.length() != PSK_LENGTH) {
        throw std::invalid_argument("bad psk length");
    }
    psk = _psk;
}

std::string SMTTBeacon::generateSecret(uint32_t _nonce) {
    nonce = _nonce;
    //DATA_LENGTH specifies Data length in Bytes, it specifies length of id and nonce together
    int DATA_LENGTH = IDENTIFIER_LENGTH + NONCE_LENGTH;
    std::string data = id + getNonceAsString();

    char* key = new char[psk.length() + 1];
    strcpy(key, psk.c_str());

    char* payload = new char[DATA_LENGTH + 1];

    const char* idArray = id.c_str();
    const char* nonceArray = getNonceAsString().c_str();
    for (int i = 0; i < IDENTIFIER_LENGTH; i++) payload[i] = idArray[i];
    for (int i = 0; i < NONCE_LENGTH; i++) payload[i + IDENTIFIER_LENGTH] = nonceArray[i];

    uint8_t hmacResult[32];

    mbedtls_md_context_t ctx;
    mbedtls_md_type_t md_type = MBEDTLS_MD_SHA256;

    const size_t payloadLength = DATA_LENGTH;
    const size_t keyLength = strlen(key);

    //Using mbedtls to generate HMAC (SHA256)
    mbedtls_md_init(&ctx);
    mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(md_type), 1);
    mbedtls_md_hmac_starts(&ctx, (const unsigned char*)key, keyLength);
    mbedtls_md_hmac_update(&ctx, (const unsigned char*)payload, payloadLength);
    mbedtls_md_hmac_finish(&ctx, hmacResult);
    mbedtls_md_free(&ctx);

    std::string hmacAsString = "";
    for (int value : hmacResult) hmacAsString += (char)value;

    //Serial.printf("[FULL HMAC STRING] ");
    //for (int value : hmacResult) Serial.printf("%x", value);
    //Serial.println();

    secret = hmacAsString.substr(0, SECRET_LENGTH);  //only consider first SECRET_LENGTH bytes of the HMAC.
    return secret;
}

std::string SMTTBeacon::getBeacon() {
    //std::stringstream stream;
    //stream << std::hex << nonce;
    //std::string strNonce( stream.str() );
    uint32_t tmp = nonce;

    std::string strNonce = "";
    strNonce += (char)(tmp >> 24);
    strNonce += (char)(tmp >> 16);
    strNonce += (char)(tmp >> 8);
    strNonce += (char)tmp;
    return id + getNonceAsString() + secret;
}

std::string SMTTBeacon::getNonceAsString() {
    uint32_t tmp = nonce;
    std::string str = "";
    str += (char)(tmp >> 24);
    str += (char)(tmp >> 16);
    str += (char)(tmp >> 8);
    str += (char)(tmp);
    return str;
}

