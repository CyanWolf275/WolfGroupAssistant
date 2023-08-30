package top.furryserver;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

public class Birthday extends TimerTask {

    private Group[] group;
    private Statement[] stmt;
    private List<Long> gptgroups;

    public Birthday(Group[] group, Statement[] stmt, List<Long> gptgroups){
        this.group = group;
        this.stmt = stmt;
        this.gptgroups = gptgroups;
    }

    @Override
    public void run() {
        for(int i=0;i<group.length;i++)
            for(NormalMember member: group[i].getMembers()){
                try {
                    stmt[i].execute("UPDATE Members SET age=" + member.queryProfile().getAge() + " WHERE id=" + member.getId());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        Date currentday = new Date();
        int month = currentday.getMonth() + 1;
        int day = currentday.getDate();
        try {
            for(int i=0;i<stmt.length;i++){
                if(gptgroups.contains(group[i].getId())){
                    ResultSet rs = stmt[i].executeQuery("SELECT id,birthday FROM Members");
                    while(rs.next()){
                        String b = rs.getString(2);
                        if(b != null && b.length() != 0){
                            String[] blist = b.split("-");
                            if(Integer.parseInt(blist[0]) == month && Integer.parseInt(blist[1]) == day){
                                String name = group[i].get(rs.getLong(1)).getNick();
                                group[i].sendMessage("今天是" + name + "的生日，祝" + name + "生日快乐！");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
