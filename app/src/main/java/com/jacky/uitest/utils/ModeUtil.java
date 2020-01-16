package com.jacky.uitest.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeUtil {
    public List<String> modes = new ArrayList<>();
    public List<String> colors = new ArrayList<>();
    public List<Integer> what = new ArrayList<>();
    public Map<String, Integer> handlerMap = new HashMap<>();


    public void addMode() {
        modes.add("TEST");
        modes.add("WIFI_TEST");
        modes.add("CANCEL");
    }

    public void addColor() {
        colors.add("#FFF44336");
        colors.add("#FF009688");
        colors.add("#FF8BC34A");
        colors.add("#FF3F51B5");
        colors.add("#FF00BCD4");
        colors.add("#D8A322FF");
    }

    public void addWhat(List<String> modes) {
        if (modes.isEmpty()) return;
        for (int i = 0; i < modes.size(); i++) {
            what.add(1000 + i);
        }

    }

    public void put(List<String> modes, List<Integer> what) {
        if (modes.isEmpty()) return;
        for (int i = 0; i < modes.size(); i++)
            handlerMap.put(modes.get(i), what.get(i));
    }
}
