package com.example.smtt;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class ProofOfLocation extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5060405161193038038061193083398101604081905261002f91610054565b600280546001600160a01b0319166001600160a01b0392909216919091179055610084565b60006020828403121561006657600080fd5b81516001600160a01b038116811461007d57600080fd5b9392505050565b61189d806100936000396000f3fe60806040526004361061004a5760003560e01c806306fc3ba11461004f5780634e4f899914610071578063731eab00146100a9578063739871e1146100c9578063cbeb9ed5146100dc575b600080fd5b34801561005b57600080fd5b5061006f61006a36600461120f565b61014a565b005b34801561007d57600080fd5b5061009161008c366004611307565b610929565b604051600f9190910b81526020015b60405180910390f35b3480156100b557600080fd5b5061006f6100c436600461134b565b610b21565b61006f6100d73660046113a4565b610ccb565b3480156100e857600080fd5b506101246100f73660046113e5565b60006020819052908152604090208054600182015460029092015490916001600160a01b031690600f0b83565b604080519384526001600160a01b039092166020840152600f0b908201526060016100a0565b604080518082019182905260025463044de33560e41b90925260009181906001600160a01b03166344de33506101868686601f6044870161145e565b60206040518083038186803b15801561019e57600080fd5b505afa1580156101b2573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906101d69190611483565b815260025460405163044de33560e41b81526020928301926001600160a01b03909216916344de335091610211918891603f9060040161145e565b60206040518083038186803b15801561022957600080fd5b505afa15801561023d573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906102619190611483565b905233600090815260016020908152604080832080548251818502810185019093528083529495509293909291849084015b8282101561037a57838290600052602060002090600302016040518060600160405290816000820180546102c69061149c565b80601f01602080910402602001604051908101604052809291908181526020018280546102f29061149c565b801561033f5780601f106103145761010080835404028352916020019161033f565b820191906000526020600020905b81548152906001019060200180831161032257829003601f168201915b50505091835250506001828101546020808401919091526002909301546001600160a01b031660409092019190915291835292019101610293565b50503360009081526020819052604081205493945091508190505b87518110156108645760008882815181106103b2576103b26114d7565b6020026020010151805190602001209050848883815181106103d6576103d66114d7565b6020026020010151815181106103ee576103ee6114d7565b602002602001015160200151811461044d5760405162461bcd60e51b815260206004820181905260248201527f4861736873206f66207369676874696e6720646f6573206e6f74206d6174636860448201526064015b60405180910390fd5b60006040518060400160405280600260009054906101000a90046001600160a01b03166001600160a01b03166344de33508d8781518110610490576104906114d7565b60200260200101516000601f6040518463ffffffff1660e01b81526004016104ba9392919061145e565b60206040518083038186803b1580156104d257600080fd5b505afa1580156104e6573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061050a9190611483565b8152602001600260009054906101000a90046001600160a01b03166001600160a01b03166344de33508d8781518110610545576105456114d7565b60200260200101516020603f6040518463ffffffff1660e01b815260040161056f9392919061145e565b60206040518083038186803b15801561058757600080fd5b505afa15801561059b573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906105bf9190611483565b905260025481516020808401518b51918c0151604051639f287d7960e01b815260048101949094526024840191909152604483019190915260648201529192506000916001600160a01b0390911690639f287d799060840160206040518083038186803b15801561062f57600080fd5b505afa158015610643573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061066791906114ed565b6002548c5191925060009188916001600160a01b0316906344de3350908f9089908110610696576106966114d7565b60200260200101516040605f6040518463ffffffff1660e01b81526004016106c09392919061145e565b60206040518083038186803b1580156106d857600080fd5b505afa1580156106ec573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906107109190611483565b61071a9190611526565b90506000808212156107395761073282600019611565565b905061073c565b50805b6000600260009054906101000a90046001600160a01b03166001600160a01b03166373b9aa918b8f8a81518110610775576107756114d7565b60200260200101518151811061078d5761078d6114d7565b6020026020010151604001516040518263ffffffff1660e01b81526004016107c491906001600160a01b0391909116815260200190565b60006040518083038186803b1580156107dc57600080fd5b505afa1580156107f0573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f191682016040526108189190810190611637565b925050506000846001600160801b0316905061083e8382846001600160801b0316610929565b610848908a61169e565b985050505050505050808061085c906116ed565b915050610395565b503360008181526020819052604090819020600280820180546001600160801b0319166001600160801b038716179055546001909101549151632a335cc160e01b81526001600160a01b0391821693632a335cc1936108c99316918a90600401611708565b600060405180830381600087803b1580156108e357600080fd5b505af11580156108f7573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f1916820160405261091f919081019061173d565b5050505050505050565b600083610937575080610b1a565b600061094285610f12565b9050600061097161095d61095886611c20611772565b610f12565b61096c61095889611c20611791565b610f30565b905083600f0b61098382600f0b610f89565b600f0b13156109985761099584610f12565b90505b6000610ac183610abc610aa5600260009054906101000a90046001600160a01b03166001600160a01b03166372de5b2f6040518163ffffffff1660e01b815260040160206040518083038186803b1580156109f257600080fd5b505afa158015610a06573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610a2a91906117a9565b6001600160a01b031663d21e19936040518163ffffffff1660e01b815260040160206040518083038186803b158015610a6257600080fd5b505afa158015610a76573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610a9a91906117df565b63ffffffff16610f12565b61096c610ab26024610f12565b61096c600a610f12565b610fbe565b90506000610ad782610ad289610f12565b610ff4565b90506000610af4610aec85610abc8587610f30565b600f0b610f89565b9050600081600f0b1215610b1057650bf15f412f319550610b14565b8095505b50505050505b9392505050565b60005b6001600160a01b038416600090815260016020526040902054811015610bfd576001600160a01b0384166000908152600160205260409020805433919083908110610b7157610b716114d7565b60009182526020909120600260039092020101546001600160a01b03161415610beb5760405162461bcd60e51b815260206004820152602660248201527f596f752063616e742075706c6f6164206d6f7265207468616e206f6e65207369604482015265676874696e6760d01b6064820152608401610444565b80610bf5816116ed565b915050610b24565b506001600160a01b0383166000908152600160208181526040808420815160608101835287815280840187905233928101929092528054938401815584529281902083518051600390940290910192610c599284920190611027565b5060208201516001820155604091820151600290910180546001600160a01b0319166001600160a01b039283161790559051908416907f40bbe6a2e1f88ca376bf5b1eeacb78f8214af21e978086a193f586671d22799c90610cbe90859085906117fa565b60405180910390a2505050565b60025460405163dbdabbd760e01b81526001600160a01b038581166004830152600092169063dbdabbd79060240160606040518083038186803b158015610d1157600080fd5b505afa158015610d25573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610d49919061181c565b509150506001600160a01b0383163314610da55760405162461bcd60e51b815260206004820152601760248201527f596f7520617265206e6f7420746865207769746e6573730000000000000000006044820152606401610444565b60008111610df55760405162461bcd60e51b815260206004820152601b60248201527f5472757374666163746f72206e6565647320746f206265203e203000000000006044820152606401610444565b604080516060810182528381526001600160a01b03868116602080840182815260008587018181523380835293829052908790209551865590516001860180546001600160a01b03191691861691909117905551600294850180546001600160801b0319166001600160801b0390921691909117905592549351634125ea6560e01b8152600481019190915260248101929092529190911690634125ea6590604401600060405180830381600087803b158015610eb157600080fd5b505af1158015610ec5573d6000803e3d6000fd5b50505050826001600160a01b03167fddeb810a189658ec5df73810fff1c817d43ceb92480fe2b097b8f2b05a38ee8a83604051610f0491815260200190565b60405180910390a250505050565b6000677fffffffffffffff821115610f2957600080fd5b5060401b90565b600081600f0b60001415610f4357600080fd5b600082600f0b604085600f0b901b81610f5e57610f5e611851565b05905060016001607f1b03198112801590610f80575060016001607f1b038113155b610b1a57600080fd5b6000604082901d60016001607f1b03198112801590610faf575060016001607f1b038113155b610fb857600080fd5b92915050565b6000600f83810b9083900b0260401d60016001607f1b03198112801590610f80575060016001607f1b03811315610b1a57600080fd5b6000600f82810b9084900b0360016001607f1b03198112801590610f80575060016001607f1b03811315610b1a57600080fd5b8280546110339061149c565b90600052602060002090601f016020900481019282611055576000855561109b565b82601f1061106e57805160ff191683800117855561109b565b8280016001018555821561109b579182015b8281111561109b578251825591602001919060010190611080565b506110a79291506110ab565b5090565b5b808211156110a757600081556001016110ac565b634e487b7160e01b600052604160045260246000fd5b604051601f8201601f1916810167ffffffffffffffff811182821017156110ff576110ff6110c0565b604052919050565b600067ffffffffffffffff821115611121576111216110c0565b5060051b60200190565b600067ffffffffffffffff821115611145576111456110c0565b50601f01601f191660200190565b600082601f83011261116457600080fd5b81356111776111728261112b565b6110d6565b81815284602083860101111561118c57600080fd5b816020850160208301376000918101602001919091529392505050565b600082601f8301126111ba57600080fd5b813560206111ca61117283611107565b82815260059290921b840181019181810190868411156111e957600080fd5b8286015b8481101561120457803583529183019183016111ed565b509695505050505050565b60008060006060848603121561122457600080fd5b833567ffffffffffffffff8082111561123c57600080fd5b818601915086601f83011261125057600080fd5b8135602061126061117283611107565b82815260059290921b8401810191818101908a84111561127f57600080fd5b8286015b848110156112b75780358681111561129b5760008081fd5b6112a98d86838b0101611153565b845250918301918301611283565b50975050870135925050808211156112ce57600080fd5b6112da878388016111a9565b935060408601359150808211156112f057600080fd5b506112fd86828701611153565b9150509250925092565b60008060006060848603121561131c57600080fd5b505081359360208301359350604090920135919050565b6001600160a01b038116811461134857600080fd5b50565b60008060006060848603121561136057600080fd5b833561136b81611333565b9250602084013567ffffffffffffffff81111561138757600080fd5b61139386828701611153565b925050604084013590509250925092565b6000806000606084860312156113b957600080fd5b83356113c481611333565b925060208401356113d481611333565b929592945050506040919091013590565b6000602082840312156113f757600080fd5b8135610b1a81611333565b60005b8381101561141d578181015183820152602001611405565b8381111561142c576000848401525b50505050565b6000815180845261144a816020860160208601611402565b601f01601f19169290920160200192915050565b6060815260006114716060830186611432565b60208301949094525060400152919050565b60006020828403121561149557600080fd5b5051919050565b600181811c908216806114b057607f821691505b602082108114156114d157634e487b7160e01b600052602260045260246000fd5b50919050565b634e487b7160e01b600052603260045260246000fd5b6000602082840312156114ff57600080fd5b815180600f0b8114610b1a57600080fd5b634e487b7160e01b600052601160045260246000fd5b60008083128015600160ff1b85018412161561154457611544611510565b6001600160ff1b038401831381161561155f5761155f611510565b50500390565b60006001600160ff1b038184138284138082168684048611161561158b5761158b611510565b600160ff1b60008712828116878305891216156115aa576115aa611510565b600087129250878205871284841616156115c6576115c6611510565b878505871281841616156115dc576115dc611510565b505050929093029392505050565b600082601f8301126115fb57600080fd5b81516116096111728261112b565b81815284602083860101111561161e57600080fd5b61162f826020830160208701611402565b949350505050565b60008060006060848603121561164c57600080fd5b835167ffffffffffffffff81111561166357600080fd5b61166f868287016115ea565b9350506020840151915060408401516001600160801b038116811461169357600080fd5b809150509250925092565b600081600f0b83600f0b600082128260016001607f1b03038213811516156116c8576116c8611510565b8260016001607f1b03190382128116156116e4576116e4611510565b50019392505050565b600060001982141561170157611701611510565b5060010190565b6001600160a01b0384811682528316602082015260606040820181905260009061173490830184611432565b95945050505050565b60006020828403121561174f57600080fd5b815167ffffffffffffffff81111561176657600080fd5b61162f848285016115ea565b600081600019048311821515161561178c5761178c611510565b500290565b600082198211156117a4576117a4611510565b500190565b6000602082840312156117bb57600080fd5b8151610b1a81611333565b805163ffffffff811681146117da57600080fd5b919050565b6000602082840312156117f157600080fd5b610b1a826117c6565b60408152600061180d6040830185611432565b90508260208301529392505050565b60008060006060848603121561183157600080fd5b8351925060208401519150611848604085016117c6565b90509250925092565b634e487b7160e01b600052601260045260246000fdfea2646970667358221220f156a6e74eb163adbcd970007ca3c54e430438dfe01b0d83821526a2dcebf1cf64736f6c63430008090033";

    public static final String FUNC_CALCULATETRUSTFACTOR = "calculateTrustFactor";

    public static final String FUNC_END = "end";

    public static final String FUNC_INITIATE = "initiate";

    public static final String FUNC_POLSTART = "polStart";

    public static final String FUNC_REPORTSIGHTINGOFWITNESS = "reportSightingOfWitness";

    public static final Event SEARCHWITNESS_EVENT = new Event("SearchWitness", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SIGHTINGREPORTED_EVENT = new Event("SightingReported", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Bytes32>() {}));
    ;

    @Deprecated
    protected ProofOfLocation(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ProofOfLocation(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ProofOfLocation(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ProofOfLocation(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<SearchWitnessEventResponse> getSearchWitnessEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SEARCHWITNESS_EVENT, transactionReceipt);
        ArrayList<SearchWitnessEventResponse> responses = new ArrayList<SearchWitnessEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SearchWitnessEventResponse typedResponse = new SearchWitnessEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.adressWitness = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SearchWitnessEventResponse> searchWitnessEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SearchWitnessEventResponse>() {
            @Override
            public SearchWitnessEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SEARCHWITNESS_EVENT, log);
                SearchWitnessEventResponse typedResponse = new SearchWitnessEventResponse();
                typedResponse.log = log;
                typedResponse.adressWitness = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SearchWitnessEventResponse> searchWitnessEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SEARCHWITNESS_EVENT));
        return searchWitnessEventFlowable(filter);
    }

    public List<SightingReportedEventResponse> getSightingReportedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SIGHTINGREPORTED_EVENT, transactionReceipt);
        ArrayList<SightingReportedEventResponse> responses = new ArrayList<SightingReportedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SightingReportedEventResponse typedResponse = new SightingReportedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.addrW = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.encryptedSighting = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.sightingHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SightingReportedEventResponse> sightingReportedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SightingReportedEventResponse>() {
            @Override
            public SightingReportedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SIGHTINGREPORTED_EVENT, log);
                SightingReportedEventResponse typedResponse = new SightingReportedEventResponse();
                typedResponse.log = log;
                typedResponse.addrW = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.encryptedSighting = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.sightingHash = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SightingReportedEventResponse> sightingReportedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SIGHTINGREPORTED_EVENT));
        return sightingReportedEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> calculateTrustFactor(BigInteger deltaT, BigInteger deltaS, BigInteger stake) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_CALCULATETRUSTFACTOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(deltaT), 
                new org.web3j.abi.datatypes.generated.Uint256(deltaS), 
                new org.web3j.abi.datatypes.generated.Uint256(stake)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int128>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> end(List<byte[]> decryptedSightings, List<BigInteger> indices, byte[] itemLocation) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_END, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.DynamicBytes>(
                        org.web3j.abi.datatypes.DynamicBytes.class,
                        org.web3j.abi.Utils.typeMap(decryptedSightings, org.web3j.abi.datatypes.DynamicBytes.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicBytes(itemLocation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initiate(String addrA, String addressWitness, BigInteger timestamp) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_INITIATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, addrA), 
                new org.web3j.abi.datatypes.Address(160, addressWitness), 
                new org.web3j.abi.datatypes.generated.Uint256(timestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple3<BigInteger, String, BigInteger>> polStart(String param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_POLSTART, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Int128>() {}));
        return new RemoteFunctionCall<Tuple3<BigInteger, String, BigInteger>>(function,
                new Callable<Tuple3<BigInteger, String, BigInteger>>() {
                    @Override
                    public Tuple3<BigInteger, String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<BigInteger, String, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> reportSightingOfWitness(String addrW, byte[] encryptedSighting, byte[] sightingHash) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTSIGHTINGOFWITNESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, addrW), 
                new org.web3j.abi.datatypes.DynamicBytes(encryptedSighting), 
                new org.web3j.abi.datatypes.generated.Bytes32(sightingHash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ProofOfLocation load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ProofOfLocation(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ProofOfLocation load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ProofOfLocation(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ProofOfLocation load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ProofOfLocation(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ProofOfLocation load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ProofOfLocation(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ProofOfLocation> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String parent) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, parent)));
        return deployRemoteCall(ProofOfLocation.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ProofOfLocation> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String parent) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, parent)));
        return deployRemoteCall(ProofOfLocation.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ProofOfLocation> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String parent) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, parent)));
        return deployRemoteCall(ProofOfLocation.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ProofOfLocation> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String parent) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, parent)));
        return deployRemoteCall(ProofOfLocation.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class SearchWitnessEventResponse extends BaseEventResponse {
        public String adressWitness;

        public BigInteger timestamp;
    }

    public static class SightingReportedEventResponse extends BaseEventResponse {
        public String addrW;

        public byte[] encryptedSighting;

        public byte[] sightingHash;
    }
}
