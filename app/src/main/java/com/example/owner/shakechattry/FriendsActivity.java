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


public class FriendsActivity extends AppCompatActivity {

    private ImageButton mRequestBtn;

    private ArrayList<String> mDisplayNames = new ArrayList<>();
    private ArrayList<String> mDescriptions = new ArrayList<>();
    private ArrayList<String> mImageUrls = new ArrayList<>();
    private ArrayList<String> mUids = new ArrayList<>();

    private DatabaseReference mFriendReference = FirebaseDatabase.getInstance().getReference().child("Friends");
    private DatabaseReference mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
    private DatabaseReference mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Toolbar toolbar = findViewById(R.id.friends_appBar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Friends List");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRequestBtn = findViewById(R.id.friends_new_req_btn);
        mNotificationReference.child(mCurrentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        if(ds.child("type").getValue().toString().equals("request")) {
                            mRequestBtn.setVisibility(View.VISIBLE);
                            break;
                        }
                    }

                    mRequestBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent requestActivity = new Intent(FriendsActivity.this, RequestActivity.class);
                            startActivity(requestActivity);
                        }
                    });

                }else{
                    mRequestBtn.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        populateLists();
    }

    private void populateLists() {
        mFriendReference.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    String uid = ds.getKey();

                    mUserReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String name = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                            String description = Objects.requireNonNull(dataSnapshot.child("description").getValue()).toString();
                            String image_url = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                            String uid = dataSnapshot.getKey();

                                mDisplayNames.add(name);
                                mDescriptions.add(description);
                                mImageUrls.add(image_url);
                                mUids.add(uid);
                                initFriendsList();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initFriendsList() {
        RecyclerView friends_list = findViewById(R.id.friends_list);
        UsersListAdapter adapter = new UsersListAdapter(mImageUrls, mDisplayNames, mDescriptions, mUids, this);
        friends_list.setAdapter(adapter);
        friends_list.setLayoutManager(new LinearLayoutManager(this));
    }
}
