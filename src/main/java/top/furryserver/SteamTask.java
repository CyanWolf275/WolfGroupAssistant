package top.furryserver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class SteamTask implements Callable<SteamStatus> {
    private String url;
    private String playername;
    private String chatdir;
    public SteamTask(String url, String playername, String chatdir){
        this.url = url;
        this.playername = playername;
        this.chatdir = chatdir;
    }
    @Override
    public SteamStatus call() throws Exception {
        Process process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + " && python steamall.py " + url);
        InputStream inputStream = process.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        SteamStatus result = null;
        if(process.waitFor(10, TimeUnit.SECONDS)){
            if(process.exitValue() == 0){
                String name = bufferedReader.readLine().trim();
                String status = bufferedReader.readLine().trim();
                name = asciiToNative(name);
                if(Objects.isNull(name) || Objects.isNull(status)){
                    result = new SteamStatus(playername, null, 8, null);
                }else{
                    if(status.equals("7")){
                        result = new SteamStatus(playername, name, Integer.parseInt(status), asciiToNative(bufferedReader.readLine()));
                    }else{
                        result = new SteamStatus(playername, name, Integer.parseInt(status), null);
                    }
                }
            }else{
                result = new SteamStatus(playername, null, 8, null);
            }
        }else{
            process.destroy();
            result = new SteamStatus(playername, null, 9, null);
        }
        bufferedReader.close();
        inputStream.close();
        return result;
    }
    private static String asciiToNative(String asciicode) {
        String[] asciis = asciicode.split("\\\\u");
        String nativeValue = asciis[0];
        try {
            for (int i = 1; i < asciis.length; i++) {
                String code = asciis[i];
                nativeValue += (char) Integer.parseInt(code.substring(0, 4), 16);
                if (code.length() > 4) {
                    nativeValue += code.substring(4);
                }
            }
        } catch (NumberFormatException e) {
            return asciicode;
        }
        return nativeValue;
    }
}
