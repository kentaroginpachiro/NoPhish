    package com.example.nophish_1;

    import androidx.appcompat.app.AppCompatActivity;

    import android.content.DialogInterface;
    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.View;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ListView;
    import android.widget.Toast;

    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import android.app.AlertDialog;
    import android.content.DialogInterface;

    import java.util.ArrayList;

    public class admin_url extends AppCompatActivity {

        private EditText urlInput;
        private Button inputBtn, homeBtn;
        private ListView listView;
        private DatabaseReference db;
        private ArrayAdapter<String> adapter;
        private ArrayList<String> urlList;
        private ArrayList<String> urlIds; // To store IDs for deletion

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_admin_url);

            urlInput = findViewById(R.id.urladmininput);
            inputBtn = findViewById(R.id.inputbtn1);
            homeBtn = findViewById(R.id.homebtnadmin);
            listView = findViewById(R.id.list1);

            db = FirebaseDatabase.getInstance().getReference("urls");

            urlList = new ArrayList<>();
            urlIds = new ArrayList<>(); // Initialize the list to hold IDs
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, urlList);
            listView.setAdapter(adapter);

            loadUrlsFromDatabase();

            inputBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = urlInput.getText().toString().trim();
                    if (!url.isEmpty()) {
                        saveUrlToDatabase(url);
                    } else {
                        Toast.makeText(admin_url.this, "Please enter a URL", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            homeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(admin_url.this, MainActivity.class);
                    startActivity(intent);
                }
            });

            // Set an item click listener to handle deletions
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedUrlId = urlIds.get(position);
                showDeleteConfirmationDialog(selectedUrlId, position);
            });
        }

        private void saveUrlToDatabase(String url) {
            String urlId = db.push().getKey();
            if (urlId != null) {
                URL urlObj = new URL(url);
                db.child(urlId).setValue(urlObj).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(admin_url.this, "URL saved successfully", Toast.LENGTH_SHORT).show();
                        urlList.add(url);
                        urlIds.add(urlId); // Store the URL ID
                        adapter.notifyDataSetChanged();
                        urlInput.setText("");
                    } else {
                        Toast.makeText(admin_url.this, "Failed to save URL", Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseError", "Error saving URL", task.getException());
                    }
                });
            }
        }

        private void loadUrlsFromDatabase() {
            db.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    urlList.clear();
                    urlIds.clear(); // Clear the list of IDs
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        URL url = postSnapshot.getValue(URL.class);
                        if (url != null) {
                            urlList.add(url.getUrl());
                            urlIds.add(postSnapshot.getKey()); // Store the URL ID
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("FirebaseError", "Error loading URLs", databaseError.toException());
                }
            });
        }

        private void showDeleteConfirmationDialog(String urlId, int position) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete URL")
                    .setMessage("Are you sure you want to delete this URL?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteUrl(urlId, position))
                    .setNegativeButton("No", null)
                    .show();
        }

        private void deleteUrl(String urlId, int position) {
            db.child(urlId).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(admin_url.this, "URL deleted successfully", Toast.LENGTH_SHORT).show();
                    urlList.remove(position);
                    urlIds.remove(position); // Remove the corresponding ID
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(admin_url.this, "Failed to delete URL", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseError", "Error deleting URL", task.getException());
                }
            });
        }
    }
