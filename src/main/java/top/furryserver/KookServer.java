package top.furryserver;

import java.util.List;

public class KookServer {
    private long id;
    private String name;
    private List<KookChannel> channels;

    public KookServer(long id, String name){
        this.id = id;
        this.name = name;
        //TODO: /api/v3/channel/list
    }
}
