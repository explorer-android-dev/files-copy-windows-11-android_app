package com.windows11.files.models;

public class QuickAccessItem {
    private String name;
    private String path;
    private int icon;
    
    public QuickAccessItem(String name, String path, int icon) {
        this.name = name;
        this.path = path;
        this.icon = icon;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPath() {
        return path;
    }
    
    public int getIcon() {
        return icon;
    }
}
