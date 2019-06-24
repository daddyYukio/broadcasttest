package com.daddysoffice.sample.broadcasttest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MJpegServer implements Runnable {

    private Thread mLooper;

    private int mPort = 10080;

    private final String CRLF = "\r\n";

    private byte [] mFrame;

    public MJpegServer(){
    }

    public void setCurrentFrame(byte [] frame){
        mFrame = frame;
    }

    @Override
    public void run() {
        ServerSocket socket = null;

        try {
            //
            // サーバソケットを作成し、指定したポートにバインド
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.setSoTimeout(500); // 500msecでタイムアウト
            socket.bind(new InetSocketAddress(mPort));
        }
        catch(Exception e){
            e.printStackTrace();

            try{
                socket.close();
            }
            catch (Exception ex){

            }
            return; //バインドエラー
        }

        //
        // クライアントの接続を待つ
        while (isRunning()) {
            Socket clientSock = null;

            try {
                clientSock = socket.accept();
                talkToClient(clientSock);
            }
            catch (SocketTimeoutException e) {
                // ソケットタイムアウト
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            try {
                socket.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start(int port){

        stop();

        mPort = port;

        mLooper = new Thread(this);
        mLooper.start();
    }

    public void stop(){
        if (mLooper != null){
            mLooper = null;
        }
    }

    public boolean isRunning(){
        return (mLooper != null);
    }


    private void talkToClient(final Socket socket){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //
                    // リクエストヘッダの解析
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                    String line;
                    if ((line = in.readLine()) == null){
                        return;
                    }

                    String [] commands = line.split(" ");

                    if (commands.length < 2){
                        return;
                    }

                    //
                    // GETコマンド以外はエラーにする
                    if(commands[0].compareToIgnoreCase("GET") == 0) {
                        responseForVideo(socket);
                    }
                    else{
                        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                        String response = "HTTP/1.1 400 Bad Request" + CRLF;
                        out.write(response.getBytes("US-ASCII"));
                        out.close();
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally{
                    try{
                        socket.close();
                    }
                    catch(Exception e){

                    }
                }
            }
        }).start();
    }

    private void responseForVideo(Socket socket){

        BufferedOutputStream out = null;

        try{
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(3000);

            //
            // レスポンスヘッダーを返却
            out = new BufferedOutputStream(socket.getOutputStream());
            String header = "HTTP/1.1 200 OK" + CRLF;
            header += "Content-Type: multipart/x-mixed-replace; boundary=--myboundary" + CRLF;
            header += CRLF;

            out.write(header.getBytes("US-ASCII"));
            //
            // multipartの各パートを生成＆返却
            while(isRunning()){
                //
                // パートヘッダー
                header = "--myboundary" + CRLF;
                header += "Content-Length: " + String.valueOf(mFrame.length) + CRLF;
                header += "Content-type: image/jpeg" + CRLF;
                header += CRLF;
                out.write(header.getBytes("US-ASCII"));
                //
                // JPEG画像
                out.write(mFrame, 0, mFrame.length);
                //
                // パート終了(\r\n)
                out.write(CRLF.getBytes("US-ASCII"));
                //
                // 少しお休み
                Thread.sleep(100);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (out != null){
                try{
                    out.close();
                }
                catch(Exception e){

                }
            }
        }
    }

}
