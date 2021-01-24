package com.example.owner.shakechattry;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar mToolBar;
    private TextInputLayout mDisplayName;
    private TextInputLayout mDescription;
    private Button mSaveBtn;
    private ImageButton mRequestBtn;
    private ImageButton mChatBtn;
    private Spinner mMajors, mColleges;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mToolBar = findViewById(R.id.setting_page_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("Profile Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMajors = findViewById(R.id.setting_major);
        ArrayAdapter majors_adapter = ArrayAdapter.createFromResource(this, R.array.ucsd_majors, android.R.layout.simple_spinner_item);
        mMajors.setAdapter(majors_adapter);

        mColleges = findViewById(R.id.setting_college);
        ArrayAdapter colleges_adapter = ArrayAdapter.createFromResource(this,
                R.array.ucsd_colleges, android.R.layout.simple_spinner_item);
        mColleges.setAdapter(colleges_adapter);

        //new friends/chats button shows up when there is a new friend request/chat
        mRequestBtn = findViewById(R.id.settings_new_req_btn);
        mChatBtn = findViewById(R.id.settings_new_chat_btn);
        mNotificationReference.child(mCurrentUser.getUid()).addValueEventListener(new ValueEventListener() {
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
                            Intent requestActivity = new Intent(SettingsActivity.this, RequestActivity.class);
                            startActivity(requestActivity);
                        }
                    });

                    mChatBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent friendsIntent = new Intent(SettingsActivity.this, FriendsActivity.class);
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

        final String curr_uid = mCurrentUser.getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(curr_uid);

        mDisplayName = findViewById(R.id.setting_display_name);
        mDescription =  findViewById(R.id.setting_description);
        mSaveBtn =  findViewById(R.id.setting_save_btn);

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgress = new ProgressDialog(SettingsActivity.this);
                mProgress.setTitle("Saving Changes");
                mProgress.setMessage("Please wait while we save the changes");
                mProgress.show();

                String name = mDisplayName.getEditText().getText().toString();
                final String description = mDescription.getEditText().getText().toString();
                final String major = mMajors.getSelectedItem().toString();
                final String college = mColleges.getSelectedItem().toString();

                Map profileData = new HashMap();
                profileData.put("name", name);
                profileData.put("description", description);
                profileData.put("college", college);
                profileData.put("major", major);

                mUserDatabase.updateChildren(profileData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError != null)
                            Toast.makeText(SettingsActivity.this, "Error in saving changes", Toast.LENGTH_LONG).show();
                        else{
                            Intent myProfileIntent = new Intent(SettingsActivity.this, MyProfileActivity.class);
                            startActivity(myProfileIntent);
                            Toast.makeText(SettingsActivity.this, "All Changes Saved", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        String DisplayName = getIntent().getStringExtra("display_name");
        String Description = getIntent().getStringExtra("description");
        mDescription.getEditText().setText(Description);
        mDisplayName.getEditText().setText(DisplayName);

        String College = getIntent().getStringExtra("college");
        String Major = getIntent().getStringExtra("major");
        int college_pos = colleges_adapter.getPosition(College);
        mColleges.setSelection(college_pos);
        int major_pos = majors_adapter.getPosition(Major);
        mMajors.setSelection(major_pos);

    }
}
