package com.example.cookit;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class AddBlogsActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    EditText title, des;
    private static final int CAMERA_REQUEST = 100;
    private static final int STORAGE_REQUEST = 200;
    String[] cameraPermission;
    String[] storagePermission;
    ProgressDialog pd;
    ImageView image;
    String edititle, editdes, editimage;
    private static final int IMAGEPICK_GALLERY_REQUEST = 300;
    private static final int IMAGE_PICKCAMERA_REQUEST = 400;

    Uri imageuri = null;
    String name, email, uid, dp;
    DatabaseReference databaseReference;
    Button upload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_blogs); // Reuse the existing layout file

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Edit Profile");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        title = findViewById(R.id.ptitle);
        des = findViewById(R.id.pdes);
        image = findViewById(R.id.imagep);
        upload = findViewById(R.id.pupload);
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);

        // Retrieving the user data like name ,email and profile pic using query
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = databaseReference.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    name = dataSnapshot1.child("name").getValue().toString();
                    email = "" + dataSnapshot1.child("email").getValue();
                    dp = "" + dataSnapshot1.child("image").getValue().toString();
                    uid="" + dataSnapshot1.child("uid").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // Initializing camera and storage permission
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // After clicking on image we will be selecting an image
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePicDialog();
            }
        });

        // Now we will upload our blog
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titl = "" + title.getText().toString().trim();
                String description = "" + des.getText().toString().trim();

                // If empty set error
                if (TextUtils.isEmpty(titl)) {
                    title.setError("Title can't be empty");
                    Toast.makeText(AddBlogsActivity.this, "Title can't be left empty", Toast.LENGTH_LONG).show();
                    return;
                }

                // If empty set error
                if (TextUtils.isEmpty(description)) {
                    des.setError("Description can't be empty");
                    Toast.makeText(AddBlogsActivity.this, "Description can't be left empty", Toast.LENGTH_LONG).show();
                    return;
                }

                // If empty show error
                if (imageuri == null) {
                    Toast.makeText(AddBlogsActivity.this, "Select an Image", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    uploadData(titl, description);
                }
            }
        });
    }

    private void showImagePicDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image From");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // check for the camera and storage permission if not given then request for permission
                if (which == 0) {
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                } else if (which == 1) {
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });
        builder.create().show();
    }

    // check for storage permission
    private Boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    // request for permission to write data into storage
    private void requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST);
    }

    // check camera permission to click picture using camera
    private Boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    // request for permission to click photo using camera in app
    private void requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST);
    }

    // handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST: {
                if (grantResults.length > 0) {
                    boolean camera_accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorage_accepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    // if request access given then pick data
                    if (camera_accepted && writeStorage_accepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Please enable Camera and Storage permissions", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST: {
                if (grantResults.length > 0) {
                    boolean writeStorage_accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    // if request access given then pick data
                    if (writeStorage_accepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Please enable Storage permissions", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
        }
    }

    // if access is given then pick image from camera and then put
    // the imageuri in intent extra and pass to startactivityforresult
    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_pic");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        imageuri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageuri);
        startActivityForResult(cameraIntent, IMAGE_PICKCAMERA_REQUEST);
    }

    // if access is given then pick image from gallery
    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGEPICK_GALLERY_REQUEST);
    }

    // Upload the value of blog data into firebase
    private void uploadData(final String titl, final String description) {
        // show the progress dialog box
        pd.setMessage("Publishing Post");
        pd.show();
        final String timestamp = String.valueOf(System.currentTimeMillis());
        String filepathname = "Posts/" + "post" + timestamp;

        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        // Initializing the storage reference for updating the data
        StorageReference storageReference1 = FirebaseStorage.getInstance().getReference().child(filepathname);
        storageReference1.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Getting the URL of the uploaded image
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                String downloadUri = uriTask.getResult().toString();
                if (uriTask.isSuccessful()) {
                    // If task is successful, update the data into Firebase
                    HashMap<Object, String> hashMap = new HashMap<>();
                    hashMap.put("uid", firebaseAuth.getCurrentUser().getUid());//firebaseAuth.getUid()
                    hashMap.put("uname", name);
                    hashMap.put("uemail", email);
                    hashMap.put("udp", dp);
                    hashMap.put("title", titl);
                    hashMap.put("description", description);
                    hashMap.put("uimage", downloadUri);
                    hashMap.put("ptime", timestamp);
                    hashMap.put("plike", "0");
                    hashMap.put("pcomments", "0");

                    // Set the data into Firebase and then clear the title, description, and image data
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                    databaseReference.child(timestamp).setValue(hashMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    pd.dismiss();
                                    Toast.makeText(AddBlogsActivity.this, "Published", Toast.LENGTH_LONG).show();
                                    title.setText("");
                                    des.setText("");
                                    image.setImageURI(null);
                                    imageuri = null;
                                    startActivity(new Intent(AddBlogsActivity.this, DashboardActivity.class));
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    pd.dismiss();
                                    Toast.makeText(AddBlogsActivity.this, "Failed to publish post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(AddBlogsActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Here we are getting data from image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGEPICK_GALLERY_REQUEST) {
                imageuri = data.getData();
                image.setImageURI(imageuri);
            } else if (requestCode == IMAGE_PICKCAMERA_REQUEST) {
                image.setImageURI(imageuri);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}

