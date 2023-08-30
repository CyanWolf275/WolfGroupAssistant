package top.furryserver;

public class ChatMessage {
    public String role;
    public String message;
    public ChatMessage(String role, String message){
        this.role = role;
        this.message = message;
    }
}
