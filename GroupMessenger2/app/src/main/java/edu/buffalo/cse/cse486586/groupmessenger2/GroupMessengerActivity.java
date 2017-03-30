package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private List<String> REMOTE_PORTS = Arrays.asList("11108","11112","11116","11120","11124");
    static final int SERVER_PORT = 10000;
    static final String ACK_STR = "PA-2B OK";
    private int MESSAGE_COUNTER = 0;
    private int INITIAL_CAPACITY = 1;
    private double MAX_SEEN_COUNTER = 0.0;
    private String MY_PORT;
    private ConcurrentHashMap<String,ConcurrentHashMap> idMap = new ConcurrentHashMap<String, ConcurrentHashMap>();

    private ConcurrentHashMap<String, Message> origMsgMap = new ConcurrentHashMap<String, Message>();

    private PriorityBlockingQueue<Message> holdBackQueue = new PriorityBlockingQueue<Message>(
                                            INITIAL_CAPACITY,sequenceIdComparator);

    private Message setMessageSeq(String msgId){
        /* Function to get max proposed sequence number
        * and return that message to be multicasted
        */
        double seqId = 0.0;
        ConcurrentHashMap<String, Message> tempMap = idMap.get(msgId);
        Message msg = null;
        for(String port : tempMap.keySet()){
            Message tempMessage = tempMap.get(port);
            double tempSeqId = tempMessage.getSequenceId();
            if (tempSeqId > seqId) {
                seqId = tempSeqId;
                msg = tempMessage;
            }
        }
//        Log.e("PRI",msg.getJSON());
        return msg;
    }

    private static Comparator<Message> sequenceIdComparator = new Comparator<Message>() {
        @Override
        public int compare(Message message, Message newMessage) {
            /*
            * Comparator function for custom message class
            */
            int val = Double.compare(message.getSequenceId(), newMessage.getSequenceId());
//            Log.e("VAL", String.valueOf(message.getSequenceId()) + " : " +
//                    String.valueOf(newMessage.getSequenceId()) + " : " +
//                    String.valueOf(val));
            return val;
        }
    };

    private void setRepeatingAsyncTask() {
        /* Function to check for dead avds and
        *  and print deliverable messages from
        *  hold back queue
        *  It starts after 10 seconds so as to give time for
        *  app to start on each avd
        *  It checks every 5 seconds
        *  Referenced from http://stackoverflow.com/questions/6531950/how-to-execute-async-task-repeatedly-after-fixed-time-intervals
        */
        final Handler handler = new Handler();
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            for(String port : REMOTE_PORTS){
                                Message fakeMessage = new Message(MY_PORT, "");
                                fakeMessage.setMessageType("FAKE");
                                Log.e("DAEMON",fakeMessage.getJSON());
                                new ClientTask().execute(fakeMessage);
                            }
                            printMessages();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        timer.schedule(task, 10000, 5000);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        MY_PORT = String.valueOf((Integer.parseInt(portStr) * 2));
//        Log.e("PORT",MY_PORT);

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setReuseAddress(true);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.
//                tv.append("\t" + msg); // This is one way to display a string.
//                tv.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                /*
                * Create new Message Object to be sent
                */
                Message message = new Message(MY_PORT, msg);
                String msgId = message.getUniqueID();
                message.setSequenceId(0.0);
                idMap.put(msgId, new ConcurrentHashMap<String, Message>());

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
            }
        });
        /*
        * Run timed thread once
        */
        setRepeatingAsyncTask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, Message, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;
            while(true) {

                try{
                    socket = serverSocket.accept();
                    DataInputStream recievedStream = new DataInputStream(socket.getInputStream());
                    String recievedMessage = recievedStream.readUTF();
                    Message message = new Message(recievedMessage);
                    DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
                    sendData.writeUTF(ACK_STR);
                    /*
                    * If any AVD doesnt recieve acknowledgment, then it can safely assume that its
                    * dead [ EOFERROR ]
                    */
                    socket.close();
                    if (message.getMessageType().equals("NEW")){
                        /*
                        * PROPOSE SEQUENCE IF NEW MESSAGE RECIEVED
                        */
                        MAX_SEEN_COUNTER++;
                        double maxSequenceId = MAX_SEEN_COUNTER +
                                (0.1 * (REMOTE_PORTS.indexOf(MY_PORT)+1));
                        message.setSequenceId(maxSequenceId);
                        message.setSenderPort(MY_PORT);
                        origMsgMap.put(message.getUniqueID(),message);
                        holdBackQueue.add(message);
//                        Log.e("AGREED",message.getJSON());
                        publishProgress(message);
                    }
                    else if (message.getMessageType().equals("PROPOSED")){
                        /*
                        * ADD MESSAGE TO HashMap of the Sender for the messageId
                        * If message recieved from all clients then get max sequence
                        * and multicast to everyone
                        */
                        String msgId = message.getUniqueID();
                        ConcurrentHashMap<String, Message> tempMap = idMap.get(msgId);
                        if (tempMap != null) {
                            tempMap.put(message.getSenderPort(), message);
                            if (tempMap.keySet().size() >= REMOTE_PORTS.size()) {
                                Message finalMessage = setMessageSeq(msgId);
                                publishProgress(finalMessage);
                            }
                            idMap.remove(msgId);
                            idMap.put(msgId,tempMap);
                        }
                    }
                    else if (message.getMessageType().equals("ACCEPTED")){
                        /*
                        * If final message recieved then set deliverable to true
                        * and update holdback queue
                        */
                        MAX_SEEN_COUNTER = Math.ceil(Math.max(MAX_SEEN_COUNTER,message.getSequenceId()));
                        String msgId = message.getUniqueID();
                        message.setDeliverable(true);
//                        Log.e("ACCEPTED",message.getJSON());
                        Message origMessage = origMsgMap.get(msgId);
                        if(origMessage != null){
                            Log.d("DELETE",origMessage.getMessage() + ":" +
                                    String.valueOf(holdBackQueue.remove(origMessage)));
                            origMsgMap.remove(msgId);
                            idMap.remove(msgId);
                            holdBackQueue.add(message);
                            publishProgress(message);
                        }
//                        Log.d("ORIG",origMessage.getJSON());



                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            return null;
        }

        protected void onProgressUpdate(Message...msgs) {
            /*
             * The following code displays what is received in doInBackground().
             */
            Message message = msgs[0];
            if (message.getMessageType().equals("NEW")){
                /*
                * Unicast to sender only
                */
                message.setMessageType("PROPOSED");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
            }
            else if (message.getMessageType().equals("PROPOSED")){
                /*
                * multicast to everyone once sequence number is decided
                */
                message.setMessageType("ACCEPTED");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);

            }

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            return;
        }
    }

    public void printMessages(){
        /*
        * Function to print messages from holdback queue
        */
        while (true){
            Message topMessage = holdBackQueue.peek();
//                    Log.e("MESSAGE",topMessage.toString());
            if (topMessage == null || !topMessage.getDeliverable()){
                break;
            }
            topMessage = holdBackQueue.poll();
            if(topMessage == null){
                break;
            }
            String strReceived = topMessage.getMessage();
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", Integer.toString(MESSAGE_COUNTER));
            contentValues.put("value", strReceived);
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            Uri uri = uriBuilder.build();
            getContentResolver().insert(uri,contentValues);
            MESSAGE_COUNTER++;
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\n");
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<Message, Void, Void> {
        @Override
        protected Void doInBackground(Message... msgs) {
            Message message = msgs[0];
            String remotePort = null;
            try {
//                Log.d("QUEUE", message.getJSON());

                if(message.getMessageType().equals("PROPOSED")){
                    List<String> portsArray = new ArrayList<String>();
                    portsArray.add(message.getSenderId());
                    sendMessage(message, portsArray);
                }
                else{
                    sendMessage(message, REMOTE_PORTS);
                }

            } catch (EOFException e) {
                /*
                * Remove Port from ports list and handle all messages related to it
                */
//                Log.d("DEAD",e.getMessage());
//                Log.e(TAG, "ClientTask socket IOException");
                remotePort = e.getMessage();
//                Log.d("SLEEP","SLEEPING 3 SEC");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
//                Log.d("SLEEP","CALLING FUNC");
                removePort(remotePort);
//                e.printStackTrace();
//                Log.d("DEAD",remotePort);
//                Log.e("DEAD", e.getMessage());
//                Log.i("DEAD", error);
//                if(e.getClass().toString().equals("class java.io.EOFException")){
//                    removePort(remotePort);
//                }
            }  catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
            }

            return null;
        }

        private void sendMessage(Message message, List<String> ports) throws EOFException {
            /*
            * generic send Message function for both Unicast and Multicast
            */
            String temp = null;
            String errPort = null;

            for ( String tempPort : ports) {
//                try {
//                Log.i("removePort","Port process = " + tempPort);
                temp = tempPort;
                String remotePort = tempPort;
                Socket socket = null;
                OutputStream sendStream = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    sendStream = socket.getOutputStream();
                    DataOutputStream sendData = new DataOutputStream(sendStream);
                    socket.setSoTimeout(4000);
                    sendData.writeUTF(message.getJSON());
                    sendData.flush();
                    DataInputStream recvData = new DataInputStream(socket.getInputStream());
                    socket.setSoTimeout(4000);
                    String resp = recvData.readUTF();
                    //                    socket.close();
                    if (resp.equals(ACK_STR)) {
                        //                        Log.i(TAG, ACK_STR);
                        socket.close();
                    }
                } catch (IOException exc) {
                    errPort = tempPort;
                }
            }
            if(errPort != null) {
                throw new EOFException(errPort);
            }
        }

        private void removePort(String port){
            /*
            * function to remove port from ports list and
            * handle all messages related to the port
            */
//            Log.d("removePort",port);
//            Log.d("removePort", String.valueOf(REMOTE_PORTS.indexOf(port)));
            List<String> newPortList = new ArrayList<String>();
            String ports = "";
            for(String tempPort : REMOTE_PORTS){
                if(!tempPort.equals(port)){
                    newPortList.add(tempPort);
                    ports += tempPort + ": ";
                }
            }
//            Log.d("removePort",ports);
            REMOTE_PORTS = new ArrayList<String>(newPortList);
//            Log.d("removePort",String.valueOf(REMOTE_PORTS.size()));
            for(String msgId : origMsgMap.keySet()){
                Message msg = origMsgMap.get(msgId);
                if (msg == null){
                    continue;
                }
                String senderId = msg.getSenderId();
                if (senderId.equals(port)){
                    /*
                    * if message was of crashed avds then
                    * we can safely remove it
                    */
                    origMsgMap.remove(msgId);
                    holdBackQueue.remove(msg);
                    idMap.remove(msgId);
                }
                else{
                    ConcurrentHashMap<String, Message> tempMap = idMap.get(msgId);
//                    Log.d("removePort", "Found hashMap for " + msg.getMessage());
                    if(tempMap != null){
//                        Log.d("removePort", Arrays.toString(tempMap.keySet().toArray()));
                        if (!tempMap.keySet().contains(port)){
                            /*
                            * If sequence number is yet to come from crashed avd then just send
                            * a fake message for crashed avd to the message creator as its sequence
                            * number wont matter
                            */
                            try{
                                Message newMsg = new Message(msg.getJSON());
                                newMsg.setSenderPort(port);
                                newMsg.setMessageType("PROPOSED");
                                newMsg.setSequenceId(0.0);
//                                Log.d("FAKE",newMsg.getJSON());
                                List<String> portsArray = new ArrayList<String>();
                                portsArray.add(newMsg.getSenderId());
                                sendMessage(newMsg, portsArray);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            /*
                            * If message already recieved from crashed avd
                            * then we can safely remove it as its sequence number
                            * wont be used
                            */
                            tempMap.remove(port);

                            if (tempMap.size() == REMOTE_PORTS.size()){
                                try{
                                    Message finalMsg = setMessageSeq(msgId);
                                    finalMsg.setMessageType("ACCEPTED");
                                    sendMessage(finalMsg, REMOTE_PORTS);
                                } catch (EOFException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }

                }

            }

        }
    }
}
