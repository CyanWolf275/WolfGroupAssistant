package top.furryserver;

import net.mamoe.mirai.contact.Group;

public class Question {
    public String msg;
    public Group group;
    public long sender;
    public String name;
    public int n;
    public byte[] data;
    public String type;

    public Question(String msg, Group group, long sender, String name, int n){
        this.msg = msg;
        this.group = group;
        this.sender = sender;
        this.name = name;
        this.n = n;
        this.data = null;
        this.type = null;
    }
    public Question(byte[] data, Group group, long sender, String name, int n, String msg, String type){
        this.group = group;
        this.sender = sender;
        this.name = name;
        this.n = n;
        this.data = data;
        this.msg = msg;
        this.type = type;
    }
}
