package top.furryserver;

import net.mamoe.mirai.event.EventKt;

import java.io.*;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Server implements Runnable{

    private Process process;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Statement mcStmt;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean command = false;
    private Thread thread;

    public Server(Statement mcStmt){
        this.mcStmt = mcStmt;
    }

    public void start() throws IOException {
        if(thread != null && thread.isAlive())
            return;
        process = Runtime.getRuntime().exec("cmd /c cd C:\\Users\\Administrator\\Desktop\\forge1.16.5\\CustomJAVA\\bin && java -Xmx1500M -Xms1024M -jar C:\\Users\\Administrator\\Desktop\\forge1.16.5\\forge-1.16.5-36.2.34.jar nogui");
        outputStream = process.getOutputStream();
        inputStream = process.getInputStream();
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        while(this.process.isAlive()){
            try {
                str = bufferedReader.readLine();
                if(str != null)
                    if(str.indexOf("Done (") != -1){
                        EventKt.broadcast(new ServerStartEvent(Double.parseDouble(str.substring(str.indexOf("Done (") + 6, str.indexOf(")") - 1))));
                    }else if (str.indexOf("logged in with entity id") != -1){
                        String temp = str.split(" ")[4];
                        EventKt.broadcast(new PlayerJoinEvent(temp.substring(0, temp.indexOf("[")), temp.substring(temp.indexOf("[") + 2, temp.indexOf(":"))));
                    }else if (str.indexOf("left the game") != -1){
                        EventKt.broadcast(new PlayerExitEvent(str.split(" ")[4]));
                    }else if(str.indexOf("whitelist") != -1) {
                        if (str.indexOf("Added") != -1)
                            EventKt.broadcast(new WhitelistEvent(0, str.split(" ")[5]));
                        else if (str.indexOf("Removed") != -1)
                            EventKt.broadcast(new WhitelistEvent(1, str.split(" ")[5]));
                        else if (str.indexOf("already") != -1)
                            EventKt.broadcast(new WhitelistEvent(2, str.split(" ")[4]));
                    }else if(str.indexOf("players online:") != -1) {
                        String[] msgtemp = str.split(" ");
                        int n = Integer.parseInt(msgtemp[6]);
                        int max = Integer.parseInt(msgtemp[11]);
                        String[] players = new String[msgtemp.length - 14];
                        for (int i = 14; i < msgtemp.length; i++)
                            if (i == msgtemp.length - 1)
                                players[i - 14] = msgtemp[i].substring(0, msgtemp[i].length());
                            else
                                players[i - 14] = msgtemp[i].substring(0, msgtemp[i].length() - 1);
                        EventKt.broadcast(new PlayerListEvent(n, max, players));
                    }else if(command){
                        String msg = "";
                        if(str.indexOf("]:") != -1)
                            msg = str.substring(str.indexOf("]:") + 3);
                        else
                            msg = str;
                        EventKt.broadcast(new CommandEvent(msg));
                    }else {
                        EventKt.broadcast(new ServerEvent(str));
                    }
                command = false;
                if(str != null){
                    str = str.replaceAll("\\\\", "\\\\\\\\");
                    str = str.replaceAll("\'", "\\\\\'");
                    str = str.replaceAll("\"", "\\\\\"");
                    mcStmt.execute("INSERT INTO ServerLog(time,info) VALUES(\'" + sdf.format(new Date()) + "\',\'" + str + "\')");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EventKt.broadcast(new ServerCloseEvent(this.process.exitValue()));
    }

    public void send(String cmd) throws IOException {
        outputStream.write((cmd + "\n").getBytes());
        outputStream.flush();
        command = true;
    }

    public void silentsend(String cmd) throws IOException {
        outputStream.write((cmd + "\n").getBytes());
        outputStream.flush();
    }

    public boolean isRunning(){
        if(thread == null)
            return false;
        return thread.isAlive();
    }

    public void end() {
        this.process.destroy();
    }
}
