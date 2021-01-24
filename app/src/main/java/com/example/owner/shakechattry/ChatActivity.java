package com.example.owner.shakechattry;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import org.w3c.dom.Text;

import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    private static final int GALLARY_PICK = 1;
    private Toolbar mToolbar;

    private ArrayList<String> mTexts = new ArrayList<>();
    private ArrayList<String> mImageUrls = new ArrayList<>();
    private ArrayList<String> mSenderIds = new ArrayList<>();
    private ArrayList<String> mTimes = new ArrayList<>();
    private ArrayList<String> mMessageIds = new ArrayList<>();
    private ArrayList<String> mChatUsers = new ArrayList<>();
    private ArrayList<String> mMessageTypes = new ArrayList<>();
    private ArrayList<String> mPhotoUrls = new ArrayList<>();

    private TextInputLayout mMessageInput;
    private ImageButton mAddBtn, mSendBtn;
    private ImageButton mRequestBtn;
    private ImageButton mChatBtn;
    //private RelativeLayout mLayout;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mNotificationReference = mRootRef.child("Notifications");

    private ChildEventListener mChildListener;

    private String curr_uid, user_id;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        curr_uid = getIntent().getStringExtra("extra_curr_uid");
        user_id = getIntent().getStringExtra("extra_uid");
        mChatUsers.add(curr_uid);
        mChatUsers.add(user_id);

        mToolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(mToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getIntent().getStringExtra("name"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //mLayout = findViewById(R.id.chat_layout);
        /*mLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Hide soft keyboard
                InputMethodManager in = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                assert in != null;
                in.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return false;
            }
        });*/

        mRequestBtn = findViewById(R.id.chat_new_req_btn);
        mChatBtn = findViewById(R.id.chat_new_chat_btn);
        mNotificationReference.child(curr_uid).addValueEventListener(new ValueEventListener() {
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
                            Intent requestActivity = new Intent(ChatActivity.this, RequestActivity.class);
                            startActivity(requestActivity);
                        }
                    });

                    mChatBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent friendsIntent = new Intent(ChatActivity.this, FriendsActivity.class);
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

        populateMessages(curr_uid, user_id);

        mMessageInput = findViewById(R.id.chat_input);
        mSendBtn = findViewById(R.id.chat_send_btn);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String input = Objects.requireNonNull(mMessageInput.getEditText()).getText().toString();
                if(!input.equals("")) {
                    mRootRef.child("Users").child(curr_uid).child("image")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            String image_url = dataSnapshot.getValue().toString();
                            String message_id = mRootRef.child("Chat Rooms").child(curr_uid).child(user_id).push().getKey();
                            String notification_id = mNotificationReference.child(user_id).push().getKey();

                            HashMap<String, String> messageData = new HashMap<>();
                            messageData.put("message", input);
                            messageData.put("sender", curr_uid);
                            messageData.put("time", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                            messageData.put("image", image_url);
                            messageData.put("seen", "false");
                            messageData.put("type", "text");

                            HashMap<String, String> notificationData = new HashMap<>();
                            notificationData.put("from", curr_uid);
                            notificationData.put("type", "chat");

                            Map updateRoot = new HashMap();
                            updateRoot.put("Chat Rooms/" + curr_uid + "/" + user_id + "/" + message_id, messageData);
                            updateRoot.put("Chat Rooms/" + user_id + "/" + curr_uid + "/" + message_id, messageData);
                            updateRoot.put("Notifications/" + user_id + "/" + notification_id, notificationData);

                            mRootRef.updateChildren(updateRoot, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if(databaseError != null)
                                        Toast.makeText(ChatActivity.this, "Error in sending message", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }else{
                    Toast.makeText(ChatActivity.this, "Empty Input", Toast.LENGTH_LONG).show();
                }
                mMessageInput.getEditText().setText("");
            }
        });

        mAddBtn = findViewById(R.id.chat_add_btn);
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLARY_PICK);
            }
        });
    }

    private void populateMessages(final String curr_uid, final String user_id) {
        mChildListener = mRootRef.child("Chat Rooms").child(curr_uid).child(user_id).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                dataSnapshot.child("seen").getRef().setValue("true");
                //remove new chat notification
                mNotificationReference.child(curr_uid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot message : dataSnapshot.getChildren()){
                            if(message.child("from").getValue().toString().equals(user_id) &&
                                    message.child("type").getValue().toString().equals("chat")){

                                message.getRef().removeValue();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                String message = "--photo message--";
                String photo = "--text message--";
                String message_type = dataSnapshot.child("type").getValue().toString();
                String time = dataSnapshot.child("time").getValue().toString();
                String sender_id = dataSnapshot.child("sender").getValue().toString();
                String image_url = dataSnapshot.child("image").getValue().toString();
                String message_id = dataSnapshot.getKey();
                if(message_type.equals("text"))
                    message = dataSnapshot.child("message").getValue().toString();
                else if(message_type.equals("image"))
                    photo = dataSnapshot.child("photo").getValue().toString();

                mTexts.add(message);
                mPhotoUrls.add(photo);
                mSenderIds.add(sender_id);
                mTimes.add(time);
                mImageUrls.add(image_url);
                mMessageIds.add(message_id);
                mMessageTypes.add(message_type);

                initMessagesList();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                int index = mMessageIds.indexOf(dataSnapshot.getKey());
                mTexts.remove(index);
                mSenderIds.remove(index);
                mTimes.remove(index);
                mImageUrls.remove(index);
                mMessageIds.remove(index);
                mMessageTypes.remove(index);
                mPhotoUrls.remove(index);

                //remove one notification if exists
                /*mNotificationReference.child(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren()){
                            if(ds.child("type").getValue().toString().equals("chat")
                                    && ds.child("from").getValue().toString().equals(curr_uid)){

                                ds.getRef().removeValue();
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });*/

                //remove message
                dataSnapshot.getRef().removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        initMessagesList();
                        Toast.makeText(ChatActivity.this, "Removed", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Toast.makeText(ChatActivity.this, dataSnapshot.getKey().toString() + " Moved", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initMessagesList() {
        RecyclerView messages_list = findViewById(R.id.chat_messages);
        MessagesAdapter adapter = new MessagesAdapter(mImageUrls, mPhotoUrls, mTexts, mSenderIds, mTimes, mMessageIds, mChatUsers, mMessageTypes, this);
        messages_list.setAdapter(adapter);
        messages_list.setLayoutManager(new LinearLayoutManager(this));
        messages_list.scrollToPosition(adapter.getItemCount()-1);
    }

    @Override
    public void onStop() {
        super.onStop();
        mRootRef.child("Chat Rooms").child(curr_uid).child(user_id).removeEventListener(mChildListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLARY_PICK && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            final String message_id = mRootRef.child("Chat Rooms").child(curr_uid).child(user_id).push().getKey();
            StorageReference filepath = FirebaseStorage.getInstance().getReference().child(curr_uid).child(user_id).child(message_id + ".jpg");
            filepath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final String photo_url = taskSnapshot.getDownloadUrl().toString();

                    mRootRef.child("Users").child(curr_uid).child("image")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    String image_url = dataSnapshot.getValue().toString();
                                    String notification_id = mNotificationReference.child(user_id).push().getKey();

                                    HashMap<String, String> messageData = new HashMap<>();
                                    messageData.put("photo", photo_url);
                                    messageData.put("sender", curr_uid);
                                    messageData.put("time", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                                    messageData.put("image", image_url);
                                    messageData.put("seen", "false");
                                    messageData.put("type", "image");

                                    HashMap<String, String> notificationData = new HashMap<>();
                                    notificationData.put("from", curr_uid);
                                    notificationData.put("type", "chat");

                                    Map updateRoot = new HashMap();
                                    updateRoot.put("Chat Rooms/" + curr_uid + "/" + user_id + "/" + message_id, messageData);
                                    updateRoot.put("Chat Rooms/" + user_id + "/" + curr_uid + "/" + message_id, messageData);
                                    updateRoot.put("Notifications/" + user_id + "/" + notification_id, notificationData);

                                    mRootRef.updateChildren(updateRoot, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                            if(databaseError != null)
                                                Toast.makeText(ChatActivity.this, "Error in sending message", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                }
            });
        }
    }
}

