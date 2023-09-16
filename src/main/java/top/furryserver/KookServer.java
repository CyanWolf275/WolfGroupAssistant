package top.furryserver;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class KookServer {
    private long id;
    private String name;
    private List<KookChannel> channels;
    private String kook;

    public KookServer(long id, String kook) throws IOException {
        this.id = id;
        URL url = new URL("https://www.kookapp.cn/api/v3/guild/view?guild_id=" + id);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Authorization", "Bot " + kook);
        urlConnection.setConnectTimeout(3000);
        urlConnection.connect();
        if(urlConnection.getResponseCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            urlConnection.disconnect();
            String content = response.toString();
            JSONObject jsonObject = new JSONObject(content);
            name = jsonObject.getJSONObject("data").getString("name");
        }
        this.kook = kook;
        channels = new ArrayList<KookChannel>();
        url = new URL("https://www.kookapp.cn/api/v3/channel/list?guild_id=" + id);
        urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Authorization", "Bot " + kook);
        urlConnection.setConnectTimeout(3000);
        urlConnection.connect();
        if(urlConnection.getResponseCode() == 200){
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            urlConnection.disconnect();
            String content = response.toString();
            JSONObject jsonObject = new JSONObject(content);
            JSONArray array = jsonObject.getJSONObject("data").getJSONArray("items");
            for(int i=0;i<array.length();i++)
                if(!array.getJSONObject(i).getBoolean("is_category"))
                    channels.add(new KookChannel(array.getJSONObject(i).getLong("id"), array.getJSONObject(i).getString("name"), array.getJSONObject(i).getInt("type"), kook));
        }else{
            channels = null;
        }
    }
    public String getName(){
        return name;
    }
    public List<KookChannel> getChannels(){
        return channels;
    }
}
