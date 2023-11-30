package com.elsistemas.bluetoothprinterlib.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class BluetoothDTO {
    public static String toJson(List<Map<String, Object>> listMap) {
        JSONArray jsonArray = new JSONArray();

        for (Map<String, Object> hashMap : listMap) {
            JSONObject jsonObject = new JSONObject(hashMap);
            jsonArray.put(jsonObject);
        }

        return jsonArray.toString();
    }
}
