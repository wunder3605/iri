package com.iota.iri.pluggables.KVStore;

import com.iota.iri.model.Hash;

import java.util.*;

public class KVFilter {
    Map<String, List<Hash>> elements;
    Map<String, List<KV>> kvs;
    List<Hash> order;

    String project;
    String key;
    String secondary;
    String third;
    String combined;

    public KVFilter() {
        elements = new HashMap<>();
        kvs = new HashMap<>();
    }

    public void setCriteria(String project,
                            String key,
                            String secondary,
                            String third,
                            List<Hash> order) {
        this.project = project;
        this.key = key;
        this.secondary = secondary;
        this.third = third;
        this.order = order;
        if (!secondary.isEmpty() && !third.isEmpty()) {
            this.combined = project + "-" + key + "-" + secondary + "-" + third;
        } else if (!secondary.isEmpty() && third.isEmpty()) {
            this.combined = project + "-" + key + "-" + secondary;
        } else if (secondary.isEmpty() && third.isEmpty()) {
            this.combined = project + "-" + key;
        } else {
            throw new RuntimeException("Key format is not correct!");
        }
    }

    public void addKV(KV k, Hash hash) {
        putKVandHash(k, hash);
    }

    public List<String> getResult() {
        List<String> ret = new LinkedList<>();
        List<String> keys = new LinkedList<>();
        List<Hash> tmp = new LinkedList<>();

        List<String> res = new LinkedList<>();

        // filter by content
        for(String kvsKey : kvs.keySet()) {
            for(int i=0; i<kvs.get(kvsKey).size(); i++) {

                KV k = kvs.get(kvsKey).get(i);
                String combKey = k.getCombinedKey();
                System.out.println(combKey + " " + combined);
                if(combKey.indexOf(combined) != -1) {
                    ret.add(k.toString());
                    keys.add(combKey);
                    tmp.add(elements.get(combKey).get(i));
                }
            }
        }

        // resolve conflict by total order
        // FIXME complexity is high here
        for(int i=0; i<ret.size(); i++) {
            boolean duplicate = false;
            boolean latest = true;
            int idx1 = order.indexOf(tmp.get(i));

            for(int j=0; j<ret.size(); j++) {
                if(j == i) {
                    continue;
                }

                if(keys.get(i).equals(keys.get(j))) {
                    int idx2 = order.indexOf(tmp.get(j));
                    duplicate = true;
                    if(idx1 < idx2) {
                        latest = false;
                        break;
                    }
                }
            }
            if(!duplicate) {
                res.add(ret.get(i));
            } else if(duplicate && latest) {
                res.add(ret.get(i));
            }
        }
        return res;
    }

    public void putKVandHash(KV k, Hash hash) {
        String combKey = k.getCombinedKey();
        if(!elements.containsKey(combKey)) {
            elements.put(combKey, new LinkedList<>());
            kvs.put(combKey, new LinkedList<>());
        }

        List<Hash> e1 = elements.get(combKey);
        List<KV> k1 = kvs.get(combKey);
        e1.add(hash);
        k1.add(k);
        elements.put(combKey, e1);
        kvs.put(combKey, k1);
    }
}
