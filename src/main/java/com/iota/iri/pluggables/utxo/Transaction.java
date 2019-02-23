package com.iota.iri.pluggables.utxo;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iota.iri.model.*;
import com.iota.iri.utils.Converter;

public class Transaction {

    String txnHash;

    List<TransactionIn> inputs;

    List<TransactionOut> outputs;

    public List<TransactionIn> getInputs() {
        return inputs;
    }

    public List<TransactionOut> getOutputs() {
        return outputs;
    }

<<<<<<< HEAD:src/main/java/com/iota/iri/pluggables/utxo/Transaction.java
    public int getTryteStringLen(Transaction txn) {
=======
    public String getTxnHash() {
        return txnHash;
    }

    public int getTryteStringLen(Txn txn) {
>>>>>>> fb56a6e... [fix #151] Restore UTXO when restarting:src/main/java/com/iota/iri/pluggables/utxo/Txn.java
        ObjectMapper mapper = new ObjectMapper();
        try {
            String str = mapper.writeValueAsString(txn);
            String trytes = Converter.asciiToTrytes(str);
            if (trytes != null) {
                return trytes.length();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
