package top.furryserver;

import net.mamoe.mirai.contact.Friend;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a chat user in the system, which implements the ChatInstance interface.
 * ChatUser maintains a list of ChatMessages, a history, a status flag indicating if it's ready for chat,
 * and other relevant information about the chat. It interacts with external scripts and communicates
 * with the user through a Friend interface provided by the Mirai library.
 *
 * @author CyanWolf
 * @version 1.0
 * @since 2023-03-25
 */
public class ChatUser implements ChatInstance{
    /**
     * List of ChatMessage objects representing the history of messages exchanged with this user.
     */
    private List<ChatMessage> msgList;
    /**
     * Represents a Friend object from the Mamoe mirai library, indicating a friend on a messaging service.
     */
    private Friend friend;
    /**
     * The unique identifier of the friend associated with this ChatUser.
     */
    private long id;
    /**
     * Represents the file that stores the historical chats of this user.
     */
    private File history;
    /**
     * Represents a temporary file used for specific operations.
     */
    private File temp;
    /**
     * Flag that indicates if the chat service is ready for this user. True if ready, false otherwise.
     */
    public boolean chatReady;
    /**
     * Static SimpleDateFormat used to format date and time as: "yyyy-MM-dd HH:mm:ss".
     */
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * Static SimpleDateFormat used to format date and time as: "MM-dd-HH-mm-ss".
     */
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("MM-dd-HH-mm-ss");
    /**
     * Represents the instructions associated with the user chat.
     */
    private String instruction;
    /**
     * Represents the directory path where the chat files are stored.
     */
    private String chatdir;
    /**
     * Flag that indicates if the GPT-4 model should be used for generating responses. True if GPT-4 should be used, false otherwise.
     */
    private boolean gpt4;

