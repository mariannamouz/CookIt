package com.example.cookit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class AddQuestionActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    EditText title, des;
    ProgressDialog pd;
    String name, email, uid, dp;
    DatabaseReference databaseReference;
    Button upload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_question); // Ensure you have a correct layout file for this

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Add Question");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        title = findViewById(R.id.qtitle);
        des = findViewById(R.id.qdes);
        upload = findViewById(R.id.qupload);
        pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);

        // Retrieving the user data like name, email, and profile pic using query
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = databaseReference.orderByChild("email").equalTo(firebaseAuth.getCurrentUser().getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    name = "" + dataSnapshot1.child("name").getValue();
                    email = "" + dataSnapshot1.child("email").getValue();
                    dp = "" + dataSnapshot1.child("image").getValue();
                    uid = "" + dataSnapshot1.child("uid").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddQuestionActivity.this, "Failed to retrieve user data", Toast.LENGTH_SHORT).show();
            }
        });

        // Now we will upload our question
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titl = "" + title.getText().toString().trim();
                String description = "" + des.getText().toString().trim();

                // If empty set error
                if (TextUtils.isEmpty(titl)) {
                    title.setError("Title can't be empty");
                    Toast.makeText(AddQuestionActivity.this, "Title can't be left empty", Toast.LENGTH_LONG).show();
                    return;
                }

                // If empty set error
                if (TextUtils.isEmpty(description)) {
                    des.setError("Description can't be empty");
                    Toast.makeText(AddQuestionActivity.this, "Description can't be left empty", Toast.LENGTH_LONG).show();
                    return;
                }

                uploadData(titl, description);
            }
        });
    }

    // Upload the value of question data into Firebase
    private void uploadData(final String titl, final String description) {
        // show the progress dialog box
        pd.setMessage("Publishing Question");
        pd.show();
        final String timestamp = String.valueOf(System.currentTimeMillis());

        // Prepare data to be uploaded
        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("uid", firebaseAuth.getCurrentUser().getUid());
        hashMap.put("uname", name);
        hashMap.put("uemail", email);
        hashMap.put("udp", dp);
        hashMap.put("qTitle", titl);
        hashMap.put("qDescription", description);
        hashMap.put("ptime", timestamp);
        hashMap.put("qComments", "0");

        // Set the data into Firebase and then clear the title and description data
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Questions");
        databaseReference.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Toast.makeText(AddQuestionActivity.this, "Published", Toast.LENGTH_LONG).show();
                        title.setText("");
                        des.setText("");
                        startActivity(new Intent(AddQuestionActivity.this, DashboardActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(AddQuestionActivity.this, "Failed to publish question: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}

