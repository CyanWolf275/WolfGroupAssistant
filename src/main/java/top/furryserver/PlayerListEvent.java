package top.furryserver;

import net.mamoe.mirai.event.AbstractEvent;

public class PlayerListEvent extends AbstractEvent {

    private int n;
    private int max;
    private String[] names;

    public PlayerListEvent(int n, int max, String[] names){
        this.n = n;
        this.max = max;
        this.names = names;
    }

    public int getN(){
        return n;
    }

    public int getMax(){
        return max;
    }

    public String[] getNames(){
        return names;
    }
}
