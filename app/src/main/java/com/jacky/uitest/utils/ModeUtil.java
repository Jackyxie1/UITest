package com.jacky.uitest.utils;

import java.util.ArrayList;
import java.util.List;

public class ModeUtil {
    public List<String> modes = new ArrayList<>();
    public List<String> colors=new ArrayList<>();

    public void addMode() {
        modes.add("TEST");
        modes.add("WIFI_TEST");
        modes.add("CANCEL");
    }

    public void addColor(){
        colors.add("#FFF44336");
        colors.add("#FF009688");
        colors.add("#FF8BC34A");
        colors.add("#FF3F51B5");
        colors.add("#FF00BCD4");
        colors.add("#D8A322FF");
    }
}
