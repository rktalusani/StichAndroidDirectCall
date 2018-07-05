package com.nexmo.enableaudio;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.SeenReceipt;
import com.nexmo.sdk.conversation.client.audio.AppRTCAudioManager;
import com.nexmo.sdk.conversation.client.audio.AudioCallEventListener;
import com.nexmo.sdk.conversation.client.audio.AudioCallStatsListener;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.ResultListener;
import com.nexmo.sdk.conversation.client.event.container.Invitation;
import com.nexmo.sdk.conversation.client.event.container.Receipt;
import com.nexmo.sdk.conversation.core.SubscriptionList;

import org.webrtc.StatsReport;

import static android.Manifest.permission.RECORD_AUDIO;

public class ChatActivity extends AppCompatActivity {
    private String TAG = ChatActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_AUDIO = 0;
    private boolean AUDIO_ENABLED = false;

    private EditText chatBox;
    private ImageButton sendBtn;
    private TextView typingNotificationTxt;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;

    private ConversationClient conversationClient;
    private Conversation conversation;
    private SubscriptionList subscriptions = new SubscriptionList();
    String codec = null;
    String packetsLostRecv = null;
    String packetsLostSend = null;
    String rtt = null;
    String jitterBufferMS=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationClient = ((ConversationClientApplication) getApplication()).getConversationClient();
        Intent intent = getIntent();
        String conversationId = intent.getStringExtra("CONVERSATION_ID");
        conversation = conversationClient.getConversation(conversationId);

        recyclerView = findViewById(R.id.recycler);
        chatAdapter = new ChatAdapter(conversation);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

