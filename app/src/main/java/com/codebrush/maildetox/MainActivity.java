package com.codebrush.maildetox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String APPLICATION_NAME = "MailDetox";
    public static final int RC_SIGN_IN = 30232;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_LABELS);
    private static final String CREDENTIALS_FILE_PATH = "./credentials.json";

    public static Gmail service = null;
    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ");
        //updateUI(service);
    }

    public void googleSignIn(View view){
        //Intent intent = new Intent(this, GoogleSignInActivity.class);
        Intent intent = new Intent(this, GmailActivity.class);
        startActivity(intent);
    }
    /*
    private void _signIn() throws IOException, GeneralSecurityException {
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        Log.d(TAG, "signIn: ");
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = MainActivity.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }


    private void updateUI(Gmail account) {
        if (account == null) {
            try {
                _signIn();
            } catch (IOException e) {
                Log.e(TAG, "onClick: " + e.getLocalizedMessage());
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "onClick: " + e.getLocalizedMessage());
            }
        }

//            SignInButton button = findViewById(R.id.sign_in_button);
//            button.setSize(SignInButton.SIZE_STANDARD);
//            button.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        switch(view.getId()){
//                            case R.id.sign_in_button:
//                                try {
//                                    signIn();
//                                } catch(IOException e) {
//                                    Log.e(TAG, "onClick: " + e.getLocalizedMessage());
//                                } catch(GeneralSecurityException e) {
//                                    Log.e(TAG, "onClick: " + e.getLocalizedMessage());
//                                }
//                                break;
//                        }
//                    }
//                });
            else {
            Intent mail = new Intent(this, MailView.class);
            startActivity(mail);
        }
        }*/
    }
