package com.iota.iri.pluggables.utxo;

import com.iota.iri.model.*;

<<<<<<< HEAD:src/main/java/com/iota/iri/pluggables/utxo/TransactionIn.java
public class TransactionIn {
    Hash txnHash;
=======
public class TxnIn {
    String txnHash;
>>>>>>> fb56a6e... [fix #151] Restore UTXO when restarting:src/main/java/com/iota/iri/pluggables/utxo/TxnIn.java
    int idx;
    String userAccount;

    public String getTxnHash() {
        return txnHash;
    }

    public int getIdx() {
        return idx;
    }

    public String getUserAccount() {
        return userAccount;
    }
}
