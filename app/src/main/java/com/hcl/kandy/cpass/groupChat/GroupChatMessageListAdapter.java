package com.hcl.kandy.cpass.groupChat;

import android.content.Context;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hcl.kandy.cpass.R;
import com.rbbn.cpaas.mobile.messaging.api.Message;

import java.util.List;

public class GroupChatMessageListAdapter extends RecyclerView.Adapter<GroupChatMessageListAdapter.MyViewHolder> {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    protected Context context;
    private List<Message> messageList;

    public GroupChatMessageListAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    public void setMessageList(List<Message> list) {
        messageList = list;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_chat_message, parent, false);

        return new GroupChatMessageHolder(view);
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Message message = messageList.get(position);
        ((GroupChatMessageHolder) holder).bind(message);
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                return false;
            }
        });
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout viewBackground, viewForeground;

        public MyViewHolder(View view) {
            super(view);
            viewBackground = view.findViewById(R.id.view_background);
            viewForeground = view.findViewById(R.id.view_foreground);
        }

    }

    private class GroupChatMessageHolder extends MyViewHolder {
        ImageView attachedImageView;
        TextView messageSender;
        TextView messageText;
        TextView timeText;

        GroupChatMessageHolder(View itemView) {
            super(itemView);

            attachedImageView = itemView.findViewById(R.id.attached_image);
            messageSender = itemView.findViewById(R.id.group_message_sender);
            messageText = itemView.findViewById(R.id.group_message_body);
            timeText = itemView.findViewById(R.id.group_message_time);
        }

        void bind(Message message) {
            String[] parts = message.getSenderAddress().split("@");
            messageSender.setText(parts[0]);

            messageText.setText(message.getMessage());


            String t_str;
            long timestamp = message.getTimestamp();
            if (timestamp == -1) {
                t_str = "Fail!";
            } else if (timestamp == 0) {
                t_str = "...";
            } else {
                t_str = GroupChatFragment.formatDate(timestamp);
            }
            timeText.setText(t_str);
        }
    }
}