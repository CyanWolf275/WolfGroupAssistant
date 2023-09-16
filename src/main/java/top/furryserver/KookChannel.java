package top.furryserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class KookChannel {
    private long id;
    private String name;
    private int type;
    private String kook;

    public KookChannel(long id, String name, int type, String kook){
        this.id = id;
        this.name = name;
        this.type = type;
        this.kook = kook;
    }
    public List<String> getVoiceUsers() throws IOException{
        String content = null;
        try{
            if(type == 1){
                return null;
            }else {
                URL url = new URL("https://www.kookapp.cn/api/v3/channel/user-list?channel_id=" + id);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Authorization", "Bot " + kook);
                urlConnection.setConnectTimeout(3000);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    List<String> users = new ArrayList<String>();
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    urlConnection.disconnect();
                    content = response.toString();
                    JSONObject jsonObject = new JSONObject(content);
                    if(jsonObject.getInt("code") == 0){
                        JSONArray array = jsonObject.getJSONArray("data");
                        for (int i = 0; i < array.length(); i++)
                            users.add(array.getJSONObject(i).getString("username"));
                        return users;
                    }else{
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }catch (JSONException e){
            throw new RuntimeException(e);
        }
    }
    public String getName(){
        return name;
    }
}
