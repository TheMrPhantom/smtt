// This code example is obtained from Solidity docs
pragma solidity >=0.8.0 <0.9.0;

contract AddressStorage {
    address smttAddress;

    function set(address x) public {
        smttAddress = x;
    }

    function get() public view returns (address) {
        return smttAddress;
    }
}