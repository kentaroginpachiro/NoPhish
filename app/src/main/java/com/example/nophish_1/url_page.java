package com.example.nophish_1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class url_page extends AppCompatActivity {

    private EditText urlInput;
    private Button submitButton, homeButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_page);

        urlInput = findViewById(R.id.input1);
        submitButton = findViewById(R.id.ntfbtn1);
        homeButton = findViewById(R.id.homebtn1);

        databaseReference = FirebaseDatabase.getInstance().getReference("urls");

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputUrl = urlInput.getText().toString().trim();

                if (inputUrl.isEmpty()) {
                    Toast.makeText(url_page.this, "No URL inputted", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkUrlInDatabase(inputUrl);
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(url_page.this, Main_page.class);
                startActivity(intent);
            }
        });
    }

    private void checkUrlInDatabase(String inputUrl) {
        String normalizedInputUrl = normalizeUrl(inputUrl);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean urlFound = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    URL urlEntry = snapshot.getValue(URL.class);
                    if (urlEntry != null) {
                        String dbUrl = normalizeUrl(urlEntry.getUrl());

                        Log.d("URL Check", "Input URL: " + normalizedInputUrl + ", DB URL: " + dbUrl);

                        // Check if the input URL contains the keyword of the DB URL
                        if (normalizedInputUrl.contains(extractKeyword(dbUrl))) {
                            urlFound = true;
                            break;
                        }
                    }
                }

                Intent intent = new Intent(url_page.this, Notification.class);
                intent.putExtra("submittedUrl", inputUrl);
                intent.putExtra("status", urlFound ? "possibly safe" : "possibly dangerous");
                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(url_page.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url; // Add HTTPS if missing
        }
        return url.toLowerCase(); // Convert to lower case for comparison
    }

    private String extractKeyword(String url) {
        String[] parts = url.split("\\.");
        return parts[parts.length - 2]; // Get the second-to-last part
    }
}
