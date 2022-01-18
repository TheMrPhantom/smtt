package deploy;

import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class DeployMaster {
	public static void main(String[] args) throws Exception {
		String account = "faf90d856d74d3608938e4d584394b215bff40c3ba0a78e5b2efee5104480261";
		String nodeUrl = "http://10.53.101.186:7545";
		Credentials credentials = Credentials.create(account);
		Web3j web3j = Web3j.build(new HttpService(nodeUrl));
        int contractGasPrice = 50;
        ContractGasProvider gasProvider = new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                //System.out.println("function called: "+contractFunc);
                return Convert.toWei(Integer.toString(contractGasPrice), Unit.GWEI).toBigInteger();
            }

            @Override
            public BigInteger getGasPrice() {
                return Convert.toWei(Integer.toString(contractGasPrice), Unit.GWEI).toBigInteger();
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                //System.out.println("function called: "+contractFunc);
                return BigInteger.valueOf(30000000);
            }

            @Override
            public BigInteger getGasLimit() {
                return BigInteger.valueOf(30000000);
            }
        };
		
        AddressStorage contract = AddressStorage.deploy(web3j, credentials, gasProvider).send();
        //AddressStorage contract = AddressStorage.load("0x2c17c13a4f9b6ecc826e09ab1ffd8a8b41ce14d5", web3j, credentials, gasProvider);
        System.out.println("Master Contract: " + contract.getContractAddress());
        System.out.println("SMTT Contract  : " + contract.get().send());
	}

}
