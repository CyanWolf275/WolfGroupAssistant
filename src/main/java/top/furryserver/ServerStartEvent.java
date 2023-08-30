package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class ServerStartEvent extends AbstractEvent {

    private Double t;
    public ServerStartEvent(Double t){
        this.t = t;
    }

    public Double getTime(){
        return t;
    }

}
