package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static int UNIQUE_MSG_ID = 1;
    private Uri providerUri = null;
    private static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    private static final float priorities[] = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    private static int unique_msg_ids[] = {1, 100, 200, 300, 400};
    private static int maxProposedSeqNo=0;
    private static int maxAgreedSeqNoOfGrp = 0;
    private int KEY_SEQUENCE_NO_FOR_AVD = 0;
    private boolean crash = false;
    private boolean crashCountFlag = true;
    private boolean crashCountFlag2 = true;
    private int crashed_avd = 5;
    Hashtable<Integer, ProposedObject> proposeMsgTable = new Hashtable();
    PriorityQueue<MessageObj> priorityQueue = new PriorityQueue();
    private static final String NEW_MESSAGE = "NEW_MESSAGE";
    private static final String AGREED_SEQUENCE = "AGREED_SEQUENCE";
    private static final String PROPOSED_SEQUENCE = "PROPOSED_SEQUENCE";
    private static final String CRASH_DETECT= "CRASH_DETECT";

    boolean isCrashDetected = false;


    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
         /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

         /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        int count=0;
        for (String REMOTE_PORT : REMOTE_PORTS) {
            if (myPort.equalsIgnoreCase(REMOTE_PORT)) {
                //indexForPort = count; //CHECK HERE IF UR GETTING THE CORRENT MYPORT
                break;
            }
            count++;
        }
        final int indexForPort= count;

            proposeMsgTable = new Hashtable<Integer, ProposedObject>();
            priorityQueue = new PriorityQueue<MessageObj>();

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                localTextView.append("\n");
                MessageObj msgObj = new MessageObj();
                msgObj.setSenderId(Integer.parseInt(myPort));
                msgObj.setMsgText(msg);
                msgObj.setMsgType(NEW_MESSAGE);
                msgObj.setDeliveryStatus(false);
                msgObj.setUniqueIdMsg(unique_msg_ids[indexForPort]++);
                msgObj.setSequenceNo(0F);

                Log.e(TAG, "MY SENDER ID IS :"+myPort);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgObj);

                return;
            }
        });

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
     * <p/>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.e(TAG, "/////// 9999999 ");

            //get the port No of the ServerTask shud be inside the while loop or outside check
            TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            final String myPortNo = String.valueOf((Integer.parseInt(portStr) * 2));
            int indexForPort = 0;
            int count = 0;

            for (String REMOTE_PORT : REMOTE_PORTS) {
                if (myPortNo.equalsIgnoreCase(REMOTE_PORT)) {
                    indexForPort = count; //CHECK HERE IF UR GETTING THE CORRENT MYPORT
                    break;
                }
                count++;
            }

            while(true) {
            Socket socket = null;
            ObjectInputStream ois = null;
             Log.e(TAG, "/////// 10101010 "+indexForPort);

                MessageObj messageRecvd;
                try {
                    socket = serverSocket.accept();
                    ois = new ObjectInputStream(socket.getInputStream());
                    messageRecvd = (MessageObj) ois.readObject();
                    String msgRecvdType = messageRecvd.getMsgType();

                    if(isCrashDetected == false && msgRecvdType.equalsIgnoreCase(CRASH_DETECT)) {
                        isCrashDetected = true;
                        crash = true;
                        crashed_avd = messageRecvd.getCrashedAVD();
                        Log.e(TAG, "Crash Detected : " + isCrashDetected);

                    } else if (msgRecvdType.equalsIgnoreCase(NEW_MESSAGE)) {
                        // Msg Type...Unique Msg id ..seqNotoPropose....Sender Id...indexForPort...msgText
                        String[] newMessage = new String[6];
                        newMessage[0] = NEW_MESSAGE;
                        newMessage[1] = messageRecvd.getUniqueIdMsg() + "";
                        newMessage[2] = "0";
                        newMessage[3] = messageRecvd.getSenderId() + "";
                        newMessage[4] = indexForPort + "";
                        newMessage[5] = messageRecvd.getMsgText();
                        //now send the proposed sequence no to the sender of the message by calling client task from onProgressUpdate Method
                        Log.e(TAG, "Executed Uptil here 000 ................. : " + msgRecvdType);
                        publishProgress(newMessage);

                    } else if (msgRecvdType.equalsIgnoreCase(PROPOSED_SEQUENCE)) {

                        String[] proposedSeqMsg = new String[6];
                        // Msg Type...Unique Msg id ..seqNotoPropose....Sender Id..indexForMyPort
                        proposedSeqMsg[0] = PROPOSED_SEQUENCE;
                        proposedSeqMsg[1] = messageRecvd.getUniqueIdMsg() + "";
                        proposedSeqMsg[2] = messageRecvd.getSequenceNo() + "";
                        proposedSeqMsg[3] = messageRecvd.getSenderId() + "";
                        proposedSeqMsg[4] = indexForPort + "";
                        proposedSeqMsg[5] = messageRecvd.getProposedById() + "";
                        Log.e(TAG, "Executed Uptil here 111 ................. : " + msgRecvdType);
                        publishProgress(proposedSeqMsg);
                    } else if (msgRecvdType.equalsIgnoreCase(AGREED_SEQUENCE)) {

                        Log.e(TAG, "Executed Uptil here.... 4");

                        String[] agreedSeqMsg = new String[6];
                        // Msg Type...Unique Msg id ..seqNotoPropose....Sender Id..indexForMyPort
                        agreedSeqMsg[0] = AGREED_SEQUENCE;
                        agreedSeqMsg[1] = messageRecvd.getUniqueIdMsg() + "";
                        agreedSeqMsg[2] = messageRecvd.getSequenceNo() + "";
                        agreedSeqMsg[3] = messageRecvd.getSenderId() + "";
                        agreedSeqMsg[4] = indexForPort + "";
                        agreedSeqMsg[5] = messageRecvd.getProposalCount() + "";
                        Log.e(TAG, "Executed Uptil here 222 ................. : " + msgRecvdType);
                        publishProgress(agreedSeqMsg);
                    }
                    ois.close();
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onProgressUpdate(String... strings) {
            String msgTypeReceived = strings[0];
            int uniqueIdMsg = Integer.parseInt(strings[1].trim());
            Float sequenceId = Float.parseFloat(strings[2].trim());
            int senderId = Integer.parseInt(strings[3].trim());
            int indexForPort = Integer.parseInt(strings[4]);

            if(crash){
                Iterator iterator = priorityQueue.iterator();
                MessageObj obj = null;
                int i=0;
                while (iterator.hasNext()) {

                    obj = (MessageObj) iterator.next();
                    if(obj.getSenderId() == crashed_avd){
                        priorityQueue.remove(obj);
                        Log.e(TAG, "Crashed AVD : "+crashed_avd);
                        Log.e(TAG, " .... Removing msgs of crashed avd no of msgs : "+i +"obj removed : "+obj); i++;

                    }

                }
              //  crashCountFlag = false;
            }

            if(crash){


                Iterator iterator = priorityQueue.iterator();
                MessageObj obj = null;
                Log.e(TAG, "I am here ???????");
                while (iterator.hasNext()) {

                    obj = (MessageObj) iterator.next();
                    int id = obj.getUniqueIdMsg();
                    if(proposeMsgTable.contains(id)) {
                        Log.e(TAG, "I am here ######");
                        int noOfProposes = proposeMsgTable.get(id).getNoOfProposals();
                        boolean proposalOfCrashedAvdRcvd = false;
                        if(proposeMsgTable.get(id).getProposalSenderMap().get(crashed_avd)==1) {
                            proposalOfCrashedAvdRcvd = true;
                        } else {
                            proposalOfCrashedAvdRcvd = false;
                        }
                        if(!proposalOfCrashedAvdRcvd && noOfProposes==4) {
                            // send the agreed sequence for msgs stuck in the queue
                            MessageObj agreedSeqMsg = obj;
                            Float newAgreedSeqNo = proposeMsgTable.get(id).getMaxProposedSeqNo();
                            agreedSeqMsg.setSequenceNo(newAgreedSeqNo);
                            agreedSeqMsg.setMsgType(AGREED_SEQUENCE);

                            if (newAgreedSeqNo > maxAgreedSeqNoOfGrp) {
                                maxAgreedSeqNoOfGrp = newAgreedSeqNo.intValue();
                            }
                            agreedSeqMsg.setMaxAgreedSequenceNo(maxAgreedSeqNoOfGrp);
                            proposeMsgTable.remove(id);

                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, agreedSeqMsg);

                            Log.e(TAG, "I am here @@@@ " + agreedSeqMsg);

                        }

                    }
                }
               // crashCountFlag2 = false;
            }



            if (msgTypeReceived.equalsIgnoreCase(NEW_MESSAGE)) {

                String msgText = strings[5];
                //set the sequence no of the newly received message
                int whatsMaxSeqNo = maxProposedSeqNo > maxAgreedSeqNoOfGrp ? maxProposedSeqNo : maxAgreedSeqNoOfGrp;
                float seqNoToPropose = (whatsMaxSeqNo + 1) + priorities[indexForPort];
                maxProposedSeqNo = whatsMaxSeqNo + 1;//cross-check float/int and final priortities if it shud be eg 3 or 3,1

                MessageObj msgToInsertInQueue = new MessageObj();
                msgToInsertInQueue.setSenderId(senderId);
                msgToInsertInQueue.setSequenceNo(seqNoToPropose);
                msgToInsertInQueue.setDeliveryStatus(false);
                msgToInsertInQueue.setMsgType(NEW_MESSAGE);
                msgToInsertInQueue.setMsgText(msgText);
                msgToInsertInQueue.setUniqueIdMsg(uniqueIdMsg);
                msgToInsertInQueue.setProposedById(Integer.parseInt(REMOTE_PORTS[indexForPort]));
                priorityQueue.add(msgToInsertInQueue);

                MessageObj proposedSeqMsg = new MessageObj();
                proposedSeqMsg.setUniqueIdMsg(uniqueIdMsg);
                proposedSeqMsg.setMsgType(PROPOSED_SEQUENCE);
                proposedSeqMsg.setSequenceNo(seqNoToPropose);
                proposedSeqMsg.setSenderId(senderId);
                proposedSeqMsg.setProposedById(Integer.parseInt(REMOTE_PORTS[indexForPort]));

                Log.e(TAG, "/////// Proposed Msg is " + proposedSeqMsg.toString());

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, proposedSeqMsg);

            } else if (msgTypeReceived.equalsIgnoreCase(PROPOSED_SEQUENCE)) {
               try {

                   int proposedByID = Integer.parseInt(strings[5]);
                   ProposedObject obj;
                   Float newMaxProposedSeqNo;
                   int newProposedCount;
                   obj = proposeMsgTable.get(uniqueIdMsg);
                    if(obj!=null) {
                        Log.e(TAG,"Obj is not null");
                    } else {
                        Log.e(TAG,"Obj is null");
                    }

                   newMaxProposedSeqNo = (obj.getMaxProposedSeqNo()) > sequenceId ? obj.getMaxProposedSeqNo() : sequenceId;
                   newProposedCount = obj.getNoOfProposals() + 1;
                   obj.setNoOfProposals(newProposedCount);
                   obj.setMaxProposedSeqNo(newMaxProposedSeqNo);
                   obj.getProposalSenderMap().put(proposedByID, 1);
                   proposeMsgTable.put(uniqueIdMsg, obj); // check table here
                   Log.e(TAG, "No of Proposals Recvd : " + newProposedCount +" Proposed By : "+proposedByID +" UniqueId : "+uniqueIdMsg);

                   int noOfProcessAlive =0;

                   if((crash && proposeMsgTable.get(uniqueIdMsg).getProposalSenderMap().get(crashed_avd)==1)|| !crash) {
                       noOfProcessAlive = 5;
                   } else if (crash) {
                       noOfProcessAlive = 4;
                   }

                   Log.e(TAG, "No of alive processes"+noOfProcessAlive);

                   if (newProposedCount == noOfProcessAlive) {
                       MessageObj agreedSeqMsg = new MessageObj();
                       agreedSeqMsg.setUniqueIdMsg(uniqueIdMsg);
                       agreedSeqMsg.setMsgType(AGREED_SEQUENCE);
                       agreedSeqMsg.setSequenceNo(newMaxProposedSeqNo);
                       agreedSeqMsg.setSenderId(senderId);
                       agreedSeqMsg.setProposalCount(newProposedCount);

                       if (newMaxProposedSeqNo > maxAgreedSeqNoOfGrp) {
                           maxAgreedSeqNoOfGrp = newMaxProposedSeqNo.intValue();
                       }

                       agreedSeqMsg.setMaxAgreedSequenceNo(maxAgreedSeqNoOfGrp);
                       Log.e(TAG, "AGREED SEQ GENErated for unique id: " +uniqueIdMsg);
                       new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, agreedSeqMsg);
                   }
               }catch(Exception e) {
                   Log.e(TAG+"22222", e.getMessage());
               }

            } else if (msgTypeReceived.equalsIgnoreCase(AGREED_SEQUENCE)) {
                try {
                    int proposedCount = Integer.parseInt(strings[5]);
                    Iterator iterator = priorityQueue.iterator();
                    MessageObj obj = null;
                    if (sequenceId > maxAgreedSeqNoOfGrp) {
                        maxAgreedSeqNoOfGrp = sequenceId.intValue();
                    }
                    while (iterator.hasNext()) {

                        obj = (MessageObj) iterator.next();
                        Log.e(TAG, "/////// ********* "+obj.toString());
                        if (obj.getUniqueIdMsg() == uniqueIdMsg) {

                            Log.e(TAG, "/////// 44444444 Sequence No Changed from : " + obj.getSequenceNo() + " to " + sequenceId + " for Unique Id :" + uniqueIdMsg);
                                priorityQueue.remove(obj);
                                obj.setSequenceNo(sequenceId);
                                obj.setDeliveryStatus(true);
                            priorityQueue.add(obj);
                            break;
                        }

                    }

                    while(!priorityQueue.isEmpty()) {

                        if (priorityQueue.peek().isDeliveryStatus()) {
                            Log.e(TAG, "I am here !!!! ");
                            //means the object is at the front of the queue needs to be delivered

                            obj = priorityQueue.poll();
                                /*
                                * The following code displays what is received in doInBackground().
                                */
                            String msgReceived = obj.getMsgText();
                            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                            remoteTextView.append(msgReceived + "\t\n");
                            remoteTextView.append("\n");
                            Log.e(TAG, "/////// 555555555 : " + msgReceived);
                            ContentValues keyValueToInsert = new ContentValues();
                            keyValueToInsert.put("key", KEY_SEQUENCE_NO_FOR_AVD++ + "");
                            keyValueToInsert.put("value", msgReceived);
                            Log.e(TAG, "/////// 5555555 Nsert: " + KEY_SEQUENCE_NO_FOR_AVD);
                            Uri newUri = getContentResolver().insert(
                                    providerUri,    // assume we already created a Uri object with our provider URI
                                    keyValueToInsert
                            );
                        } /*else if (crash && !priorityQueue.peek().isDeliveryStatus() && proposedCount==4) {
                            MessageObj msgToBeProposedAgain = priorityQueue.peek();
                            msgToBeProposedAgain.setMsgType(PROPOSED_SEQUENCE);
                            msgToBeProposedAgain.setProposedById(Integer.parseInt(REMOTE_PORTS[indexForPort]));
                            Log.e(TAG, "I am here @@@@ "+msgToBeProposedAgain);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToBeProposedAgain);
                        }*/ else {
                            Log.e(TAG, "Head of queue ***** "+priorityQueue.peek()+" crash status"+crash);

                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "/////// 2222 Proposed Msg is "+e.getMessage());
                    e.printStackTrace();
                }
                return;
            }
        }
    }

        /***
         * ClientTask is an AsyncTask that should send a string over the network.
         * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
         * an enter key press event.
         *
         * @author stevko
         */
        private class ClientTask extends AsyncTask<MessageObj, Void, Void> {

            @Override
            protected Void doInBackground(MessageObj... msgs) {

                MessageObj msgObj = msgs[0];
                int indexForPort = 0, count = 0;
                for (String REMOTE_PORT : REMOTE_PORTS) {
                    if (msgObj.getSenderId() == Integer.parseInt(REMOTE_PORT)) {
                        indexForPort = count;
                        break;
                    }
                    count++;
                }

                if (msgObj.getMsgType().equalsIgnoreCase(NEW_MESSAGE)) {
                    //insert in proposeMsgTable a new entry for the message

                    proposeMsgTable.put(msgObj.getUniqueIdMsg(), new ProposedObject(0, 0));
                }

                if (msgObj.getMsgType().equalsIgnoreCase(NEW_MESSAGE) || msgObj.getMsgType().equalsIgnoreCase(AGREED_SEQUENCE)) {
                    int i = 0;


                        for (String REMOTE_PORT : REMOTE_PORTS) {
                            try {

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(REMOTE_PORT));
                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                            oos.writeObject(msgObj);
                                Log.e(TAG, "NEW MSG SENT TO: " + REMOTE_PORT +"No of NEW MSG Sent :"+(i+1));
                            oos.flush();
                            oos.close();
                            socket.close();
                            i++;
                        } catch (UnknownHostException e) {
                                Log.e(TAG, "ClientTask UnknownHostException2");
                            } catch (SocketTimeoutException e) {
                                Log.e(TAG, "ClientTask SocketTimeoutException2");
                            } catch (IOException e) {
                                crash = true;
                                crashed_avd = Integer.parseInt(REMOTE_PORT);
                                Log.e(TAG, "ClientTask socket IOException2... Crashed avd: "+crashed_avd);
                                if(isCrashDetected==false) {
                                    int j=0;
                                    //broadcase the Crash to everyone
                                    for (String REMOTE_PORT1 : REMOTE_PORTS) {
                                        try {
                                            if(Integer.parseInt(REMOTE_PORT1) != crashed_avd) {
                                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                        Integer.parseInt(REMOTE_PORT1));
                                                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                                MessageObj crashDetect = new MessageObj();
                                                crashDetect.setMsgType(CRASH_DETECT);
                                                crashDetect.setCrash(true);
                                                crashDetect.setCrashedAVD(crashed_avd);
                                                oos.writeObject(crashDetect);
                                                Log.e(TAG, "Crash MSG SENT TO: " + REMOTE_PORT1 +"No of NEW MSG Sent :"+(j+1));
                                                oos.flush();
                                                oos.close();
                                                socket.close();
                                                j++;
                                            }

                                        } catch (UnknownHostException e1) {
                                            Log.e(TAG, "ClientTask UnknownHostException2");
                                        } catch (SocketTimeoutException e1) {
                                            Log.e(TAG, "ClientTask SocketTimeoutException2");
                                        } catch (IOException e1) {
                                            Log.e(TAG, "ClientTask socket IOException2... Crashed avd: "+crashed_avd);
                                        }catch (Exception e1) {
                                            Log.e(TAG, "ClientTask socket Exception2... Crashed avd: "+crashed_avd);
                                        }

                                    }
                                }


                            }catch (Exception e) {
                                crash = true;
                                crashed_avd = Integer.parseInt(REMOTE_PORTS[i]);
                                Log.e(TAG, "ClientTask socket Exception2... Crashed avd: "+crashed_avd);
                            }
                        Log.e(TAG, "//Executed til Here Clientask1 for : " + msgObj.getMsgType());
                        Log.e(TAG, "//Executed til Here Clientask2: " + msgObj.toString());

                    }
                } else if (msgObj.getMsgType().equalsIgnoreCase(PROPOSED_SEQUENCE)) {
                    try {
                        
                        Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                msgObj.getSenderId()); //should create a new socket here or not?
                        ObjectOutputStream oos = new ObjectOutputStream((socket2.getOutputStream()));
                        oos.writeObject(msgObj);
                        oos.flush();
                        oos.close();
                        socket2.close();

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ServerTask UnknownHostException3");
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "ServerTask SocketTimeoutException3");
                    } catch (IOException e) {
                        crash = true;
                        crashed_avd = msgObj.getSenderId();
                        Log.e(TAG, "ClientTask socket IOException2... Crashed avd: "+crashed_avd);
                        if(isCrashDetected==false) {
                            int i=0;
                            //broadcase the Crash to everyone
                            for (String REMOTE_PORT : REMOTE_PORTS) {
                                try {
                                    if(Integer.parseInt(REMOTE_PORT) != crashed_avd) {
                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                Integer.parseInt(REMOTE_PORT));
                                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                        MessageObj crashDetect = new MessageObj();
                                        crashDetect.setMsgType(CRASH_DETECT);
                                        crashDetect.setCrash(true);
                                        crashDetect.setCrashedAVD(crashed_avd);
                                        oos.writeObject(crashDetect);
                                        Log.e(TAG, "Crash MSG SENT TO: " + REMOTE_PORT +"No of NEW MSG Sent :"+(i+1));
                                        oos.flush();
                                        oos.close();
                                        socket.close();
                                        i++;
                                    }

                                } catch (UnknownHostException e1) {
                                    Log.e(TAG, "ClientTask UnknownHostException2");
                                } catch (SocketTimeoutException e1) {
                                    Log.e(TAG, "ClientTask SocketTimeoutException2");
                                } catch (IOException e1) {
                                    Log.e(TAG, "ClientTask socket IOException2... Crashed avd: "+crashed_avd);
                                }catch (Exception e1) {
                                    Log.e(TAG, "ClientTask socket Exception2... Crashed avd: "+crashed_avd);
                                }

                            }
                        }

                    } catch (NullPointerException e) {
                        Log.e(TAG, "ServerTask socket Null Pointer Exception3");
                    }catch (Exception e) {
                        crash = true;
                        crashed_avd = msgObj.getSenderId();
                        Log.e(TAG, "ClientTask socket Exception2... Crashed avd: "+crashed_avd);
                    }

                }

                return null;
            }
        }

    private class ProposedObject {
        MessageObj msg;
        int noOfProposals;
        float maxProposedSeqNo;
        HashMap<Integer,Integer> proposalSenderMap = new HashMap();

        public ProposedObject() {
        }

        public ProposedObject(int noOfProposals, float maxProposedSeqNo) {
            this.noOfProposals = noOfProposals;
            this.maxProposedSeqNo = maxProposedSeqNo;
            proposalSenderMap.put(11108,0);
            proposalSenderMap.put(11112,0);
            proposalSenderMap.put(11116,0);
            proposalSenderMap.put(11120,0);
            proposalSenderMap.put(11124,0);

        }

        public int getNoOfProposals() {
            return noOfProposals;
        }

        public void setNoOfProposals(int noOfProposals) {
            this.noOfProposals = noOfProposals;
        }

        public float getMaxProposedSeqNo() {
            return maxProposedSeqNo;
        }

        public void setMaxProposedSeqNo(float maxProposedSeqNo) {
            this.maxProposedSeqNo = maxProposedSeqNo;
        }

        public MessageObj getMsg() {
            return msg;
        }

        public void setMsg(MessageObj msg) {
            this.msg = msg;
        }

        public HashMap<Integer, Integer> getProposalSenderMap() {
            return proposalSenderMap;
        }

        public void setProposalSenderMap(HashMap<Integer, Integer> proposalSenderMap) {
            this.proposalSenderMap = proposalSenderMap;
        }
    }


    }



