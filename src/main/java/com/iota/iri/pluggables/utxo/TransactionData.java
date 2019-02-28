package com.iota.iri.pluggables.utxo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.iota.iri.model.HashFactory;
import com.iota.iri.storage.Tangle;
import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import com.alibaba.fastjson.JSON;

public class TransactionData {

    Tangle tangle;
    List<Transaction> transactions;
    List<Txn> transactions;
    HashMap<Hash, Hash> txnToTangleMap;
    HashMap<Hash, HashSet<Txn>> tangleToTxnMap;
    List<List<Txn>> tmpStorage;

    private static TransactionData txnData;

    // TODO make this thread safe
    public static void setInstance(TransactionData txnData) 
    {
        if (txnData == null)
        {
            txnData = txnData;
        }
    }
    public static TransactionData getInstance() {
        if(txnData == null)
        {
            txnData = new TransactionData();
            txnData.init();
        }
        return txnData; 
    }

    public TransactionData() {
        //empty constructor
        txnToTangleMap = new HashMap<Hash, Hash>();
        tangleToTxnMap = new HashMap<Hash, HashSet<Txn>>();
        tmpStorage = new ArrayList<>();
        init();
    }

    static class RawTxn {
        String from;
        String to;
        long amnt;

        public void setFrom(String from) {
            this.from = from;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public void setAmnt(long amnt) {
            this.amnt = amnt;
        }

        public String toString() {
            return from + ":" + to + ":" +amnt+"\n";
        }
    }

    public void createTmpStorageForBlock(BatchTxns tmpBatch){
        List<Txn> newList = tmpBatch.txn_content.stream().collect(Collectors.toList());
        tmpStorage.add(newList);
    }

    public void batchPutIndex(List<Hash> hashList) {
        if(hashList.size() != tmpStorage.size() || hashList.size() <= 1) {
            return;
        }
        int i=hashList.size()-1;
        for(Hash h : hashList) {
            for (Txn t : tmpStorage.get(i)) {
                putIndex(t, h);
            }
            i--;
        }
        tmpStorage.clear();
    }

    public void putIndex(Txn tx, Hash blockHash) {
        txnToTangleMap.put(tx.txnHash, blockHash);
        if(tangleToTxnMap.get(blockHash) != null) {
            HashSet<Txn> s = tangleToTxnMap.get(blockHash);
            s.add(tx);
            tangleToTxnMap.put(blockHash, s);
        } else {
            HashSet<Txn> s = new HashSet<Txn>();
            s.add(tx);
            tangleToTxnMap.put(blockHash, s);
        }
    }

    public String getData() {
        String ret = "";
        if(checkConsistency()) {
            for(Hash h : tangleToTxnMap.keySet()) {
                ret += IotaUtils.abbrieviateHash(h, 4) + " : ";
                BatchTxns btx = new BatchTxns();
                for(Txn tx : tangleToTxnMap.get(h)) {
                    btx.addTxn(tx);
                }
                ret += btx.getString(btx) + "\n";
            }
        }
        return ret;
    }

