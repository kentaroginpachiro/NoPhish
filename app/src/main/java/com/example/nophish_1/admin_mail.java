package com.example.nophish_1;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;

public class admin_mail extends AppCompatActivity {

    private EditText emailInput;
    private Button inputBtn, urlBtn;
    private ListView listView;
    private DatabaseReference db;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> emailList;
    private ArrayList<String> emailIds; // To store IDs for deletion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_mail);

        emailInput = findViewById(R.id.emailadmininput);
        inputBtn = findViewById(R.id.inputbtn2);
        urlBtn = findViewById(R.id.urlbtn1);
        listView = findViewById(R.id.list2);

        db = FirebaseDatabase.getInstance().getReference("emails");

        emailList = new ArrayList<>();
        emailIds = new ArrayList<>(); // Initialize the list to hold IDs
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emailList);
        listView.setAdapter(adapter);

        loadEmailsFromDatabase();

        inputBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                if (!email.isEmpty()) {
                    saveEmailToDatabase(email);
                } else {
                    Toast.makeText(admin_mail.this, "Please enter an email", Toast.LENGTH_SHORT).show();
                }
            }
        });

        urlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(admin_mail.this, admin_url.class);
                startActivity(intent);
            }
        });

        // Set an item click listener to handle deletions
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmailId = emailIds.get(position);
            showDeleteConfirmationDialog(selectedEmailId, position);
        });
    }

    private void saveEmailToDatabase(String email) {
        String emailId = db.push().getKey();
        if (emailId != null) {
            Email emailObj = new Email(email);
            db.child(emailId).setValue(emailObj).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(admin_mail.this, "Email saved successfully", Toast.LENGTH_SHORT).show();
                    emailList.add(email);
                    emailIds.add(emailId); // Store the email ID
                    adapter.notifyDataSetChanged();
                    emailInput.setText("");
                } else {
                    Toast.makeText(admin_mail.this, "Failed to save email", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseError", "Error saving email", task.getException());
                }
            });
        }
    }

    private void loadEmailsFromDatabase() {
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                emailList.clear();
                emailIds.clear(); // Clear the list of IDs
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Email email = postSnapshot.getValue(Email.class);
                    if (email != null) {
                        emailList.add(email.getEmail());
                        emailIds.add(postSnapshot.getKey()); // Store the email ID
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Error loading emails", databaseError.toException());
            }
        });
    }

    private void showDeleteConfirmationDialog(String emailId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Email")
                .setMessage("Are you sure you want to delete this email?")
                .setPositiveButton("Yes", (dialog, which) -> deleteEmail(emailId, position))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteEmail(String emailId, int position) {
        db.child(emailId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(admin_mail.this, "Email deleted successfully", Toast.LENGTH_SHORT).show();
                emailList.remove(position);
                emailIds.remove(position); // Remove the corresponding ID
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(admin_mail.this, "Failed to delete email", Toast.LENGTH_SHORT).show();
                Log.e("FirebaseError", "Error deleting email", task.getException());
            }
        });
    }
}
