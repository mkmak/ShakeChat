package com.example.owner.shakechattry;

import java.util.Random;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Toolbar mToolBar;

    private ImageButton mRequestBtn;
    private ImageButton mChatBtn;
    private TextView mMainText;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    private FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private DatabaseReference mSearchReference = FirebaseDatabase.getInstance().getReference().child("Search");
    private DatabaseReference mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mToolBar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("Shake Chat");

        if(mCurrentUser != null) {
            mMainText = findViewById(R.id.main_text);
            mUserReference.child(mCurrentUser.getUid()).child("name").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String welcomeText;
                    if (dataSnapshot.getValue() != null)
                        welcomeText = "Welcome \n" + dataSnapshot.getValue().toString();
                    else
                        welcomeText = "Welcome";
                    mMainText.setText(welcomeText);
                    mMainText.setTextColor(Color.parseColor("#29352E"));
                    mMainText.setPadding(20, 20, 20, 20);
                    mMainText.setTypeface(null, Typeface.BOLD);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        if(mCurrentUser != null) {
            mRequestBtn = findViewById(R.id.main_new_req_btn);
            mChatBtn = findViewById(R.id.main_new_chat_btn);
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
                                Intent requestActivity = new Intent(MainActivity.this, RequestActivity.class);
                                startActivity(requestActivity);
                            }
                        });

                        mChatBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent friendsIntent = new Intent(MainActivity.this, FriendsActivity.class);
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
        }

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                /*
                 * The following method, "handleShakeEvent(count):" is a stub //
                 * method you would use to setup whatever you want done once the
                 * device has been shook.
                 */
                handleShakeEvent(count);
            }
        });

    }

    private void handleShakeEvent(int count) {
        populateOnlineUsers();
    }

    private String getRandomUser(ArrayList<String> online_users) {
        if(!online_users.isEmpty()) {
            Random r = new Random();
            int matched_index = r.nextInt(online_users.size());
            return online_users.get(matched_index);
        }else {
            return "";
        }
    }

    private void populateOnlineUsers() {
        mUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> online_users = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if(ds.child("online").getValue().equals(true) && !ds.getKey().equals(mCurrentUser.getUid())){
                        online_users.add(ds.getKey());
                    }
                }
                String matched_uid = getRandomUser(online_users);
                if(matched_uid.equals("")) {
                    Toast.makeText(MainActivity.this, "No user found", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                profileIntent.putExtra("extra_uid", matched_uid);
                startActivity(profileIntent);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)

        if(mCurrentUser == null){
            sendToStart();
        }
        else{
            mUserReference.child(mCurrentUser.getUid()).child("online").setValue(true);
            mUserReference.child(mCurrentUser.getUid()).child("online").onDisconnect().setValue(false);
        }
    }

    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem search = menu.findItem(R.id.main_search_btn);
        SearchView searchView = (SearchView) search.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String emailEncoded = query.replace(".", ",");

                mSearchReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Object uid_obj = dataSnapshot.child(emailEncoded).getValue();

                        if(uid_obj == null)
                            Toast.makeText(MainActivity.this, "You got some errors.", Toast.LENGTH_LONG).show();
                        else if(uid_obj.toString().equals(mCurrentUser.getUid())){
                            Intent myProfileIntent = new Intent(MainActivity.this, MyProfileActivity.class);
                            startActivity(myProfileIntent);
                        }
                        else {
                            String uid = uid_obj.toString();
                            Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                            profileIntent.putExtra("extra_uid", uid);
                            startActivity(profileIntent);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.main_friends_btn){
            Intent friendsIntent = new Intent(MainActivity.this, FriendsActivity.class);
            startActivity(friendsIntent);
        }

        if(item.getItemId() == R.id.main_my_profile_btn){
            Intent myProfileIntent = new Intent(MainActivity.this, MyProfileActivity.class);
            startActivity(myProfileIntent);
        }

        if(item.getItemId() == R.id.main_logout_btn){
            mUserReference.child(mCurrentUser.getUid()).child("online").setValue(false);
            FirebaseAuth.getInstance().signOut();
            sendToStart();
        }

        return true;
    }
}
