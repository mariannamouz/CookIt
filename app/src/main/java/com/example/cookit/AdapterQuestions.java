package com.example.cookit;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterQuestions extends RecyclerView.Adapter<AdapterQuestions.MyHolder> {

    private static final String TAG = "AdapterQuestions";

    Context context;
    List<ModelQuestion> questionList;
    String myUid;
    DatabaseReference questionRef;

    public AdapterQuestions(Context context, List<ModelQuestion> questionList) {
        this.context = context;
        this.questionList = questionList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        questionRef = FirebaseDatabase.getInstance().getReference("Questions");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_question, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, int position) {
        final int currentPos = holder.getAdapterPosition();
        if (currentPos == RecyclerView.NO_POSITION) {
            return;
        }

        final ModelQuestion currentQuestion = questionList.get(currentPos);
        final String qId = currentQuestion.getPid();
        final String uid = currentQuestion.getUid();
        String uname = currentQuestion.getUname();
        final String qTitle = currentQuestion.getTitle();
        final String qDescription = currentQuestion.getDescription();
        final String qTimestamp = currentQuestion.getPtime();
        String qComments = currentQuestion.getPcomments();
        String udp = currentQuestion.getUdp();

        holder.uname.setText(uname);
        holder.qTitle.setText(qTitle);
        holder.qDescription.setText(qDescription);
        holder.qComments.setText(qComments + " Comments");

        // Log qId to check its value
        Log.d(TAG, "onBindViewHolder: qId = " + qId);

        // Check if timestamp is not null and not empty before parsing
        if (qTimestamp != null && !qTimestamp.isEmpty()) {
            try {
                Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                calendar.setTimeInMillis(Long.parseLong(qTimestamp));
                String timeDate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
                holder.qTime.setText(timeDate);
            } catch (NumberFormatException e) {
                holder.qTime.setText(R.string.unknown_time);  // Set a default value or handle appropriately
                Log.e(TAG, "Invalid timestamp format", e); // Log the error
            }
        } else {
            holder.qTime.setText(R.string.unknown_time);  // Set a default value or handle appropriately
            Log.w(TAG, "Timestamp is null or empty"); // Log a warning
        }

        try {
            Glide.with(context).load(udp).into(holder.uPicture);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load user picture", e); // Log the error
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qId != null) {
                    // Fetch question details and display a toast for demonstration
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Questions").child(qId);
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String title = snapshot.child("qTitle").getValue(String.class);
                                String description = snapshot.child("qDescription").getValue(String.class);
                                Toast.makeText(context, "Title: " + title + "\nDescription: " + description, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Question details loaded successfully: Title: " + title + ", Description: " + description); // Log success
                            } else {
                                Toast.makeText(context, "Question not found", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "Question not found for qId = " + qId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to load data", error.toException()); // Log the error
                        }
                    });
                } else {
                    Toast.makeText(context, "Question ID is null", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Question ID is null");
                }
            }
        });
        holder.qComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }

                Intent intent = new Intent(context, QuestionDetailsActivity.class);
                intent.putExtra("qid", qTimestamp);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {
        ImageView uPicture;
        TextView uname, qTime, qTitle, qDescription, qComments;
        ImageButton moreBtn;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            uPicture = itemView.findViewById(R.id.picturetv);
            uname = itemView.findViewById(R.id.uName);
            qTime = itemView.findViewById(R.id.qTime);
            qTitle = itemView.findViewById(R.id.qTitle);
            qDescription = itemView.findViewById(R.id.qDescription);
            qComments = itemView.findViewById(R.id.qComments);
            moreBtn = itemView.findViewById(R.id.morebtn); // Ensure this matches your row_question.xml
        }
    }
}





