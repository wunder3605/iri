package com.iota.iri.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.iota.iri.conf.BaseIotaConfig;

import java.io.*;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import java.util.zip.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.iota.iri.controllers.*;
import com.iota.iri.pluggables.utxo.*;

import com.iota.iri.hash.Sponge;
import com.iota.iri.hash.SpongeFactory;

public class IotaIOUtils extends IOUtils {

    private static final Logger log = LoggerFactory.getLogger(IotaIOUtils.class);

    public static void closeQuietly(AutoCloseable... autoCloseables) {
        for (AutoCloseable it : autoCloseables) {
            try {
                if (it != null) {
                    it.close();
                }
            } catch (Exception ignored) {
                log.debug("Silent exception occured", ignored);
            }
        }
    }

    public static String processBatchTxnMsg(final String message) {
        // decompression goes here
        String msgStr = message;
        if(BaseIotaConfig.getInstance().isEnableCompressionTxns()) {
            try {
                byte[] bytes = Converter.trytesToBytes(message);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                GZIPInputStream inStream = new GZIPInputStream(in);
                byte[] buffer = new byte[16384];
                int num = 0;
                while ((num = inStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, num);
                }
                byte[] unCompressed = out.toByteArray();
                msgStr = new String(unCompressed);
            } catch (IOException e) {
                log.error("Uncompressing error", e);
                return null;
            }
        }

        String ret = "";

        // parse json here
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(msgStr);
            JsonNode numNode = rootNode.path("tx_num");
            JsonNode txsNode = rootNode.path("txn_content");
            long txnCount = numNode.asLong();

            int size = (int)TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE / 3;
            if(txsNode.isArray()) {
                BatchTxns tmpBatch = new BatchTxns();
                for (final JsonNode txn : txsNode) {
                    TransactionData.getInstance().readFromStr(txn.toString().replace("\\", "").replace("\"{", "{").replace("}\"", "}"));
                    tmpBatch.addTxn(TransactionData.getInstance().getLast());
                    if(tmpBatch.getTryteStringLen(tmpBatch) + 200 > size) {
                        String s = StringUtils.rightPad(tmpBatch.getTryteString(tmpBatch), size, '9');
                        log.info("[zhaoming"+size+"] "+s);
                        ret += s;
                        tmpBatch.clear();
                    }
                }
                if(tmpBatch.tx_num > 0) {
                    String s = StringUtils.rightPad(tmpBatch.getTryteString(tmpBatch), size, '9');
                    ret += s;
                    tmpBatch.clear();
                }
            }
            ret+=","+String.valueOf(txnCount);
            return ret;
        } catch (IOException e) {
            log.error("Parse json error", e);
            return null;
        }
    }
}
