package top.furryserver;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Plugin extends JavaPlugin {
    public static final Plugin INSTANCE = new Plugin();
    private static Connection[] conn;
    private static Statement[] stmt;
    private static Connection mcConn;
    private static Statement mcStmt;
    private static Connection chatConn;
    private static Statement chatStmt;
    private static List<Long> groupid = new ArrayList<Long>();
    private static List<Long> gptGroupid = new ArrayList<Long>();
    private static Group[] groups;
    private static int len;
    private static int[] levels;
    private static String dir;
    private static String chatdir;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat sdfhm = new SimpleDateFormat("HH:mm");
    private static Timer timer;
    private static Timer backup_timer;
    private static Timer hb_timer;
    private static KeepLive kl;
    private static Backup backup;
    private static Birthday birthday;
    private static Server server;
    private static Group mcGroup;
    private static boolean loginMsg = true;
    private static Connection jrrpConn;
    private static Statement jrrpStmt;
    private static Connection[] gptConn;
    private static Statement[] gptStmt;
    private static Statement steamStmt;
    private static boolean chatReady;
    private static List<ChatMessage> chatMessages;
    private static int chatMode;
    private static Map<Long, ChatUser> chatUserList;
    private static Bot bot;
    private static Map<Long, NewFriendRequestEvent> requestEvents;
    private static ArrayBlockingQueue<Question> questionQueue;
    private static ArrayBlockingQueue<Question> imageQueue;
    private static ArrayBlockingQueue<Question> varimgQueue;
    private static ArrayBlockingQueue<Question> queryQueue;
    private static ArrayBlockingQueue<Question> bingQueue;
    private static ArrayBlockingQueue<Question> steamQueue;
    private static Thread chat;
    private static Thread imagechat;
    private static Thread varimgchat;
    private static Thread querychat;
    private static Thread bingchat;
    private static Thread steam;
    private static PreparedStatement[] pslst;
    private static PreparedStatement[] chatStmtlst;
    private static PreparedStatement[] memberpslst;
    private static PreparedStatement groupStmt;
    private static Properties properties;
    private static BCryptPasswordEncoder bCryptPasswordEncoder;
    private static String steamapi = "";
    private static long admin = 0L;
    private static final String[] STATUS_LIST = {"离线", "在线", "忙碌", "离开", "打盹", "正在寻找交易", "想玩游戏", "正在玩", "获取玩家信息失败", "获取玩家信息超时"};
    private static String kook = null;

    private Plugin() {
        super(new JvmPluginDescriptionBuilder("top.furryserver.plugin", "1.0-SNAPSHOT")
                .author("CyanWolf")
                .build());
    }

    @Override
    public void onEnable() {
        properties = new Properties();
        try {
            FileInputStream propertyInput = new FileInputStream("config" + File.separator + "config.properties");
            properties.load(propertyInput);
            propertyInput.close();
            String[] grouplst = properties.getProperty("groups").split(",");
            len = grouplst.length;
            for(String groupnum : grouplst)
                groupid.add(Long.parseLong(groupnum.trim()));
            dir = properties.getProperty("filedir") + File.separator;
            chatdir = properties.getProperty("chatdir") + File.separator;
            String[] gptgrouplst = properties.getProperty("gpt_groups").split(",");
            for(String groupnum : gptgrouplst)
                gptGroupid.add(Long.parseLong(groupnum.trim()));
            steamapi = properties.getProperty("steam");
            admin = Long.parseLong(properties.getProperty("admin").trim());
            kook = properties.getProperty("kook").trim();
        } catch (IOException | NumberFormatException e) {
            this.getLogger().error("加载配置文件出错");
            System.exit(-1);
            throw new RuntimeException(e);
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        len = groupid.size();
        conn = new Connection[len];
        stmt = new Statement[len];
        groups = new Group[len];
        levels = new int[len];
        pslst = new PreparedStatement[len];
        memberpslst = new PreparedStatement[len];
        chatStmtlst = new PreparedStatement[len];
        gptConn = new Connection[len];
        gptStmt = new Statement[len];
        bCryptPasswordEncoder = new BCryptPasswordEncoder();
        try {
            for(int i=0;i<groupid.size();i++){
                conn[i] = DriverManager.getConnection(properties.getProperty("db.url") + groupid.get(i) + "?useSSL=false&autoreconnect=true&autoreconnectforpools=true&failOverReadOnly=false", properties.getProperty("db.username.server"), properties.getProperty("db.password.server"));
                stmt[i] = conn[i].createStatement();
                stmt[i].execute("SET character_set_client = utf8mb4");
                pslst[i] = conn[i].prepareStatement("INSERT INTO Message(time,type,sender,content) VALUES(?, ?, ?, ?)");
                pslst[i].clearParameters();
                memberpslst[i] = conn[i].prepareStatement("INSERT INTO Members(id,name,jointime,level) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE name=?, jointime=?, level=?");
                if(gptGroupid.contains(groupid.get(i))){
                    chatStmtlst[i] = conn[i].prepareStatement("INSERT INTO Chat(user,question,response,time,tokens) VALUES(?, ?, ?, ?, ?)");
                    chatStmtlst[i].clearParameters();
                    gptConn[i] = DriverManager.getConnection(properties.getProperty("db.url") + groupid.get(i) + "?useSSL=false&autoreconnect=true&autoreconnectforpools=true&failOverReadOnly=false", properties.getProperty("db.username.gpt"), properties.getProperty("db.password.gpt"));
                    gptStmt[i] = gptConn[i].createStatement();
                    gptStmt[i].execute("SET character_set_client = utf8mb4");
                }
            }
            mcConn = DriverManager.getConnection(properties.getProperty("db.url") + "Minecraft?useSSL=false&autoreconnect=true&autoreconnectforpools=true&failOverReadOnly=false", properties.getProperty("db.username.server"), properties.getProperty("db.password.server"));
            mcStmt = mcConn.createStatement();
            mcStmt.execute("SET character_set_client = utf8mb4");
            jrrpConn = DriverManager.getConnection(properties.getProperty("db.url") + "Views?useSSL=false&autoreconnect=true&autoreconnectforpools=true&failOverReadOnly=false", properties.getProperty("db.username.server"), properties.getProperty("db.password.server"));
            jrrpStmt = jrrpConn.createStatement();
            jrrpStmt.execute("SET character_set_client = utf8mb4");
            chatConn = DriverManager.getConnection(properties.getProperty("db.url") + "Views?useSSL=false&autoreconnect=true&autoreconnectforpools=true&failOverReadOnly=false", properties.getProperty("db.username.server"), properties.getProperty("db.password.server"));
            chatStmt = chatConn.createStatement();
            chatStmt.execute("SET character_set_client = utf8mb4");
            steamStmt = jrrpConn.createStatement();
            groupStmt = chatConn.prepareStatement("INSERT INTO Groups(id, name) VALUES(?, ?) ON DUPLICATE KEY UPDATE name = ?");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        chatReady = true;
        chatMessages = new ArrayList<ChatMessage>();
        chatMode = 0;
        chatUserList = new HashMap<Long, ChatUser>();
        requestEvents = new HashMap<Long, NewFriendRequestEvent>();
        questionQueue = new ArrayBlockingQueue<>(5);
        imageQueue = new ArrayBlockingQueue<>(3);
        varimgQueue = new ArrayBlockingQueue<>(3);
        //queryQueue = new ArrayBlockingQueue<>(5);
        bingQueue = new ArrayBlockingQueue<>(5);
        steamQueue = new ArrayBlockingQueue<>(10);
        chat = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    Question q;
                    try {
                        q = questionQueue.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        /*
                        File hs = new File("C:\\Users\\Administrator\\Desktop\\chat\\history.txt");
                        FileOutputStream hsfs = new FileOutputStream(hs);
                        hsfs.write(chatHistoryString(q.n, 10).getBytes("UTF-8"));
                        hsfs.close();
                         */
                        File js = new File(chatdir + "chat.txt");
                        FileOutputStream fs = new FileOutputStream(js);
                        String message = "";
                        if(q.sender == 3271285330l)
                            message += "坦奇: " + q.msg;
                        else
                            message += q.name + ": " + q.msg;
                        fs.write(message.getBytes("UTF-8"));
                        fs.close();
                        String response = "";
                        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python chat.py");
                        if (process.waitFor(90, TimeUnit.SECONDS)) {
                            if (process.exitValue() == 0) {
                                File file = new File(chatdir + "response.txt");
                                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                                while (reader.ready())
                                    response += reader.readLine();
                                String responsetime = sdf.format(new Date());
                                File tokens = new File(chatdir + "tokens.txt");
                                BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                                int numtoken = Integer.parseInt(tokenreader.readLine());
                                tokenreader.close();
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, response);
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                chatStmtlst[q.n].setLong(1, q.sender);
                                chatStmtlst[q.n].setString(2, q.msg);
                                chatStmtlst[q.n].setString(3, response);
                                chatStmtlst[q.n].setString(4, responsetime);
                                chatStmtlst[q.n].setInt(5, numtoken);
                                chatStmtlst[q.n].execute();
                                chatStmtlst[q.n].clearParameters();
                                q.group.sendMessage(response);
                                reader.close();
                            } else {
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, "获取回答时API发生错误");
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage("获取回答时API发生错误");
                            }
                        } else {
                            process.destroy();
                            String responsetime = sdf.format(new Date());
                            pslst[q.n].setString(1, responsetime);
                            pslst[q.n].setInt(2, 0);
                            pslst[q.n].setLong(3, 2784617026l);
                            pslst[q.n].setString(4, "获取回答超时");
                            pslst[q.n].execute();
                            pslst[q.n].clearParameters();
                            q.group.sendMessage("获取回答超时");
                        }
                    } catch (IOException | InterruptedException e) {
                        q.group.sendMessage("获取回答时发生IO错误");
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        q.group.sendMessage("获取回答时发生SQL错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        imagechat = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    Question q;
                    try {
                        q = imageQueue.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        File js = new File(chatdir + "image.txt");
                        FileOutputStream fs = new FileOutputStream(js);
                        fs.write(q.msg.getBytes(StandardCharsets.UTF_8));
                        fs.close();
                        String response = "";
                        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python image.py");
                        if (process.waitFor(60, TimeUnit.SECONDS)) {
                            if (process.exitValue() == 0) {
                                File file = new File(chatdir + "imageresponse.txt");
                                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                                response = reader.readLine();
                                String responsetime = sdf.format(new Date());
                                File tokens = new File(chatdir + "imagetokens.txt");
                                BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                                int numtoken = Integer.parseInt(tokenreader.readLine());
                                tokenreader.close();
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, response);
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                chatStmtlst[q.n].setLong(1, q.sender);
                                chatStmtlst[q.n].setString(2, q.msg);
                                chatStmtlst[q.n].setString(3, response);
                                chatStmtlst[q.n].setString(4, responsetime);
                                chatStmtlst[q.n].setInt(5, numtoken);
                                chatStmtlst[q.n].execute();
                                chatStmtlst[q.n].clearParameters();
                                String imgname = response.substring(response.indexOf("/img-") + 1, response.indexOf("?"));
                                File imgfile = new File(dir + "ai-img" + File.separator + imgname);
                                URL imglink = new URL(response);
                                HttpURLConnection httpconn = (HttpURLConnection) imglink.openConnection();
                                httpconn.setRequestMethod("GET");
                                InputStream is = httpconn.getInputStream();
                                byte[] data = readInputStream(is);
                                FileOutputStream outstream = new FileOutputStream(imgfile);
                                outstream.write(data);
                                outstream.close();
                                httpconn.disconnect();
                                ExternalResource externalResource = ExternalResource.create(imgfile);
                                MessageChain chain = MessageUtils.newChain(q.group.uploadImage(externalResource));
                                q.group.sendMessage(chain);
                                externalResource.close();
                                reader.close();
                            } else {
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026L);
                                pslst[q.n].setString(4, "获取回答时API发生错误");
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage("获取回答时API发生错误");
                            }
                        } else {
                            process.destroy();
                            String responsetime = sdf.format(new Date());
                            pslst[q.n].setString(1, responsetime);
                            pslst[q.n].setInt(2, 0);
                            pslst[q.n].setLong(3, 2784617026L);
                            pslst[q.n].setString(4, "获取回答超时");
                            pslst[q.n].execute();
                            pslst[q.n].clearParameters();
                            q.group.sendMessage("获取回答超时");
                        }
                    } catch (IOException | InterruptedException e) {
                        q.group.sendMessage("获取回答时发生IO错误");
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        q.group.sendMessage("获取回答时发生SQL错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        varimgchat = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    Question q;
                    try {
                        q = varimgQueue.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        File js = new File(chatdir + "image");
                        FileOutputStream fs = new FileOutputStream(js);
                        fs.write(q.data);
                        fs.close();
                        String response = "";
                        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python varimage.py");
                        if (process.waitFor(60, TimeUnit.SECONDS)) {
                            if (process.exitValue() == 0) {
                                File file = new File(chatdir + "varimageresponse.txt");
                                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                                response = reader.readLine();
                                String responsetime = sdf.format(new Date());
                                File tokens = new File(chatdir + "varimagetokens.txt");
                                BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                                int numtoken = Integer.parseInt(tokenreader.readLine());
                                tokenreader.close();
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, response);
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                chatStmtlst[q.n].setLong(1, q.sender);
                                chatStmtlst[q.n].setString(2, q.msg + "." + q.type);
                                chatStmtlst[q.n].setString(3, response);
                                chatStmtlst[q.n].setString(4, responsetime);
                                chatStmtlst[q.n].setInt(5, numtoken);
                                chatStmtlst[q.n].execute();
                                chatStmtlst[q.n].clearParameters();
                                String imgname = response.substring(response.indexOf("/img-") + 1, response.indexOf("?"));
                                File imgfile = new File(dir + "ai-img" + File.separator + imgname);
                                URL imglink = new URL(response);
                                HttpURLConnection httpconn = (HttpURLConnection) imglink.openConnection();
                                httpconn.setRequestMethod("GET");
                                InputStream is = httpconn.getInputStream();
                                byte[] data = readInputStream(is);
                                FileOutputStream outstream = new FileOutputStream(imgfile);
                                outstream.write(data);
                                outstream.close();
                                httpconn.disconnect();
                                ExternalResource externalResource = ExternalResource.create(imgfile);
                                MessageChain chain = MessageUtils.newChain(q.group.uploadImage(externalResource));
                                q.group.sendMessage(chain);
                                externalResource.close();
                                reader.close();
                            } else {
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, "获取回答时API发生错误");
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage("获取回答时API发生错误");
                            }
                        } else {
                            process.destroy();
                            String responsetime = sdf.format(new Date());
                            pslst[q.n].setString(1, responsetime);
                            pslst[q.n].setInt(2, 0);
                            pslst[q.n].setLong(3, 2784617026l);
                            pslst[q.n].setString(4, "获取回答超时");
                            pslst[q.n].execute();
                            pslst[q.n].clearParameters();
                            q.group.sendMessage("获取回答超时");
                        }
                    } catch (IOException | InterruptedException e) {
                        q.group.sendMessage("获取回答时发生IO错误");
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        q.group.sendMessage("获取回答时发生SQL错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        querychat = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Question q = null;
                    try {
                        q = queryQueue.take();
                        query(q.msg, q.n, q.group, q.sender);
                    } catch (InterruptedException e) {
                        q.group.sendMessage("获取回答时发生中断错误");
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        q.group.sendMessage("获取回答时发生SQL错误");
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        q.group.sendMessage("获取回答时发生IO错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        bingchat = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    Question q;
                    try {
                        q = bingQueue.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        File js = new File(chatdir + "bingchat.txt");
                        FileOutputStream fs = new FileOutputStream(js);
                        fs.write(q.msg.getBytes("UTF-8"));
                        fs.close();
                        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python bingchat.py");
                        if (process.waitFor(120, TimeUnit.SECONDS)) {
                            if (process.exitValue() == 0) {
                                String response = new String(Files.readAllBytes(Paths.get(chatdir + "bingresponse.txt")), StandardCharsets.UTF_8);
                                response = response.replaceAll("\\[\\^\\d+\\^\\]", "");
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, response);
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage(response);
                            } else {
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, "获取回答时API发生错误");
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage("获取回答时API发生错误");
                            }
                        } else {
                            process.destroy();
                            String responsetime = sdf.format(new Date());
                            pslst[q.n].setString(1, responsetime);
                            pslst[q.n].setInt(2, 0);
                            pslst[q.n].setLong(3, 2784617026l);
                            pslst[q.n].setString(4, "获取回答超时");
                            pslst[q.n].execute();
                            pslst[q.n].clearParameters();
                            q.group.sendMessage("获取回答超时");
                        }
                    } catch (IOException | InterruptedException e) {
                        q.group.sendMessage("获取回答时发生IO错误");
                        throw new RuntimeException(e);
                    } catch (SQLException e) {
                        q.group.sendMessage("获取回答时发生SQL错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        steam = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Question q;
                    try {
                        q = steamQueue.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try{
                        File link = new File(chatdir + "steam.txt");
                        FileOutputStream fs = new FileOutputStream(link);
                        fs.write(q.msg.getBytes("UTF-8"));
                        fs.close();
                        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python steam.py");
                        if(process.waitFor(10, TimeUnit.SECONDS)){
                            if(process.exitValue() == 0){
                                String response = new String(Files.readAllBytes(Paths.get(chatdir + "steamresponse.txt")), StandardCharsets.UTF_8);
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, response);
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage(response);
                            }else{
                                process.destroy();
                                String responsetime = sdf.format(new Date());
                                pslst[q.n].setString(1, responsetime);
                                pslst[q.n].setInt(2, 0);
                                pslst[q.n].setLong(3, 2784617026l);
                                pslst[q.n].setString(4, "获取回答出错");
                                pslst[q.n].execute();
                                pslst[q.n].clearParameters();
                                q.group.sendMessage("获取回答出错");
                            }
                        }else{
                            process.destroy();
                            String responsetime = sdf.format(new Date());
                            pslst[q.n].setString(1, responsetime);
                            pslst[q.n].setInt(2, 0);
                            pslst[q.n].setLong(3, 2784617026l);
                            pslst[q.n].setString(4, "获取回答超时");
                            pslst[q.n].execute();
                            pslst[q.n].clearParameters();
                            q.group.sendMessage("获取回答超时");
                        }
                    } catch (IOException | SQLException | InterruptedException e) {
                        q.group.sendMessage("获取回答出错");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Listener login = GlobalEventChannel.INSTANCE.subscribeOnce(BotOnlineEvent.class, event -> {
            for (int i = 0; i < groupid.size(); i++)
                groups[i] = Bot.getInstances().get(0).getGroup(groupid.get(i));
            bot = Bot.getInstances().get(0);
            mcGroup = bot.getGroup(1078074683l);
            timer = new Timer();
            backup_timer = new Timer();
            hb_timer = new Timer();
            kl = new KeepLive(groups, stmt, mcStmt, jrrpStmt, gptStmt, chatStmt, levels, groupStmt, gptGroupid, memberpslst, steamStmt);
            timer.schedule(kl, 0l, 3600000l);
            this.run();
            server = new Server(mcStmt);
            backup = new Backup(server, mcGroup);
            birthday = new Birthday(groups, stmt, gptGroupid);
            Date currenttime = new Date();
            Date backup_time;
            Date hb_time;
            Date backup_newtime;
            Date hb_newtime;
            try {
                backup_time = sdf.parse((currenttime.getYear() + 1900) + "-" + (currenttime.getMonth() + 1) + "-" + currenttime.getDate() + " 03:50:00");
                hb_time = sdf.parse((currenttime.getYear() + 1900) + "-" + (currenttime.getMonth() + 1) + "-" + currenttime.getDate() + " 00:00:00");
                if (backup_time.getTime() < currenttime.getTime()) {
                    backup_newtime = new Date(currenttime.getTime() + 86400000l - (currenttime.getTime() - backup_time.getTime()));
                } else {
                    backup_newtime = new Date(backup_time.getTime());
                }
                hb_newtime = new Date(currenttime.getTime() + 86400000l - (currenttime.getTime() - hb_time.getTime()));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            backup_timer.schedule(backup, backup_newtime.getTime() - currenttime.getTime(), 86400000l);
            hb_timer.schedule(birthday, hb_newtime.getTime() - currenttime.getTime(), 86400000l);
            //mcGroup.sendMessage("大狼更新日志：\n");
        });
    }

    public void run() {
        chat.start();
        imagechat.start();
        varimgchat.start();
        //querychat.start();
        bingchat.start();
        steam.start();
        getLogger().info("Plugin loaded!");
        Listener serverstart = GlobalEventChannel.INSTANCE.subscribeOnce(ServerStartEvent.class, serverst -> {
            mcGroup.sendMessage("服务器已在" + serverst.getTime() + "秒内启动");
        });
        Listener serverevent = GlobalEventChannel.INSTANCE.subscribeAlways(ServerEvent.class, se -> {
            getLogger().info(se.getAction());
        });
        Listener playerjoin = GlobalEventChannel.INSTANCE.subscribeAlways(PlayerJoinEvent.class, pj -> {
            try {
                mcStmt.execute("INSERT INTO Log(name,ip,time,status) VALUES(\'" + pj.getName() + "\',\'" + pj.getIp() + "\',\'" + sdf.format(new Date()) + "\',1)");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (loginMsg) {
                URL url = null;
                try {
                    url = new URL("http://ip-api.com/csv/" + pj.getIp()/* + "?lang=zh-CN" */);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection con = null;
                try {
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.connect();
                    if (con.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                        String content = new String(in.readLine().getBytes(), "UTF-8");
                        String[] msg = content.split(",");
                        mcGroup.sendMessage(pj.getName() + "已登录\n" + "来自" + msg[1] + "/" + msg[4]/*.substring(0, msg[4].length() - 2)*/ + "/" + msg[5]);
                        in.close();
                    }
                    con.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Listener playerexit = GlobalEventChannel.INSTANCE.subscribeAlways(PlayerExitEvent.class, pe -> {
            try {
                mcStmt.execute("INSERT INTO Log(name,time,status) VALUES(\'" + pe.getName() + "\',\'" + sdf.format(new Date()) + "\',0)");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (loginMsg)
                mcGroup.sendMessage(pe.getName() + "退出了游戏");
        });
        Listener serverclose = GlobalEventChannel.INSTANCE.subscribeAlways(ServerCloseEvent.class, sc -> {
            mcGroup.sendMessage("服务器已关闭，退出代码为" + sc.getExitcode());
            server.end();
        });
        Listener whitelist = GlobalEventChannel.INSTANCE.subscribeAlways(WhitelistEvent.class, wl -> {
            switch (wl.getType()) {
                case 0:
                    mcGroup.sendMessage("已将" + wl.getName() + "加入白名单");
                    break;
                case 1:
                    mcGroup.sendMessage("已将" + wl.getName() + "移除白名单");
                    break;
                case 2:
                    mcGroup.sendMessage("该玩家已在白名单中");
            }
        });
        Listener playerlist = GlobalEventChannel.INSTANCE.subscribeAlways(PlayerListEvent.class, pl -> {
            String msg = "在线人数：" + pl.getN() + " / " + pl.getMax() + "\n";
            String[] names = pl.getNames();
            for (String name : names)
                msg += name + "\n";
            mcGroup.sendMessage(msg.substring(0, msg.length() - 1));
        });
        Listener command = GlobalEventChannel.INSTANCE.subscribeAlways(CommandEvent.class, cmd -> {
            mcGroup.sendMessage(cmd.getCmd());
        });
        /*
        Listener recall = GlobalEventChannel.INSTANCE.subscribeAlways(MessageRecallEvent.GroupRecall.class, groupRecall -> {
            int num = groupid.indexOf(groupRecall.getGroup().getId());
        });
         */
        Listener strangerMsg = GlobalEventChannel.INSTANCE.subscribeAlways(StrangerMessageEvent.class, strangerMessageEvent -> {
            String msg = strangerMessageEvent.getMessage().contentToString();
            if(msg.startsWith(">steam")){
                if(msg.substring(7).startsWith("remove")){
                    try{
                        jrrpStmt.execute("update all_members_groups set steam=null where id=" + strangerMessageEvent.getSender().getId());
                    } catch (SQLException e) {
                        strangerMessageEvent.getSender().sendMessage("系统发生错误");
                        throw new RuntimeException(e);
                    }
                }else{
                    try{
                        long steamid = Long.parseLong(msg.substring(7));
                        jrrpStmt.execute("update all_members_groups set steam=" + steamid + " where id=" + strangerMessageEvent.getSender().getId());
                        strangerMessageEvent.getSender().sendMessage("成功更新steam id");
                    }catch (NumberFormatException | IndexOutOfBoundsException e){
                        strangerMessageEvent.getSender().sendMessage("Steam id格式错误，请输入>steam [你的17位steam id]。");
                    } catch (SQLException e) {
                        strangerMessageEvent.getSender().sendMessage("系统发生错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Listener tempMsg = GlobalEventChannel.INSTANCE.subscribeAlways(GroupTempMessageEvent.class, groupTempMessageEvent -> {
            String msg = groupTempMessageEvent.getMessage().contentToString();
            if(msg.startsWith(">steam")){
                if(msg.substring(7).startsWith("remove")){
                    try{
                        jrrpStmt.execute("update all_members_groups set steam=null where id=" + groupTempMessageEvent.getSender().getId());
                    } catch (SQLException e) {
                        groupTempMessageEvent.getSender().sendMessage("系统发生错误");
                        throw new RuntimeException(e);
                    }
                }else{
                    try{
                        long steamid = Long.parseLong(msg.substring(7));
                        jrrpStmt.execute("update all_members_groups set steam=" + steamid + " where id=" + groupTempMessageEvent.getSender().getId());
                        groupTempMessageEvent.getSender().sendMessage("成功更新steam id");
                    }catch (NumberFormatException | IndexOutOfBoundsException e){
                        groupTempMessageEvent.getSender().sendMessage("Steam id格式错误，请输入>steam [你的17位steam id]。");
                    } catch (SQLException e) {
                        groupTempMessageEvent.getSender().sendMessage("系统发生错误");
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Listener friendMessage = GlobalEventChannel.INSTANCE.subscribeAlways(FriendMessageEvent.class, friendmsg -> {
            //TODO
            long id = friendmsg.getSender().getId();
            String msg = friendmsg.getMessage().contentToString();
            Friend friend = friendmsg.getFriend();
            ChatUser chatUser;
            if(!chatUserList.containsKey(id)) {
                try {
                    chatUser = new ChatUser(friend, chatdir, id == admin);
                    chatUserList.put(id, chatUser);
                } catch (IOException e) {
                    friend.sendMessage("发生未知错误");
                    throw new RuntimeException(e);
                }
            }else{
                chatUser = chatUserList.get(id);
            }
            if(msg.startsWith(">")){
                String[] cmdlst = msg.substring(1).split("\\s+");
                if(cmdlst[0].equals("display"))
                    friend.sendMessage("当前的指示为\"" + chatUser.getInstruction() + "\"");
                else if(cmdlst[0].equals("instru")){
                    if(cmdlst[1].equals("num")){
                        try{
                            chatUser.setInstruction(Integer.parseInt(cmdlst[2]));
                            friend.sendMessage("成功将指示变更为" + cmdlst[2] + "号");
                        }catch(NumberFormatException e){
                            friend.sendMessage("请输入数字编号");
                        }
                    }else{
                        String instr = msg.substring(8);
                        chatUser.setInstruction(instr);
                        friend.sendMessage("成功将指示变更为\"" + instr + "\"");
                    }
                }else if(cmdlst[0].equals("C")){
                    chatUser.clearHistory();
                    friend.sendMessage("成功清除聊天历史");
                }else if(cmdlst[0].equals("R")){
                    if(cmdlst.length > 1){
                        if(chatUser.chatReady){
                            friend.sendMessage("[重新生成]");
                            chatUser.pushback();
                            String sqlresult = chatUser.chat(cmdlst[1]);
                            if(sqlresult != null){
                                try {
                                    chatStmt.execute(sqlresult);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }else{
                            friend.sendMessage("[大狼正在回答上一条消息，请稍后]");
                        }
                    }else{
                        if(chatUser.chatReady){
                            friend.sendMessage("[重新生成上条消息]");
                            String sqlresult = chatUser.regenerate();
                            if(sqlresult != null){
                                try {
                                    chatStmt.execute(sqlresult);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }else{
                            friend.sendMessage("[大狼正在回答上一条消息，请稍后]");
                        }
                    }
                }else if(cmdlst[0].equals("B")){
                    if(chatUser.pushback())
                        friend.sendMessage("成功回退到上一条消息");
                    else
                        friend.sendMessage("回退失败，无消息历史");
                }else if(cmdlst[0].equals("save")){
                    File msgfile;
                    if(cmdlst.length <= 1)
                        msgfile = chatUser.saveToFile();
                    else
                        msgfile = chatUser.saveToFile(cmdlst[1]);
                    if(msgfile == null)
                        friend.sendMessage("保存至文件出错，请检查文件名是否合法");
                    else{
                        friend.sendMessage("保存成功" + msgfile.getName());
                    }
                }else if(cmdlst[0].equals("read")){
                    if(cmdlst.length > 1){
                        try {
                            int msgcount = chatUser.readFromFile(cmdlst[1]);
                            if(msgcount >= 0)
                                friend.sendMessage("成功读取" + msgcount + "条消息");
                            else
                                friend.sendMessage("文件不存在");
                        } catch (IOException e) {
                            friend.sendMessage("读取文件出错");
                            throw new RuntimeException(e);
                        }
                    }else{
                        friend.sendMessage("请输入文件名");
                    }
                }else if(cmdlst[0].equals("list")) {
                    String[] filenames = chatUser.listFiles();
                    if (filenames.length == 0)
                        friend.sendMessage("没有已保存的聊天记录");
                    else {
                        String files = "已保存的聊天记录：\n";
                        for (String file : filenames)
                            files += file + "\n";
                        friend.sendMessage(files.substring(0, files.length() - 1));
                    }
                }else if(cmdlst[0].equals("steam")){
                    if(cmdlst[1].equals("remove")){
                        try{
                            jrrpStmt.execute("update all_members_groups set steam=null where id=" + friend.getId());
                            friend.sendMessage("成功删除steam id");
                        } catch (SQLException e) {
                            friend.sendMessage("系统发生错误");
                            throw new RuntimeException(e);
                        }
                    }else{
                        try{
                            long steamid = Long.parseLong(cmdlst[1]);
                            jrrpStmt.execute("update all_members_groups set steam=" + steamid + " where id=" + friend.getId());
                            friend.sendMessage("成功更新steam id");
                        }catch (NumberFormatException | IndexOutOfBoundsException e){
                            friend.sendMessage("Steam id格式错误，请输入>steam [你的17位steam id]。");
                        } catch (SQLException e) {
                            friend.sendMessage("系统发生错误");
                            throw new RuntimeException(e);
                        }
                    }
                }else if(cmdlst[0].equals("friend")){
                    if(id == admin)
                        if(cmdlst.length >= 2){
                            try{
                                if(cmdlst[1].equals("accept")){
                                    requestEvents.get(Long.parseLong(cmdlst[2])).accept();
                                    friend.sendMessage("已同意" + cmdlst[2] + "的好友申请");
                                    requestEvents.remove(Long.parseLong(cmdlst[2]));
                                }else if(cmdlst[1].equals("reject")){
                                    requestEvents.get(Long.parseLong(cmdlst[2])).reject(false);
                                    friend.sendMessage("已拒绝" + cmdlst[2] + "的好友申请");
                                    requestEvents.remove(Long.parseLong(cmdlst[2]));
                                }else if(cmdlst[1].equals("list")){
                                    if(requestEvents.size() != 0){
                                        String result = "";
                                        for(Long num : requestEvents.keySet())
                                            result += num + ", ";
                                        friend.sendMessage(result);
                                    }else{
                                        friend.sendMessage("没有待处理的好友申请");
                                    }
                                }else{
                                    friend.sendMessage("没有这个选项");
                                }
                            }catch (NullPointerException e){
                                friend.sendMessage("没有此人的好友申请");
                            }catch (NumberFormatException e){
                                friend.sendMessage("QQ号格式不正确");
                            }
                        }else{
                            friend.sendMessage("请给出选项");
                        }
                }
            }else{
                if(chatUser.chatReady){
                    String sqlresult = chatUser.chat(msg);
                    if(sqlresult != null){
                        try {
                            chatStmt.execute(sqlresult);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }else{
                    friend.sendMessage("[大狼正在回答上一条消息，请稍后]");
                }
            }
                /*
                if (msg.startsWith(">")) {
                    if (msg.charAt(1) == 'P') {
                        try {
                            String prompt = msg.substring(3);
                            File p = new File("C:\\Users\\Administrator\\Desktop\\chat\\prompt.txt");
                            FileOutputStream fs = new FileOutputStream(p);
                            fs.write(prompt.getBytes("UTF-8"));
                            fs.close();
                            friendmsg.getFriend().sendMessage("已成功将群指示改为\"" + prompt + "\"。");
                        } catch (IOException e) {
                            friendmsg.getFriend().sendMessage("调整群指示发生错误，" + e.getMessage());
                            e.printStackTrace();
                        }
                    } else if (msg.charAt(1) == 'D') {
                        try {
                            File file = new File("C:\\Users\\Administrator\\Desktop\\chat\\prompt.txt");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                            String result = "";
                            while (reader.ready())
                                result += reader.readLine();
                            reader.close();
                            friendmsg.getFriend().sendMessage("当前群指示为\"" + result + "\"。");
                        } catch (IOException e) {
                            friendmsg.getFriend().sendMessage("读取群指示发生错误，" + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                }*/
        });
        Listener addFriend = GlobalEventChannel.INSTANCE.subscribeAlways(FriendAddEvent.class, friendAddEvent -> {
            friendAddEvent.getFriend().sendMessage("欢迎使用大狼聊天系统！以下是一些注意事项和使用方法。\n" +
                    "1. 每一段对话对话前，大狼会收到一份指示，告诉大狼应该在这段对话中做什么或扮演怎样的角色。默认的指示为\"你是一位助手，负责回答问题\"。你可以通过\">I [指示内容]\"来更改指示，但请注意每次执行此操作时都会清空聊天历史并开始一段新的对话。\n" +
                    "3. 所有命令的格式为\">[指令] [参数]\"，方括号表示此处应该填入相应的内容，请不要输入方括号。\n" +
                    "4. 你可以使用\">D\"指令显示当前对话的指示内容。\n" +
                    "5. 你可以使用\">C\"指令来清空聊天历史并开始新的对话。\n" +
                    "祝你聊天愉快！");
        });
        Listener friendRequest = GlobalEventChannel.INSTANCE.subscribeAlways(NewFriendRequestEvent.class, friendreq -> {
            Friend adminFriend = bot.getFriend(admin);
            adminFriend.sendMessage(friendreq.getFromId() + "请求添加为好友");
            requestEvents.put(friendreq.getFromId(), friendreq);
        });
        Listener group = GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, groupmsg -> {
            if (groupid.contains(groupmsg.getGroup().getId())) {
                int idx = groupid.indexOf(groupmsg.getGroup().getId());
                MessageChain msg = groupmsg.getMessage();
                long sender = groupmsg.getSender().getId();
                String time = sdf.format(new Date(1000l * (long) groupmsg.getTime()));
                String temptxt = "";
                for (SingleMessage element : msg) {
                    if (element instanceof PlainText || element instanceof At || element instanceof AtAll || element instanceof Face) {
                        temptxt += element.contentToString();
                    } else if (element instanceof Image) {
                        if (temptxt.length() != 0) {
                            try {
                                pslst[idx].setString(1, time);
                                pslst[idx].setInt(2, 0);
                                pslst[idx].setLong(3, sender);
                                pslst[idx].setString(4, temptxt);
                                pslst[idx].execute();
                                pslst[idx].clearParameters();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            temptxt = "";
                        }
                        boolean exist = true;
                        FileOutputStream outstream = null;
                        HttpURLConnection httpconn = null;
                        try {
                            BigInteger bigInteger = new BigInteger(1, ((Image) element).getMd5());
                            String md5 = bigInteger.toString(16);
                            String type = ((Image) element).getImageType().getFormatName();
                            File imagefile = new File(dir + groupid.get(idx) + File.separator + md5 + "." + type);
                            if (!imagefile.exists()) {
                                exist = false;
                                URL imgurl = new URL(Image.queryUrl((Image) element));
                                httpconn = (HttpURLConnection) imgurl.openConnection();
                                httpconn.setRequestMethod("GET");
                                InputStream is = httpconn.getInputStream();
                                byte[] data = readInputStream(is);
                                outstream = new FileOutputStream(imagefile);
                                outstream.write(data);
                                outstream.close();
                                httpconn.disconnect();
                            }
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 1);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, imagefile.getName());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            if (!exist) {
                                try {
                                    outstream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                httpconn.disconnect();
                            }
                        }
                    } else if (element instanceof FlashImage) {
                        Image img = ((FlashImage) element).getImage();
                        boolean exist = true;
                        FileOutputStream outstream = null;
                        HttpURLConnection httpconn = null;
                        try {
                            BigInteger bigInteger = new BigInteger(1, img.getMd5());
                            String md5 = bigInteger.toString(16);
                            String type = img.getImageType().getFormatName();
                            File imagefile = new File(dir + groupid.get(idx) + File.separator + md5 + "." + type);
                            if (!imagefile.exists()) {
                                exist = false;
                                URL imgurl = new URL(Image.queryUrl(img));
                                httpconn = (HttpURLConnection) imgurl.openConnection();
                                httpconn.setRequestMethod("GET");
                                InputStream is = httpconn.getInputStream();
                                byte[] data = readInputStream(is);
                                outstream = new FileOutputStream(imagefile);
                                outstream.write(data);
                                outstream.close();
                                httpconn.disconnect();
                            }
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 5);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, imagefile.getName());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            if (!exist) {
                                try {
                                    outstream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                httpconn.disconnect();
                            }
                        }
                    } else if (element instanceof PokeMessage) {
                        try {
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 6);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, "[戳一戳：" + ((PokeMessage) element).getName() + "]");
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else if (element instanceof LightApp) {
                        try {
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 8);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, ((LightApp) element).contentToString());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else if (element instanceof MarketFace) {
                        try {
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 10);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, element.toString());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else if (element instanceof ForwardMessage) {
                        try {
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 11);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, element.contentToString());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else if (element instanceof MusicShare) {
                        try {
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 13);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, ((MusicShare) element).getBrief() + ((MusicShare) element).getMusicUrl());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else if (element instanceof Dice) {
                        try {
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 14);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, ((Dice) element).getName());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else if (element instanceof Audio) {
                        String url = ((OnlineAudio) element).getUrlForDownload();
                        String name = ((Audio) element).getFilename();
                        FileOutputStream outstream = null;
                        HttpURLConnection httpconn = null;
                        boolean exist = true;
                        try {
                            File audiofile = new File(dir + groupid.get(idx) + File.separator + name + ".amr");
                            if (!audiofile.exists()) {
                                exist = false;
                                URL audiourl = new URL(url);
                                httpconn = (HttpURLConnection) audiourl.openConnection();
                                httpconn.setRequestMethod("GET");
                                httpconn.connect();
                                InputStream is = httpconn.getInputStream();
                                byte[] data = readInputStream(is);
                                outstream = new FileOutputStream(audiofile);
                                outstream.write(data);
                            }
                            pslst[idx].setString(1, time);
                            pslst[idx].setInt(2, 16);
                            pslst[idx].setLong(3, sender);
                            pslst[idx].setString(4, audiofile.getName());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            if (!exist) {
                                try {
                                    outstream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                httpconn.disconnect();
                            }
                        }
                    }
                }
                if (temptxt.length() != 0) {
                    try {
                        pslst[idx].setString(1, time);
                        pslst[idx].setInt(2, 0);
                        pslst[idx].setLong(3, sender);
                        pslst[idx].setString(4, temptxt);
                        pslst[idx].execute();
                        pslst[idx].clearParameters();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    temptxt = "";
                }
            }
            String groupmsgContent = groupmsg.getMessage().contentToString();
            if (groupmsg.getGroup().getId() == 1078074683l && groupmsg.getMessage().contentToString().startsWith(">")) {
                String msg = groupmsg.getMessage().contentToString().substring(1);
                if (groupmsg.getSender().getPermission().getLevel() > 0) {
                    if (msg.startsWith("start")) {
                        if (server.isRunning()) {
                            groupmsg.getGroup().sendMessage("服务器已启动");
                        } else {
                            try {
                                server.start();
                                groupmsg.getGroup().sendMessage("服务器正在启动");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (msg.startsWith("sql")) {
                        if (groupmsg.getSender().getId() == admin) {
                            try {
                                ResultSet cmdrs = jrrpStmt.executeQuery(msg.substring(4));
                                ResultSetMetaData rsmd = cmdrs.getMetaData();
                                int numcol = rsmd.getColumnCount();
                                int type = rsmd.getColumnType(1);
                                List<Object> resultlst = new ArrayList<Object>();
                                if (type == Types.VARCHAR || type == Types.CHAR || type == Types.TIME) {
                                    while (cmdrs.next())
                                        resultlst.add(cmdrs.getString(1));
                                } else if (type == Types.INTEGER) {
                                    while (cmdrs.next())
                                        resultlst.add(cmdrs.getInt(1));
                                } else if (type == Types.BIGINT) {
                                    while (cmdrs.next())
                                        resultlst.add(cmdrs.getLong(1));
                                }
                                if (numcol > 1 || resultlst.size() > 10) {
                                    groupmsg.getGroup().sendMessage("[" + resultlst.size() + "行" + numcol + "列的数据]");
                                } else {
                                    String sqlmsg = "";
                                    for (Object o : resultlst)
                                        sqlmsg += o.toString() + "\n";
                                    groupmsg.getGroup().sendMessage(sqlmsg.substring(0, sqlmsg.length() - 1));
                                }
                            } catch (SQLException e) {
                                groupmsg.getGroup().sendMessage("SQL指令执行出错");
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        try {
                            if (server.isRunning()) {
                                server.send(msg);
                            } else {
                                groupmsg.getGroup().sendMessage("服务器未在运行");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    groupmsg.getGroup().sendMessage("没有访问权限");
                }
            } else if (groupmsg.getGroup().getId() == 1078074683l && groupmsg.getMessage().contentToString().startsWith("在线人数")) {
                try {
                    if (server.isRunning())
                        server.silentsend("list");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }/*else if(groupmsg.getGroup().getId() == 1078074683l && groupmsg.getMessage().contentToString().startsWith("航班信息")) {
                try {
                    groupmsg.getGroup().sendMessage(flight(groupmsg.getMessage().contentToString().substring(4)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }*/ else if (groupmsg.getGroup().getId() == 1078074683l && groupmsg.getMessage().contentToString().startsWith("登录提示")) {
                loginMsg = !loginMsg;
                if (loginMsg)
                    groupmsg.getGroup().sendMessage("登录提示已开启");
                else
                    groupmsg.getGroup().sendMessage("登录提示已关闭");
            }else if(gptGroupid.contains(groupmsg.getGroup().getId()) && groupmsgContent.startsWith(".注册")){
                char[] pwd = new char[8];
                for(int i=0;i<8;i++)
                    pwd[i] = (char)((int)(Math.random() * 75) + 48);
                long id = groupmsg.getSender().getId();
                String encrypted = "{bcrypt}" + bCryptPasswordEncoder.encode(String.valueOf(pwd));
                try {
                    jrrpStmt.execute("INSERT INTO Users (id,username,password) VALUES (" + id + ",\'" + id + "\',\'" + encrypted + "\') ON DUPLICATE KEY UPDATE password=\'" + encrypted + "\'");
                    groupmsg.getSender().sendMessage("用户名：" + groupmsg.getSender().getId() + "\n密码：" + String.valueOf(pwd));
                    pwd = null;
                } catch (SQLException e) {
                    groupmsg.getSender().sendMessage("创建用户时出错");
                    throw new RuntimeException(e);
                }
            } else if (gptGroupid.contains(groupmsg.getGroup().getId()) && (groupmsgContent.startsWith("@2784617026") || groupmsgContent.startsWith("@大狼"))) {
                String question = "";
                String[] options = groupmsg.getMessage().contentToString().split("\\s+");
                boolean includeImage = false;
                Image imgmsg = null;
                for(SingleMessage msg : groupmsg.getMessage())
                    if(msg instanceof Image){
                        includeImage = true;
                        imgmsg = (Image) msg;
                    }
                String type = "";
                byte[] data = null;
                String filename = "";
                if(includeImage){
                    BigInteger bigInteger = new BigInteger(1, imgmsg.getMd5());
                    filename = bigInteger.toString(16);
                    HttpURLConnection httpconn = null;
                    try {
                        type = imgmsg.getImageType().getFormatName();
                        URL imgurl = new URL(Image.queryUrl(imgmsg));
                        httpconn = (HttpURLConnection) imgurl.openConnection();
                        httpconn.setRequestMethod("GET");
                        InputStream is = httpconn.getInputStream();
                        data = readInputStream(is);
                        httpconn.disconnect();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        httpconn.disconnect();
                    }
                }else if(groupmsgContent.startsWith("@2784617026")){
                    if(groupmsg.getMessage().contentToString().charAt(11) == ' ')
                        question = groupmsg.getMessage().contentToString().substring(12);
                    else
                        question = groupmsg.getMessage().contentToString().substring(11);
                }else if(groupmsgContent.startsWith("@大狼")){
                    if(groupmsg.getMessage().contentToString().charAt(4) == ' ')
                        question = groupmsg.getMessage().contentToString().substring(5);
                    else
                        question = groupmsg.getMessage().contentToString().substring(4);
                }
                if(includeImage){
                    if(varimgQueue.size() < 3){
                        try {
                            varimgQueue.put(new Question(data, groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId()), filename, type));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }else
                        groupmsg.getGroup().sendMessage("大狼的图片队列已达上限，请稍后再问");
                    /*
                }else if (question.charAt(0) == '>') {
                    if (question.charAt(1) == '0') {
                        chatMode = 0;
                        groupmsg.getGroup().sendMessage("已将聊天模式切换为单个问题");
                    } else if (question.charAt(1) == '1') {
                        chatMode = 1;
                        groupmsg.getGroup().sendMessage("已将聊天模式切换为连续问答");
                    } else if (question.charAt(1) == 'C') {
                        chatMessages.clear();
                        groupmsg.getGroup().sendMessage("已清除聊天历史");
                    }
                     */
                }else {
                    if (chatMode == 0) {
                        try {
                            /*if(options[1].equals("查询")){
                                queryQueue.put(new Question(question.substring(3), groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId())));
                    }else */if(options[1].equals("图片")){
                                if(question.length() > 3){
                                    if(imageQueue.size() < 3)
                                        imageQueue.put(new Question(question.substring(3), groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId())));
                                    else
                                        groupmsg.getGroup().sendMessage("大狼的图片队列已达上限，请稍后再问");
                                }
                            }else if(options[1].equals("bing")) {
                                bingQueue.put(new Question(question.substring(5), groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId())));
                            }else {
                                if (questionQueue.size() < 5) {
                                    questionQueue.put(new Question(question, groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId())));
                                } else {
                                    groupmsg.getGroup().sendMessage("大狼的问题队列已达上限，请稍后再问");
                                }
                            }
                        } catch (InterruptedException e) {
                            chatReady = true;
                            throw new RuntimeException(e);
                        }
                    } else if (chatMode == 1) {
                        try {
                            if (chatReady) {
                                chatReady = false;
                                chatchain(question, groupid.indexOf(groupmsg.getGroup().getId()), groupmsg);
                            } else {
                                groupmsg.getGroup().sendMessage("大狼正在回答上一个问题，请稍后再向我提问");
                            }
                        } catch (SQLException | IOException | InterruptedException e) {
                            chatReady = true;
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else if(gptGroupid.contains(groupmsg.getGroup().getId()) && groupmsg.getMessage().contentToString().startsWith(".steam")){
                int n = groupid.indexOf(groupmsg.getGroup().getId());
                String[] question = groupmsg.getMessage().contentToString().split("\\s+");
                if(question.length == 1){
                    //Print all players in this group
                    try {
                        ExecutorService steamexec = Executors.newFixedThreadPool(12);
                        List<Future<SteamStatus>> steamresult = new ArrayList<Future<SteamStatus>>();
                        StringBuilder statusmsg = new StringBuilder();
                        ResultSet steamRs = steamStmt.executeQuery("select id,steam from all_members_groups where groups like \"%" + groupmsg.getGroup().getId() + "%\" and steam is not null");
                        while(steamRs.next()){
                            long id = steamRs.getBigDecimal(1).longValue();
                            steamresult.add(steamexec.submit(new SteamTask(steamapi + " " + steamRs.getBigDecimal(2).longValue(), groupmsg.getGroup().getMembers().get(id).getNick(), chatdir)));
                        }
                        steamexec.shutdown();
                        if(steamexec.awaitTermination(12, TimeUnit.SECONDS)){
                            SteamStatus[] statusList = new SteamStatus[steamresult.size()];
                            for(int i=0;i<steamresult.size();i++){
                                try{
                                    statusList[i] = steamresult.get(i).get();
                                } catch (ExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            Arrays.sort(statusList);
                            statusmsg.append(statuslstString(statusList));
                        }else{
                            steamexec.shutdownNow();
                            statusmsg.append("获取玩家信息超时");
                        }
                        String resultstatus = statusmsg.toString().trim();
                        try{
                            pslst[n].setString(1, sdf.format(new Date()));
                            pslst[n].setInt(2, 0);
                            pslst[n].setLong(3, 2784617026l);
                            pslst[n].setString(4, resultstatus);
                            pslst[n].execute();
                            pslst[n].clearParameters();
                        }catch (SQLException e2){
                            throw new RuntimeException(e2);
                        }
                        groupmsg.getGroup().sendMessage(MessageUtils.newChain(new PlainText(resultstatus)));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    try{
                        long id = Long.parseLong(question[1]);
                        if(id / 1000000000000l > 0)
                            steamQueue.put(new Question("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + steamapi + "&steamids=" + id, groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId())));
                        else{
                            ResultSet steamRs = jrrpStmt.executeQuery("select steam,groups from all_members_groups where id=" + id);
                            steamRs.next();
                            String[] checkgroups = steamRs.getString(2).split(",");
                            if(in(checkgroups, groupmsg.getGroup().toString())){
                                BigDecimal steamid = steamRs.getBigDecimal(1);
                                if(!Objects.isNull(steamid))
                                    steamQueue.put(new Question("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + steamapi + "&steamids=" + steamid.longValue(), groupmsg.getGroup(), groupmsg.getSender().getId(), groupmsg.getSender().getNick(), groupid.indexOf(groupmsg.getGroup().getId())));
                                else{
                                    try{
                                        pslst[n].setString(1, sdf.format(new Date()));
                                        pslst[n].setInt(2, 0);
                                        pslst[n].setLong(3, 2784617026l);
                                        pslst[n].setString(4, "成员未添加steam id");
                                        pslst[n].execute();
                                        pslst[n].clearParameters();
                                    }catch (SQLException e2){
                                        throw new RuntimeException(e2);
                                    }
                                    groupmsg.getGroup().sendMessage("成员未添加steam id");
                                }
                            }else{
                                try{
                                    pslst[n].setString(1, sdf.format(new Date()));
                                    pslst[n].setInt(2, 0);
                                    pslst[n].setLong(3, 2784617026l);
                                    pslst[n].setString(4, "查询的玩家QQ号有误或不在本群内");
                                    pslst[n].execute();
                                    pslst[n].clearParameters();
                                }catch (SQLException e2){
                                    throw new RuntimeException(e2);
                                }
                                groupmsg.getGroup().sendMessage("查询的玩家QQ号有误或不在本群内");
                            }
                        }
                    }catch (NumberFormatException e){
                        try{
                            pslst[n].setString(1, sdf.format(new Date()));
                            pslst[n].setInt(2, 0);
                            pslst[n].setLong(3, 2784617026l);
                            pslst[n].setString(4, "输入格式错误，用法为.steam [QQ号或17位steam id]");
                            pslst[n].execute();
                            pslst[n].clearParameters();
                        }catch (SQLException e2){
                            throw new RuntimeException(e2);
                        }
                        groupmsg.getGroup().sendMessage("输入格式错误，用法为.steam [QQ号或17位steam id]");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }else if(gptGroupid.contains(groupmsg.getGroup().getId()) && groupmsg.getMessage().contentToString().startsWith(".kook")){
                int n = groupid.indexOf(groupmsg.getGroup().getId());
                try {
                    ResultSet rs = jrrpStmt.executeQuery("select kook from Groups where id=" + groupmsg.getGroup().getId());
                    if(rs.next()){
                        long kookid = rs.getLong(1);
                        KookServer kookServer = new KookServer(kookid, kook);
                        String msg = "";
                        boolean useronline = false;
                        if(Objects.isNull(kookServer.getChannels()))
                            msg = "获取kook服务器信息出错";
                        else{
                            msg += kookServer.getName() + "\n";
                            for(KookChannel kookChannel : kookServer.getChannels()){
                                List<String> lst = kookChannel.getVoiceUsers();
                                if(!Objects.isNull(lst) && lst.size() != 0){
                                    useronline = true;
                                    msg += kookChannel.getName() + "：" + list2String(lst) + "\n";
                                }
                            }
                            if(useronline)
                                msg = msg.substring(0, msg.length() - 1);
                            else
                                msg += "没有成员在语音频道中";
                        }
                        try{
                            pslst[n].setString(1, sdf.format(new Date()));
                            pslst[n].setInt(2, 0);
                            pslst[n].setLong(3, 2784617026l);
                            pslst[n].setString(4, msg);
                            pslst[n].execute();
                            pslst[n].clearParameters();
                        }catch (SQLException e2){
                            throw new RuntimeException(e2);
                        }
                        groupmsg.getGroup().sendMessage(msg);
                    }else{
                        try{
                            pslst[n].setString(1, sdf.format(new Date()));
                            pslst[n].setInt(2, 0);
                            pslst[n].setLong(3, 2784617026l);
                            pslst[n].setString(4, "群聊未添加kook服务器");
                            pslst[n].execute();
                            pslst[n].clearParameters();
                        }catch (SQLException e2){
                            throw new RuntimeException(e2);
                        }
                        groupmsg.getGroup().sendMessage("群聊未添加kook服务器");
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }else if(gptGroupid.contains(groupmsg.getGroup().getId()) && groupmsg.getMessage().contentToString().startsWith(".mc")){
                int n = groupid.indexOf(groupmsg.getGroup().getId());
                String[] mcargs = groupmsg.getMessage().contentToString().split("\\s+");
                MineStat mineStat = null;
                if(mcargs.length == 2){
                    mineStat = new MineStat(mcargs[1], 25565);
                }else if(mcargs.length == 3) {
                    try{
                        mineStat = new MineStat(mcargs[1], Integer.parseInt(mcargs[2]));
                    }catch (NumberFormatException e){
                        mineStat = null;
                        try{
                            pslst[n].setString(1, sdf.format(new Date()));
                            pslst[n].setInt(2, 0);
                            pslst[n].setLong(3, 2784617026l);
                            pslst[n].setString(4, "端口号输入错误");
                            pslst[n].execute();
                            pslst[n].clearParameters();
                        }catch (SQLException e2){
                            throw new RuntimeException(e2);
                        }
                        groupmsg.getGroup().sendMessage("端口号输入错误");
                    }
                }
                if(!Objects.isNull(mineStat)){
                    String result = mineStat.getAddress() + ":" + mineStat.getPort() + "\n";
                    if(mineStat.getConnectionStatus().equals(MineStat.ConnectionStatus.SUCCESS)){
                        result += "延迟：" + mineStat.getLatency() + "ms" + "\n";
                        result +=  "在线人数：" + mineStat.getCurrentPlayers() + " / " + mineStat.getMaximumPlayers() + "\n";
                        result += mineStat.getStrippedMotd();
                    }else if(mineStat.getConnectionStatus().equals(MineStat.ConnectionStatus.TIMEOUT)){
                        result += "连接服务器超时";
                    }else{
                        result += "连接服务器失败";
                    }
                    try{
                        pslst[n].setString(1, sdf.format(new Date()));
                        pslst[n].setInt(2, 0);
                        pslst[n].setLong(3, 2784617026l);
                        pslst[n].setString(4, result);
                        pslst[n].execute();
                        pslst[n].clearParameters();
                    }catch (SQLException e2){
                        throw new RuntimeException(e2);
                    }
                    groupmsg.getGroup().sendMessage(result);
                }
            }else if (gptGroupid.contains(groupmsg.getGroup().getId()) && groupmsg.getMessage().contentToString().startsWith(".jrrp")) {
                int num = 0;
                String includeImage = "";
                long groupnum = groupmsg.getGroup().getId();
                if (groupnum == 471773301l)
                    groupnum = 1078074683l; //test
                if (groupmsg.getMessage().contentToString().length() >= 7) {
                    try {
                        num = Integer.parseInt(groupmsg.getMessage().contentToString().substring(5, 7));
                    } catch (NumberFormatException e) {
                        num = (int) (Math.random() * 100);
                    }
                } else {
                    num = (int) (Math.random() * 100);
                }
                String msg = "";
                MessageChain chain = null;
                ResultSet rs;
                if (num < 15) {
                    long total;
                    /*
                    //总消息
                    try {
                        rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR)");
                        rs.next();
                        total = rs.getLong(1);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    if(num < 2){
                        try{
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type = 0");
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月群内共发了" + total + "条消息，其中" + Math.round((double)rs.getLong(1) / (double)total * (double)100) + "%为文本。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }else if(num < 4){
                        try {
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND (type = 1 OR type = 5)");
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月群内共发了" + total + "条消息，其中" + Math.round((double)rs.getLong(1) / (double)total * (double)100) + "%为图片。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }else{
                        try {
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type = 10");
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月群内共发了" + total + "条消息，其中" + Math.round((double)rs.getLong(1) / (double)total * (double)100) + "%为商城表情。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                     */
                    long senderId = groupmsg.getSender().getId();
                    //个人
                    try {
                        rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND sender = " + senderId);
                        rs.next();
                        total = rs.getLong(1);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    if (num < 5) {
                        try {
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type = 0 AND sender = " + senderId);
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月" + groupmsg.getSender().getNick() + "共发了" + total + "条消息，其中" + Math.round((double) rs.getLong(1) / (double) total * (double) 100) + "%为文本。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }else if(num < 8){
                        File disk = new File("C:");
                        chain = MessageUtils.newChain(new PlainText("大狼所在服务器硬盘剩余空间为" + String.format("%.1f", disk.getFreeSpace() / ((double) 1024 * 1024 * 1024)) + "GB。"));
                    } else {
                        String b = "";
                        try {
                            rs = jrrpStmt.executeQuery("SELECT birthday FROM `" + groupnum + "Members" + "` WHERE id = " + senderId);
                            rs.next();
                            b = rs.getString(1);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        if (b != null) {
                            Date nowtime = new Date();
                            Date accutime;
                            try {
                                accutime = sdf.parse((nowtime.getYear() + 1900) + "-" + (nowtime.getMonth() + 1) + "-" + nowtime.getDate() + " 00:00:00");
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            String[] bdate = b.split("-");
                            int m = Integer.parseInt(bdate[0]);
                            int d = Integer.parseInt(bdate[1]);
                            long days = 0;
                            Date birthd;
                            if ((m - 1) > nowtime.getMonth() || ((m - 1) == nowtime.getMonth() && d >= nowtime.getDate())) {
                                try {
                                    birthd = sdf.parse((nowtime.getYear() + 1900) + "-" + m + "-" + d + " 00:00:00");
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                try {
                                    birthd = sdf.parse((nowtime.getYear() + 1901) + "-" + m + "-" + d + " 00:00:00");
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            days = (birthd.getTime() - accutime.getTime()) / 86400000;
                            chain = MessageUtils.newChain(new PlainText("距离" + groupmsg.getSender().getNick() + "的生日还有" + days + "天。"));
                        } else {
                            try {
                                rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND (type = 1 OR type = 5) AND sender = " + senderId);
                                rs.next();
                                chain = MessageUtils.newChain(new PlainText("近一个月" + groupmsg.getSender().getNick() + "共发了" + total + "条消息，其中" + Math.round((double) rs.getLong(1) / (double) total * (double) 100) + "%为图片。"));
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } else if (num < 25) {
                    long total;
                    long senderId = groupmsg.getSender().getId();
                    //个人
                    try {
                        rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR)");
                        rs.next();
                        total = rs.getLong(1);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    if (num < 22) {
                        try {
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND sender = " + senderId);
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月群内共发了" + total + "条消息，其中有" + Math.round((double) rs.getLong(1) / (double) total * (double) 100) + "%来自" + groupmsg.getSender().getNick() + "。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    /* else if (num < 22) {
                        try {
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND (type = 1 OR type = 5)");
                            rs.next();
                            total = rs.getLong(1);
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND (type = 1 OR type = 5) AND sender = " + senderId);
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月群内共发了" + total + "个图片，其中" + Math.round((double) rs.getLong(1) / (double) total * (double) 100) + "%来自" + groupmsg.getSender().getNick() + "。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        */
                    } else {
                        /*
                        try {
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type = 10");
                            rs.next();
                            total = rs.getLong(1);
                            rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type = 10 AND sender = " + senderId);
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("近一个月群内共发了" + total + "个商城表情，其中" + Math.round((double) rs.getLong(1) / (double) total * (double) 100) + "%来自" + groupmsg.getSender().getNick() + "。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                         */
                        Date joindate = new Date(1000l * (long) groupmsg.getGroup().get(senderId).getJoinTimestamp());
                        Date nowtime = new Date();
                        chain = MessageUtils.newChain(new PlainText(groupmsg.getSender().getNick() + "加入群聊已经" + (nowtime.getTime() - joindate.getTime()) / 86400000 + "天了。"));
                    }
                } else if (num < 65) {
                    long senderId = groupmsg.getSender().getId();
                    if (num < 30) {
                        try {
                            rs = jrrpStmt.executeQuery("SELECT content FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type=0");
                            int count = 0;
                            int length = 0;
                            while (rs.next()) {
                                count++;
                                length += rs.getString(1).length();
                            }
                            rs = jrrpStmt.executeQuery("SELECT content FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type=0 AND sender=" + senderId);
                            int sendercount = 0;
                            int senderlength = 0;
                            while (rs.next()) {
                                sendercount++;
                                senderlength += rs.getString(1).length();
                            }
                            chain = MessageUtils.newChain(new PlainText("近一个月，群友们的消息平均长度为" + String.format("%.1f", ((double) length) / ((double) count)) + "，而" + groupmsg.getSender().getNick() + "的消息平均长度为" + String.format("%.1f", ((double) senderlength) / ((double) sendercount)) + "。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    } else if(num < 40){
                        try {
                            rs = jrrpStmt.executeQuery("select content from `" + groupnum + "` where time >= (NOW() - INTERVAL 720 HOUR) and type=0 and sender=" + senderId + " and content regexp \"@[0-9]{5,11}\"");
                            Map<Long, Integer> count = new HashMap<Long, Integer>();
                            Pattern pattern = Pattern.compile("@[0-9]{5,11}");
                            while(rs.next()){
                                Matcher matcher = pattern.matcher(rs.getString(1));
                                if(matcher.find()){
                                    long person = Long.parseLong(matcher.group().substring(1));
                                    if(person != 2784617026L){
                                        if(count.containsKey(person))
                                            count.replace(person, count.get(person) + 1);
                                        else
                                            count.put(person, 1);
                                    }
                                }
                            }
                            String outmsg = "";
                            if(count.isEmpty()){
                                outmsg = "近一个月内，" + groupmsg.getSender().getNick() + "没有@过群用户。";
                            }else{
                                long maxid = 0;
                                int maxcount = 0;
                                for(Long key : count.keySet()) {
                                    if (count.get(key) >= maxcount) {
                                        maxid = key;
                                        maxcount = count.get(key);
                                    }
                                }
                                rs = jrrpStmt.executeQuery("select name from `" + groupnum + "Members` where id=" + maxid);
                                if(rs.next()){
                                    outmsg = "近一个月内，" + groupmsg.getSender().getNick() + "@次数最多的群用户是" + rs.getString(1) + "，共@了" + maxcount + "次。";
                                }else{
                                    outmsg = "获取jrrp信息出错，jrrp代码" + num + "。";
                                }
                            }
                            chain = MessageUtils.newChain(new PlainText(outmsg));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (num < 55) {
                        try {
                            Date sendertime = null;
                            long nextsender = 0l;
                            long timeint = 0l;
                            rs = jrrpStmt.executeQuery("SELECT sender,time FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR)");
                            List<String> timelst = new ArrayList<String>();
                            List<Long> senderlst = new ArrayList<Long>();
                            while (rs.next()) {
                                senderlst.add(rs.getLong(1));
                                timelst.add(rs.getString(2));
                            }
                            for (int i = 0; i < timelst.size(); i++) {
                                if (senderlst.get(i) == senderId) {
                                    Date t1 = sdf.parse(timelst.get(i));
                                    if (i < timelst.size() - 1) {
                                        Date t2 = sdf.parse(timelst.get(i + 1));
                                        if ((t2.getTime() - t1.getTime()) > timeint) {
                                            if (i + 1 < timelst.size() - 1) {
                                                if (sdf.parse(timelst.get(i + 2)).getTime() > t2.getTime()) {
                                                    timeint = t2.getTime() - t1.getTime();
                                                    nextsender = senderlst.get(i + 1);
                                                    sendertime = t1;
                                                }
                                            } else {
                                                timeint = t2.getTime() - t1.getTime();
                                                nextsender = senderlst.get(i + 1);
                                                sendertime = t1;
                                            }
                                        }
                                    } else
                                        break;
                                }
                            }
                            rs = jrrpStmt.executeQuery("SELECT name FROM `" + groupnum + "Members` WHERE id=" + nextsender);
                            rs.next();
                            chain = MessageUtils.newChain(new PlainText("在一天" + sdfhm.format(sendertime) + "，" + groupmsg.getSender().getNick() + "发送了一条消息，而" + rs.getString(1) + "在长达" + Math.round(((double) timeint) / ((double) 60000)) + "分钟后发送了下一条消息。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            rs = jrrpStmt.executeQuery("SELECT id FROM `" + groupnum + "Members`");
                            List<Long> memberlst = new ArrayList<Long>();
                            while (rs.next())
                                memberlst.add(rs.getLong(1));
                            int[] maxcountlst = new int[memberlst.size()];
                            Date[] startdate = new Date[memberlst.size()];
                            for (int i = 0; i < memberlst.size(); i++) {
                                rs = jrrpStmt.executeQuery("SELECT time FROM `" + groupnum + "` WHERE sender=" + memberlst.get(i) + " ORDER BY time");
                                List<Date> timelst = new ArrayList<Date>();
                                int idx = 0;
                                int starttime = 0;
                                while (rs.next()) {
                                    Date currenttime = sdf.parse(rs.getString(1));
                                    timelst.add(currenttime);
                                    if ((currenttime.getTime() - timelst.get(idx).getTime()) > 3600000) {
                                        if (maxcountlst[i] < timelst.size() - idx) {
                                            maxcountlst[i] = timelst.size() - idx;
                                            starttime = idx;
                                            idx++;
                                        }
                                        while ((currenttime.getTime() - timelst.get(idx).getTime()) > 3600000)
                                            idx++;
                                    }
                                }
                                if ((timelst.size() - idx + 1) > maxcountlst[i]) {
                                    starttime = idx;
                                    maxcountlst[i] = timelst.size() - idx + 1;
                                }
                                if (timelst.size() > 0)
                                    startdate[i] = timelst.get(starttime);
                            }
                            int globalmaxidx = 0;
                            for (int i = 0; i < maxcountlst.length; i++) {
                                if (maxcountlst[i] > maxcountlst[globalmaxidx]) {
                                    globalmaxidx = i;
                                }
                            }
                            int senderidx = memberlst.indexOf(senderId);
                            Date daytime = startdate[senderidx];
                            if (senderidx == globalmaxidx)
                                chain = MessageUtils.newChain(new PlainText((daytime.getMonth() + 1) + "月" + daytime.getDate() + "日，" + groupmsg.getSender().getNick() + "在一小时内发送了" + maxcountlst[senderidx] + "条消息，" + groupmsg.getSender().getNick() + "是该记录的保持者。"));
                            else
                                chain = MessageUtils.newChain(new PlainText((daytime.getMonth() + 1) + "月" + daytime.getDate() + "日，" + groupmsg.getSender().getNick() + "在一小时内发送了" + maxcountlst[senderidx] + "条消息，该记录的保持者在" + (startdate[globalmaxidx].getMonth() + 1) + "月" + startdate[globalmaxidx].getDate() + "日在一小时内发送了" + maxcountlst[globalmaxidx] + "条消息。"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else if (num < 70) {
                    double avg = (double) levels[groupid.indexOf(groupmsg.getGroup().getId())] / (double) groupmsg.getGroup().getMembers().size();
                    int lv = 0;
                    try {
                        rs = jrrpStmt.executeQuery("SELECT level FROM `" + groupnum + "Members` WHERE id=" + groupmsg.getSender().getId());
                        rs.next();
                        lv = rs.getInt(1);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    chain = MessageUtils.newChain(new PlainText(groupmsg.getSender().getNick() + "的QQ等级是" + lv + "级，而群成员的平均QQ等级是" + String.format("%.1f", avg) + "级。"));
                } else {
                    long senderId = groupmsg.getSender().getId();
                    if (num % 5 == 0) {
                        try {
                            rs = jrrpStmt.executeQuery("SELECT time,content FROM `" + groupnum + "` WHERE hour(time) >= 1 AND hour(time) < 6 AND type=0 AND sender=" + senderId);
                            List<String> contentlst = new ArrayList<String>();
                            List<String> datelst = new ArrayList<String>();
                            while (rs.next()) {
                                String tempmsg = rs.getString(2);
                                if (!tempmsg.startsWith(".jrrp")) {
                                    datelst.add(rs.getString(1));
                                    contentlst.add(tempmsg);
                                }
                            }
                            if (contentlst.size() == 0)
                                chain = MessageUtils.newChain(new PlainText(groupmsg.getSender().getNick() + "在深夜没有发过消息。"));
                            else {
                                int a = (int) (Math.random() * contentlst.size());
                                chain = MessageUtils.newChain(new PlainText("在一天深夜" + sdfhm.format(sdf.parse(datelst.get(a))) + "，" + groupmsg.getSender().getNick() + "说了\"" + contentlst.get(a) + "\"。"));
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (num % 5 == 1) {
                        int[] groupFreq = new int[24];
                        int[] senderFreq = new int[24];
                        for (int i = 0; i < 24; i++) {
                            try {
                                rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE hour(time) = " + i + " AND time >= (NOW() - INTERVAL 720 HOUR)");
                                rs.next();
                                groupFreq[i] = rs.getInt(1);
                                rs = jrrpStmt.executeQuery("SELECT count(*) FROM `" + groupnum + "` WHERE hour(time) = " + i + " AND time >= (NOW() - INTERVAL 720 HOUR) AND sender=" + senderId);
                                rs.next();
                                senderFreq[i] = rs.getInt(1);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        chain = MessageUtils.newChain(new PlainText("群友们最常在" + maxIdx(groupFreq) + "时聊天，而" + groupmsg.getSender().getNick() + "最常在" + maxIdx(senderFreq) + "时聊天。"));
                    } else if (num % 5 == 2) {
                        if (groupmsg.getGroup().getId() == 1078074683l) {
                            try {
                                rs = jrrpStmt.executeQuery("SELECT COUNT(*) FROM `" + groupnum + "Chat` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND user = " + senderId);
                                rs.next();
                                chain = MessageUtils.newChain(new PlainText("近一个月，" + groupmsg.getSender().getNick() + "共调用了大狼" + rs.getInt(1) + "次。"));
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            String totalmsg = "";
                            try {
                                rs = jrrpStmt.executeQuery("SELECT content FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type=0 AND sender=" + senderId);
                                while (rs.next()) {
                                    totalmsg += rs.getString(1);
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                            char[] chars = totalmsg.toCharArray();
                            Map<Character, Integer> strmap = new HashMap<Character, Integer>();
                            for (char c : chars) {
                                if (c > 64) {
                                    if (strmap.containsKey(c))
                                        strmap.put(c, strmap.get(c) + 1);
                                    else
                                        strmap.put(c, 1);
                                }
                            }
                            char maxChar = 'A';
                            int count = 0;
                            for (Map.Entry<Character, Integer> entry : strmap.entrySet()) {
                                if (entry.getValue() > count) {
                                    maxChar = entry.getKey();
                                    count = entry.getValue();
                                }
                            }
                            chain = MessageUtils.newChain(new PlainText("近一个月，" + groupmsg.getSender().getNick() + "最常说的字是\"" + String.format("%c", maxChar) + "\"，共说了" + count + "次。"));
                        }
                    } else if (num % 5 == 3) {
                        Map<String, Integer> imgmap = new HashMap<String, Integer>();
                        try {
                            rs = jrrpStmt.executeQuery("SELECT content FROM `" + groupnum + "` WHERE time >= (NOW() - INTERVAL 720 HOUR) AND type=1 AND sender=" + senderId);
                            while (rs.next()) {
                                String temp = rs.getString(1);
                                if (imgmap.containsKey(temp))
                                    imgmap.put(temp, imgmap.get(temp) + 1);
                                else
                                    imgmap.put(temp, 1);
                            }
                            String maxImg = "";
                            int count = 0;
                            for (Map.Entry<String, Integer> entry : imgmap.entrySet()) {
                                if (entry.getValue() > count) {
                                    maxImg = entry.getKey();
                                    count = entry.getValue();
                                }
                            }
                            if (imgmap.size() == 0) {
                                chain = MessageUtils.newChain(new PlainText(groupmsg.getSender().getNick() + "近一个月没有发送过图片。"));
                            } else {
                                ExternalResource externalResource = ExternalResource.create(new File(dir + groupnum + File.separator + maxImg));
                                chain = MessageUtils.newChain(new PlainText(groupmsg.getSender().getNick() + "近一个月最常发的图片是"), groupmsg.getGroup().uploadImage(externalResource));
                                externalResource.close();
                                includeImage = maxImg;
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }else if(num % 5 == 4){
                        try {
                            rs = stmt[groupid.indexOf(groupnum)].executeQuery("select time,question from Chat where user=" + senderId + " and question is not null order by rand() limit 1");
                            if(rs.next()){
                                chain = MessageUtils.newChain(new PlainText("在一天" + sdfhm.format(sdf.parse(rs.getString(1))) + "，" + groupmsg.getSender().getNick() + "问了大狼“" + rs.getString(2) + "”。"));
                            }else{
                                chain = MessageUtils.newChain(new PlainText(groupmsg.getSender().getNick() + "没有问过大狼问题。"));
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (chain != null){
                    int idx = groupid.indexOf(groupnum);
                    if(includeImage.length() != 0){
                        for(SingleMessage singleMessage : chain){
                            if(singleMessage instanceof PlainText){
                                try {
                                    pslst[idx].setString(1, sdf.format(new Date()));
                                    pslst[idx].setInt(2, 0);
                                    pslst[idx].setLong(3, 2784617026l);
                                    pslst[idx].setString(4, singleMessage.contentToString());
                                    pslst[idx].execute();
                                    pslst[idx].clearParameters();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }else if(singleMessage instanceof Image){
                                try {
                                    pslst[idx].setString(1, sdf.format(new Date()));
                                    pslst[idx].setInt(2, 1);
                                    pslst[idx].setLong(3, 2784617026l);
                                    pslst[idx].setString(4, includeImage);
                                    pslst[idx].execute();
                                    pslst[idx].clearParameters();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }else{
                        try {
                            pslst[idx].setString(1, sdf.format(new Date()));
                            pslst[idx].setInt(2, 0);
                            pslst[idx].setLong(3, 2784617026l);//TODO
                            pslst[idx].setString(4, chain.contentToString());
                            pslst[idx].execute();
                            pslst[idx].clearParameters();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    includeImage = "";
                    groupmsg.getGroup().sendMessage(chain);
                }
            }
        });
        Listener join = GlobalEventChannel.INSTANCE.subscribeAlways(MemberJoinEvent.class, memberJoinEvent -> {
            NormalMember member = memberJoinEvent.getMember();
            Date date = new Date(1000l * (long) member.getJoinTimestamp());
            long id = memberJoinEvent.getGroup().getId();
            if (groupid.contains(id)) {
                try {
                    stmt[groupid.indexOf(memberJoinEvent.getGroup().getId())].execute("INSERT INTO Members(id,name,jointime) VALUES(" + member.getId() + ",\'" + member.getNick() + "\',\'" + sdf.format(date) + "\') ON DUPLICATE KEY UPDATE name=\'" + member.getNick() + "\',jointime=\'" + sdf.format(date) + "\'");
                    String grouplst = "";
                    for(int i=0;i<stmt.length;i++){
                        ResultSet rs2 = stmt[i].executeQuery("select name from Members where id=" + id);
                        if(rs2.next())
                            grouplst += groups[i].getId() + ",";
                        rs2.close();
                    }
                    if(!grouplst.isEmpty())
                        grouplst = grouplst.substring(0, grouplst.length() - 1);
                    chatStmt.execute("INSERT INTO all_members_groups (id,groups) VALUES(" + id + ",\'" + grouplst + "\') ON DUPLICATE KEY UPDATE groups=\'" + grouplst + "\'");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        Listener exit = GlobalEventChannel.INSTANCE.subscribeAlways(MemberLeaveEvent.class, memberLeaveEvent -> {
            Member member = memberLeaveEvent.getMember();
            Date date = new Date();
            if (groupid.contains(memberLeaveEvent.getGroup().getId())) {
                try {
                    stmt[groupid.indexOf(memberLeaveEvent.getGroup().getId())].execute("INSERT INTO Members(id,name,exittime) VALUES(" + member.getId() + ",\'" + member.getNick() + "\',\'" + sdf.format(date) + "\')");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDisable(){
        try {
            jrrpStmt.close();
            chatStmt.close();
            for(Statement statement : gptStmt)
                statement.close();
            for(Statement statement : stmt)
                statement.close();
            jrrpConn.close();
            chatConn.close();
            for(Connection connection : gptConn)
                connection.close();
            for(Connection connection : conn)
                connection.close();
            steamStmt.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for(ChatUser chatUser : chatUserList.values())
            chatUser.save();
    }

    private static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while((len = is.read(buffer)) != -1)
            out.write(buffer, 0, len);
        is.close();
        return out.toByteArray();
    }

    private static int maxIdx(int[] arr){
        int result = 0;
        for(int i=0;i<arr.length;i++){
            if(arr[i] > arr[result])
                result = i;
        }
        return result;
    }

    private static String flight(String n) throws IOException, InterruptedException {
        URL url = new URL("https://zh.flightaware.com/live/flight/" + n);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.connect();
        if (con.getResponseCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String content = response.toString();
            content = content.substring(content.indexOf("<script>var trackpollBootstrap = ") + 33);
            content = content.substring(0, content.indexOf(";</script>"));
            File js = new File("C:\\Users\\Administrator\\Desktop\\flight\\flight.json");
            FileOutputStream fs = new FileOutputStream(js);
            fs.write(content.getBytes("UTF-8"));
            fs.close();
            Process process = Runtime.getRuntime().exec("cmd /c cd C:\\Users\\Administrator\\Desktop\\flight\\ && python flight.py");
            if(process.waitFor(1, TimeUnit.SECONDS)){
                if(process.exitValue() == 0){
                    File file = new File("C:\\Users\\Administrator\\Desktop\\flight\\flight.txt");
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String result = "";
                    while(reader.ready())
                        result += reader.readLine();
                    return result;
                }
            }
        }
        return "获取航班信息出错";
    }
    private static void chatchain(String msg, int n, GroupMessageEvent event) throws IOException, InterruptedException, SQLException {
        File js = new File(chatdir + "chatchain.txt");
        FileOutputStream fs = new FileOutputStream(js);
        chatMessages.add(new ChatMessage("user", event.getSender().getNick() + ": " + msg));
        fs.write(chatListString(chatMessages).getBytes(StandardCharsets.UTF_8));
        chatMessages.remove(chatMessages.size() - 1);
        fs.close();
        String response = "";
        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python chatchain.py");
        if(process.waitFor(80, TimeUnit.SECONDS)){
            if(process.exitValue() == 0){
                File file = new File(chatdir + "responsechain.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                while(reader.ready())
                    response += reader.readLine();
                String responsetime = sdf.format(new Date());
                File tokens = new File(chatdir + "chaintokens.txt");
                BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                int numtoken = Integer.parseInt(tokenreader.readLine());
                pslst[n].setString(1, responsetime);
                pslst[n].setInt(2, 0);
                pslst[n].setLong(3, 2784617026l);
                pslst[n].setString(4, response);
                pslst[n].execute();
                pslst[n].clearParameters();
                stmt[n].execute("INSERT INTO Chat(user,question,response,time,tokens) VALUES(" + event.getSender().getId() + ",\'" + msg + "\',\'" + response + "\',\'" + responsetime + "\'," + numtoken + ")");
                chatMessages.add(new ChatMessage("user", msg));
                chatMessages.add(new ChatMessage("assistant", response));
                event.getGroup().sendMessage(response);
                reader.close();
            }else{
                event.getGroup().sendMessage("获取回答出错");
            }
        }else{
            process.destroy();
            event.getGroup().sendMessage("获取回答超时");
        }
        chatReady = true;
    }

    private static String chatListString(List<ChatMessage> list){
        String result = "";
        for(ChatMessage chatMessage : list){
            result += chatMessage.role + ":" + chatMessage.message + "\\|\n";
        }
        return result;
    }
    private static void query(String msg, int n, Group group, long id) throws SQLException, IOException, InterruptedException {
        List<ChatMessage> templst = new ArrayList<ChatMessage>();
        templst.add(new ChatMessage("system", "你是一个群聊助手，这个群的消息都记录在一个MYSQL数据库里，你能够访问这个数据库并查询数据，你负责根据群成员的要求查询他们想知道的信息。这个数据库有三张表格。表格Message储存群消息，有5个字段。idMessage字段类型为INT，储存消息编号。time字段类型为DATETIME，储存消息发送时间。type字段储存消息类型，类型为INT，0为文本，1为图片，10为表情。sender字段为发送者id，类型为BIGINT(10)。content字段为消息内容，类型是VARCHAR(5000)。表格Members储存群成员信息，有5个字段，字段id储存群成员id类型为BIGINT(10)，name字段储存群成员名称，类型为VARCHAR，jointime字段储存群成员加入群聊的时间，类型为DATETIME，birthday字段储存群成员生日，类型为CHAR(5)，格式为MM-DD，level字段储存群成员等级，类型为INT。表格Chat记录了群成员调用群助手大狼的信息，有6个字段，idChat字段为调用记录编号，类型为INT，user字段为调用者id，类型为BIGINT(10)，time字段为调用时间，类型为DATETIME，question字段为调用者的提问内容，类型为VARCHAR，字段response为大狼的回答，类型为VARCHAR，tokens字段为本次调用所消耗的代币数量，类型为INT。当你收到群成员的查询要求时，请按照下列操作执行。第一请先请输出符合群成员要求的sql代码，sql代码的格式为<sql>[代码]</sql>，然后系统会执行sql代码并将结果以文字的形式告诉你，收到结果后请根据结果输出完整的句子告诉群成员查询结果。"));
        templst.add(new ChatMessage("user", msg));
        File js = new File(chatdir + "querytemp.txt");
        FileOutputStream fs = new FileOutputStream(js);
        fs.write(chatListString(templst).getBytes(StandardCharsets.UTF_8));
        fs.close();
        String response = "";
        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python query.py");
        if(process.waitFor(90, TimeUnit.SECONDS)){
            if(process.exitValue() == 0){
                File file = new File(chatdir + "queryresponse.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                while(reader.ready())
                    response += (char)reader.read();
                String responsetime = sdf.format(new Date());
                File tokens = new File(chatdir + "querytokens.txt");
                BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                int numtoken = Integer.parseInt(tokenreader.readLine());
                pslst[n].setString(1, responsetime);
                pslst[n].setInt(2, 0);
                pslst[n].setLong(3, 2784617026l);
                pslst[n].setString(4, response);
                pslst[n].execute();
                pslst[n].clearParameters();
                chatStmtlst[n].setLong(1, id);
                chatStmtlst[n].setString(2, msg);
                chatStmtlst[n].setString(3, response);
                chatStmtlst[n].setString(4, responsetime);
                chatStmtlst[n].setInt(5, numtoken);
                chatStmtlst[n].execute();
                chatStmtlst[n].clearParameters();
                String sqlstmt = response.substring(response.indexOf("<sql>") + 5);
                sqlstmt = sqlstmt.substring(0, sqlstmt.indexOf("</sql>"));
                templst.add(new ChatMessage("assistant", response));
                ResultSet rs = gptStmt[n].executeQuery(sqlstmt);
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnnum = rsmd.getColumnCount();
                String output = "";
                if(columnnum <= 5){
                    for(int i=1;i<=columnnum;i++)
                        output += rsmd.getColumnLabel(i) + " ";
                    output += "\n";
                    int count = 0;
                    while(count < 20 && rs.next()){
                        for(int i=1;i<=columnnum;i++)
                            output += rs.getString(i) + " ";
                        output += "\n";
                        count++;
                    }
                    if(rs.next())
                        output += "...";
                }else{
                    output = "[" + columnnum + "列的数据]";
                }
                //templst.add(new ChatMessage("system", output));
                group.sendMessage(output);
                reader.close();
                tokenreader.close();
            }else{
                String responsetime = sdf.format(new Date());
                pslst[n].setString(1, responsetime);
                pslst[n].setInt(2, 0);
                pslst[n].setLong(3, 2784617026l);
                pslst[n].setString(4, "获取回答时API发生错误");
                pslst[n].execute();
                pslst[n].clearParameters();
                group.sendMessage("获取回答时API发生错误");
                return;
            }
        }else{
            process.destroy();
            String responsetime = sdf.format(new Date());
            pslst[n].setString(1, responsetime);
            pslst[n].setInt(2, 0);
            pslst[n].setLong(3, 2784617026l);
            pslst[n].setString(4, "获取回答超时");
            pslst[n].execute();
            pslst[n].clearParameters();
            group.sendMessage("获取回答超时");
            return;
        }
        /*
        File js2 = new File("C:\\Users\\Administrator\\Desktop\\chat\\querytemp.txt");
        FileOutputStream fs2 = new FileOutputStream(js2);
        fs2.write(chatListString(templst).getBytes("UTF-8"));
        fs2.close();
        String response2 = "";
        Process process2 = Runtime.getRuntime().exec("cmd /c cd C:\\Users\\Administrator\\Desktop\\chat\\ && python query.py");
        if(process2.waitFor(60, TimeUnit.SECONDS)){
            if(process2.exitValue() == 0){
                File file2 = new File("C:\\Users\\Administrator\\Desktop\\chat\\queryresponse.txt");
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(file2), "GBK"));
                while(reader2.ready())
                    response2 += (char)reader2.read();
                String responsetime2 = sdf.format(new Date());
                File tokens2 = new File("C:\\Users\\Administrator\\Desktop\\chat\\querytokens.txt");
                BufferedReader tokenreader2 = new BufferedReader(new InputStreamReader(new FileInputStream(tokens2)));
                int numtoken2 = Integer.parseInt(tokenreader2.readLine());
                String sqlrepsonse2 = response2.replace("\'", "\'\'");
                stmt[n].execute("INSERT INTO Message(time,type,sender,content) VALUES(\'" + responsetime2 + "\',0," + 2784617026l + ",\'" + sqlrepsonse2 + "\')");
                stmt[n].execute("INSERT INTO Chat(user,question,response,time,tokens) VALUES(" + event.getSender().getId() + ",\'" + msg + "\',\'" + sqlrepsonse2 + "\',\'" + responsetime2 + "\'," + numtoken2 + ")");
                event.getGroup().sendMessage(response2);
                reader2.close();
                tokenreader2.close();
                chatReady = true;
            }else{
                event.getGroup().sendMessage("获取回答出错");
                chatReady = true;
            }
        }else{
            process.destroy();
            event.getGroup().sendMessage("获取回答超时");
            chatReady = true;
        }

         */
    }
    private static String chatHistoryString(int i, int num) throws SQLException {
        num++;
        int num2 = num - 1;
        String result = "";
        ResultSet rs = chatStmt.executeQuery("SELECT time, name, content FROM (SELECT id, time, name, content FROM `" + groupid.get(i) + "` ORDER BY id DESC LIMIT " + num + ") sub ORDER BY id ASC LIMIT " + num2);
        while(rs.next()){
            String content = rs.getString(3);
            if(content.length() > 100)
                content = content.substring(0, 100) + "...";
            result += "[" + rs.getString(1) + "]" + rs.getString(2) + ": " + content + "\n";
        }
        return result;
    }

    /**
     * Checks if a given value is present in the given array of strings.
     *
     * @param lst   the array of strings to search in
     * @param value the value to search for
     * @return true if the value is found in the array, false otherwise
     */
    private static boolean in(String[] lst, String value){
        for(String lststring : lst)
            if(lststring.equals(value))
                return true;
        return false;
    }

    private static String statuslstString(SteamStatus[] statuslst){
        if(statuslst.length == 0)
            return "没有已添加steam id的成员";
        String result = "";
        int statusid = -1;
        int i = 0;
        for(;i<statuslst.length && statuslst[i].status==7;i++)
            result += statuslst[i].getStringStatus() + "\n";
        result += "\n";
        for(;i<statuslst.length;i++){
            if(statuslst[i].status == 0)
                continue;
            if(statuslst[i].status != statusid) {
                statusid = statuslst[i].status;
                result += STATUS_LIST[statusid] + "：";
                while(i < statuslst.length && statuslst[i].status == statusid){
                    result += statuslst[i++].getStringStatus() + "，";
                }
                result = result.substring(0, result.length() - 1) + "\n";
                i--;
            }
        }
        if(result.trim().isEmpty())
            result = "没有在线玩家";
        return result.trim();
    }
    private static String list2String(List<String> list){
        if(list.isEmpty())
            return "";
        else{
            String result = "";
            for(String str : list)
                result += str + "，";
            return result.substring(0, result.length() - 1);
        }
    }
}