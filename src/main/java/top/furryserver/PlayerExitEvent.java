package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class PlayerExitEvent extends AbstractEvent {

    private String name;

    public PlayerExitEvent(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
