package com.hcl.kandy.cpass.groupChat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.hcl.kandy.cpass.R;
import com.rbbn.cpaas.mobile.CPaaS;
import com.rbbn.cpaas.mobile.messaging.api.MessagingCallback;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatGroup;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatGroupParticipant;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatService;
import com.rbbn.cpaas.mobile.messaging.chat.api.FetchGroupsCallback;
import com.rbbn.cpaas.mobile.utilities.exception.MobileError;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class GroupChatFragment extends Fragment implements View.OnClickListener {

    protected ChatService chatService;

    protected ListView listView;

    protected GroupAdapter adapter;

    protected List<ChatGroup> groupList = new ArrayList<>();

    public GroupChatFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        if (context != null)
            initChatService(context);
    }

    private void initChatService(@NonNull Context context) {
        App applicationContext = (App) context.getApplicationContext();
        CPaaS cpass = applicationContext.getCpass();
        chatService = cpass.getChatService();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groupchat, container, false);

        FloatingActionButton newGroupButton = view.findViewById(R.id.newGroupButton);
        newGroupButton.setOnClickListener(this);

        listView = view.findViewById(R.id.groupList);

        registerForContextMenu(listView);

        listView.setOnItemClickListener((adapterView, v, i, l) -> {
            ChatGroup cg = adapter.getItem(i);

            if (cg != null && cg.isConnected()) {
                String name = cg.getName();
                Log.i("GroupChatFragment", "Opening group chat for " + name);

                Intent intent = new Intent(getActivity(), GroupChatDetailActivity.class);
                intent.putExtra("groupId", cg.getId());
                intent.putExtra("name", name);
                intent.putExtra("subject", cg.getSubject());
                startActivity(intent);
            } else {
                // don't open the group chat if you're in the "invited" state
            }
        });

        adapter = new GroupAdapter(getActivity(), groupList);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        // This gets called when entering the fragment, or when coming back to the fragment from another activity
        super.onResume();

        chatService = getService();

        if (chatService != null) {
            fetchGroups();
        } else {
            Log.e("ChatService", "onResume: ChatService is null");
        }
    }

    public ChatService getService() {
        App applicationContext = (App) getContext().getApplicationContext();
        return applicationContext.getCpass().getChatService();
    }

    @Override
    public void onClick(View view) {
        if (chatService != null) {
            switch (view.getId()) {
                case R.id.newGroupButton:
                    newGroup();
                    break;
            }
        } else {
            Toast.makeText(getContext(), "Account cannot be used with Chat", Toast.LENGTH_SHORT).show();
        }
    }

    protected void fetchGroups() {
        chatService.fetchAllGroups(new FetchGroupsCallback() {
            @Override
            public void onSuccess(List<ChatGroup> groups) {
                // Sort the conversations by last message time
                groupList = groups;
                if (adapter != null) {
                    adapter.setGroupList(groupList);
                }
            }

            @Override
            public void onFail(MobileError error) {

            }
        });
    }

    private void newGroup() {
        Intent intent = new Intent(getActivity(), GroupInfoActivity.class);
        startActivity(intent);
    }

    protected static String formatDate(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat df = new SimpleDateFormat("M/d/yy' 'HH:mm:ss", Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return df.format(date);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ChatGroup chatGroup = adapter.getItem(adapterContextMenuInfo.position);
        if (chatGroup != null) {
            if (chatGroup.isConnected()) {
                // connected
                if (chatGroup.isAdmin()) {
                    inflater.inflate(R.menu.chat_group_admin_menu, menu);
                } else {
                    inflater.inflate(R.menu.chat_group_member_menu, menu);
                }
            } else {
                // invited
                inflater.inflate(R.menu.chat_group_invited_menu, menu);
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        //find out which menu item was pressed
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ChatGroup chatGroup = adapter.getItem(info.position);

        switch (item.getItemId()) {
            case R.id.accept_invitation_option:
                acceptInvitation(chatGroup);
                return true;

            case R.id.decline_invitation_option:
                declineInvitation(chatGroup);
                return true;

            case R.id.delete_group_option:
                deleteChatGroup(chatGroup);
                return true;

            case R.id.leave_group_option:
                leaveChatGroup(chatGroup);
                return true;

            case R.id.view_members_option:
                viewChatGroupParticipants(chatGroup);
                return true;

            default:
                return false;
        }
    }

    private void acceptInvitation(ChatGroup chatGroup) {
        String groupId = chatGroup.getId();
        chatService.acceptGroupInvitation(groupId, new MessagingCallback() {
            @Override
            public void onSuccess() {
                fetchGroups();
            }

            @Override
            public void onFail(MobileError error) {
            }
        });
    }

    private void declineInvitation(ChatGroup chatGroup) {
        String groupId = chatGroup.getId();
        chatService.declineGroupInvitation(groupId, new MessagingCallback() {
            @Override
            public void onSuccess() {
                groupList.remove(chatGroup);
                adapter.setGroupList(groupList);
            }

            @Override
            public void onFail(MobileError error) {
            }
        });
    }

    private void deleteChatGroup(ChatGroup chatGroup) {
        String groupId = chatGroup.getId();
        chatService.deleteGroup(groupId, new MessagingCallback() {
            @Override
            public void onSuccess() {
                groupList.remove(chatGroup);
                adapter.setGroupList(groupList);
            }

            @Override
            public void onFail(MobileError error) {
            }
        });
    }

    private void leaveChatGroup(ChatGroup chatGroup) {
        String groupId = chatGroup.getId();
        chatService.leaveGroup(groupId, new MessagingCallback() {
            @Override
            public void onSuccess() {
                groupList.remove(chatGroup);
                adapter.setGroupList(groupList);
            }

            @Override
            public void onFail(MobileError error) {
            }
        });
    }

    private void viewChatGroupParticipants(ChatGroup chatGroup) {
        List<ChatGroupParticipant> chatGroupParticipants = chatGroup.getChatGroupParticipants();
        Context context = getContext();

        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        // build a list view of the group members
        final ListView listView = new ListView(context);
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = params.leftMargin;
        container.addView(listView);

        // convert the ChatGroupParticipant list to a list of Strings
        final List<String> participants = new ArrayList<>();
        for (ChatGroupParticipant c : chatGroupParticipants) {
            participants.add(c.getAddress());
        }

        // populate the list view
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, participants);
        listView.setAdapter(adapter);

        alert.setTitle("Group Members");
        alert.setView(container);

        alert.setPositiveButton("Close", (dialog, whichButton) -> {
        });

        alert.show();
    }
}
