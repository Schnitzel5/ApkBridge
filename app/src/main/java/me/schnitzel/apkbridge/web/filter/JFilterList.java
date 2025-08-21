package me.schnitzel.apkbridge.web.filter;

import java.util.List;

public class JFilterList {
    public String name;
    public String type;
    public String stateString;
    public int stateInt;
    public List<JGroupFilter> stateList;
    public JSortFilter stateSort;

    public JFilterList() {}

    public static class JGroupFilter {
        public String name;
        public String type;
        public boolean stateBoolean;
        public int stateInt;

        public JGroupFilter() {}
    }

    public static class JSortFilter {
        public boolean ascending;
        public int index;

        public JSortFilter() {}
    }
}