    /**
     * Constructs a new ChatUser with the provided Friend, chat directory and boolean for GPT4.
     * It reads in chat history if it exists, and creates new files for history, responses, and tokens
     * if they don't exist. The ChatUser is set as ready to chat.
     *
     * @param friend  the Friend interface this ChatUser uses to communicate.
     * @param chatdir the directory where the chat history and related files are stored.
     * @param gpt4    a boolean flag indicating if the GPT4 model is used for this ChatUser.
     * @throws IOException if an I/O error occurs.
     */
    public ChatUser(Friend friend, String chatdir, boolean gpt4) throws IOException {
        this.friend = friend;
        this.chatdir = chatdir;
        this.gpt4 = gpt4;
        id = friend.getId();
        msgList = new ArrayList<ChatMessage>();
        history = new File(chatdir + "private" + File.separator + id + ".txt");
        temp = new File(chatdir + "private" + File.separator + id + "temp.txt");
        instruction = defaultInstr;
        if(history.exists()){
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(history), "UTF-8"));
            StringBuilder tempstr = new StringBuilder();
            while(reader.ready())
                tempstr.append((char)reader.read());
            reader.close();
            String[] historylst = tempstr.toString().split("\\\\\\|\\n");
            for(String msg : historylst){
                if(msg.length() != 0){
                    int idx = msg.indexOf(":");
                    msgList.add(new ChatMessage(msg.substring(0, idx), msg.substring(idx + 1)));
                }
            }
            if(msgList.size() != 0)
                instruction = msgList.get(0).message;
        }else{
            history.createNewFile();
            temp.createNewFile();
            File responsefile = new File(chatdir + "private" + File.separator + id + "response.txt");
            File tokenfile = new File(chatdir + "private" + File.separator + id + "tokens.txt");
            responsefile.createNewFile();
            tokenfile.createNewFile();
        }
        chatReady = true;
    }
    /**
     * Clears the chat history of the user.
     * Removes all messages and sets the first message as an instruction.
     */
    public void clearHistory(){
        msgList.clear();
        msgList.add(new ChatMessage("system", instruction));
    }

    /**
     * Sends a chat message and receives a response from an external script.
     * It adds the sent message and received response to the chat message list.
     * If the external script doesn't respond in 60 seconds, it aborts the process and removes the message from the list.
     * The chat status is updated before and after sending a message.
     *
     * @param msg the message to be sent.
     * @return a string representing the SQL insert command to log this chat into a database.
     */
    public String chat(String msg){
        chatReady = false;
        msgList.add(new ChatMessage("user", msg));
        String result = "";
        try{
            FileOutputStream fs = new FileOutputStream(temp);
            fs.write(chatListString(msgList).getBytes("UTF-8"));
            fs.close();
            String response = "";
            Process process = null;
            if(gpt4)
                process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + "private" + File.separator + " && python privatechatgpt4.py " + id);
            else
                process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + "private" + File.separator + " && python privatechat.py " + id);
            if(process.waitFor(60, TimeUnit.SECONDS)){
                if(process.exitValue() == 0){
                    File file = new File(chatdir + "private" + File.separator + id + "response.txt");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                    while(reader.ready())
                        response += (char)reader.read();
                    String responsetime = SDF.format(new Date());
                    File tokens = new File(chatdir + "private" + File.separator + id + "tokens.txt");
                    BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                    int numtoken = Integer.parseInt(tokenreader.readLine());
                    result = "INSERT INTO PrivateChat(user,time,tokens) VALUES(" + id + ",\'" + responsetime + "\'," + numtoken + ")";
                    msgList.add(new ChatMessage("assistant", response));
                    friend.sendMessage(response);
                    reader.close();
                    tokenreader.close();
                }else{
                    msgList.remove(msgList.size() - 1);
                    friend.sendMessage("获取回答出错");
                    chatReady = true;
                    return null;
                }
            }else{
                msgList.remove(msgList.size() - 1);
                friend.sendMessage("获取回答超时");
                process.destroy();
                chatReady = true;
                return null;
            }
        }catch (Exception e){
            msgList.remove(msgList.size() - 1);
            friend.sendMessage("获取回答发生异常，请将下列信息反馈给管理员：" + e.getMessage());
            chatReady = true;
            throw new RuntimeException(e);
        }
        chatReady = true;
        return result;
    }
    /**
     * Saves the chat history to a file with a default name based on current time.
     *
     * @return the file where the chat history is saved, or null if the file already exists.
     */
    public File saveToFile(){
        Date now = new Date();
        return saveToFile(SDF2.format(now));
    }
    /**
     * Saves the current chat history to a file with the given name. If the file already exists,
     * the method returns null and no changes are made. If the file does not exist, it is created
     * and the chat history is written to it. If the file is successfully created and written,
     * the method returns a reference to the new file.
     *
     * @param name the name of the file to create and write to, not including the ".txt" extension
     * @return a File object representing the new file, or null if the file already exists or could not be created or written to
     * @throws RuntimeException if there was an error writing to the new file
     */
    public File saveToFile(String name){
        File result = new File(chatdir + "private" + File.separator + id + File.separator + name + ".txt");
        try{
            if(result.exists()){
                return null;
            }else{
                if(result.createNewFile()){
                    FileOutputStream fs = new FileOutputStream(result);
                    fs.write(chatListString(msgList).getBytes("UTF-8"));
                    fs.close();
                }else
                    return null;
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        return result;
    }
    /**
     * Reads the user chat history from a specified file.
     *
     * @param name The name of the file (without the extension) from which the chat history should be read.
     * @return The number of messages that have been read. Returns -1 if the file does not exist.
     * @throws IOException if there is a problem reading the file.
     */
    public int readFromFile(String name) throws IOException {
        File file = new File(chatdir + "private" + File.separator + id + File.separator + name);
        if(file.exists()){
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder tempstr = new StringBuilder();
            while(reader.ready())
                tempstr.append((char)reader.read());
            reader.close();
            String[] historylst = tempstr.toString().split("\\\\\\|\\n");
            msgList.clear();
            for(String msg : historylst){
                if(msg.length() != 0){
                    int idx = msg.indexOf(":");
                    msgList.add(new ChatMessage(msg.substring(0, idx), msg.substring(idx + 1)));
                }
            }
            if(msgList.size() != 0)
                instruction = msgList.get(0).message;
            return historylst.length;
        }else{
            return -1;
        }
    }
    /**
     * Lists all chat history files of the user.
     *
     * @return An array of filenames (without paths) of all chat history files of the user.
     */
    public String[] listFiles(){
        File folder = new File(chatdir + "private" + File.separator + id);
        File[] files = folder.listFiles();
        String[] filenames = new String[files.length];
        for(int i=0;i<files.length;i++)
            filenames[i] = files[i].getName();
        return filenames;
    }
    /**
     * Attempts to regenerate the last response from the chatbot.
     * Removes the last user message and the assistant response from the history and initiates a chat with the message prior to the last.
     *
     * @return The response from the chatbot to the message prior to the last.
     */
    public String regenerate(){
        String msg = msgList.get(msgList.size() - 2).message;
        msgList.remove(msgList.size() - 1);
        msgList.remove(msgList.size() - 1);
        return chat(msg);
    }
    /**
     * Reverts the last conversation turn.
     * Removes the last user message and the assistant response from the history.
     *
     * @return true if the operation was successful, false if there were not enough messages in the history to perform the operation.
     */
    public boolean pushback(){
        if(msgList.size() <= 1){
            return false;
        }else{
            msgList.remove(msgList.size() - 1);
            msgList.remove(msgList.size() - 1);
            return true;
        }
    }
    /**
     * Clears the chat history and sets the initial instruction message for the chat session.
     * This message appears at the start of the chat history when it is cleared.
     *
     * @param instr The message to be set as the initial instruction.
     */
    public void setInstruction(String instr){
        msgList.clear();
        instruction = instr;
        msgList.add(new ChatMessage("system", instruction));
    }

    public void setInstruction(int n){
        msgList.clear();
        instruction = instrlst[n];
        msgList.add(new ChatMessage("system", instruction));
    }
    /**
     * Returns the current instruction of the ChatUser. The instruction is initially set to the first
     * message of the chat history (if it exists), or a default instruction if the chat history is empty.
     * It can be changed using the setInstruction methods.
     *
     * @return a String representing the current instruction
     */
    public String getInstruction(){
        return instruction;
    }
    /**
     * Saves the current chat history to a history file. The history file is overwritten each time
     * this method is called. If the file is successfully written, the method returns true.
     *
     * @return true if the chat history was successfully written to the history file
     * @throws RuntimeException if there was an error writing to the history file
     */
    public boolean save(){
        try {
            FileOutputStream fs = new FileOutputStream(history);
            fs.write(chatListString(msgList).getBytes("UTF-8"));
            fs.close();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Converts the provided list of ChatMessages into a String representation.
     * Each ChatMessage in the list is translated into a string in the format: "role:message\|\n".
     * Note that the resulting string uses the "role" and "message" properties of each ChatMessage.
     * The messages are separated by a "\|\n" delimiter.
     *
     * @param list The list of ChatMessages to be converted to a String representation.
     * @return A String representation of the ChatMessages list.
     */
    private static String chatListString(List<ChatMessage> list){
        String result = "";
        for(ChatMessage chatMessage : list){
            result += chatMessage.role + ":" + chatMessage.message + "\\|\n";
        }
        return result;
    }
    private static String chatListSave(List<ChatMessage> list){
        String result = "";
        for(ChatMessage chatMessage : list){
            result += "[" + chatMessage.role + "]: " + chatMessage.message + "\n";
        }
        return result;
    }
}
