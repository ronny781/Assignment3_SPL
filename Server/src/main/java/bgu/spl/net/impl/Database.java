package bgu.spl.net.impl;

import bgu.spl.net.api.Message;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Database {
    private ConcurrentHashMap<String,User> registeredUsers = new ConcurrentHashMap<>(); // Registered users map
    private ConcurrentLinkedQueue<Message> pmMessages = new ConcurrentLinkedQueue<>(); // list of all PM that have been sent
    private ConcurrentLinkedQueue<Message> posts = new ConcurrentLinkedQueue<>(); // list of all posts that have been posted
    private HashSet<String> wordsToFilter = new HashSet<String>(){{ //words that are forbidden and will be filtered with "<filtered>"
        add("Trump");
        add("War");
    }};


    public ConcurrentHashMap<String, User> getRegisteredUsers() {
        return registeredUsers;
    }

    public ConcurrentLinkedQueue<Message> getPmMessages() {
        return pmMessages;
    }

    public ConcurrentLinkedQueue<Message> getPosts() {
        return posts;

    }
    public User getUser(String username){
        return registeredUsers.get(username);
    }

    public boolean isRegistered(String username){
        return registeredUsers.containsKey(username);
    }
    public void addUser(String username, String password, String birthday){
        registeredUsers.put(username, new User(username, password, birthday));
    }
    public boolean correctCredentials(String username, String password){
        return registeredUsers.containsKey(username) && registeredUsers.get(username).getPassword().equals(password);
    }
    public HashSet<String> getWordsToFilter() {
        return wordsToFilter;
    }

    public static Database getInstance() {
        return getDatabase.instance ;
    }
    private static class getDatabase {
        private static  Database instance = new Database() ;
    }

}
