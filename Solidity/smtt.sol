// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.8.0 <0.9.0;

import "./ProofOfLocation.sol";
import "./constants.sol";
import "./MathLibrary.sol";

contract SMTT {
    mapping(address => Account) public account; //wallet-ID -> (stake)
    mapping(uint32 => address) public ethAddress;
    mapping(address => SearchOrder) public searchOrders; // namespace -> SearchOrder
    mapping(address => Sighting[]) public reportedSighting; //namespace -> ReportedSighting
    mapping(address => bool) public secretRevealed;
    ProofOfLocation locationProofs;
    Constants public constants;

    event OrderCreated(
        address indexed addr,
        uint256 trustfactor,
        uint256 reward
    ); //Adresse von Auftraggeber, TF, r

    event ReportSighting(
        address indexed addrA,
        address addrW,
        uint256 timestamp
    ); //Adresse von Auftraggeber, t, h_s

    event ReportSecret(address indexed addr, bytes foundSecret);

    event SearchFinished(address indexed addr, bytes pos);

    struct Account {
        string publicKey;
        uint256 identifier;
        uint128 stake;
    }

    struct SearchOrder {
        uint256 trustFactor;
        uint256 reward;
        uint32 minNonce;
    }

    struct Sighting {
        address wittness;
        uint32 nonce;
        bytes32 hashedSighting;
        bytes secret;
        bool startedPOL;
    }

    constructor() {
        locationProofs = new ProofOfLocation(this);
        constants = new Constants();
    }

    function getProofOfLocationContract() public view returns (address pol) {
        return address(locationProofs);
    }

    function register(string memory publicKey, uint32 identifier)
        public
        payable
    {
        require(msg.value == 0.02 ether, "You have to send 0.02 Ether");
        require(
            ethAddress[identifier] == address(0x0),
            "Identifier already exists"
        );
        require(account[msg.sender].stake == 0, "You have already registered.");

        account[msg.sender] = Account(
            publicKey,
            identifier,
            constants.initialStake()
        );
        ethAddress[identifier] = msg.sender;
    }

    function startSearchTask(uint256 trustfactor, uint32 minNonce)
        public
        payable
    {
        require(account[msg.sender].stake != 0, "Your are not registered.");
        require(minNonce > 0, "Nonce cannot be 0");
        require(
            searchOrders[msg.sender].minNonce == 0,
            "You already have a search task"
        );

        searchOrders[msg.sender] = SearchOrder(
            trustfactor,
            msg.value,
            minNonce
        );

        emit OrderCreated(msg.sender, trustfactor, msg.value);
    }

    function reportHashedSighting(
        address addrA,
        uint32 nonce,
        bytes32 hashedSighting
    ) public {
        require(!secretRevealed[addrA], "Secret already revealed");
        require(nonce > searchOrders[addrA].minNonce, "Nonce is too smal");
        require(
            !getSightingIndexBool(addrA, msg.sender),
            "You already reported a sighting"
        );

        reportedSighting[addrA].push(
            Sighting(msg.sender, nonce, hashedSighting, "0x00", false)
        );
        emit ReportSighting(addrA, msg.sender, nonce); //Adresse von Auftraggeber, Adresse von Zeuge und timestamp
    }

    function getSightingIndex(address addrA, address addressW)
        public
        view
        returns (uint256 index)
    {
        for (uint256 i = 0; i < reportedSighting[addrA].length; i++) {
            if (reportedSighting[addrA][i].wittness == addressW) {
                return i;
            }
        }
        require(false, "No sighting reported");
    }

    function getSightingIndexBool(address addrA, address addressW)
        public
        view
        returns (bool hasReported)
    {
        for (uint256 i = 0; i < reportedSighting[addrA].length; i++) {
            if (reportedSighting[addrA][i].wittness == addressW) {
                return true;
            }
        }
        return false;
    }

    function activatePOL(address addrA, address addrW) public {
        //        require(addrW == msg.sender || addrW == address(locationProofs), "You are not the wittness");
        require(
            msg.sender == address(locationProofs),
            "Only the Proof Of Location Contract is allowd to use this method."
        );
        require(
            getSightingIndexBool(addrA, addrW),
            "You have not reported a sighting yet"
        );
        uint256 index = getSightingIndex(addrA, addrW);
        reportedSighting[addrA][index].startedPOL = true;
    }

    function revealSecret(address addrW, bytes memory foundSecret) public {
        require(
            getSightingIndexBool(msg.sender, addrW),
            "No sighting reported yet"
        );
        uint256 index = getSightingIndex(msg.sender, addrW);
        reportedSighting[msg.sender][index].secret = foundSecret;
        secretRevealed[msg.sender] = true;
        emit ReportSecret(addrW, foundSecret);
    }

    function senderInBytes() public view returns (bytes memory t) {
        return abi.encode(msg.sender);
    }

    function hashStuff(
        bytes memory id,
        uint32 nonce,
        bytes memory secret,
        bytes memory position,
        address addres
    ) public pure returns (bytes32 t) {
        bytes memory unhashedSighting = bytes.concat(
            id,
            abi.encode(nonce),
            secret,
            position,
            abi.encode(addres)
        );
        return keccak256(unhashedSighting);
    }

    function hashAddress(address addres) public pure returns (bytes32 t) {
        bytes memory unhashedSighting = bytes.concat(abi.encode(addres));
        return keccak256(unhashedSighting);
    }

    function finishSearch(
        address addrA,
        address addrW,
        bytes memory position
    ) public returns (string memory info) {
        if (msg.sender != address(locationProofs)) {
            require(
                msg.sender == addrW,
                "You have to insert your own address for addrw"
            );
        }
        uint256 index = getSightingIndex(addrA, addrW);
        if (searchOrders[addrA].trustFactor > 0) {
            (, , int128 tfFromCalc) = locationProofs.polStart(addrW);
            require(
                reportedSighting[addrA][index].startedPOL,
                "Proof of locatoin not yet started"
            );
            require(
                tfFromCalc >= int128(int256(searchOrders[addrA].trustFactor)),
                "Trustfactor has not been reached"
            );
        }
        // No proof of location needed as trust factor is 0
        // Continue in original protocol
        bytes memory secret = reportedSighting[addrA][index].secret;

        bytes memory unhashedSighting = bytes.concat(
            abi.encode(account[addrA].identifier),
            abi.encode(reportedSighting[addrA][index].nonce),
            secret,
            position,
            abi.encode(addrW)
            //            senderInBytes()
        );

        bytes32 witnessHash = keccak256(unhashedSighting);

        bytes32 witnessControllHash = reportedSighting[addrA][index]
            .hashedSighting;

        require(witnessHash == witnessControllHash, "Hashes do no not match!");
        if (witnessHash == witnessControllHash) {
            payable(address(addrW)).transfer(searchOrders[addrA].reward);
            delete reportedSighting[addrA];
            delete searchOrders[addrA];
            secretRevealed[addrA] = false;
            emit SearchFinished(addrA, position);
            return "Success!";
        } else {
            return "Ooops, something went wrong. Please try again later";
        }
    }

    function uintFromBytes(
        bytes memory input,
        uint256 start,
        uint256 end
    ) public pure returns (uint256 output) {
        uint256 outp = 0;

        for (uint256 i = start; i < end + 1; i++) {
            uint256 walk = i - start;
            outp += uint8(input[i]) * (2**(8 * (32 - (walk + 1))));
        }

        return outp;
    }

    function distanceInMeter(
        uint256 x1,
        uint256 y1,
        uint256 x2,
        uint256 y2
    ) public pure returns (int128 output) {
        int128 x1_f = ABDKMath64x64.fromUInt(x1);
        int128 y1_f = ABDKMath64x64.fromUInt(y1);
        int128 x2_f = ABDKMath64x64.fromUInt(x2);
        int128 y2_f = ABDKMath64x64.fromUInt(y2);

        x1_f = ABDKMath64x64.div(x1_f, ABDKMath64x64.fromUInt(10**8));
        y1_f = ABDKMath64x64.div(y1_f, ABDKMath64x64.fromUInt(10**8));
        x2_f = ABDKMath64x64.div(x2_f, ABDKMath64x64.fromUInt(10**8));
        y2_f = ABDKMath64x64.div(y2_f, ABDKMath64x64.fromUInt(10**8));

        int128 a = ABDKMath64x64.fromInt(71);
        int128 b = ABDKMath64x64.fromInt(113);

        int128 gepiiit = ABDKMath64x64.div(
            ABDKMath64x64.fromUInt(17453292519943295),
            ABDKMath64x64.fromUInt(10**18)
        );
        int128 lat = ABDKMath64x64.mul(
            ABDKMath64x64.div(
                ABDKMath64x64.add(y1_f, y2_f),
                ABDKMath64x64.fromInt(2)
            ),
            gepiiit
        );

        a = ABDKMath64x64.add(
            a,
            ABDKMath64x64.div(
                ABDKMath64x64.fromInt(1),
                ABDKMath64x64.fromInt(2)
            )
        );
        b = ABDKMath64x64.add(
            b,
            ABDKMath64x64.div(
                ABDKMath64x64.fromInt(3),
                ABDKMath64x64.fromInt(10)
            )
        );

        int128 dx = ABDKMath64x64.mul(b, ABDKMath64x64.sub(x1_f, x2_f));
        int128 dy = ABDKMath64x64.mul(a, ABDKMath64x64.sub(y1_f, y2_f));

        dx = ABDKMath64x64.mul(dx, taylor(lat));

        dx = ABDKMath64x64.pow(dx, 2);
        dy = ABDKMath64x64.pow(dy, 2);

        int128 ff = ABDKMath64x64.toInt(dx);
        ff = ABDKMath64x64.toInt(dy);
        ff = ABDKMath64x64.toInt(ABDKMath64x64.add(dx, dy));
        int128 out = ABDKMath64x64.sqrt(ABDKMath64x64.add(dx, dy));

        ff = ABDKMath64x64.toInt(
            ABDKMath64x64.mul(out, ABDKMath64x64.fromUInt(1000))
        );

        return ff;
    }

    function taylor(int128 x) public pure returns (int128 output) {
        uint128 fak = 1;
        for (uint128 i = 0; i < 12; i++) {
            if (i > 0) {
                fak *= i;
            }
            if (int128(i) % 2 == 1) {
                continue;
            }

            int128 loopX = ABDKMath64x64.pow(x, i);
            int128 factor = ABDKMath64x64.div(
                ABDKMath64x64.fromInt(1),
                ABDKMath64x64.fromUInt(fak)
            );
            if ((int128(i) - 2) % 4 == 0) {
                factor = ABDKMath64x64.mul(factor, ABDKMath64x64.fromInt(-1));
            }
            output += ABDKMath64x64.mul(factor, loopX);
        }
    }
}
