package com.example.smtt;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.ArrayUtils;
import org.web3j.abi.EventEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.utils.Convert;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert.Unit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class BlockchainConnection {
    private Web3j web3j;
    private Credentials credentials;
    private AddressStorage masterContract;
    private SMTT smttContract;
    private ProofOfLocation polContract;
    private ContractGasProvider gasProvider;
    private HashMap<String, BigInteger[]> ongoingSearchOrders = new HashMap<String, BigInteger[]>();
    private HashMap<String, LinkedList<Tuple2<String, BigInteger>>> reportedSightings = new HashMap<String, LinkedList<Tuple2<String, BigInteger>>>();
    private HashMap<String, LinkedList<Tuple2<byte[], byte[]>>> reportedPolSightings = new HashMap<String, LinkedList<Tuple2<byte[], byte[]>>>();
    //private Set<String, Tuple2<String, BigInteger>> reportedSightings = new HashMap<String, Tuple2<String, BigInteger>>();
    private Flowable searchTaskEventSubscriber;
    private Flowable sightingReportEventSubscriber;
    private Flowable searchWitnessEventSubscriber;
    private Flowable sightingReportedEventSubscriber;
    private Flowable searchFinishedEventSubscriber;
    private static BlockchainConnection instance = null;

    private BlockchainConnection(String account, String nodeUrl){
        String masterContractAddress = "0x2c17c13a4f9b6ecc826e09ab1ffd8a8b41ce14d5";

        this.credentials = Credentials.create(account);
        this.web3j = Web3j.build(new HttpService(nodeUrl));
        int contractGasPrice = 50;
        this.gasProvider = new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                System.out.println("function called: "+contractFunc);
                return Convert.toWei(Integer.toString(contractGasPrice), Unit.GWEI).toBigInteger();
            }

            @Override
            public BigInteger getGasPrice() {
                return Convert.toWei(Integer.toString(contractGasPrice), Unit.GWEI).toBigInteger();
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                System.out.println("function called: "+contractFunc);
                return BigInteger.valueOf(30000000);
            }

            @Override
            public BigInteger getGasLimit() {
                return BigInteger.valueOf(30000000);
            }
        };

        this.masterContract = AddressStorage.load(masterContractAddress, web3j, credentials, gasProvider);
    }

    private String getSmttAddress() {
        CompletableFuture<String> future = this.masterContract.get().sendAsync();
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e){
            System.out.println("OH COCK!!!");
            e.printStackTrace();
            return "0x0000000000000000000000000000000000000000";
        }
        }

    private void setSmttAddress(String address){
        this.masterContract.set(address).sendAsync();
    }


    public static BlockchainConnection getInstance(String account, String nodeUrl){
        if(instance == null){
            instance = new BlockchainConnection(account, nodeUrl);
        }
        return instance;
    }

    public static BlockchainConnection resetInstance(String account, String nodeUrl){
        instance = new BlockchainConnection(account, nodeUrl);
        return instance;
    }

    public byte[] senderInByteArray() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture future = this.smttContract.senderInBytes().sendAsync();
        return (byte[]) future.get(10, TimeUnit.SECONDS);
    }

    public BigInteger getUserBalance(String address) throws InterruptedException, ExecutionException, TimeoutException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
        return ethGetBalance.getBalance();
    }

    public byte[] createReportHash(BigInteger id, BigInteger nonce, byte[] secret, byte[] latitude, byte[] longitude, String addressZ) throws InterruptedException, ExecutionException, TimeoutException {
        byte[] nonceByteArray = new byte[32];
        byte[] idByteArray = new byte[32];
        System.arraycopy(nonce.toByteArray(),0,nonceByteArray,32-nonce.toByteArray().length,nonce.toByteArray().length);
        System.arraycopy(id.toByteArray(),0,idByteArray,32-id.toByteArray().length,id.toByteArray().length);
        byte[] stringArray = senderInByteArray();
        byte[] unhashed = new byte[idByteArray.length + stringArray.length + secret.length + latitude.length + longitude.length + stringArray.length];
        System.arraycopy(idByteArray, 0, unhashed, 0, idByteArray.length);
        System.arraycopy(nonceByteArray, 0, unhashed, idByteArray.length, stringArray.length);
        System.arraycopy(secret, 0, unhashed, idByteArray.length+stringArray.length, secret.length);
        System.arraycopy(latitude, 0, unhashed, idByteArray.length+stringArray.length+secret.length, latitude.length);
        System.arraycopy(longitude, 0, unhashed, idByteArray.length+stringArray.length+secret.length+latitude.length, longitude.length);
        System.arraycopy(stringArray, 0, unhashed, idByteArray.length+stringArray.length+secret.length+latitude.length+longitude.length, stringArray.length);
        return Hash.sha3(unhashed);
    }

    public void stopAll(){
        if(sightingReportEventSubscriber != null) {
            sightingReportEventSubscriber.unsubscribeOn(Schedulers.newThread());
        }
        if(searchTaskEventSubscriber != null) {
            searchTaskEventSubscriber.unsubscribeOn(Schedulers.newThread());
        }
        if(searchWitnessEventSubscriber != null) {
            searchWitnessEventSubscriber.unsubscribeOn(Schedulers.newThread());
        }
        if(sightingReportedEventSubscriber != null) {
            sightingReportedEventSubscriber.unsubscribeOn(Schedulers.newThread());
        }
        if(searchFinishedEventSubscriber != null){
            searchFinishedEventSubscriber.unsubscribeOn(Schedulers.newThread());
        }
        this.ongoingSearchOrders = new HashMap<>();
        this.reportedSightings = new HashMap<>();
        this.reportedPolSightings = new HashMap<>();
        this.searchTaskEventSubscriber = null;
        this.sightingReportEventSubscriber = null;
        this.searchWitnessEventSubscriber = null;
        this.sightingReportedEventSubscriber = null;
        this.searchFinishedEventSubscriber = null;
        this.smttContract = null;
        this.polContract = null;
    }


    public Contract deploySmtt() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<SMTT> future = SMTT.deploy(web3j, credentials, gasProvider).sendAsync();
        SMTT cotract = future.get(8, TimeUnit.SECONDS);
        setSmttAddress(cotract.getContractAddress());
        this.smttContract = cotract;
        return (Contract) cotract;
    }


    public Contract deploySyncSmtt() throws Exception {
        Contract contract = SMTT.deploy(web3j, credentials, gasProvider).send();
        return contract;
    }

    public String getPolAddress() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture future = this.smttContract.getProofOfLocationContract().sendAsync();
        return (String) future.get(4, TimeUnit.SECONDS);
    }


    public Contract loadSmtt(String address) {
        this.smttContract = SMTT.load(getSmttAddress(), web3j, credentials, gasProvider);
        return this.smttContract;
    }


    public Contract loadPOL(String address) {
        this.polContract = ProofOfLocation.load(address, web3j, credentials, gasProvider);
        return this.polContract;
    }


    public Contract loadPOL() throws InterruptedException, ExecutionException, TimeoutException {
        String address = getPolAddress();
        this.polContract = ProofOfLocation.load(address, web3j, credentials, gasProvider);
        return this.polContract;
    }


    public void register(BigInteger id, String publicKey) {
        Log.i("SMTT", "Creating Account");
        CompletableFuture f = this.smttContract.register(publicKey, id,  Convert.toWei("0.02", Unit.ETHER).toBigInteger()).sendAsync();
        try {
            System.out.println(f.get(2, TimeUnit.SECONDS));
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void startSerchTask(BigInteger trustFactor, BigInteger minNonce, BigInteger reward) {
        CompletableFuture f = this.smttContract.startSearchTask(trustFactor, minNonce, reward).sendAsync();
        try {
            System.out.println(f.get(2, TimeUnit.SECONDS));
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void reportSighting(String identifier, BigInteger nonce, byte[] hashedSighting){
        CompletableFuture f = this.smttContract.reportHashedSighting(identifier, nonce, hashedSighting).sendAsync();
        try {
            System.out.println(f.get(2, TimeUnit.SECONDS));
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public Tuple3<String, BigInteger, BigInteger> getAccount(String address){
        CompletableFuture future = this.smttContract.account(address).sendAsync();
        try {
            Tuple3<String, BigInteger, BigInteger> account = (Tuple3<String, BigInteger, BigInteger>) future.get(2, TimeUnit.SECONDS);
            return account;
        } catch (Exception e){
            System.out.println(e.toString()
            );
            e.printStackTrace();
            return new Tuple3<>(e.toString(), BigInteger.valueOf(-1), BigInteger.valueOf(-1));
        }
    }

    public Tuple3<BigInteger, BigInteger, BigInteger> getSearchTask(String address) throws InterruptedException, ExecutionException, TimeoutException {
        return this.smttContract.searchOrders(address).sendAsync().get(5, TimeUnit.SECONDS);
    }

    public Tuple5<String, BigInteger, byte[], byte[], Boolean> getSighting(String address, BigInteger index) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture future = this.smttContract.reportedSighting(address, index).sendAsync();
            Tuple5<String, BigInteger, byte[], byte[], Boolean> account = (Tuple5<String, BigInteger, byte[], byte[], Boolean>) future.get(4, TimeUnit.SECONDS);
            return account;
    }


    public void revealSecret(String address, byte[] secret){
        System.out.println("Address W: " + address);
        CompletableFuture f = this.smttContract.revealSecret(address, secret).sendAsync();
        try {
            System.out.println(f.get(2, TimeUnit.SECONDS));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getAddressFromNonce(BigInteger nonce) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture future = this.smttContract.ethAddress(nonce).sendAsync();
            return (String) future.get(2, TimeUnit.SECONDS);
    }


    public boolean isRegistered(String address){
        Tuple3<String, BigInteger, BigInteger> account = getAccount(address);
        return !account.equals(new Tuple3<String, BigInteger,  BigInteger>("", BigInteger.valueOf(0),BigInteger.valueOf(0)));
    }


    private BigInteger getLatestBlockNumber() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture future = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).sendAsync();
        EthBlock block = (EthBlock) future.get(1, TimeUnit.SECONDS);
        return block.getBlock().getNumber();
    }


    public void finishSearch(String addressA, String addressW, byte[] position, Context context) {
        CompletableFuture<TransactionReceipt> future = this.smttContract.finishSearch(addressA, addressW, position).sendAsync();
        try {
            TransactionReceipt a = future.get(1, TimeUnit.SECONDS);
            //Toast.makeText(context, (String) s, Toast.LENGTH_SHORT);
            System.out.println(a);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void startPOL(String addressA, String addressW, BigInteger timeStamp){
        CompletableFuture f = this.polContract.initiate(addressA, addressW, timeStamp).sendAsync();
        try{
            System.out.println(f.get(4, TimeUnit.SECONDS));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static byte[] createPolSighting(byte[] latitudeArray, byte[] longitudeArray, BigInteger timestamp, BigInteger id){
        byte[] timestampArray = new byte[32];
        System.arraycopy(timestamp.toByteArray(),0,timestampArray,32-timestamp.toByteArray().length,timestamp.toByteArray().length);
        byte[] idArray = new byte[4];
        System.arraycopy(id.toByteArray(),0,idArray,4-id.toByteArray().length,id.toByteArray().length);

        byte[] concat = new byte[latitudeArray.length + longitudeArray.length + timestampArray.length + idArray.length];
        System.arraycopy(latitudeArray, 0, concat, 0, latitudeArray.length);
        System.arraycopy(longitudeArray, 0, concat, latitudeArray.length, longitudeArray.length);
        System.arraycopy(timestampArray, 0, concat, latitudeArray.length + longitudeArray.length, timestampArray.length);
        System.arraycopy(idArray, 0, concat, latitudeArray.length + longitudeArray.length + timestampArray.length, idArray.length);
        return concat;
    }

    public void reportWitness(String addressW, byte[] sighting, byte[] secret, byte[] publicKey) throws Exception {
        Cipher rsa = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey key = keyFactory.generatePublic(spec);
        rsa.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedSighting = rsa.doFinal(sighting);
        byte[] sightingHash = Hash.sha3(sighting);
        byte[] sightingProof = Hash.sha3(Hash.sha3(secret));
        this.polContract.reportSightingOfWitness(addressW, encryptedSighting, sightingHash).sendAsync();
    }

    public Tuple3<BigInteger, String, BigInteger> getProofOfLocationStatus(String addressW) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Tuple3<BigInteger, String, BigInteger>> future = this.polContract.polStart(addressW).sendAsync();
        return future.get(4, TimeUnit.SECONDS);
    }

    public BigInteger distanceInMeter(BigInteger x1, BigInteger y1, BigInteger x2, BigInteger y2) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<BigInteger> future = this.smttContract.distanceInMeter(x1, y1, x2, y2).sendAsync();
        return future.get(4, TimeUnit.SECONDS);
    }

    public void startSearchTaskSubscription() {
        if(this.searchTaskEventSubscriber == null) {
            try {
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(getLatestBlockNumber().subtract(BigInteger.valueOf(45500))), DefaultBlockParameterName.LATEST, this.smttContract.getContractAddress());
                filter.addSingleTopic(EventEncoder.encode(SMTT.ORDERCREATED_EVENT));
                Flowable flow = this.smttContract.orderCreatedEventFlowable(filter);
                this.searchTaskEventSubscriber = flow.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread());
                this.searchTaskEventSubscriber.subscribe(log -> searchTaskEventCallback((SMTT.OrderCreatedEventResponse) log));
                Log.i("[SMTT General]", "Search Task started.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void searchTaskEventCallback(SMTT.OrderCreatedEventResponse task){
        String address = task.addr.toUpperCase();
        BigInteger trustFactor = task.trustfactor;
        BigInteger reward = task.reward;
        BigInteger blockNumber = task.log.getBlockNumber();
        String log = String.format("Address: %s, TF: %s, Reward: %s, Block: %s", address, trustFactor, reward, blockNumber);
        if(this.ongoingSearchOrders.containsKey(address)){
            if(this.ongoingSearchOrders.get(address)[2].compareTo(blockNumber) < 0) {
                Log.i("[SMTT Event]", "Updated search task: " + log);
                this.ongoingSearchOrders.replace(address, new BigInteger[]{task.trustfactor, task.reward, blockNumber});
            }
        } else{
            Log.i("[SMTT Event]", "New search task: " + log);
            this.ongoingSearchOrders.put(address, new BigInteger[]{task.trustfactor, task.reward, blockNumber});
        }
    }


    public HashMap<String, BigInteger[]> getOngoingSearchOrders(){
        return ongoingSearchOrders;
    }

    public void endPOL(List<byte[]> sightings, List<BigInteger> indexes, byte[] itemLocation) throws Exception {
        CompletableFuture future = this.polContract.end(sightings, indexes, itemLocation).sendAsync();
        System.out.println(future.get(5, TimeUnit.SECONDS));
    }


    public void startSightingReportSubscription() {
        if(this.sightingReportEventSubscriber == null) {
            try {
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(getLatestBlockNumber().subtract(BigInteger.valueOf(45500))),
                        DefaultBlockParameterName.LATEST, this.smttContract.getContractAddress());
                filter.addSingleTopic(EventEncoder.encode(SMTT.REPORTSIGHTING_EVENT));
                Flowable flow = this.smttContract.reportSightingEventFlowable(filter);
                this.sightingReportEventSubscriber = flow.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread());
                this.sightingReportEventSubscriber.subscribe(log -> sightingReportEventCallback((SMTT.ReportSightingEventResponse) log));
                Log.i("[SMTT General]", "Sighting Subscriber started.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void sightingReportEventCallback(SMTT.ReportSightingEventResponse sighting) {
        String addressA = sighting.addrA.toUpperCase();
        String addressW = sighting.addrW;
        BigInteger timestamp = sighting.timestamp;
        String log = String.format("AddressA: %s, AddressW: %s, Timestamp: %s", addressA, addressW, timestamp);
        if(this.reportedSightings.containsKey(addressA)) {
            LinkedList<Tuple2<String, BigInteger>> list = this.reportedSightings.get(addressA);
            Tuple2<String, BigInteger> data = new Tuple2<String, BigInteger>(addressW, timestamp);
            for(Tuple2<String, BigInteger> tuple :list){
                if(tuple.component1().equals(addressW)){
                    Log.i("[SMTT EVENT]", "Ignoring duplicate sighting. "+log);
                    return;
                }
            }
            list.add(data);
            this.reportedSightings.replace(addressA, list);
            Log.i("[SMTT EVENT]", "New Sighting: " + log);

        } else {
            LinkedList<Tuple2<String, BigInteger>> list = new LinkedList<>();
            list.add(new Tuple2<String, BigInteger>(addressW, timestamp));
            this.reportedSightings.put(addressA, list);
            Log.i("[SMTT EVENT]", "New Sighting: " + log);
        }
    }

    public HashMap<String, LinkedList<Tuple2<String, BigInteger>>>getReportedSightings(){
        return this.reportedSightings;
    }

    public void startSearchWitnessSubscription() {
        if(this.searchWitnessEventSubscriber == null) {
            try {
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(getLatestBlockNumber().subtract(BigInteger.valueOf(45500))), DefaultBlockParameterName.LATEST, this.polContract.getContractAddress());
                filter.addSingleTopic(EventEncoder.encode(ProofOfLocation.SEARCHWITNESS_EVENT));
                Flowable flow = this.polContract.searchWitnessEventFlowable(filter);
                this.searchWitnessEventSubscriber = flow.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread());
                this.searchWitnessEventSubscriber.subscribe(log -> searchWitnessEventCallback((ProofOfLocation.SearchWitnessEventResponse) log));
                Log.i("[SMTT General]", "Search Witness Subscriber started.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void searchWitnessEventCallback(ProofOfLocation.SearchWitnessEventResponse response){
        Log.i("[SMTT Event]", String.format("Witness Address: %s,Timestamp %s", response.adressWitness, response.timestamp.toString()));
    }

    public void startSightingReportedEventSubscriber() {
        if(this.sightingReportedEventSubscriber == null) {
            try {
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(getLatestBlockNumber().subtract(BigInteger.valueOf(45500))), DefaultBlockParameterName.LATEST, this.polContract.getContractAddress());
                filter.addSingleTopic(EventEncoder.encode(ProofOfLocation.SIGHTINGREPORTED_EVENT));
                Flowable flow = this.polContract.sightingReportedEventFlowable(filter);
                this.sightingReportedEventSubscriber = flow.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread());
                this.sightingReportedEventSubscriber.subscribe(log -> sightingReportedEventCallback((ProofOfLocation.SightingReportedEventResponse) log));
                Log.i("[SMTT General]", "Witness Sighting Report Subscriber started.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public BigInteger calculateTrustfactor(BigInteger dT, BigInteger dS, BigInteger stake) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture future = this.polContract.calculateTrustFactor(dT, dS, stake).sendAsync();
        return (BigInteger) future.get(5, TimeUnit.SECONDS);
    }

    public void sightingReportedEventCallback(ProofOfLocation.SightingReportedEventResponse response){
        String address = response.addrW.toUpperCase();
        byte[] sighting = response.encryptedSighting;
        byte[] hash = response.sightingHash;
        if(this.reportedPolSightings.containsKey(address)){
            LinkedList<Tuple2<byte[], byte[]>> list = this.reportedPolSightings.get(address);
            if(list.contains(new Tuple2(sighting, hash))){return;}
            list.add(new Tuple2(sighting, hash));
            this.reportedPolSightings.replace(address, list);
        } else {
            LinkedList<Tuple2<byte[], byte[]>> list = new LinkedList();
            list.add(new Tuple2(sighting, hash));
            this.reportedPolSightings.put(address, list);
        }
        Log.i("[POL Sighting Event]", "New Witness Report for " + address);
    }

    public LinkedList<Tuple2<byte[], byte[]>> getPolSightigs(String addressW){
        return this.reportedPolSightings.get(addressW.toUpperCase());
    }

    public void startSearchFinishedEventSubscriber() {
        if(this.searchFinishedEventSubscriber == null) {
            try {
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(getLatestBlockNumber().subtract(BigInteger.valueOf(45500))), DefaultBlockParameterName.LATEST, this.smttContract.getContractAddress());
                filter.addSingleTopic(EventEncoder.encode(SMTT.SEARCHFINISHED_EVENT));
                Flowable flow = this.smttContract.searchFinishedEventFlowable(filter);
                this.searchFinishedEventSubscriber = flow.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread());
                this.searchFinishedEventSubscriber.subscribe(log -> searchFinishedCallback((SMTT.SearchFinishedEventResponse) log));
                Log.i("[SMTT General]", "Search Finished Subscriber started.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void searchFinishedCallback(SMTT.SearchFinishedEventResponse response){
        BigInteger blockNumber = response.log.getBlockNumber();
        String addressA = response.addr;
        if(this.ongoingSearchOrders.containsKey(addressA.toUpperCase())){
            if(this.ongoingSearchOrders.get(addressA.toUpperCase())[2].compareTo(blockNumber) == -1){
                this.ongoingSearchOrders.remove(addressA.toUpperCase());
                Log.i("[Search Finished]", "Removed finished Task: "+addressA);
                if(this.reportedSightings.containsKey(addressA.toUpperCase())){
                    this.reportedSightings.remove(addressA.toUpperCase());
                    Log.i("[Search Finished]", "Removed Sightings of finished Task: "+addressA);
                }
            }
        }

        Log.i("[Search Finished Event]", String.format("Search Finished. ArressA: %s", response.addr));
    }




    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }


}
