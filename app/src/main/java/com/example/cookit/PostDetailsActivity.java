package com.example.cookit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailsActivity extends AppCompatActivity {

    private String hisuid, ptime, myuid, myname, myemail, mydp, uimage, postId, plike, hisdp, hisname;
    private ImageView picture, image, imagep;
    private TextView name, time, title, description, like, tcomment;
    private ImageButton more, sendb;
    private Button likebtn;
    private LinearLayout profile;
    private EditText comment;
    private RecyclerView recyclerView;
    private List<ModelComment> commentList;
    private AdapterComment adapterComment;
    private boolean mlike = false;
    private ActionBar actionBar;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postdetails);

        initUI();
        setupActionBar();
        setupRecyclerView();

        postId = getIntent().getStringExtra("pid");
        if (postId == null) {
            Toast.makeText(this, "Post ID is null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myuid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (myuid == null) {
            Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myemail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        loadPostInfo();
        loadUserInfo();
        setLikes();
        loadComments();

        sendb.setOnClickListener(v -> postComment());
        likebtn.setOnClickListener(v -> likepost());

        like.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailsActivity.this, PostLikedByActivity.class);
            intent.putExtra("pid", postId);
            startActivity(intent);
        });
    }

    private void initUI() {
        recyclerView = findViewById(R.id.recyclecomment);
        picture = findViewById(R.id.pictureco);
        image = findViewById(R.id.pimagetvco);
        name = findViewById(R.id.unameco);
        time = findViewById(R.id.utimeco);
        more = findViewById(R.id.morebtn);
        title = findViewById(R.id.ptitleco);
        description = findViewById(R.id.descriptco);
        tcomment = findViewById(R.id.pcommenttv);
        like = findViewById(R.id.plikebco);
        likebtn = findViewById(R.id.like);
        comment = findViewById(R.id.typecommet);
        sendb = findViewById(R.id.sendcomment);
        imagep = findViewById(R.id.commentimge);
        profile = findViewById(R.id.profilelayout);
        progressDialog = new ProgressDialog(this);
    }

    private void setupActionBar() {
        actionBar = getSupportActionBar();
        actionBar.setTitle("Post Details");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        //actionBar.setSubtitle("SignedInAs:" + myemail);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        commentList = new ArrayList<>();
        adapterComment = new AdapterComment(getApplicationContext(), commentList, myuid, postId);
        recyclerView.setAdapter(adapterComment);
    }

    private void loadComments() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelComment modelComment = snapshot.getValue(ModelComment.class);
                    if (modelComment != null) {
                        commentList.add(modelComment);
                    }
                }
                adapterComment.notifyDataSetChanged();
                // Update comment count
                tcomment.setText(commentList.size() + " Comments");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    private void setLikes() {
        final DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts").child(postId);
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("plike").exists()) {
                    plike = dataSnapshot.child("plike").getValue().toString();
                    like.setText(plike + " Likes");
                } else {
                    like.setText("0 Likes");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    private void likepost() {
        mlike = true;
        final DatabaseReference likeRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mlike) {
                    if (dataSnapshot.child(postId).hasChild(myuid)) {
                        postRef.child(postId).child("plike").setValue("" + (Integer.parseInt(plike) - 1));
                        likeRef.child(postId).child(myuid).removeValue();
                        mlike = false;
                    } else {
                        postRef.child(postId).child("plike").setValue("" + (Integer.parseInt(plike) + 1));
                        likeRef.child(postId).child(myuid).setValue("Liked");
                        mlike = false;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    private void postComment() {
        progressDialog.setMessage("Adding Comment");

        final String commentss = comment.getText().toString().trim();
        if (TextUtils.isEmpty(commentss)) {
            Toast.makeText(PostDetailsActivity.this, "Empty comment", Toast.LENGTH_LONG).show();
            return;
        }
        progressDialog.show();
        String timestamp = String.valueOf(System.currentTimeMillis());
        DatabaseReference datarf = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("cId", timestamp);
        hashMap.put("comment", commentss);
        hashMap.put("ptime", timestamp);
        hashMap.put("uid", myuid);
        hashMap.put("uemail", myemail);
        hashMap.put("udp", mydp);
        hashMap.put("uname", myname);
        datarf.child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressDialog.dismiss();
                Toast.makeText(PostDetailsActivity.this, "Added", Toast.LENGTH_LONG).show();
                comment.setText("");
                updateCommentCount();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(PostDetailsActivity.this, "Failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateCommentCount() {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String comments = dataSnapshot.child("pcomments").getValue(String.class);
                if (comments != null) {
                    int newCommentCount = Integer.parseInt(comments) + 1;
                    reference.child("pcomments").setValue(String.valueOf(newCommentCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailsActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserInfo() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        userRef.orderByChild("uid").equalTo(myuid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    myname = snapshot.child("name").getValue(String.class);
                    mydp = snapshot.child("image").getValue(String.class);
                    if (mydp != null) {
                        Glide.with(PostDetailsActivity.this).load(mydp).into(imagep);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailsActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPostInfo() {
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String ptitle = dataSnapshot.child("title").getValue(String.class);
                    String pdescription = dataSnapshot.child("description").getValue(String.class);
                    uimage = dataSnapshot.child("uimage").getValue(String.class);
                    hisdp = dataSnapshot.child("udp").getValue(String.class);
                    hisname = dataSnapshot.child("uname").getValue(String.class);
                    ptime = dataSnapshot.child("ptime").getValue(String.class);
                    plike = dataSnapshot.child("plike").getValue(String.class);
                    String commentCount = dataSnapshot.child("pcomments").getValue(String.class);

                    if (ptime != null) {
                        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                        calendar.setTimeInMillis(Long.parseLong(ptime));
                        String timeDate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
                        time.setText(timeDate);
                    }

                    title.setText(ptitle);
                    description.setText(pdescription);
                    like.setText(plike + " Likes");
                    tcomment.setText(commentCount + " Comments");

                    if ("noImage".equals(uimage)) {
                        image.setVisibility(View.GONE);
                    } else {
                        image.setVisibility(View.VISIBLE);
                        if (uimage != null) {
                            Glide.with(PostDetailsActivity.this).load(uimage).into(image);
                        }
                    }

                    if (hisdp != null) {
                        Glide.with(PostDetailsActivity.this).load(hisdp).into(picture);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailsActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}

