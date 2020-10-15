package com.hcl.kandy.cpass.groupChat;

import android.app.Activity;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hcl.kandy.cpass.R;
import com.rbbn.cpaas.mobile.messaging.chat.api.ChatGroupParticipant;

import java.util.List;


public class GroupMembersAdapter extends ArrayAdapter<ChatGroupParticipant> {
    private Activity context;
    private List<ChatGroupParticipant> memberList;

    public GroupMembersAdapter(Activity context, List<ChatGroupParticipant> groupMembers) {
        super(context, 0, groupMembers);

        this.context = context;
        this.memberList = groupMembers;
    }

    public void setGroupMembersList(List<ChatGroupParticipant> list) {
        memberList.clear();
        memberList.addAll(list);
        context.runOnUiThread(() -> this.notifyDataSetChanged());
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.group_member_item, parent, false);
        }

        ImageView imageView = listItemView.findViewById(R.id.group_member_image_view);
        TextView groupMemberTextView = listItemView.findViewById(R.id.group_member_text_view);
        TextView groupMemberMessageTextView = listItemView.findViewById(R.id.group_member_message_text_view);

        ChatGroupParticipant participant = memberList.get(position);
        String address = participant.getAddress();
        String status = participant.getStatus();

        Log.i("GroupMembersAdapter", "Adding member for " + address);

        if (participant.isAdmin()) {
            imageView.setImageResource(R.drawable.ic_person_admin_24dp);
            groupMemberMessageTextView.setText("Admin");
        } else if (status != null && status.equals("Connected")) {
            imageView.setImageResource(R.drawable.ic_person_black_24dp);
            groupMemberMessageTextView.setText("Member");
        } else {
            imageView.setImageResource(R.drawable.ic_person_outline_black_24px);
            groupMemberMessageTextView.setTypeface(null, Typeface.ITALIC);
            groupMemberMessageTextView.setText("Invited");
        }

        // Set the participant
        groupMemberTextView.setText(address);

        return listItemView;
    }
}
