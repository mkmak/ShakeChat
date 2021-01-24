package com.example.owner.shakechattry;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class RequestActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageButton mChatBtn;
    private ArrayList<String> mDisplayNames = new ArrayList<>();
    private ArrayList<String> mDescriptions = new ArrayList<>();
    private ArrayList<String> mImageUrls = new ArrayList<>();
    private ArrayList<String> mUids = new ArrayList<>();

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private DatabaseReference mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        mToolbar = findViewById(R.id.request_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Request List");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mChatBtn = findViewById(R.id.request_new_chat_btn);
        mNotificationReference.child(mCurrentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        if(ds.child("type").getValue().toString().equals("chat")) {
                            mChatBtn.setVisibility(View.VISIBLE);
                            break;
                        }
                    }

                    mChatBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent friendsIntent = new Intent(RequestActivity.this, FriendsActivity.class);
                            startActivity(friendsIntent);
                        }
                    });

                }else{
                    mChatBtn.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        populateLists();
    }

    private void populateLists() {
        mNotificationReference.child(mCurrentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if(Objects.requireNonNull(ds.child("type").getValue()).toString().equals("request")) {
                        String user_id = Objects.requireNonNull(ds.child("from").getValue()).toString();
                        mUserReference.child(user_id).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String display_name = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                                String description = Objects.requireNonNull(dataSnapshot.child("description").getValue()).toString();
                                String image_url = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                                String uid = dataSnapshot.getKey();

                                mDisplayNames.add(display_name);
                                mDescriptions.add(description);
                                mImageUrls.add(image_url);
                                mUids.add(uid);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
                initRequestsList();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void initRequestsList() {
        RecyclerView request_list = findViewById(R.id.request_list);
        UsersListAdapter adapter = new UsersListAdapter(mImageUrls, mDisplayNames, mDescriptions, mUids, this);
        request_list.setAdapter(adapter);
        request_list.setLayoutManager(new LinearLayoutManager(this));
    }
}


