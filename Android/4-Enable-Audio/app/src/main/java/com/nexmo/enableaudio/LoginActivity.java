package com.nexmo.enableaudio;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.auth0.jwt.JWTSigner;
/*import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;*/
import com.auth0.jwt.internal.org.apache.commons.codec.StringEncoderComparator;
import com.nexmo.client.NexmoUnexpectedException;
import com.nexmo.sdk.conversation.client.Call;
import com.nexmo.sdk.conversation.client.CallEvent;
import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.EventSource;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.MemberMedia;
import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.client.audio.AppRTCAudioManager;
import com.nexmo.sdk.conversation.client.audio.AudioCallEventListener;
import com.nexmo.sdk.conversation.client.audio.AudioCallStatsListener;
import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.ResultListener;
import com.nexmo.sdk.conversation.client.event.container.Invitation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import java.nio.file.Paths;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nexmo.client.NexmoClient;
import com.nexmo.client.auth.JWTAuthMethod;
import com.nexmo.sdk.conversation.client.event.network.NetworkState;
import com.nexmo.sdk.conversation.client.event.network.NetworkingStateListener;

import org.webrtc.StatsReport;

import static android.Manifest.permission.RECORD_AUDIO;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    RequestQueue queue = null;
    String url ="https://rajkiran.nexmodemo.com/generateToken/jwt.php?user=";

    private String USER_JWT="";
    private static final int PERMISSION_REQUEST_AUDIO = 0;
    String myName="";
    String calleeName="";
    private Button loginBtn;
    private Button callBtn;
    private Button chatBtn;
    private Button audioBtn;
    private ConversationClient conversationClient;
    private TextView loginTxt;

    private Vibrator v;
    MediaPlayer player;
    private Call directCall;

    String codec = null;
    int packetsLostRecv = 0;
    int packetsLostSend = 0;
    int rtt = 0;
    int jitterBufferMS=0;
    int jitterBufferMSMax=0;
    int jitterBufferMSMin=100000;
    int reportCount =0;

    int localPktRecv=0;
    int localPktSend=0;
    int localRTT=0;
    int localJitter=0;
    int avgRTT=0;
    int avgPktSend=0;
    int avgPktRecv=0;
    int avgJitter=0;

    @Override
    public void onDestroy()
    {
        super.onDestroy();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("myUser", myName);
        savedInstanceState.putString("otherUser", calleeName);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            myName = savedInstanceState.getString("myUser");
            calleeName = savedInstanceState.getString("otherUser");
        }
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        conversationClient = ((ConversationClientApplication) getApplication()).getConversationClient();
        queue= Volley.newRequestQueue(this);
        loginTxt = findViewById(R.id.login_text);
        loginBtn = findViewById(R.id.login);
        callBtn = findViewById(R.id.callbutton);
        audioBtn = findViewById(R.id.AudioDevice);

        requestAudio();

        if(conversationClient.isLoggedIn())
        {
            loginBtn.setVisibility(View.INVISIBLE);
            callBtn.setVisibility(View.VISIBLE);
        }
        audioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(directCall != null){

                    Set<AppRTCAudioManager.AudioDevice> devices = directCall.getConversation().media(Conversation.MEDIA_TYPE.AUDIO).getAvailableAudioRoutes();
                    if(devices == null) {
                        Toast.makeText(LoginActivity.this,"Not in Active audio conversation",Toast.LENGTH_SHORT).show();

                        return;
                    }
                    List<String> strings = new ArrayList<String>();
                    for(AppRTCAudioManager.AudioDevice dev : devices){
                        Log.d("DEVICELIST",dev.name());
                        strings.add(dev.name());
                    }
                    final CharSequence[] items = {"WIRED_HEADSET","EARPIECE","SPEAKER_PHONE","BLUETOOTH"};//strings.toArray(new String[strings.size()]);
                    AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
                    adb.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface d, int n) {
                            AppRTCAudioManager.AudioDevice selectedDev = AppRTCAudioManager.AudioDevice.EARPIECE;
                            if(items[n].toString().equalsIgnoreCase("WIRED_HEADSET")){
                                selectedDev = AppRTCAudioManager.AudioDevice.WIRED_HEADSET;
                            }
                            else if(items[n].toString().equalsIgnoreCase("EARPIECE"))
                            {
                                selectedDev = AppRTCAudioManager.AudioDevice.EARPIECE;
                            }
                            else if(items[n].toString().equalsIgnoreCase("SPEAKER_PHONE"))
                            {
                                selectedDev = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE;
                            }
                            else if(items[n].toString().equalsIgnoreCase("BLUETOOTH"))
                            {
                                selectedDev = AppRTCAudioManager.AudioDevice.BLUETOOTH;
                            }
                            Log.d("CHANGEDEVICE","Changing device to "+selectedDev.toString());
                            directCall.getConversation().media(Conversation.MEDIA_TYPE.AUDIO).setAudioRoute(selectedDev);
                            d.dismiss();
                        }

                    });
                    adb.setNegativeButton("Cancel", null);
                    adb.setTitle("Select Audio Device");
                    adb.show();

                }
                else{
                        Toast.makeText(LoginActivity.this,"Not in Active audio conversation",Toast.LENGTH_SHORT).show();
                        return;

                }
            }
        });
        //chatBtn = findViewById(R.id.chat);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(callBtn.getText().toString().equalsIgnoreCase("call")) {
                    makecall();
                }else{
                    loginTxt.setText("Call Ended");
                    disconnect();
                }
            }
        });
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loginBtn.getText().toString().equalsIgnoreCase("login")) {
                    login();
                }
                else{
                    Log.d("LOGOUT","Logging out");
                    logout();
                }

            }
        });
        /*chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrieveConversations();
            }
        });*/

    }


    private String authenticate(String username) {
        //return username.toLowerCase().equals("jamie") ? USER_JWT : SECOND_USER_JWT;
        return USER_JWT;
    }

    private void disconnect(){

       /* directCall.hangup(new RequestHandler<Void>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                Log.d("LEAVEERROR","error leaving call "+apiError.toString());
            }

            @Override
            public void onSuccess(Void result) {
                Log.d("LEAVE","left call");
            }
        });*/
        if(directCall != null) {
            directCall.getConversation().leave(new RequestHandler<Void>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    Log.d("ERRORLEAVE", "not left call" + apiError.toString());
                }

                @Override
                public void onSuccess(Void result) {
                    Log.d("LEAVE", "left call");
                }
            });
        }
        showOrHideStats(View.INVISIBLE);
    }

    private void makecall(){
        List<String> invitees = null;

        invitees = new ArrayList<String>();
        invitees.add(calleeName);

        loginTxt.setText("Connecting...");
        conversationClient.listenToConnectionEvents(new NetworkingStateListener() {
            @Override
            public void onNetworkingState(NetworkState networkingState) {
                Log.d("NETWORKINGSTATE",networkingState.toString());
            }
        });
        conversationClient.removeConnectionListener(new NetworkingStateListener() {
            @Override
            public void onNetworkingState(NetworkState networkingState) {
                Log.d("REMOVENETWORKINGSTATE",networkingState.toString());
            }
        });
        conversationClient.call(invitees, new RequestHandler<Call>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                Log.d("INVITER","Error in Inviting "+apiError.toString());
            }

            @Override
            public void onSuccess(Call result) {
                Log.e("INVITER", "Invited successfully");
                loginTxt.setText("Calling "+calleeName);
                showOrHideStats(View.VISIBLE);
                directCall = result;

                result.getConversation().messageEvent().add(new ResultListener<Event>() {
                    @Override
                    public void onSuccess(Event result) {
                        Log.e("EVENT","Event from: "+result.getMember().getName()+" Type:"+result.getType());
                        if(result.getType().equals(EventType.MEMBER_MEDIA)){
                            MemberMedia mm = (MemberMedia)result;
                            Log.e("MEMBERMEDIA",mm.getType()+" "+mm.isAudioEnabled());
                            if(result.getMember().getName().equalsIgnoreCase(calleeName)&& mm.isAudioEnabled()==false){
                                disconnect();
                                Button callButton = (Button)findViewById(R.id.callbutton);
                                callButton.setText("Call");
                                loginTxt.setText("Call Ended");
                            }
                        }
                    }
                });
                result.getConversation().media(Conversation.MEDIA_TYPE.AUDIO).enableCallStats(new AudioCallStatsListener() {
                    @Override
                    public void onStatsAvailable(StatsReport[] report) {
                        showReports(report);
                    }
                });

                result.getConversation().memberJoinedEvent().add(new ResultListener<Member>() {
                    @Override
                    public void onSuccess(Member result) {
                        Log.e("MEMBERJOINED",result.getMemberId()+"-"+result.getName()+"-"+result.toString());
                        if(result.getName().equalsIgnoreCase(calleeName))
                            loginTxt.setText(result.getName()+" joined");
                        Button callButton = (Button)findViewById(R.id.callbutton);
                        callButton.setText("Disconnect");
                    }
                });
                result.getConversation().memberLeftEvent().add(new ResultListener<Member>() {
                    @Override
                    public void onSuccess(Member result) {
                        Log.e("MEMBERLEFT",result.getMemberId()+"-"+result.getName()+"-"+result.toString());
                        if(result.getName().equalsIgnoreCase(calleeName))
                            loginTxt.setText(result.getName()+" left the conversation");
                        disconnect();
                        Button callButton = (Button)findViewById(R.id.callbutton);
                        callButton.setText("Call");
                    }
                });

                result.event().add(new ResultListener<CallEvent>() {
                    @Override
                    public void onSuccess(CallEvent result) {
                        Log.d("CALLEVENT", result.getType().toString()+"-"+result.getState().toString());
                        if(result.getState().toString().equalsIgnoreCase("REJECTED")){
                            loginTxt.setText(calleeName+" rejected call");
                            disconnect();
                        }
                    }
                });
            }
        });

    }

    private void showReports(StatsReport [] report)
    {
        reportCount++;
        for(int i=0;i<report.length;i++){
            // Log.d("MAINCHECK","ssrc_"+report[i].id.toLowerCase().contains("ssrc_")+" and _recv"+report[i].id.toLowerCase().contains("_recv"));
            if(report[i].id.toLowerCase().contains("ssrc_") && report[i].id.toLowerCase().contains("_recv")) {
                for (int j = 0; j < report[i].values.length; j++) {
                    // Log.d("STATS","Comparing "+report[i].values[j].name+" and value:" +report[i].values[j].value);
                    if(report[i].values[j].name.equals("googCodecName")){
                        codec = report[i].values[j].value;
                    }
                    if(report[i].values[j].name.equals("packetsLost")){
                        localPktRecv = Integer.parseInt(report[i].values[j].value);
                        packetsLostRecv = localPktRecv;
                        avgPktRecv = Math.round(packetsLostRecv/reportCount);
                    }
                    if(report[i].values[j].name.equals("googJitterBufferMs")){
                        localJitter = Integer.parseInt(report[i].values[j].value);
                        jitterBufferMS += localJitter;
                        avgJitter = Math.round(jitterBufferMS/reportCount);
                        if(localJitter > jitterBufferMSMax)
                            jitterBufferMSMax = localJitter;
                        if(localJitter>0 && localJitter < jitterBufferMSMin)
                            jitterBufferMSMin = localJitter;

                    }
                }
            }

            if(report[i].id.toLowerCase().contains("ssrc_") && report[i].id.toLowerCase().contains("_send")) {
                for (int j = 0; j < report[i].values.length; j++) {
                    // Log.d("STATS","Comparing "+report[i].values[j].name+" and value:" +report[i].values[j].value);
                    if(report[i].values[j].name.equals("googRtt")){
                        localRTT = Integer.parseInt(report[i].values[j].value);
                        rtt += localRTT;
                        avgRTT = Math.round(rtt/reportCount);
                    }
                    if(report[i].values[j].name.equals("packetsLost")){
                        localPktSend = Integer.parseInt(report[i].values[j].value);
                        packetsLostSend = localPktSend;
                        avgPktSend = Math.round(packetsLostSend/reportCount);
                    }

                }
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView codecView = (TextView)findViewById(R.id.statsCodec);
                TextView rttView = (TextView)findViewById(R.id.statsRTT);
                TextView jitterView = (TextView)findViewById(R.id.statsJitter);
                TextView packetLostRecvView = (TextView)findViewById(R.id.statsPktRecv);
                TextView packetLostSentView = (TextView)findViewById(R.id.statsPktSend);

                codecView.setText(codec);
                rttView.setText("RTT:"+ String.valueOf(localRTT)+" Avg:"+String.valueOf(avgRTT));
                jitterView.setText("Jitter (ms):"+String.valueOf(localJitter)+" Avg:"+String.valueOf(avgJitter)+" Max:"+String.valueOf(jitterBufferMSMax)+" Min:"+String.valueOf(jitterBufferMSMin));
                packetLostRecvView.setText("PacketLoss (Recv):"+String.valueOf(localPktRecv)+" Avg:"+String.valueOf(avgPktRecv)+"/sec");
                packetLostSentView.setText("PakcetLoss (Send):"+String.valueOf(localPktSend)+" Avg:"+String.valueOf(avgPktSend)+"/sec");
            }
        });

    }

    private void showOrHideStats(int v){

        avgJitter=0;
        avgPktRecv=0;
        avgPktSend=0;
        avgRTT=0;
        reportCount=0;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView codecView = (TextView) findViewById(R.id.statsCodec);
                TextView rttView = (TextView) findViewById(R.id.statsRTT);
                TextView jitterView = (TextView) findViewById(R.id.statsJitter);
                TextView packetLostRecvView = (TextView) findViewById(R.id.statsPktRecv);
                TextView packetLostSentView = (TextView) findViewById(R.id.statsPktSend);

                codecView.setVisibility(v);
                rttView.setVisibility(v);
                jitterView.setVisibility(v);
                packetLostRecvView.setVisibility(v);
                packetLostSentView.setVisibility(v);
            }
        });
    }
    private void logout(){
        conversationClient.logout(new RequestHandler() {
            @Override
            public void onError(NexmoAPIError apiError) {
                Log.d("LOGOUT",apiError.toString());
            }

            @Override
            public void onSuccess(Object result) {
                Button loginBtn = (Button)findViewById(R.id.login);
                Button callBtn = (Button)findViewById(R.id.callbutton);
                loginBtn.setText("Login");
                callBtn.setVisibility(View.INVISIBLE);
            }
        });

    }

    private void login() {
        final EditText input = new EditText(LoginActivity.this);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Enter your username")
                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loginTxt.setText("Generating Auth Token");
                        myName = input.getText().toString();
                        if(myName.equalsIgnoreCase("user3")){
                            calleeName="user4";
                        }
                        else{
                            calleeName="user3";
                        }
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, url+myName,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        // Display the first 500 characters of the response string.
                                        //Toast.makeText(getApplication().getBaseContext(),response.toString(),Toast.LENGTH_LONG).show();
                                        Log.d("VOLLY",response.toString());
                                        USER_JWT=response.toString();

                                        Log.d("TOKEN",USER_JWT);
                                        loginAsUser(USER_JWT);

                                        //mTextView.setText("Response is: "+ response.substring(0,500));
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(getApplication().getBaseContext(),"failed",Toast.LENGTH_LONG).show();
                                //mTextView.setText("That didn't work!");
                            }
                        });
                        queue.add(stringRequest);
                    }
                });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        dialog.setView(input);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private void loginAsUser(String token) {
        loginTxt.setText("Logging in...");

        conversationClient.login(token, new RequestHandler<User>() {
            @Override
            public void onError(final NexmoAPIError apiError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loginTxt.setText("Login Error: " + apiError.getMessage());
                    }
                });
                logAndShow("Login Error: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(User user) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Button loginBtn = (Button)findViewById(R.id.login);
                        Button callBtn = (Button)findViewById(R.id.callbutton);
                        loginBtn.setText("Logout");
                        loginBtn.setVisibility(View.INVISIBLE);
                        callBtn.setVisibility(View.VISIBLE);
                        showLoginSuccessAndAddInvitationListener(user);
                    }
                });

                //retrieveConversations();
            }
        });
    }

    private void retrieveConversations() {
        conversationClient.getConversations(new RequestHandler<List<Conversation>>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                logAndShow("Error listing conversations: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(List<Conversation> conversationList) {
                if (conversationList.size() > 0) {
                    showConversationList(conversationList);
                } else {
                    logAndShow("You are not a member of any conversations");
                }
            }
        });
    }

    private void showConversationList(final List<Conversation> conversationList) {
        List<String> conversationNames = new ArrayList<>(conversationList.size());
        for (Conversation convo : conversationList) {
            conversationNames.add(convo.getDisplayName());
        }

        final AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Choose a conversation")
                .setItems(conversationNames.toArray(new CharSequence[conversationNames.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToConversation(conversationList.get(which));
                    }
                });
        ;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private void goToConversation(final Conversation conversation) {


        conversation.updateEvents(null, null, new RequestHandler<Conversation>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                logAndShow("Error Updating Conversation: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(final Conversation result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //User u = new User("USR-e4024ee8-2b46-4c5a-945e-e89f91864193","user4");
                        conversation.inviteWithAudio("USR-e4024ee8-2b46-4c5a-945e-e89f91864193", "user4", false, false, new RequestHandler<Member>() {
                            @Override
                            public void onError(NexmoAPIError apiError) {

                            }

                            @Override
                            public void onSuccess(Member result) {
                                Log.d("INVITECALL","Success");
                                Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
                                intent.putExtra("CONVERSATION_ID", conversation.getConversationId());
                                startActivity(intent);
                            }
                        });



                    }
                });
            }
        });
    }

    private void requestAudio() {
        if (ContextCompat.checkSelfPermission(LoginActivity.this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO)) {
                logAndShow("Need permissions granted for Audio to work");
            } else {
                ActivityCompat.requestPermissions(LoginActivity.this, new String[]{RECORD_AUDIO}, PERMISSION_REQUEST_AUDIO);
            }
        }
    }
    private void showLoginSuccessAndAddInvitationListener(final User user) {
       conversationClient.callEvent().add(new ResultListener<Call>() {
           @Override
           public void onSuccess(Call result) {
               result.getConversation().messageEvent().add(new ResultListener<Event>() {
                   @Override
                   public void onSuccess(Event result) {
                       Log.e("EVENT","Event from: "+result.getMember().getName()+" Type:"+result.getType());
                       if(result.getType().equals(EventType.MEMBER_MEDIA)){
                           MemberMedia mm = (MemberMedia)result;
                           Log.e("MEMBERMEDIA",mm.getType()+" "+mm.isAudioEnabled());
                           if(result.getMember().getName().equalsIgnoreCase(calleeName)&& mm.isAudioEnabled()==false){
                               disconnect();
                               Button callButton = (Button)findViewById(R.id.callbutton);
                               callButton.setText("Call");
                               loginTxt.setText("Call Ended");
                           }
                       }
                   }
               });
               result.getConversation().memberLeftEvent().add(new ResultListener<Member>() {
                   @Override
                   public void onSuccess(Member result) {
                       Log.e("MEMBERLEFT",result.getName()+" left");
                       if(result.getName().equalsIgnoreCase(calleeName))
                            loginTxt.setText(result.getName()+" left");
                       disconnect();

                       Button callButton = (Button)findViewById(R.id.callbutton);
                       callButton.setText("Call");
                   }
               });

               DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       v.cancel();
                       player.stop();
                       switch (which){
                           case DialogInterface.BUTTON_POSITIVE:
                               //Yes button clicked
                               Button callButton = (Button)findViewById(R.id.callbutton);
                               callButton.setText("Disconnect");
                               directCall = result;
                               showOrHideStats(View.VISIBLE);
                               result.getConversation().media(Conversation.MEDIA_TYPE.AUDIO).enableCallStats(new AudioCallStatsListener() {
                                   @Override
                                   public void onStatsAvailable(StatsReport[] report) {
                                       showReports(report);
                                   }
                               });
                               result.answer(new RequestHandler<Void>() {
                                   @Override
                                   public void onError(NexmoAPIError apiError) {
                                       Log.d("INCOMING", "ERROR Incoming call answered "+apiError.toString());
                                   }

                                   @Override
                                   public void onSuccess(Void result) {
                                       Log.d("INCOMING", "Incoming call answered");
                                       loginTxt.setText("Accpted call from "+calleeName);
                                   }
                               });
                               break;

                           case DialogInterface.BUTTON_NEGATIVE:
                               //No button clicked
                               result.reject(new RequestHandler<Void>() {
                                   @Override
                                   public void onError(NexmoAPIError apiError) {

                                   }

                                   @Override
                                   public void onSuccess(Void result) {
                                        Log.d("CALL","rejected");
                                        loginTxt.setText("Rejected call from "+calleeName);
                                   }
                               });
                               break;
                       }
                   }
               };

               AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
               builder.setMessage("Accept incoming call from "+calleeName+"?").setPositiveButton("Accept", dialogClickListener)
                       .setNegativeButton("Reject", dialogClickListener).show();
               v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                // Start without a delay
                // Vibrate for 100 milliseconds
                // Sleep for 1000 milliseconds
               long[] pattern = {0, 100, 1000};
               v.vibrate(pattern, 0);
               Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
               player = MediaPlayer.create(LoginActivity.this, notification);
               player.setLooping(true);
               player.start();

           }
       });

        conversationClient.invitedEvent().add(new ResultListener<Invitation>() {
            @Override
            public void onSuccess(final Invitation invitation) {
                logAndShow(invitation.getInvitedBy() + " invited you to their chat");
                invitation.getConversation().join(new RequestHandler<Member>() {
                    @Override
                    public void onError(NexmoAPIError apiError) {
                        logAndShow("Error joining conversation: " + apiError.getMessage());
                    }

                    @Override
                    public void onSuccess(Member member) {
                        goToConversation(invitation.getConversation());
                    }
                });
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loginTxt.setText("Logged in as " + user.getName() );
            }
        });
    }

    private void logAndShow(final String message) {
        Log.d(TAG, message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }


}
