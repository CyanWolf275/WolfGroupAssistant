package top.furryserver;

import java.io.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ImgGPT{

    private Process process;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private PrintStream printStream;

    public void start() throws IOException {
        if(process != null && process.isAlive())
            return;
        process = Runtime.getRuntime().exec("cmd /c cd C:\\Users\\Administrator\\Desktop\\chat && python web.py");
        outputStream = process.getOutputStream();
        inputStream = process.getInputStream();
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        printStream = new PrintStream(outputStream);
    }

    public String question(String file, String msg) throws IOException, InterruptedException {
        printStream.println(file + " " + msg);
        printStream.flush();
        long t = new Date().getTime();
        String response = "";
        while((new Date().getTime() - t) < 60000){
            if(bufferedReader.ready()){
                response = bufferedReader.readLine();
                if(response.startsWith("~"))
                    break;
                else{
                    System.out.println(response);
                }
            }
        }
        if((new Date().getTime() - t) > 60000)
            return null;
        return response.substring(1);
    }

    public void exit() throws InterruptedException {
        printStream.println("exit");
        printStream.flush();
        if(!process.waitFor(10, TimeUnit.SECONDS))
            process.destroy();
    }


}
