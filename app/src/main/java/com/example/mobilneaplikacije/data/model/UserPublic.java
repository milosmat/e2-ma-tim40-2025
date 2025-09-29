package com.example.mobilneaplikacije.data.model;

public class UserPublic {
    public String uid;
    public String username;
    public String avatar;
    public Integer level;
    public String title;
    public Integer pp;
    public Integer coins;

    public String status;

    public UserPublic() {}
    public UserPublic(String uid, String username, String avatar) {
        this.uid = uid;
        this.username = username;
        this.avatar = avatar;
        this.status = "none";
    }
}
