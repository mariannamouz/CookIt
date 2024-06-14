package com.example.cookit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OtherUserProfileActivity extends AppCompatActivity {
//entering another users profile
    private ImageView profileImageView;
    private TextView nameTextView, emailTextView, followingTextView, followersTextView;
    private Button followBtn;
    private RecyclerView recyclerViewPosts;
    private List<ModelPost> postList;
    private AdapterPosts adapterPosts;
    private String currentUserId, profileUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("User's Profile");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // Initialize views
        profileImageView = findViewById(R.id.avatartv);
        nameTextView = findViewById(R.id.nametv);
        emailTextView = findViewById(R.id.emailtv);
        followingTextView = findViewById(R.id.followingtv);
        followersTextView = findViewById(R.id.followerstv);
        followBtn = findViewById(R.id.follow_btn);
        recyclerViewPosts = findViewById(R.id.recyclerposts);

        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();

        // Get data from intent
        String userName = getIntent().getStringExtra("userName");
        String userEmail = getIntent().getStringExtra("userEmail");
        String userImage = getIntent().getStringExtra("userImage");
        profileUserId = getIntent().getStringExtra("userId");

        // Set data to views
        nameTextView.setText(userName);
        emailTextView.setText(userEmail);
        Glide.with(this).load(userImage).into(profileImageView);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        checkFollowStatus();
        loadFollowingCount();
        loadFollowersCount();
        loadUserPosts();

        followBtn.setOnClickListener(v -> {
            if (followBtn.getText().toString().equals("Follow")) {
                followUser();
            } else {
                unfollowUser();
            }
        });
    }

    private void checkFollowStatus() {
        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("Follows").child(currentUserId);
        followRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(profileUserId).exists()) {
                    followBtn.setText("Unfollow");
                } else {
                    followBtn.setText("Follow");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void followUser() {
        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("Follows");
        followRef.child(currentUserId).child(profileUserId).setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                followBtn.setText("Unfollow");
                Toast.makeText(OtherUserProfileActivity.this, "Followed", Toast.LENGTH_SHORT).show();
                loadFollowingCount(); // Update count after following
                loadFollowersCount(); // Update count after following
            } else {
                Toast.makeText(OtherUserProfileActivity.this, "Failed to follow", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unfollowUser() {
        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("Follows");
        followRef.child(currentUserId).child(profileUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                followBtn.setText("Follow");
                Toast.makeText(OtherUserProfileActivity.this, "Unfollowed", Toast.LENGTH_SHORT).show();
                loadFollowingCount(); // Update count after unfollowing
                loadFollowersCount(); // Update count after unfollowing
            } else {
                Toast.makeText(OtherUserProfileActivity.this, "Failed to unfollow", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFollowingCount() {
        DatabaseReference followingRef = FirebaseDatabase.getInstance().getReference("Follows").child(profileUserId);
        followingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followingTextView.setText("Following: " + snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OtherUserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFollowersCount() {
        DatabaseReference followersRef = FirebaseDatabase.getInstance().getReference("Follows");
        followersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int followersCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.hasChild(profileUserId)) {
                        followersCount++;
                    }
                }
                followersTextView.setText("Followers: " + followersCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OtherUserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserPosts() {
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = postsRef.orderByChild("uid").equalTo(profileUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    postList.add(modelPost);
                }
                adapterPosts = new AdapterPosts(OtherUserProfileActivity.this, postList);
                recyclerViewPosts.setAdapter(adapterPosts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(OtherUserProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}