        chatBox = findViewById(R.id.chat_box);
        sendBtn = findViewById(R.id.send_btn);
        typingNotificationTxt = findViewById(R.id.typing_notification);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        subscriptions.unsubscribeAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.audio:
                requestAudio();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void attachListeners() {
        conversation.messageEvent().add(new ResultListener<Event>() {
            @Override
            public void onSuccess(Event result) {
                chatAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(chatAdapter.getItemCount());
            }
        }).addTo(subscriptions);

        chatBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //intentionally left blank
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    sendTypeIndicator(Member.TYPING_INDICATOR.ON);
                } else {
                    sendTypeIndicator(Member.TYPING_INDICATOR.OFF);
                }
            }
        });

        conversation.typingEvent().add(new ResultListener<Member>() {
            @Override
            public void onSuccess(final Member member) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String typingMsg = member.getTypingIndicator().equals(Member.TYPING_INDICATOR.ON) ? member.getName() + " is typing" : null;
                        typingNotificationTxt.setText(typingMsg);
                    }
                });
            }
        }).addTo(subscriptions);

        conversation.seenEvent().add(new ResultListener<Receipt<SeenReceipt>>() {
            @Override
            public void onSuccess(Receipt<SeenReceipt> result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).addTo(subscriptions);
    }

    private void sendTypeIndicator(Member.TYPING_INDICATOR typingIndicator) {
        switch (typingIndicator){
            case ON: {
                conversation.startTyping(new RequestHandler<Member.TYPING_INDICATOR>() {
                    @Override
                    public void onSuccess(Member.TYPING_INDICATOR typingIndicator) {
                        //intentionally left blank
                    }

                    @Override
                    public void onError(NexmoAPIError apiError) {
                        logAndShow("Error start typing: " + apiError.getMessage());
                    }
                });
                break;
            }
            case OFF: {
                conversation.stopTyping(new RequestHandler<Member.TYPING_INDICATOR>() {
                    @Override
                    public void onSuccess(Member.TYPING_INDICATOR typingIndicator) {
                        //intentionally left blank
                    }

                    @Override
                    public void onError(NexmoAPIError apiError) {
                        logAndShow("Error stop typing: " + apiError.getMessage());
                    }
                });
                break;
            }
        }
    }

    private void sendMessage() {
        conversation.sendText(chatBox.getText().toString(), new RequestHandler<Event>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                logAndShow("Error sending message: " + apiError.getMessage());
            }

            @Override
            public void onSuccess(Event result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatBox.setText(null);
                    }
                });
            }
        });
    }

    private void requestAudio() {
        if (ContextCompat.checkSelfPermission(ChatActivity.this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            toggleAudio();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO)) {
                logAndShow("Need permissions granted for Audio to work");
            } else {
                ActivityCompat.requestPermissions(ChatActivity.this, new String[]{RECORD_AUDIO}, PERMISSION_REQUEST_AUDIO);
            }
        }
    }

    private void toggleAudio() {

        if(AUDIO_ENABLED) {
            conversation.media(Conversation.MEDIA_TYPE.AUDIO).disable(new RequestHandler<Void>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    logAndShow(apiError.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    AUDIO_ENABLED = false;
                    logAndShow("Audio is disabled");
                }
            });
        } else {
            conversation.media(Conversation.MEDIA_TYPE.AUDIO).enable(new AudioCallEventListener() {
                @Override
                public void onRinging() {
                    logAndShow("Ringing");
                }

                @Override
                public void onCallConnected() {
                    logAndShow("Connected");
                    AUDIO_ENABLED = true;
                }

                @Override
                public void onCallEnded() {
                    logAndShow("Call Ended");
                    AUDIO_ENABLED = false;
                }

                @Override
                public void onGeneralCallError(NexmoAPIError apiError) {
                    logAndShow(apiError.getMessage());
                    AUDIO_ENABLED = false;
                }

                @Override
                public void onAudioRouteChange(AppRTCAudioManager.AudioDevice device) {
                    logAndShow("Audio Route changed");
                }
            });

            conversation.media(Conversation.MEDIA_TYPE.AUDIO).enableCallStats(new AudioCallStatsListener() {
                @Override
                public void onStatsAvailable(StatsReport[] report) {

                    for(int i=0;i<report.length;i++){
                       // Log.d("MAINCHECK","ssrc_"+report[i].id.toLowerCase().contains("ssrc_")+" and _recv"+report[i].id.toLowerCase().contains("_recv"));
                        if(report[i].id.toLowerCase().contains("ssrc_") && report[i].id.toLowerCase().contains("_recv")) {
                            for (int j = 0; j < report[i].values.length; j++) {
                               // Log.d("STATS","Comparing "+report[i].values[j].name+" and value:" +report[i].values[j].value);
                                if(report[i].values[j].name.equals("googCodecName")){
                                    codec = report[i].values[j].value;
                                }
                                if(report[i].values[j].name.equals("packetsLost")){
                                    packetsLostRecv = report[i].values[j].value;
                                }
                                if(report[i].values[j].name.equals("googJitterBufferMs")){
                                    jitterBufferMS = report[i].values[j].value;
                                }
                            }
                        }

                        if(report[i].id.toLowerCase().contains("ssrc_") && report[i].id.toLowerCase().contains("_send")) {
                            for (int j = 0; j < report[i].values.length; j++) {
                                // Log.d("STATS","Comparing "+report[i].values[j].name+" and value:" +report[i].values[j].value);
                                if(report[i].values[j].name.equals("googRtt")){
                                    rtt = report[i].values[j].value;
                                }
                                if(report[i].values[j].name.equals("packetsLost")){
                                    packetsLostSend = report[i].values[j].value;
                                }

                            }
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView codecView = (TextView)findViewById(R.id.codec);
                            TextView rttView = (TextView)findViewById(R.id.rtt);
                            TextView jitterView = (TextView)findViewById(R.id.jitterBufferMS);
                            TextView packetLostRecvView = (TextView)findViewById(R.id.packetsLostRecv);
                            TextView packetLostSentView = (TextView)findViewById(R.id.packetsLostSent);

                            codecView.setText("Codec: "+codec);
                            rttView.setText("RTT: "+rtt);
                            jitterView.setText("Jitter Buffer (ms): "+jitterBufferMS);
                            packetLostRecvView.setText("PacketsLost (Recv): "+packetsLostRecv);
                            packetLostSentView.setText("PacketsLost (Send): "+packetsLostSend);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleAudio();
                    break;
                } else {
                    logAndShow("Enable audio permissions to continue");
                    break;
                }
            }
            default: {
                logAndShow("Issue with onRequestPermissionsResult");
                break;
            }
        }
    }

    private void logAndShow(final String message) {
        Log.d(TAG, message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
