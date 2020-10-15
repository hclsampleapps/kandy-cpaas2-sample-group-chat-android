package com.hcl.kandy.cpass.groupChat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.hcl.kandy.cpass.R;
import com.rbbn.cpaas.mobile.CPaaS;
import com.rbbn.cpaas.mobile.messaging.api.Conversation;
import com.rbbn.cpaas.mobile.messaging.api.FetchConversationCallback;
import com.rbbn.cpaas.mobile.messaging.api.FetchMessagesCallback;
import com.rbbn.cpaas.mobile.messaging.api.InboundMessage;
import com.rbbn.cpaas.mobile.messaging.api.Message;
import com.rbbn.cpaas.mobile.messaging.api.MessageDeliveryStatus;
import com.rbbn.cpaas.mobile.messaging.api.MessageState;
import com.rbbn.cpaas.mobile.messaging.api.MessagingCallback;
import com.rbbn.cpaas.mobile.messaging.api.OutboundMessage;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatConversation;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatGroupParticipant;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatListener;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatService;
import com.rbbn.cpaas.mobile.utilities.exception.MobileError;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GroupChatDetailActivity extends AppCompatActivity  {

    private static final String TAG = "ChatDetailActivity";
    protected ChatConversation chatConversation;
    String groupId;
    String name;
    String subject;
    int max;

    LinearLayout groupLayout;
    EditText messageEditText;

    List<Message> messageList = new ArrayList<>();

    private RecyclerView messageRecycler;
    private GroupChatMessageListAdapter messageAdapter;

    private boolean isComposing = false;
    private Timer mComposingTimer;
    protected ChatService chatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_detail);

        groupLayout = findViewById(R.id.layout_group);
        TextView groupSubjectTextView = findViewById(R.id.group_subject_textview);
        messageEditText = findViewById(R.id.messageEditText);

        messageRecycler = findViewById(R.id.group_message_reyclerview);
        // set reverseLayout to true so the list is built from the bottom up
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        messageRecycler.setLayoutManager(manager);
        messageAdapter = new GroupChatMessageListAdapter(this, new ArrayList<>());
        messageRecycler.setAdapter(messageAdapter);

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");
        name = intent.getStringExtra("name");
        subject = intent.getStringExtra("subject");

        if (chatService == null)
            initChatService(this);

        chatService.fetchGroupChatSession(groupId, new FetchConversationCallback() {
            @Override
            public void onSuccess(Conversation conversation) {
                chatConversation = (ChatConversation) conversation;

                max = intent.getIntExtra("max", 50);
                if (max > 50)
                    max = 50;

                chatConversation.fetchGroupChatMessages(max, new FetchMessagesCallback() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        messageList = messages;

//                        Collections.reverse(messageList);

                        messageAdapter.setMessageList(messageList);
                        runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onFail(MobileError error) {

                    }
                });
            }

            @Override
            public void onFail(MobileError error) {
                chatConversation = (ChatConversation) chatService.createConversation(groupId);
                String sender = intent.getStringExtra("sender");
                String destination = intent.getStringExtra("destination");
                String messageId = intent.getStringExtra("messageId");
                String message = intent.getStringExtra("message");
                long timestamp = intent.getLongExtra("timestamp", 0L);

                if (sender.length() > 0 && messageId.length() > 0) {
                    InboundMessage inboundMessage = new com.rbbn.cpaas.mobile.messaging.InboundMessage(sender, destination, messageId, message, timestamp);
                    messageList.add(0, inboundMessage);
                }
                messageAdapter.setMessageList(messageList);
                runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
            }
        });

        setTitle(name);
        groupSubjectTextView.setText(subject);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initChatService(@NonNull Context context) {
        App applicationContext = (App) context.getApplicationContext();
        CPaaS cpass = applicationContext.getCpass();
        chatService = cpass.getChatService();
        chatService.setChatListener(new ChatListener() {
            @Override
            public void inboundChatMessageReceived(InboundMessage message) {
                String messageGroupId = message.getDestinationAddress();
                Log.d(TAG, "InboundMessage is GroupChat - id: " + message.getMessageId() + ", sender: " + message.getSenderAddress() + ", groupId: " + messageGroupId);

                if (messageGroupId.equals(groupId)) {
                    // add the inbound message to the message list if it is for this group
                    Log.d(TAG, "GroupChat is for this Group. Will refresh screen - id: " + message.getMessageId());
                    messageList.add(0, message);
                    runOnUiThread(() -> messageAdapter.notifyDataSetChanged());


                    // notify the backend that the message was displayed
                    chatConversation.sendGroupChatDisplayed(message.getMessageId(), new MessagingCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFail(MobileError error) {
                        }
                    });
                } else {
                    Log.d(TAG, "GroupChat is not for this Group. - screen groupId: " + groupId);
                }
            }

            @Override
            public void chatDeliveryStatusChanged(String s, MessageDeliveryStatus messageDeliveryStatus, String messageID) {
                for (Message msg : messageList) {
                    String id = msg.getMessageId();
                    if (messageID != null && messageID.equals(id)) {
                        msg.setStatus(messageDeliveryStatus);
                    }
                }
            }

            @Override
            public void chatParticipantStatusChanged(ChatGroupParticipant chatGroupParticipant, String s) {

            }

            @Override
            public void outboundChatMessageSent(OutboundMessage message) {
                String messageID = message.getMessageId();
                for (Message msg : messageList) {
                    String id = msg.getMessageId();
                    if (messageID != null && messageID.equals(id)) {
                        // If this message is already in the local message list, then disregard the notification
                        return;
                    }
                }

                messageList.add(0, message);
                runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
            }

            @Override
            public void isComposingReceived(String s, MessageState messageState, long l) {

            }

            @Override
            public void groupChatSessionInvitation(List<ChatGroupParticipant> list, String s, String s1) {

            }

            @Override
            public void groupChatEventNotification(String s, String s1, String s2) {

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void isComposingActive() {
        mComposingTimer = new Timer();
        mComposingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    isComposing = false;
                    chatConversation.sendGroupChatComposing(false, new MessagingCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFail(MobileError error) {
                        }
                    });
                    if (mComposingTimer != null)
                        mComposingTimer.cancel();
                });
            }
        }, 5000);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isComposing) {
            isComposing = true;
            chatConversation.sendGroupChatComposing(true, new MessagingCallback() {
                @Override
                public void onSuccess() {
                    isComposingActive();
                }

                @Override
                public void onFail(MobileError error) {
                }
            });
        }
        return super.onKeyUp(keyCode, event);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }


    public void sendMessage(View view) {
        // do nothing if no message text is present
        String txt = messageEditText.getText().toString();
        if (txt.length() == 0) {

            Log.w(TAG, "Message text not specified");
            return;
        }

        isComposing = false;
        if (mComposingTimer != null)
            mComposingTimer.cancel();

        // create a new message and send it to the adapter for display
        OutboundMessage message = chatService.createMessage(txt);

        // clear out the message EditText
        messageEditText.setText("");
        hideKeyboard();

        // Add the new message to the front of the list
        messageList.add(0, message);

        // send the message to the backend
        chatConversation.sendGroupChatMessage(message, new MessagingCallback() {
            @Override
            public void onSuccess() {
                // update the UI when confirmation has been received
                runOnUiThread(() -> {
                    messageAdapter.notifyDataSetChanged();
                });

            }

            @Override
            public void onFail(MobileError error) {
                // update the UI when confirmation has been received
                runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
            }
        });

        // update the RecyclerView with the newly-sent message
        runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
    }

    public void handleIsComposingReceived(String participant, String state, long lastActive) {
        String str = participant + (state.equals("active") ? " is typing" : " has stopped typing");
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public void handleChatEventNotification(String groupId, String type, String description) {
        if (groupId.equals(this.groupId)) {
            Toast.makeText(this, description, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_chat_detail_menu, menu);
        return true;
    }


}
