package com.example.nophish_1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Notification extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> notificationList;
    private Button homeButton;
    private ImageView restartLogo;
    private DatabaseReference db;

    // Static lists to hold URL and email history
    public static ArrayList<String> urlHistory = new ArrayList<>();
    public static ArrayList<String> emailHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        listView = findViewById(R.id.list3);
        notificationList = new ArrayList<>(urlHistory); // Initialize with URL history
        notificationList.addAll(emailHistory); // Initialize with email history
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notificationList);
        listView.setAdapter(adapter);

        db = FirebaseDatabase.getInstance().getReference("fetchedEmails"); // Reference to the "fetched_emails" node in Firebase

        loadEmailsFromDatabase();

        String submittedUrl = getIntent().getStringExtra("submittedUrl");
        String status = getIntent().getStringExtra("status");
        String emailContent = getIntent().getStringExtra("emailContent");

        if (submittedUrl != null && status != null) {
            String displayUrl = shortenUrl(submittedUrl);
            String notification = displayUrl + "\nStatus: " + status;
            urlHistory.add(notification);
            notificationList.add(notification);
            adapter.notifyDataSetChanged();
        }

        if (emailContent != null) {
            emailHistory.add(emailContent);
            notificationList.add(emailContent);
            adapter.notifyDataSetChanged();
        }

        homeButton = findViewById(R.id.homebtn2);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(Notification.this, url_page.class);
            startActivity(intent);
        });

        restartLogo = findViewById(R.id.restartlogo);
        restartLogo.setOnClickListener(v -> deleteAllEmails());
    }

    private void loadEmailsFromDatabase() {
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notificationList.clear();
                notificationList.addAll(urlHistory); // Add URL history to the notification list
                notificationList.addAll(emailHistory); // Add email history to the notification list
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    FetchedEmail email = postSnapshot.getValue(FetchedEmail.class);
                    if (email != null) {
                        String contextMessage = (email.getContext() != null && email.getContext().equals("This email came from a trusted source")) ?
                                "This email came from a trusted source" :
                                "This email came from an unknown source";
                        String notification = "From: " + email.getFrom() + "\n" +
                                "Subject: " + email.getSubject() + "\n" +
                                "Content: " + (email.getContent().length() > 40 ? email.getContent().substring(0, 40) + "..." : email.getContent()) + "\n" +
                                email.getUrlStatus() + "\n" +
                                contextMessage;
                        emailHistory.add(notification); // Add to email history
                        notificationList.add(notification); // Add to notification list
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }

    private void deleteAllEmails() {
        db.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                notificationList.clear();
                urlHistory.clear(); // Clear URL history
                emailHistory.clear(); // Clear email history
                adapter.notifyDataSetChanged();
            } else {
                // Handle the failure if needed
            }
        });
    }

    // Method to shorten the URL for display
    private String shortenUrl(String url) {
        if (url.length() > 30) {
            return url.substring(0, 30) + "..."; // Show first 30 characters
        }
        return url;
    }
}
