package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class ServerSaveEvent extends AbstractEvent {

    private boolean status;

    public ServerSaveEvent(boolean status){
        this.status = status;
    }

    public boolean getStatus(){
        return status;
    }

}
