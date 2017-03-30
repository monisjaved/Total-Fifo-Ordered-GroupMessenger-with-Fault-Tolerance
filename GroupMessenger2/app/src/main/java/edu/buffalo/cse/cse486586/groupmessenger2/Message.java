package edu.buffalo.cse.cse486586.groupmessenger2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by moonisjaved on 3/10/17.
 */


public class Message {
    private String senderId;
    private String uniqueID;
    private boolean deliverable;
    private String message;
    private double sequenceId;
    private String messageType;
    private String senderPort;


    public Message(String id, String msg){
        this.senderId = id;
        this.deliverable = false;
        this.message = msg;
        this.messageType = "NEW";
        this.uniqueID = UUID.randomUUID().toString();
    }

    public Message(String jsonString){
        JSONObject json = null;
        try {
            json = new JSONObject(jsonString);
            this.senderId = json.get("senderId").toString();
            this.deliverable = Boolean.parseBoolean(json.get("deliverable").toString());
            this.message = json.get("message").toString();
            this.messageType = json.get("messageType").toString();
            this.uniqueID = json.get("uniqueID").toString();
            this.sequenceId = Float.parseFloat(json.get("sequenceId").toString());
            this.senderPort = json.get("senderPort").toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String getSenderPort() { return this.senderPort; }

    public void setSenderPort(String senderPort) { this.senderPort = senderPort; }

    public String getUniqueID(){
        return this.uniqueID;
    }

    public void setSequenceId(double id){
        this.sequenceId = id;
    }

    public double getSequenceId(){
        return this.sequenceId;
    }

    public void setDeliverable(boolean val){
        this.deliverable = val;
    }

    public String getSenderId(){
        return this.senderId;
    }

    public boolean getDeliverable(){
        return this.deliverable;
    }

    public String getMessage(){
        return this.message;
    }

    public void setMessageType(String val){
        this.messageType = val;
    }

    public String getMessageType() {
        return this.messageType;
    }

    public String getString(){
        String result = "";
        result += this.message + ":::" + this.senderId + ":::" +
                this.uniqueID + ":::" + this.messageType;
        return result;
    }

    public String getJSON(){
        Map<String, String> jsonMap = new HashMap<String, String>();
        jsonMap.put("senderId",String.valueOf(this.senderId));
        jsonMap.put("uniqueID", this.uniqueID);
        jsonMap.put("deliverable",String.valueOf(this.deliverable));
        jsonMap.put("message",String.valueOf(this.message));
        jsonMap.put("sequenceId",String.valueOf(this.sequenceId));
        jsonMap.put("messageType",String.valueOf(this.messageType));
        jsonMap.put("senderPort", this.senderPort);
        JSONObject json = new JSONObject(jsonMap);
        return json.toString();
    }
}
