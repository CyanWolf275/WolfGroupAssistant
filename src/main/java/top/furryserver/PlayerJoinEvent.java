package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class PlayerJoinEvent extends AbstractEvent {

    private String name;
    private String ip;

    public PlayerJoinEvent(String name, String ip){
        this.name = name;
        this.ip = ip;
    }

    public String getName(){
        return name;
    }

    public String getIp(){
        return ip;
    }
}
