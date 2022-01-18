// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.8.0 <0.9.0;
import "./smtt.sol";
import "./constants.sol";
import "./MathLibrary.sol";

contract ProofOfLocation {
    mapping(address => POLStart) public polStart; //namespace -> public key
    mapping(address => EncryptedSighting[]) sightingsOfWitness; //Address of Witness -> EncrypedSighting

    struct EncryptedSighting {
        bytes cypherText;
        bytes32 sightingHash;
        address anchor;
    }

    struct POLStart {
        uint256 timestamp;
        address addrA;
        int128 reachedTF;
    }

    event SearchWitness(address indexed adressWitness, uint256 timestamp);
    event SightingReported(
        address indexed addrW,
        bytes encryptedSighting,
        bytes32 sightingHash
    );

    SMTT smtt;

    constructor(SMTT parent) {
        smtt = parent;
    }

    function initiate(
        address addrA,
        address addressWitness,
        uint256 timestamp
    ) public payable {
        (, uint256 tf, ) = smtt.searchOrders(addrA);
        require(addressWitness == msg.sender, "You are not the witness");
        require(tf > 0, "Trustfactor needs to be > 0");
        polStart[msg.sender] = POLStart(timestamp, addrA, 0);
        smtt.activatePOL(addrA, msg.sender);
        //publicKeys[addressWitness] = publicKey;
        emit SearchWitness(addressWitness, timestamp);
    }

    function reportSightingOfWitness(
        address addrW,
        bytes memory encryptedSighting,
        bytes32 sightingHash
    ) public {
        for (uint256 i = 0; i < sightingsOfWitness[addrW].length; i++) {
            require(
                sightingsOfWitness[addrW][i].anchor != msg.sender,
                "You cant upload more than one sighting"
            );
        }
        sightingsOfWitness[addrW].push(
            EncryptedSighting(encryptedSighting, sightingHash, msg.sender)
        );
        emit SightingReported(addrW, encryptedSighting, sightingHash);
    }

    function end(
        bytes[] memory decryptedSightings,
        uint256[] memory indices,
        bytes memory itemLocation
    ) public {
        uint256[2] memory xyWitness = [
            smtt.uintFromBytes(itemLocation, 0, 31),
            smtt.uintFromBytes(itemLocation, 32, 63)
        ];

        EncryptedSighting[] memory sightings = sightingsOfWitness[msg.sender];
        uint256 polStartTime = polStart[msg.sender].timestamp;
        int128 tf = 0;
        for (uint256 i = 0; i < decryptedSightings.length; i++) {
            bytes32 hashedSighting = keccak256(decryptedSightings[i]);
            require(
                hashedSighting == sightings[indices[i]].sightingHash,
                "Hashs of sighting does not match"
            );
            uint256[2] memory xyAnchor = [
                smtt.uintFromBytes(decryptedSightings[i], 0, 31),
                smtt.uintFromBytes(decryptedSightings[i], 32, 63)
            ];

            int128 deltaS = smtt.distanceInMeter(
                xyAnchor[0],
                xyAnchor[1],
                xyWitness[0],
                xyWitness[1]
            ); //In meters * 10^11

            int256 deltaT_signed = int256(
                smtt.uintFromBytes(decryptedSightings[i], 64, 95)
            ) - int256(polStartTime);

            uint256 deltaT = 0;

            if (deltaT_signed < 0) {
                deltaT = uint256(deltaT_signed * (-1));
            } else {
                deltaT = uint256(deltaT_signed);
            }
            (, , uint128 stake) = smtt.account(sightings[indices[i]].anchor);
            uint256 u_deltaS = uint128(deltaS);
            tf += calculateTrustFactor(deltaT, u_deltaS, stake);
        }

        polStart[msg.sender].reachedTF = tf;
        //        polStart[msg.sender].reachedTF = 20;
        smtt.finishSearch(polStart[msg.sender].addrA, msg.sender, itemLocation);
    }

    //Call with params multiplied by 10^11 because there are no floats
    function calculateTrustFactor(
        uint256 deltaT,
        uint256 deltaS,
        uint256 stake
    ) public pure returns (int128 tf) {
        if (deltaT == 0) {
            return int128(uint128(stake));
        }

        int128 convertedDeltaT = ABDKMath64x64.fromUInt(deltaT);

        int128 left = ABDKMath64x64.div(ABDKMath64x64.fromUInt(7200*stake),ABDKMath64x64.fromUInt(deltaT+7200));
        
        if (ABDKMath64x64.from128x128(left) > int128(uint128(stake))) {
            left = ABDKMath64x64.fromUInt(stake);
        }

        int128 rightDown = ABDKMath64x64.mul(
            convertedDeltaT,
            ABDKMath64x64.div(
                ABDKMath64x64.fromUInt(50),
                ABDKMath64x64.div(
                    ABDKMath64x64.fromUInt(36),
                    ABDKMath64x64.fromUInt(10)
                )
            )
        );

        int128 rightUp = ABDKMath64x64.sub(
            rightDown,
            ABDKMath64x64.fromUInt(deltaS)
        );
        
        int128 result = ABDKMath64x64.from128x128(
            ABDKMath64x64.mul(left, ABDKMath64x64.div(rightUp, rightDown))
        );

        if (result < 0) {
            tf = 13131313131313;
        } else {
            tf = result;
        }
        return tf;
    }
}
