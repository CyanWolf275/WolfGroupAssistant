package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class WhitelistEvent extends AbstractEvent {

    private int type;
    private String name;

    public WhitelistEvent(int type, String name){
        this.type = type;
        this.name = name;
    }

    public int getType(){
        return type;
    }

    public String getName(){
        return name;
    }
}
