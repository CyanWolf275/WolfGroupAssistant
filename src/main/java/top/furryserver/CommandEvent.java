package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class CommandEvent extends AbstractEvent {

    private String cmd;
    public CommandEvent(String cmd){
        this.cmd = cmd;
    }
    public String getCmd(){
        return cmd;
    }
}
