package com.example.cookit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast; // Added import statement for Toast

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp; // Added import for FirebaseApp
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    FirebaseUser currentUser;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            currentUser = mAuth.getCurrentUser();
        }

        // Here we are checking that if the user is null then go to LoginActivity. Else move to DashboardActivity.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(SplashScreen.this, "User not logged in, redirecting to LoginActivity", Toast.LENGTH_SHORT).show(); // Added Toast message
                    Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SplashScreen.this, "User logged in, redirecting to DashboardActivity", Toast.LENGTH_SHORT).show(); // Added Toast message
                    Intent mainIntent = new Intent(SplashScreen.this, DashboardActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();
                }
            }
        }, 1000);
    }
}
