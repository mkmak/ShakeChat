package com.example.owner.shakechattry;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyProfileActivity extends AppCompatActivity {

    private Toolbar mToolBar;
    private CircleImageView mImage;
    private TextView mDisplayName, mDescription, mMajor, mCollege;
    private ImageButton mEditBtn;
    private Button mChangeImageBtn;
    private ImageButton mRequestBtn;
    private ImageButton mChatBtn;

    private static final int GALLARY_PICK = 1;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
    private DatabaseReference mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");
    private FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
    private StorageReference mImageStorage;

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        mImage = findViewById(R.id.myprofile_image);
        mDisplayName = findViewById(R.id.myprofile_display_name);
        mDescription = findViewById(R.id.myprofile_description);
        mMajor = findViewById(R.id.myprofile_major);
        mCollege = findViewById(R.id.myprofile_college);

        mEditBtn = findViewById(R.id.myprofile_edit_btn);
        //go to SettingActivity with extras
        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String display_name = mDisplayName.getText().toString();
                String description = mDescription.getText().toString();
                String college = mCollege.getText().toString();
                String major = mMajor.getText().toString();

                Intent settingsIntent = new Intent(MyProfileActivity.this, SettingsActivity.class);
                settingsIntent.putExtra("display_name", display_name);
                settingsIntent.putExtra("description", description);
                settingsIntent.putExtra("college", college);
                settingsIntent.putExtra("major", major);
                startActivity(settingsIntent);
            }
        });

        mChangeImageBtn = findViewById(R.id.myprofile_change_image_btn);
        //go to gallery to choose image
        mChangeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLARY_PICK);
            }
        });

        //MyProfileActivity toolbar
        mToolBar = findViewById(R.id.myprofile_page_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("My Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRequestBtn = findViewById(R.id.myprofile_new_req_btn);
        mChatBtn = findViewById(R.id.myprofile_new_chat_btn);
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
                            Intent requestActivity = new Intent(MyProfileActivity.this, RequestActivity.class);
                            startActivity(requestActivity);
                        }
                    });

                    mChatBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent friendsIntent = new Intent(MyProfileActivity.this, FriendsActivity.class);
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

        String uid = mCurrentUser.getUid();
        //change profile base on information in database
        mUserDatabase.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String description = dataSnapshot.child("description").getValue().toString();

                if(dataSnapshot.child("college").getValue() != null &&
                        !dataSnapshot.child("college").getValue().toString().equals("--Select Your College--")) {
                    mCollege.setText(dataSnapshot.child("college").getValue().toString());
                    mCollege.setVisibility(View.VISIBLE);
                }
                else
                    mCollege.setVisibility(View.GONE);

                if(dataSnapshot.child("major").getValue() != null &&
                        !dataSnapshot.child("major").getValue().toString().equals("--Select Your Major--")) {
                    mMajor.setText(dataSnapshot.child("major").getValue().toString());
                    mMajor.setVisibility(View.VISIBLE);
                }
                else
                    mMajor.setVisibility(View.GONE);

                mDisplayName.setText(name);
                mDescription.setText(description);
                Picasso.get().load(image).into(mImage); //get image from url
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //crop image put store into Firebase Storage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLARY_PICK && resultCode == RESULT_OK){
            Uri imageUri = data.getData();

            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(imageUri)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                mProgress = new ProgressDialog(MyProfileActivity.this);
                mProgress.setTitle("Uploading Image...");
                mProgress.setMessage("Please wait while we upload and process the image.");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                final Uri resultUri = result.getUri();

                final String uid = mCurrentUser.getUid();
                mImageStorage = FirebaseStorage.getInstance().getReference();
                final StorageReference filepath = mImageStorage.child("profile_images").child(uid + ".jpg");
                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            String download_url = task.getResult().getDownloadUrl().toString();

                            mUserDatabase.child(uid).child("image").setValue(download_url).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        mProgress.dismiss();
                                        Toast.makeText(MyProfileActivity.this, "Finish Uploading", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(MyProfileActivity.this, "Error in uploading", Toast.LENGTH_LONG).show();
                            mProgress.dismiss();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
