package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class ServerCloseEvent extends AbstractEvent {

    private int exitcode;

    public ServerCloseEvent(int exitcode){
        this.exitcode = exitcode;
    }

    public int getExitcode(){
        return exitcode;
    }
}
