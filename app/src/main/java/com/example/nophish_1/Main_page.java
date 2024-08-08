    package com.example.nophish_1;

    import androidx.appcompat.app.AppCompatActivity;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.StrictMode;
    import android.widget.Button;
    import android.widget.Switch;
    import android.widget.TextView;

    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;

    import java.io.IOException;
    import java.util.Properties;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    import javax.mail.Folder;
    import javax.mail.Message;
    import javax.mail.MessagingException;
    import javax.mail.Multipart;
    import javax.mail.Part;
    import javax.mail.Session;
    import javax.mail.Store;

    import org.jsoup.Jsoup;

    public class Main_page extends AppCompatActivity {
        private TextView textView;
        private Switch emailSwitch;
        private String stringMailHost = "imap.gmail.com";
        private Session session;
        private Store store;
        private Properties properties;
        private String stringUserName = "cspnophish";
        private String stringPassword = "fxqrmpoksqenokhp";
        private Handler handler = new Handler();
        private Runnable emailChecker;
        private final long CHECK_INTERVAL = 3000; // 3 seconds
        private String lastEmailId = null;
        private SharedPreferences sharedPreferences;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main_page);

            textView = findViewById(R.id.textView2);
            emailSwitch = findViewById(R.id.switch1);
            TextView urlPage = findViewById(R.id.UrlPage);

            properties = new Properties();
            properties.setProperty("mail.store.protocol", "imaps");

            sharedPreferences = getSharedPreferences("Main_page", MODE_PRIVATE);
            boolean switchState = sharedPreferences.getBoolean("emailSwitchState", false);
            emailSwitch.setChecked(switchState);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            // Connect to the mail server and start checking emails if the switch is on
            connectToMailServer(() -> {
                if (switchState) {
                    handler.post(emailChecker); // Start checking emails if the switch was on
                }
            });

            emailChecker = new Runnable() {
                @Override
                public void run() {
                    if (emailSwitch.isChecked()) {
                        readGmail();
                        handler.postDelayed(this, CHECK_INTERVAL);
                    }
                }
            };

            emailSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("emailSwitchState", isChecked);
                editor.apply();

                if (isChecked) {
                    handler.post(emailChecker); // Start checking emails
                } else {
                    handler.removeCallbacks(emailChecker); // Stop checking emails
                }
            });

            urlPage.setOnClickListener(v -> {
                Intent intent = new Intent(Main_page.this, url_page.class);
                startActivity(intent);
            });

            Button button1 = findViewById(R.id.BTN1);
            button1.setOnClickListener(v -> {
                Intent intent = new Intent(Main_page.this, Notification.class);
                startActivity(intent);
            });
        }

        // Method to connect to the mail server
        private void connectToMailServer(final Runnable onSuccess) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    session = Session.getDefaultInstance(properties, null);
                    store = session.getStore("imaps");
                    store.connect(stringMailHost, stringUserName, stringPassword);

                    runOnUiThread(() -> {
                        textView.setText("Connection successful");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    });
                } catch (MessagingException e) {
                    runOnUiThread(() -> textView.setText("MessagingException: " + e.getMessage()));
                }
            });
        }

        // read emails from the Gmail inbox
        public synchronized void readGmail() {
            if (store == null || !store.isConnected()) {
                connectToMailServer(this::readGmailInternal); // Retry reading Gmail after successful connection
            } else {
                readGmailInternal();
            }
        }

        // Internal method to read emails from the Gmail inbox
        private void readGmailInternal() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Folder folder = store.getFolder("Inbox");
                    folder.open(Folder.READ_ONLY);
                    Message[] messages = folder.getMessages();
                    if (messages.length > 0) {
                        Message latestMessage = messages[messages.length - 1];

                        String emailId = latestMessage.getHeader("Message-ID")[0];
                        if (!emailId.equals(lastEmailId)) {
                            lastEmailId = emailId;

                            String from = latestMessage.getFrom()[0].toString();
                            String content = getTextFromMessage(latestMessage);
                            String plainTextContent = convertHtmlToPlainText(content);
                            fetchTrustedEmailsAndProcess(from, latestMessage, plainTextContent);
                        }
                    } else {
                        runOnUiThread(() -> textView.setText("No messages in the inbox."));
                    }
                } catch (MessagingException | IOException e) {
                    runOnUiThread(() -> textView.setText("Exception: " + e.getMessage()));
                }
            });
        }

        // Method to fetch trusted email addresses from Firebase and process the received email
        private void fetchTrustedEmailsAndProcess(String from, Message message, String plainTextContent) {
            String normalizedFrom = normalizeEmailAddress(from);

            DatabaseReference db = FirebaseDatabase.getInstance().getReference("emails");
            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean isTrusted = false;
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        Email email = postSnapshot.getValue(Email.class);
                        if (email != null && email.getEmail().equalsIgnoreCase(normalizedFrom)) {
                            isTrusted = true;
                            break;
                        }
                    }

                    String context = isTrusted ? "This email came from a trusted source" : "This email came from an unknown source";
                    processEmail(message, context, plainTextContent);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    runOnUiThread(() -> textView.setText("Error fetching trusted emails: " + databaseError.getMessage()));
                }
            });
        }

        // Method to normalize email
        private String normalizeEmailAddress(String from) {
            if (from.contains("<")) {
                return from.substring(from.indexOf("<") + 1, from.indexOf(">")).trim();
            } else {
                return from.trim();
            }
        }

        // Method to process the content and save it to the database
        private void processEmail(Message message, String context, String plainTextContent) {
            runOnUiThread(() -> {
                try {
                    String from = message.getFrom()[0].toString();
                    String subject = message.getSubject();

                    long timestamp = System.currentTimeMillis();
                    String truncatedContent = plainTextContent.length() > 30 ? plainTextContent.substring(0, 30) : plainTextContent;

                    // Extract and normalize the URL
                    String extractedUrl = extractAndNormalizeUrl(plainTextContent);
                    String normalizedUrl = normalizeUrl(extractedUrl);

                    // Determine the URL status
                    determineUrlStatus(normalizedUrl, urlStatus -> {
                        FetchedEmail fetchedEmail = new FetchedEmail(
                                normalizeEmailAddress(from), subject, plainTextContent, truncatedContent, timestamp, context, urlStatus, normalizedUrl
                        );
                        saveFetchedEmailToDatabase(fetchedEmail);
                    });
                } catch (MessagingException e) {
                    textView.setText("Exception: " + e.getMessage());
                }
            });
        }

        // Method to extract and normalize URLs from the email content
        private String extractAndNormalizeUrl(String content) {
            Pattern urlPattern = Pattern.compile(
                    "(https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = urlPattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        }

        // Method to normalize URLs (add 'https://' if missing and convert to lowercase)
        private String normalizeUrl(String url) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            return url.toLowerCase();
        }

        // Method to determine the URL status (probably safe or probably dangerous)
        private void determineUrlStatus(String normalizedUrl, final OnUrlStatusDeterminedListener listener) {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("urls");
            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean urlFound = false;
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        URL urlEntry = postSnapshot.getValue(URL.class);
                        if (urlEntry != null) {
                            String dbUrl = normalizeUrl(urlEntry.getUrl());

                            if (normalizedUrl.contains(extractKeyword(dbUrl))) {
                                urlFound = true;
                                break;
                            }
                        }
                    }
                    listener.onUrlStatusDetermined(urlFound ? "possibly safe" : "possibly dangerous");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    runOnUiThread(() -> textView.setText("Error fetching URLs: " + databaseError.getMessage()));
                }
            });
        }

        //extract the main keyword(will be used to compare)
        private String extractKeyword(String url) {
            Pattern keywordPattern = Pattern.compile("https?://(?:www\\.)?([\\w-]+)(?:\\.[\\w-]+)+");
            Matcher matcher = keywordPattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return url;
        }

    //fetch content email
        private String getTextFromMessage(Message message) throws MessagingException, IOException {
            String contentType = message.getContentType();
            if (message.isMimeType("text/plain")) {
                return message.getContent().toString();
            } else if (message.isMimeType("multipart/*")) {
                return getTextFromMultipart((Multipart) message.getContent());
            } else if (message.isMimeType("text/html")) {
                return message.getContent().toString();
            }
            return "Unsupported content type.";
        }

        private String getTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    result.append(part.getContent().toString());
                    break;
                } else if (part.isMimeType("multipart/*")) {
                    result.append(getTextFromMultipart((Multipart) part.getContent()));
                } else if (part.isMimeType("text/html")) {
                    result.append(part.getContent().toString());
                }
            }
            return result.toString();
        }

        // Method to save fetched email data to the Firebase database
        private void saveFetchedEmailToDatabase(FetchedEmail email) {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("fetchedEmails");
            String emailId = db.push().getKey();
            if (emailId != null) {
                db.child(emailId).setValue(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Successfully saved
                    } else {
                        textView.setText("Failed to save email to database.");
                    }
                });
            }
        }

        private String convertHtmlToPlainText(String html) {
            return Jsoup.parse(html).text();
        }

        private interface OnUrlStatusDeterminedListener {
            void onUrlStatusDetermined(String status);
        }
    }
