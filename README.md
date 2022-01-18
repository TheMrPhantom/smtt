# Design and Implementation of a Secure Mobile Target Tracking System Using Smart Contracts
## Android
### Neccessary changes
Inside the RegisterActivity.java file you can change all neccesary data for the ethereum node address, user data and beacon data.

### Deploying the Master Contract
A Master Contract is used to sync the contract address between Android devices. This contract has to be deployed manually via the deployMasterContract Java project. This will return the address of the Master Contract, which has to set inside the BlockchainConnection.java file inside the Android project.

### Sideloading the Android App
To Sideload the App onto a Android device open the Android project inside of AndroidStudio, connect your device to pc and enable USB debugging inside the Developer Options.

### Deploying the Smart Contract
The Smart Contract can be deployed from the Android App. When creating a user Account click on Register and Deploy and a new contract will be theployed that all devices will use. to re-enter the Registration Activity press and hold the Reload Button in the main menu


## Bluetooth-Beacon
To retrieve lost mobile objects, mobile objects are tagged with a Bluetooth Low Energy (BLE) Beacon, whose implementation is found here.

They broadcast an **id** to identify a lost mobile object.
To proove having sighted a beacon, the beacon also broadcasts a **secret** in dependence ob a **nonce** (broadcasted as well).

Goal of this implementation is to have a reliable, cheap and energy-efficient bluetooth beacon to broadcast **id, nonce and secret** via Bluetooth Low Energy to support the _Secure Mobile Target Tracking Application_.

## How to run
The provided sketch can be run using the [Arduino IDE](https://www.arduino.cc/en/software) (tested with version 1.8.13). Unfortunately, the Arduino IDE does not support auto-completion.

To get auto completion and other convenient coding functionality, use [Visual Studio Code](https://code.visualstudio.com/download) and the [Arduino Extension](https://marketplace.visualstudio.com/items?itemName=vsciot-vscode.vscode-arduino).

Make sure the ESP32 is added in your board manager ([Tutorial](https://randomnerdtutorials.com/installing-the-esp32-board-in-arduino-ide-windows-instructions/)).

### Hardware Used
Powerful and popular hardware: ESP32 by Espressif.
Any board containing an
 ESP32 can be used (as long as an antenna is present).

The sketch was tested using the nodemcu-32s [NodeMCU Development Board](https://www.reichelt.de/nodemcu-esp32-wifi-und-bluetooth-modul-debo-jt-esp32-p219897.html).


### Energy Efficiency
To achieve a battery runtime, the ESP spends most of the time in power-efficient _deep sleep_ with a current consumption of only $10 \mu A$.

### Frame Format
Goal of the custom advertisement format is to have maximum space available to broadcast id, nonce and secret of the lost mobile object.

Bluetooth advertisements can have a maximum length of 31 Bytes (except for BLE Extended Advertisements, which are not supported by the ESP32).

BLE-Advertisements are structured as follows. Each element consists of:
* remaining length of this element
* advertisement data type
* data

The **31-byte payload** consists of
* **3 byte** to set flags (GeneralDiscoverable, BrEdrNotSupported)
* **28 byte** manufacturer data
    * **1 byte** remaining length
    * **1 byte** type manufacuter data (0xFF)
    * **2 byte** company identifier (0xFOOD)
    * **24 bytes** of payload, which consist of id, nonce and secret.

![](https://pad.stuvus.uni-stuttgart.de/uploads/upload_011655ae8b12c151d9f8c4d2a2fc94d3.PNG)

### Generation of the secret
What is called a 'secret' is actually a hash-based message authtication code (HMAC), which verifies the authenticity of id and nonce using a pre-shared key.
The generate the HMAC, the library [mbedtls](https://tls.mbed.org/) is used.

## Solidity
### Web3J-Library (Gradle)
- Java
  - `compile ('org.web3j:core:4.8.4')`
- Android
  - `compile ('org.web3j:core:4.6.0-android')`

### Solidity Compiler
Install Solidity Compiler
```bash
sudo add-apt-repository ppa:ethereum/ethereum;
sudo apt update;
sudo apt install solc;
```

### Web3j-Cli
Download and install Solidity to Java compiler on Linux
```bash
curl -L get.web3j.io | sh && source ~/.web3j/source.sh
```

## Compiling Smart-Contract to Java-Code
### Compile the Solidty-Smart-Contract file
```bash
solc <contract file> --bin --abi --optimize -o <output path>
```

### Compile compiled contract to Java-File
```bash
web3j generate solidity -b <path to bin> -a <path to abi> -o <output folder> -p <package where contract file will go>
```
