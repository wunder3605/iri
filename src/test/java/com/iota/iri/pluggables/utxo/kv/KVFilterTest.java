package com.iota.iri.pluggables.utxo.kv;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.iota.iri.model.Hash;
import com.iota.iri.pluggables.KVStore.*;
import java.util.LinkedList;
import java.util.List;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;


public class KVFilterTest {


    @BeforeClass
    public static void setUp() throws Exception {

    }

    @Test
    public void testFilterResults() {
        KVFilter filter = new KVFilter();

        Hash hash1 = getRandomTransactionHash();
        KV kv1 = new KV("zhaoming", "vipinfo","","val1","diamond", "KV");
        Hash hash2 = getRandomTransactionHash();
        KV kv2 = new KV("zhaoming", "vipinfo","","val2","diamond", "KV");
        Hash hash3 = getRandomTransactionHash();
        KV kv3 = new KV("zhaoming-sale", "201907","10","sale1","diamond", "KV");
        Hash hash4 = getRandomTransactionHash();
        KV kv4 = new KV("zhaoming-sale", "201907","11","sale2","diamond", "KV");
        Hash hash5 = getRandomTransactionHash();
        KV kv5 = new KV("zhaoming1", "vipinfo","","val2","diamond", "KV");
        Hash hash6 = getRandomTransactionHash();
        KV kv6 = new KV("zhaoming1", "vipinfo","","val1","diamond", "KV");

        List<Hash> order = new LinkedList<>();
        order.add(hash1);
        order.add(hash2);
        order.add(hash3);
        order.add(hash4);
        order.add(hash5);
        order.add(hash6);

        filter.addKV(kv1, hash1);
        filter.addKV(kv2, hash2);
        filter.addKV(kv3, hash3);
        filter.addKV(kv4, hash4);
        filter.addKV(kv5, hash5);
        filter.addKV(kv6, hash6);

        filter.setCriteria("diamond", "zhaoming", "vipinfo", "", order);
        List<String> res = filter.getResult();
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("val2", res.get(0));

        filter.setCriteria("diamond", "zhaoming1", "vipinfo", "", order);
        List<String> res1 = filter.getResult();
        Assert.assertEquals(1, res1.size());
        Assert.assertEquals("val1", res1.get(0));

        filter.setCriteria("diamond", "zhaoming-sale", "", "", order);
        List<String> res2 = filter.getResult();
        Assert.assertEquals(2, res2.size());

        filter.setCriteria("diamond", "zhaoming-sale", "201907", "", order);
        List<String> res3 = filter.getResult();
        Assert.assertEquals(2, res3.size());

        filter.setCriteria("diamond", "zhaoming-sale", "201907", "08", order);
        List<String> res4 = filter.getResult();
        Assert.assertEquals(0, res4.size());

        filter.setCriteria("diamond", "zhaoming-sale", "201907", "10", order);
        List<String> res5 = filter.getResult();
        Assert.assertEquals(1, res5.size());
        Assert.assertEquals("sale1", res5.get(0));
    }

}
