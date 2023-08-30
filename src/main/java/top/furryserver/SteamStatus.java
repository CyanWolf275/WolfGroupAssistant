package top.furryserver;

import org.jetbrains.annotations.NotNull;
public class SteamStatus implements Comparable<SteamStatus>{
    public final String[] STATUS_LIST = {"离线", "在线", "忙碌", "离开", "打盹", "正在寻找交易", "想玩游戏", "正在玩", "获取玩家信息失败", "获取玩家信息超时"};
    public final int[] STATUS_COMPARE = {7, 1, 2, 3, 4, 5, 6, 0, 8, 9};
    public String name;
    public String steamname;
    public int status;
    public String gamename;
    public SteamStatus(String name, String steamname, int status, String gamename){
        this.name = name;
        this.steamname = steamname;
        this.status = status;
        this.gamename = gamename;
    }
    public String getStringStatus(){
        String result = name;
        if(status < 8){
            if(status == 7)
                result += "（" + steamname + "）：" + STATUS_LIST[status] + gamename;
            else
                result += "（" + steamname + "）";
        }
        return result;
    }

    @Override
    public int compareTo(@NotNull SteamStatus o) {
        if(status == o.status){
            if(status == 7)
                return gamename.compareTo(o.gamename);
            else
                return name.compareTo(o.name);
        }
        else{
            return STATUS_COMPARE[status] - STATUS_COMPARE[o.status];
        }
    }
}
