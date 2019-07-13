package com.iota.iri.pluggables.KVStore;

import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
        if(value.getClass().equals(String.class)) {
            return value.toString();
        } else if(value.getClass().equals(ArrayList.class)) {
            ArrayList<String> arr = new ArrayList<>();
            ArrayList list = (ArrayList)value;
            for(Object o : list) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.toJsonTree(o).getAsJsonObject();
                String json = gson.toJson(jsonObject);
                arr.add(json);
            }
            return arr.toString();
        } else {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.toJsonTree(value).getAsJsonObject();
            String json = gson.toJson(jsonObject);
            return json;
        }
    }
}
