package bgu.spl.net.impl;

import bgu.spl.net.impl.Messages.NotificationMessage;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class User {
    private String username;
    private String password;
    private String birthday;
    private boolean isLoggedIn;
    private HashMap<String, User> followers;
    private HashMap<String, User> following;
    private HashSet<String> blocked;
    private short age;
    private short numOfPosts = 0;
    private int connectionId= 0;
    // notification queue will store users notifications when logged out
    private ConcurrentLinkedQueue<NotificationMessage> notifications = new ConcurrentLinkedQueue<>();


    public int getConnectionId() {
        return connectionId;
    }

    public User(String username, String password, String birthday) {
        this.username = username;
        this.password = password;
        this.birthday = birthday;
        String[] splitDate = birthday.split("-");
        isLoggedIn = false;
        this.followers = new HashMap<>();
        this.following = new HashMap<>();
        age = (short) (2021 - Short.parseShort(splitDate[2]));
        blocked = new HashSet<>();

    }

    public String getStats() {
        return age + " " + numOfPosts + " " + (short) followers.size() + " " + (short) following.size();
    }

    public void setLoggedIn(boolean state) {
        this.isLoggedIn = state;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public HashMap<String, User> getFollowers() {
        return followers;
    }


    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isFollowing(String username) {
        return following.containsKey(username);
    }

    public void follow(User user) {
        following.put(user.getUsername(), user);
        user.addToFollowers(this);

    }
    public void addToFollowers(User user) {
        followers.put(user.getUsername(), user);
    }

    public void removeFromFollowing(String username) {
        following.remove(username);
    }

    public void removeFromFollowers(String username) {
        followers.remove(username);
    }

    public void block(String username) {
        removeFromFollowing(username);
        removeFromFollowers(username);
        blocked.add(username);
    }

    public boolean isBlocked(String username) {
        return blocked.contains(username);
    }

    public void increaseNumOfPosts() {
        numOfPosts++;
    }
    public void setConnectionId(int id) {
        connectionId = id;
    }
    public ConcurrentLinkedQueue<NotificationMessage> getNotifications() {
        return notifications;
    }
}

