package com.example.mobilneaplikacije.data.model;

import java.util.ArrayList;
import java.util.List;

public class PublicProfile {
    public String uid;
    public String username;
    public String avatar;
    public int level;
    public String title;
    public int xp;
    public List<String> badges = new ArrayList<>();
    public List<Item> activeItems = new ArrayList<>();
}
