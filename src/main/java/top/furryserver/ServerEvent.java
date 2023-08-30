package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class ServerEvent extends AbstractEvent {

    private String action;

    public ServerEvent(String action){
        this.action = action;
    }

    public String getAction(){
        return action;
    }
}
