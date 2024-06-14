package com.example.cookit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private ImageView avatartv;
    private TextView nam, email, followingTextView, followersTextView;
    private RecyclerView postrecycle;
    private List<ModelPost> posts;
    private AdapterPosts adapterPosts;
    private String uid;
    private ProgressDialog pd;
    private FloatingActionButton fab;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        avatartv = view.findViewById(R.id.avatartv);
        nam = view.findViewById(R.id.nametv);
        email = view.findViewById(R.id.emailtv);
        followingTextView = view.findViewById(R.id.followingtv);
        followersTextView = view.findViewById(R.id.followerstv);
        uid = firebaseUser.getUid();
        fab = view.findViewById(R.id.fab);
        postrecycle = view.findViewById(R.id.recyclerposts);
        posts = new ArrayList<>();
        pd = new ProgressDialog(getActivity());

        loadMyPosts();
        loadFollowingCount();
        loadFollowersCount();

        pd.setCanceledOnTouchOutside(false);

        // Retrieving user data from firebase
        Query query = databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    String name = "" + dataSnapshot1.child("name").getValue();
                    String emaill = "" + dataSnapshot1.child("email").getValue();
                    String image = "" + dataSnapshot1.child("image").getValue();
                    nam.setText(name);
                    email.setText(emaill);
                    try {
                        Glide.with(getActivity()).load(image).into(avatartv);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), EditProfilePage.class));
            }
        });

        followersTextView.setOnClickListener(v -> openUserListActivity("followers"));
        followingTextView.setOnClickListener(v -> openUserListActivity("following"));

        return view;
    }

    private void loadMyPosts() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        postrecycle.setLayoutManager(layoutManager);

        // Initialize the adapter
        adapterPosts = new AdapterPosts(getActivity(), posts);
        postrecycle.setAdapter(adapterPosts);

        // retrieve value from the Posts node
        // so that uid of the current user is equal to uid of the user having a post
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                posts.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    ModelPost modelPost = dataSnapshot1.getValue(ModelPost.class);
                    posts.add(modelPost);
                }
                // Notify adapter of data changes
                adapterPosts.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadFollowingCount() {
        DatabaseReference followingRef = FirebaseDatabase.getInstance().getReference("Follows").child(uid);
        followingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followingTextView.setText("Following: " + snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
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
                    if (ds.hasChild(uid)) {
                        followersCount++;
                    }
                }
                followersTextView.setText("Followers: " + followersCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUserListActivity(String listType) {
        Intent intent = new Intent(getActivity(), UsersFollowActivity.class);
        intent.putExtra("userId", uid);
        intent.putExtra("listType", listType);
        startActivity(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }
}
