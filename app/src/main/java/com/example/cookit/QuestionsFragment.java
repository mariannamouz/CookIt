package com.example.cookit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionsFragment extends Fragment {

    private static final String TAG = "QuestionsFragment";
    FirebaseAuth firebaseAuth;
    String myuid;
    RecyclerView recyclerView;
    List<ModelQuestion> questions;
    AdapterQuestions adapterQuestions;
    Set<String> followingUserIds;

    public QuestionsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_question, container, false);
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            myuid = firebaseAuth.getCurrentUser().getUid();
        } else {
            Log.e(TAG, "User not logged in");
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        recyclerView = view.findViewById(R.id.questionrecyclerview);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        questions = new ArrayList<>();
        followingUserIds = new HashSet<>();
        loadFollowingUsers();
        return view;
    }

    // retrieves the list of users you follow and then calls loadQuestions
    private void loadFollowingUsers() {
        DatabaseReference followingRef = FirebaseDatabase.getInstance().getReference("Follows").child(myuid);
        followingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followingUserIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    followingUserIds.add(ds.getKey());
                }
                loadQuestions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load following users", error.toException());
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // filters questions to include only those from users you follow
    private void loadQuestions() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Questions");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                questions.clear();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelQuestion modelQuestion = ds.getValue(ModelQuestion.class);
                    if (modelQuestion != null && (followingUserIds.contains(modelQuestion.getUid()) || modelQuestion.getUid().equals(currentUserId))) {
                        questions.add(modelQuestion);
                    }
                }
                adapterQuestions = new AdapterQuestions(getActivity(), questions);
                recyclerView.setAdapter(adapterQuestions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load questions", databaseError.toException());
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // Search question code
    private void searchQuestions(final String search) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Questions");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                questions.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelQuestion modelQuestion = ds.getValue(ModelQuestion.class);
                    if (modelQuestion != null && (modelQuestion.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                            modelQuestion.getDescription().toLowerCase().contains(search.toLowerCase()))) {
                        questions.add(modelQuestion);
                    }
                }
                adapterQuestions = new AdapterQuestions(getActivity(), questions);
                recyclerView.setAdapter(adapterQuestions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to search questions", databaseError.toException());
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    searchQuestions(query);
                } else {
                    loadQuestions();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    searchQuestions(newText);
                } else {
                    loadQuestions();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    // Logout functionality
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            firebaseAuth.signOut();
            startActivity(new Intent(getContext(), SplashScreen.class));
            getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
