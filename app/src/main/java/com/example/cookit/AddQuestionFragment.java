package com.example.cookit;  //AddBlogFragment.java

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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


/**
 * A simple {@link Fragment} subclass.
 */


public class AddQuestionFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    EditText qTitle, qDes;
    ProgressDialog pd;
    String name, email, uid, dp;
    DatabaseReference databaseReference;
    Button upload;

    public AddQuestionFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_question, container, false);

       /* ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Edit Profile");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);*/

        firebaseAuth = FirebaseAuth.getInstance();
        qTitle = view.findViewById(R.id.qtitle);
        qDes = view.findViewById(R.id.qdes);
        upload = view.findViewById(R.id.qupload);
        pd = new ProgressDialog(getContext());
        pd.setCanceledOnTouchOutside(false);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        DatabaseReference userRef = databaseReference.child(firebaseAuth.getCurrentUser().getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                name = dataSnapshot.child("name").getValue().toString();
                email = "" + dataSnapshot.child("email").getValue();
                dp = "" + dataSnapshot.child("image").getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String questionTitle = qTitle.getText().toString().trim();
                String questionDescription = qDes.getText().toString().trim();

                if (TextUtils.isEmpty(questionTitle)) {
                    qTitle.setError("Title can't be empty");
                    Toast.makeText(getContext(), "Title can't be left empty", Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(questionDescription)) {
                    qDes.setError("Description can't be empty");
                    Toast.makeText(getContext(), "Description can't be left empty", Toast.LENGTH_LONG).show();
                    return;
                }

                uploadQuestion(questionTitle, questionDescription);
            }
        });

        return view;
    }

    private void uploadQuestion(final String title, final String description) {
        pd.setMessage("Publishing Question");
        pd.show();
        final String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("uid", firebaseAuth.getCurrentUser().getUid());
        hashMap.put("uname", name);
        hashMap.put("uemail", email);
        hashMap.put("udp", dp);
        hashMap.put("title", title);
        hashMap.put("description", description);
        hashMap.put("ptime", timestamp);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Questions");
        databaseReference.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Toast.makeText(getContext(), "Question Published", Toast.LENGTH_LONG).show();
                        qTitle.setText("");
                        qDes.setText("");
                        startActivity(new Intent(getContext(), DashboardActivity.class));
                        getActivity().finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(getContext(), "Failed to publish question: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /*@Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }*/
}

