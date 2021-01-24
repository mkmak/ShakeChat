package com.example.owner.shakechattry;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder>{

    private ArrayList<String> mImages = new ArrayList<>();
    private ArrayList<String> mTexts = new ArrayList<>();
    private ArrayList<String> mSenderIds = new ArrayList<>();
    private ArrayList<String> mTimes = new ArrayList<>();
    private ArrayList<String> mMessageIds = new ArrayList<>();
    private ArrayList<String> mChatUsers = new ArrayList<>();
    private ArrayList<String> mMessageTypes = new ArrayList<>();
    private ArrayList<String> mPhotoUrls = new ArrayList<>();
    private Context mContext;

    public MessagesAdapter(ArrayList<String> mImage, ArrayList<String>mPhotoUrl, ArrayList<String> mText,
                           ArrayList<String> mSenderId, ArrayList<String> mTime, ArrayList<String> mMessageId,
                           ArrayList<String> mChatUser, ArrayList<String> mMessageType, Context mContext) {

        this.mImages = mImage;
        this.mTexts = mText;
        this.mContext = mContext;
        this.mSenderIds = mSenderId;
        this.mTimes = mTime;
        this.mMessageIds = mMessageId;
        this.mChatUsers = mChatUser;
        this.mMessageTypes = mMessageType;
        this.mPhotoUrls = mPhotoUrl;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final String text = mTexts.get(position);
        final String image_url = mImages.get(position);
        final String photo_url = mPhotoUrls.get(position);
        final String time = mTimes.get(position);
        String message_type = mMessageTypes.get(position);
        String curr_uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(mSenderIds.get(position).equals(curr_uid)) {

            holder.image.setVisibility(View.INVISIBLE);
            holder.text.setVisibility(View.INVISIBLE);
            holder.time.setVisibility(View.INVISIBLE);

            holder.curr_image.setVisibility(View.VISIBLE);
            holder.curr_time.setVisibility(View.VISIBLE);
            Picasso.get().load(image_url).into(holder.curr_image);
            holder.curr_time.setText(time);

            if(message_type.equals("text")) {
                holder.curr_text.setVisibility(View.VISIBLE);
                holder.curr_text.setText(text);
                holder.curr_text.setBackgroundColor(0xff5D6D7E);
            }else if(message_type.equals("image")){
                holder.curr_text.setVisibility(View.INVISIBLE);
                holder.curr_photo.setVisibility(View.VISIBLE);
                Picasso.get().load(photo_url).into(holder.curr_photo);
            }

            holder.parent_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    holder.cancel.setVisibility(View.VISIBLE);
                    holder.recall.setVisibility(View.VISIBLE);

                    holder.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.cancel.setVisibility(View.INVISIBLE);
                            holder.recall.setVisibility(View.INVISIBLE);
                        }
                    });

                    holder.recall.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DatabaseReference chat_rooms_ref = FirebaseDatabase.getInstance().getReference().child("Chat Rooms");
                            chat_rooms_ref.child(mChatUsers.get(0)).child(mChatUsers.get(1)).child(mMessageIds.get(position)).removeValue();
                            chat_rooms_ref.child(mChatUsers.get(1)).child(mChatUsers.get(0)).child(mMessageIds.get(position)).removeValue();
                        }
                    });
                }
            });
        }
        else {

            holder.curr_time.setVisibility(View.INVISIBLE);
            holder.curr_text.setVisibility(View.INVISIBLE);
            holder.curr_image.setVisibility(View.INVISIBLE);

            holder.image.setVisibility(View.VISIBLE);
            holder.time.setVisibility(View.VISIBLE);
            holder.time.setText(time);
            Picasso.get().load(image_url).into(holder.image);

            if(message_type.equals("text")) {
                holder.text.setVisibility(View.VISIBLE);
                holder.text.setText(text);
                holder.text.setBackgroundColor(0xff2471A3);
            }
            else if(message_type.equals("image")){
                holder.text.setVisibility(View.INVISIBLE);
                holder.photo.setVisibility(View.VISIBLE);
                Picasso.get().load(photo_url).into(holder.photo);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mTexts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        CircleImageView image, curr_image;
        TextView text, time, curr_text, curr_time;
        RelativeLayout parent_layout;
        Button recall, cancel;
        ImageView photo, curr_photo;

        public ViewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.message_single_image);
            text = itemView.findViewById(R.id.message_single_text);
            parent_layout = itemView.findViewById(R.id.message_single_layout);
            time = itemView.findViewById(R.id.message_single_time);
            curr_image = itemView.findViewById(R.id.message_curr_single_image);
            curr_text = itemView.findViewById(R.id.message_curr_single_text);
            curr_time = itemView.findViewById(R.id.message_curr_single_time);
            recall = itemView.findViewById(R.id.message_single_recall_btn);
            cancel = itemView.findViewById(R.id.message_single_cancel_btn);
            photo = itemView.findViewById(R.id.message_single_photo);
            curr_photo = itemView.findViewById(R.id.message_curr_single_photo);

        }
    }
}
