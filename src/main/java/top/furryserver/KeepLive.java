package top.furryserver;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

public class KeepLive extends TimerTask {

    private Group[] groups;
    private Statement[] stmt;
    private Statement mcStmt;
    private Statement jrrpStmt;
    private Statement[] gptStmt;
    private Statement chatStmt;
    private int[] levels;
    private PreparedStatement groupstmt;
    private List<Long> gptgroups;
    private PreparedStatement[] pslst;
    private Statement steamStmt;
    private static String dir = "C:" + File.separator + "WolfBot" + File.separator;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public KeepLive(Group[] groups, Statement[] stmt, Statement mcStmt, Statement jrrpStmt, Statement[] gptStmt, Statement chatStmt, int[] levels, PreparedStatement groupstmt, List<Long> gptgroups, PreparedStatement[] pslst, Statement steamStmt){
        this.groups = groups;
        this.stmt = stmt;
        this.mcStmt = mcStmt;
        this.jrrpStmt = jrrpStmt;
        this.gptStmt = gptStmt;
        this.chatStmt = chatStmt;
        this.levels = levels;
        this.groupstmt = groupstmt;
        this.gptgroups = gptgroups;
        this.pslst = pslst;
        this.steamStmt = steamStmt;
    }
    @Override
    public void run() {
        try {
            mcStmt.executeQuery("select idUsers from Users");
            steamStmt.executeQuery("select id from all_members_groups");
            ResultSet rs = jrrpStmt.executeQuery("select id from all_members");
            while(rs.next()){
                String grouplst = "";
                long id = rs.getLong(1);
                for(int i=0;i<stmt.length;i++){
                    ResultSet rs2 = stmt[i].executeQuery("select name from Members where id=" + id);
                    if(rs2.next())
                        grouplst += groups[i].getId() + ",";
                    rs2.close();
                }
                if(grouplst.length() > 0)
                    grouplst = grouplst.substring(0, grouplst.length() - 1);
                chatStmt.execute("INSERT INTO all_members_groups (id,groups) VALUES(" + id + ",\'" + grouplst + "\') ON DUPLICATE KEY UPDATE groups=\'" + grouplst + "\'");
            }
            rs.close();
            chatStmt.executeQuery("select idPrivateChat from PrivateChat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for(int i=0;i<groups.length;i++){
            File groupimg = new File(dir + groups[i].getId() + ".jpg");
            if(groupimg.exists())
                groupimg.delete();
            URL groupimgurl = null;
            try {
                groupimgurl = new URL(groups[i].getAvatarUrl());
                HttpURLConnection groupurlconn = (HttpURLConnection) groupimgurl.openConnection();
                groupurlconn.setRequestMethod("GET");
                InputStream groupinput = groupurlconn.getInputStream();
                byte[] groupdata = readInputStream(groupinput);
                FileOutputStream groupfs = new FileOutputStream(groupimg);
                groupfs.write(groupdata);
                groupfs.close();
                groupinput.close();
                groupurlconn.disconnect();
            } catch (ProtocolException | MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            levels[i] = 0;
            try {
                if(gptgroups.contains(groups[i].getId())){
                    gptStmt[i].executeQuery("select id from Members");
                }
                groupstmt.setLong(1, groups[i].getId());
                groupstmt.setString(2, groups[i].getName());
                groupstmt.setString(3, groups[i].getName());
                groupstmt.execute();
                groupstmt.clearParameters();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            for(NormalMember member:groups[i].getMembers()){
                try {
                    //stmt[i].execute("UPDATE Members SET name=\'" + member.getNick() + "\' WHERE id=" + member.getId());
                    Date date = new Date(1000l * (long)member.getJoinTimestamp());
                    int level = member.queryProfile().getQLevel();
                    pslst[i].setLong(1, member.getId());
                    pslst[i].setString(2, member.getNick());
                    pslst[i].setString(3, sdf.format(date));
                    pslst[i].setInt(4, level);
                    pslst[i].setString(5, member.getNick());
                    pslst[i].setString(6, sdf.format(date));
                    pslst[i].setInt(7, level);
                    //pslst[i].execute("INSERT INTO Members(id,name,jointime,level) VALUES(" + member.getId() + ",\'" + member.getNick() + "\',\'"+ sdf.format(date) + "\'," + level + ") ON DUPLICATE KEY UPDATE name=\'" + member.getNick() + "\',jointime=\'" + sdf.format(date) + "\',level=" + level);
                    pslst[i].execute();
                    pslst[i].clearParameters();
                    levels[i] += level;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                FileOutputStream outstream = null;
                HttpURLConnection httpconn = null;
                try {
                    File imagefile = new File(dir + "avatars" + File.separator + member.getId() + ".jpg");
                    if(imagefile.exists())
                        imagefile.delete();
                    URL imgurl = new URL(member.getAvatarUrl());
                    httpconn = (HttpURLConnection) imgurl.openConnection();
                    httpconn.setRequestMethod("GET");
                    InputStream is = httpconn.getInputStream();
                    byte[] data = readInputStream(is);
                    outstream = new FileOutputStream(imagefile);
                    outstream.write(data);
                    outstream.close();
                    httpconn.disconnect();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
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
    private static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while((len = is.read(buffer)) != -1)
            out.write(buffer, 0, len);
        is.close();
        return out.toByteArray();
    }
}
