package com.iota.iri.pluggables.KVStore;

import java.util.*;

public class KV {
    public String key;
    public String secondary;
    public String third;
    public Object value;
    public String project;
    public String tag;

    public KV(String key,
              String secondary,
              String third,
              String val,
              String project,
              String tag) {
        this.key = key;
        this.secondary = secondary;
        this.third = third;
        this.value = val;
        this.project = project;
        this.tag = tag;
    }

    public String getCombinedKey() {
        return project + "-" + key + "-" + secondary + "-" + third;
    }

    public String toString() {
        return value.toString();
    }
}
