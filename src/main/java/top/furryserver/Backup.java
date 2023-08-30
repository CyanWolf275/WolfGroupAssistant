package top.furryserver;

import net.mamoe.mirai.contact.Group;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Backup extends TimerTask {

    private Server server;
    private Group mcGroup;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    public Backup(Server server, Group mcGroup){
        this.server = server;
        this.mcGroup = mcGroup;
    }

    @Override
    public void run() {
        if(!server.isRunning())
            return;
        try {
            Date starttime = new Date();
            server.silentsend("say Server will temporarily shutdown in 10 minutes for backup. ");
            Thread.sleep(300000l);
            server.silentsend("say Server will temporarily shutdown in 5 minutes for backup. ");
            Thread.sleep(240000l);
            server.silentsend("say Server will temporarily shutdown in 60 seconds for backup. ");
            Thread.sleep(50000l);
            server.silentsend("say Server will temporarily shutdown in 10 seconds for backup. ");
            Thread.sleep(5000l);
            server.silentsend("say Server will temporarily shutdown in 5 seconds for backup. ");
            Thread.sleep(5000l);
            server.silentsend("stop");
            while(server.isRunning());
            Date date = new Date();
            Process process = Runtime.getRuntime().exec("cmd /c cd C:\\Program Files\\7-Zip && 7z.exe a -tzip C:\\Users\\Administrator\\Desktop\\PokemonWorld\\" + sdf.format(date) + ".zip C:\\Users\\Administrator\\Desktop\\forge1.16.5\\CustomJAVA\\bin\\world");
            if(process.waitFor(10, TimeUnit.MINUTES)){
                if(process.exitValue() == 0){
                    File file = new File("C:\\Users\\Administrator\\Desktop\\PokemonWorld\\" + sdf.format(date) + ".zip");
                    BigDecimal bigDecimal = new BigDecimal((double)file.length() / 1024 / 1024 / 1024);
                    bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
                    mcGroup.sendMessage("服务器世界在" + sdf.format(date) + "完成备份，备份大小" + bigDecimal.toString() + "GB，下次备份将在" + sdf.format(new Date(starttime.getTime() + 87000000l)) + "进行。");
                    server.start();
                }else{
                    mcGroup.sendMessage("服务器备份出错！");
                }
            }else{
                mcGroup.sendMessage("服务器备份超时！");
                process.destroy();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