    public boolean checkConsistency() {
        try {
            // forward check
            for(Hash h : tangleToTxnMap.keySet()) {
                TransactionViewModel model = TransactionViewModel.find(tangle, h.bytes());
                String sig = Converter.trytes(model.getSignature());
                String txnsStr = Converter.trytesToAscii(sig);

                JSONObject jo = new JSONObject(txnsStr);

                JSONArray jsonArray = (JSONArray) jo.get("txn_content");

                for(Txn t : tangleToTxnMap.get(h)) {
                    boolean found = false;
                    for (Object object : jsonArray) {
                        JSONObject jo1 = new JSONObject(object.toString());
                        JSONObject jo2 = new JSONObject(JSON.toJSONString(t));
                        if(jo1.toString().equals(jo2.toString())) {
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        return false;
                    }
                }
            }
            // TODO backward check
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }  
    }

    public void readFromStr(String txnsStr){

        List<RawTxn> transactionList = new ArrayList<>();

        RawTxn tx = JSON.parseObject(txnsStr, RawTxn.class);
        transactionList.add(tx);

        constructTxnsFromRawTxns(transactionList);
    }

    public void readFromLines(String[] lines){
        List<RawTxn> transactionList = new ArrayList<>();

        for (String line:lines) {
            RawTxn tx = JSON.parseObject(line, RawTxn.class);
            transactionList.add(tx);
        }

        constructTxnsFromRawTxns(transactionList);
    }

    public void readFromIPFSAddr(String ipfsAddr) throws IOException{
        List<RawTxn> rawTxnsList = readRawTxnInfoFromIPFS(ipfsAddr);
        constructTxnsFromRawTxns(rawTxnsList);
    }


    public void init() {
        transactions = new ArrayList<>();

        List<TransactionOut> txnOutList = new ArrayList<>();
        TransactionOut txOut = new TransactionOut();
        txOut.amount = 10000;  //just for testing
        txOut.userAccount = "A";  //just for testing
        txnOutList.add(txOut);

        Transaction newTxn = new Transaction();
        newTxn.inputs = null;
        newTxn.outputs = txnOutList;

        newTxn.txnHash = HashFactory.TRANSACTION.create(JSON.toJSONBytes(newTxn));

        transactions.add(newTxn);
    }

    public Transaction getLast() {
        return transactions.get(transactions.size()-1);
    }

    private boolean constructTxnsFromRawTxns(List<RawTxn> rawTxns) {
        int size = transactions.size();
        boolean undoFlag = false;

        for (RawTxn txn: rawTxns) {
            boolean flag = doStoreRawTxn(txn);
            if (flag == false){
                undoFlag = true;
                break;
            }
        }

        if (undoFlag == true){
            int undoSize = transactions.size();
            if (undoSize > size) {
                for (int i = size; i < undoSize; i++) {
                    transactions.remove(i);
                }
            }
            return false;
        }

        return true;
    }

    private List<RawTxn> readRawTxnInfoFromIPFS (String ipfsAddr) throws IOException {

        List<RawTxn> transactionList = new ArrayList<>();

        IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");

        Multihash filePointer = Multihash.fromBase58(ipfsAddr);
        byte[] fileContents = ipfs.cat(filePointer);

        String str = new String(fileContents);

        String[] lines = str.split(System.getProperty("line.separator"));

        for (String line:lines) {
            RawTxn tx = JSON.parseObject(line, RawTxn.class);
            transactionList.add(tx);
        }

        return transactionList;
    }


    private boolean doStoreRawTxn(RawTxn txn) {
        String formAddr = txn.from;
        String toAddr = txn.to;
        long left = txn.amnt;
        long total = 0;

        List<TransactionIn> txnInList = new ArrayList<>();

        for (int i = transactions.size() - 1; i >= 0; i--){

            List<TransactionOut> txnOutList = transactions.get(i).outputs;
            for (int j = 0; j < txnOutList.size(); j++) {
                TransactionOut txnOut = txnOutList.get(j);
                if (txnOut.userAccount.equals(formAddr)){

                    boolean jumpFlag = false;
                    for (int k = transactions.size() - 1; k > i; k--){
                        for (TransactionIn tempTxnIn: transactions.get(k).inputs) {
                            if (tempTxnIn.txnHash == transactions.get(i).txnHash && tempTxnIn.idx == j){
                                jumpFlag = true; // already spend
                                break;
                            }
                        }
                    }
                    if (jumpFlag == true){
                        continue;
                    }

                    TransactionIn txnIn = new TransactionIn();
                    txnIn.userAccount = formAddr;
                    txnIn.txnHash = transactions.get(i).txnHash;
                    txnIn.idx = j;

                    txnInList.add(txnIn);
                    total += txnOut.amount;
                    if (txnOut.amount >= left){
                        break;
                    }
                    else{
                        left -= txnOut.amount;
                    }
                }
            }
        }

        if (txnInList.size() == 0){
            return false;
        }

        Transaction newTxn = new Transaction();
        newTxn.inputs = txnInList;

        List<TransactionOut> txnOutList = new ArrayList<>();

        TransactionOut toTxOut = new TransactionOut();
        toTxOut.amount = txn.amnt;
        toTxOut.userAccount = toAddr;
        txnOutList.add(toTxOut);

        if ((total - txn.amnt) > 0) {
            TransactionOut fromTxOut = new TransactionOut();
            fromTxOut.amount = total - txn.amnt;
            fromTxOut.userAccount = formAddr;
            txnOutList.add(fromTxOut);
        }

        newTxn.outputs = txnOutList;
        newTxn.txnHash = HashFactory.TRANSACTION.create(JSON.toJSONBytes(newTxn));

        transactions.add(newTxn);
        return true;
    }
}
