package com.example.owner.shakechattry;

import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersListAdapter extends RecyclerView.Adapter<UsersListAdapter.ViewHolder>{

    private ArrayList<String> mImages = new ArrayList<>();
    private ArrayList<String> mNames = new ArrayList<>();
    private ArrayList<String> mDescriptions = new ArrayList<>();
    private ArrayList<String> mUids = new ArrayList<>();
    private Context mContext;

    public UsersListAdapter(ArrayList<String> mImage, ArrayList<String> mName
            , ArrayList<String> mDescription, ArrayList<String> mUid, Context mContext) {

        this.mImages = mImage;
        this.mNames = mName;
        this.mDescriptions = mDescription;
        this.mUids = mUid;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_list_layout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final String name = mNames.get(position);
        final String description = mDescriptions.get(position);
        final String image_url = mImages.get(position);
        final String uid = mUids.get(position);

        Picasso.get().load(image_url).into(holder.image);
        holder.name.setText(name);
        holder.description.setText(description);

        DatabaseReference online_ref = FirebaseDatabase.getInstance().getReference().child("Users/" + uid + "/online");
        online_ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue().equals(true)){
                    holder.online.setVisibility(View.VISIBLE);
                }else{
                    holder.online.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        String curr_uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference seen_ref = FirebaseDatabase.getInstance().getReference().child("Chat Rooms/" + curr_uid + "/" + uid);
        seen_ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean unread_message = false;
                for(DataSnapshot message : dataSnapshot.getChildren()){
                    if(message.child("seen").getValue().equals("false")){
                        unread_message = true;
                        break;
                    }
                }
                if(unread_message)
                    holder.unread.setVisibility(View.VISIBLE);
                else
                    holder.unread.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        holder.parent_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileActivity = new Intent(mContext, ProfileActivity.class);
                profileActivity.putExtra("extra_uid", uid);
                mContext.startActivity(profileActivity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mNames.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        CircleImageView image;
        TextView name, description;
        ImageView online, unread;
        RelativeLayout parent_layout;

        public ViewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.friends_single_image);
            name = itemView.findViewById(R.id.friends_single_name);
            description = itemView.findViewById(R.id.friends_single_description);
            online = itemView.findViewById(R.id.friends_single_online_icon);
            unread = itemView.findViewById(R.id.friends_single_unread_icon);
            parent_layout = itemView.findViewById(R.id.friends_parent_layout);
        }
    }
}
