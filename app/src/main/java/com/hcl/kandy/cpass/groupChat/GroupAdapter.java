package com.hcl.kandy.cpass.groupChat;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hcl.kandy.cpass.R;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatGroup;

import java.util.List;

public class GroupAdapter extends ArrayAdapter<ChatGroup> {


    private Activity context;
    private List<ChatGroup> groupList;

    public GroupAdapter(Activity context, List<ChatGroup> chatGroups) {
        super(context, 0, chatGroups);

        this.context = context;
        this.groupList = chatGroups;
    }

    public void setGroupList(List<ChatGroup> list) {
        groupList.clear();
        groupList.addAll(list);
        context.runOnUiThread(() -> this.notifyDataSetChanged());
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.group_chat_item, parent, false);
        }

        ImageView imageView = listItemView.findViewById(R.id.group_chat_item_image_view);
        TextView nameTextView = listItemView.findViewById(R.id.group_chat_text_view);
        TextView messageTextView = listItemView.findViewById(R.id.group_chat_message_text_view);

        ChatGroup group = groupList.get(position);
        String name = group.getName();
        String subject = group.getSubject();

        Log.i("GroupAdapter", "Adding group for " + name);

        nameTextView.setText(name);

        if (group.isConnected()) {
            messageTextView.setText(subject);
            messageTextView.setTypeface(null, Typeface.NORMAL);
            imageView.setImageResource(R.drawable.ic_group_black_24px);
        } else {
            messageTextView.setText("Invited to group");
            messageTextView.setTypeface(null, Typeface.ITALIC);
            imageView.setImageResource(R.drawable.ic_group_outline_black_24px);
        }

        return listItemView;
    }
}
