package top.furryserver;

import java.util.List;

public class KookChannel {
    private long id;
    private String name;
    private int type;

    public KookChannel(long id, String name, int type){
        this.id = id;
        this.name = name;
        this.type = type;
    }
    public List<String> getVoiceUsers(){
        if(type == 1){
            return null;
        }else{
            return null;
            //TODO: api/v3/channel/user-list
        }
    }
}
