package com.example.owner.shakechattry;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileActivity extends AppCompatActivity {

    private TextView mDisplayName, mDescription;
    private Button mAcceptBtn, mDeclineBtn;
    private CircleImageView mImage;
    private ImageButton mRequestBtn;
    private ImageButton mChatBtn;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private DatabaseReference mRequestsReference = FirebaseDatabase.getInstance().getReference().child("Requests");
    private DatabaseReference mFriendsReference = FirebaseDatabase.getInstance().getReference().child("Friends");
    private DatabaseReference mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");

    private String mCurrentState = "not_friends";
    private String mNotificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();

        mRequestBtn = findViewById(R.id.profile_new_req_btn);
        mChatBtn = findViewById(R.id.profile_new_chat_btn);
        mNotificationReference.child(current_user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        if(ds.child("type").getValue().toString().equals("request"))
                            mRequestBtn.setVisibility(View.VISIBLE);
                        if(ds.child("type").getValue().toString().equals("chat"))
                            mChatBtn.setVisibility(View.VISIBLE);
                    }

                    mRequestBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent requestActivity = new Intent(ProfileActivity.this, RequestActivity.class);
                            startActivity(requestActivity);
                        }
                    });

                    mChatBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent friendsIntent = new Intent(ProfileActivity.this, FriendsActivity.class);
                            startActivity(friendsIntent);
                        }
                    });

                }else{
                    mRequestBtn.setVisibility(View.INVISIBLE);
                    mChatBtn.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Toolbar toolbar = findViewById(R.id.profile_app_bar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final String user_id = getIntent().getStringExtra("extra_uid");
        final String current_uid = current_user.getUid();

        mDisplayName = findViewById(R.id.profile_display_name);
        mDescription = findViewById(R.id.profile_description);
        mImage = findViewById(R.id.profile_image);

        mUserReference.child(user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                String description = Objects.requireNonNull(dataSnapshot.child("description").getValue()).toString();
                String image_url = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();

                mDisplayName.setText(name);
                mDescription.setText(description);
                Picasso.get().load(image_url).into(mImage);

                if(!mCurrentState.equals("request_received"))
                    mDeclineBtn.setVisibility(View.INVISIBLE);

                mRequestsReference.child(current_uid).addValueEventListener(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(user_id)){
                            String req_type = Objects.requireNonNull(dataSnapshot.child(user_id).child("request_type").getValue()).toString();

                            if(req_type.equals("received")){

                                mCurrentState = "request_received";
                                mAcceptBtn.setText("Accept Friend Request");
                                mDeclineBtn.setText("Decline Friend Request");
                                mDeclineBtn.setVisibility(View.VISIBLE);

                            }else if(req_type.equals("sent")){

                                mCurrentState = "request_sent";
                                mAcceptBtn.setText("Cancel Friend Request");
                                mDeclineBtn.setVisibility(View.INVISIBLE);

                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(ProfileActivity.this, "You've gotten some errors", Toast.LENGTH_LONG).show();
                    }
                });

                mFriendsReference.child(current_uid).addValueEventListener(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(user_id)){
                            mCurrentState = "friends";
                            mAcceptBtn.setText("UnFriend");
                            mDeclineBtn.setText("Send Message");
                            mDeclineBtn.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(ProfileActivity.this, "You've gotten some errors", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "You've gotten some errors", Toast.LENGTH_LONG).show();
            }
        });

        mAcceptBtn = findViewById(R.id.profile_send_btn);
        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrentState.equals("not_friends")){

                    mNotificationId = mNotificationReference.child(user_id).push().getKey();

                    HashMap<String, String> notificationsData = new HashMap<>();
                    notificationsData.put("from", current_uid);
                    notificationsData.put("type", "request");

                    HashMap<String,String> requestsSentData = new HashMap<>();
                    requestsSentData.put("notification_id", mNotificationId);
                    requestsSentData.put("request_type", "sent");

                    HashMap<String, String> requestsReceivedData = new HashMap<>();
                    requestsReceivedData.put("notification_id", mNotificationId);
                    requestsReceivedData.put("request_type", "received");

                    Map requestsMap = new HashMap();
                    requestsMap.put("Requests/" + current_uid + "/" + user_id, requestsSentData);
                    requestsMap.put("Requests/" + user_id + "/" + current_uid, requestsReceivedData);
                    requestsMap.put("Notifications/" + user_id + "/" + mNotificationId, notificationsData);

                    mRootRef.updateChildren(requestsMap, new DatabaseReference.CompletionListener() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null)
                                Toast.makeText(ProfileActivity.this, "Errors in sending request", Toast.LENGTH_LONG).show();
                            else{
                                mCurrentState = "request_sent";
                                mAcceptBtn.setText("Cancel Friend Request");
                                mDeclineBtn.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
                if(mCurrentState.equals("request_sent")){
                    mRequestsReference.child(current_uid).child(user_id).child("notification_id")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String notification_id = dataSnapshot.getValue().toString();
                            mNotificationReference.child(user_id).child(notification_id).removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    deleteRequest("Send Friend Request", "not_friends");
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(ProfileActivity.this, "You've gotten some errors", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                if(mCurrentState.equals("request_received")){

                    final String current_date = DateFormat.getDateTimeInstance().format(new Date());

                    Map requestsMap = new HashMap();
                    requestsMap.put("Friends/" + current_uid + "/" + user_id, current_date);
                    requestsMap.put("Friends/" + user_id + "/" + current_uid, current_date);

                    mRootRef.updateChildren(requestsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null)
                                Toast.makeText(ProfileActivity.this, "Errors in accepting request", Toast.LENGTH_LONG).show();
                            else{
                                mRootRef.child("Requests/" + user_id + "/" + current_uid + "/notification_id")
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        String notification_id = Objects.requireNonNull(dataSnapshot.getValue()).toString();
                                        mNotificationReference.child(current_uid + "/" + notification_id).removeValue()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @SuppressLint("SetTextI18n")
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        mDeclineBtn.setText("Send Message");
                                                        mDeclineBtn.setVisibility(View.VISIBLE);
                                                        deleteRequest("UnFriend", "friends" );
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                    });
                }
                if(mCurrentState.equals("friends")){

                    mFriendsReference.child(current_uid).child(user_id).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mFriendsReference.child(user_id).child(current_uid).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @SuppressLint("SetTextI18n")
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    mAcceptBtn.setText("Send Friend Request");
                                                    mCurrentState = "not_friends";
                                                    mDeclineBtn.setVisibility(View.INVISIBLE);

                                                }
                                            });
                                }
                            });
                }
            }

            private void deleteRequest(final String btnText, final String state) {
                mRequestsReference.child(current_uid).child(user_id).removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                mRequestsReference.child(user_id).child(current_uid)
                                        .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        mAcceptBtn.setText(btnText);
                                        mCurrentState = state;
                                    }
                                });
                            }
                        });
            }
        });

        mDeclineBtn = findViewById(R.id.profile_decline_btn);
        mDeclineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //remove notifications & requests
                if(mCurrentState.equals("request_received")) {
                    mRequestsReference.child(user_id).child(current_uid).child("notification_id")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String notification_id = dataSnapshot.getValue().toString();
                                    mNotificationReference.child(current_uid).child(notification_id).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mRequestsReference.child(current_uid).child(user_id).removeValue()
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    mRequestsReference.child(user_id).child(current_uid).removeValue()
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {

                                                                                    mAcceptBtn.setText("Send Friend Request");
                                                                                    mCurrentState = "not_friends";
                                                                                    mDeclineBtn.setVisibility(View.INVISIBLE);

                                                                                }
                                                                            });
                                                                }
                                                            });
                                                }
                                            });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(ProfileActivity.this, "You've gotten some errors", Toast.LENGTH_LONG).show();
                                }
                            });
                }
                //send message
                else if(mCurrentState.equals("friends")){
                    Intent chatIntent = new Intent(ProfileActivity.this, ChatActivity.class);
                    chatIntent.putExtra("extra_curr_uid", current_uid);
                    chatIntent.putExtra("extra_uid", user_id);
                    chatIntent.putExtra("name", mDisplayName.getText().toString());
                    startActivity(chatIntent);
                }
            }
        });
    }
}
