package com.codebrush.maildetox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.api.client.auth.oauth2.Credential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

public class GoogleSignInActivity extends AppCompatActivity {
    String TAG = "GoogleSignInActivity";

    public static final int RC_SIGN_IN = 30232;
    private static final int RC_RECOVERABLE = 9002;

    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "app/src/main/assets/tokens";

    // Bundle key for account object
    private static final String KEY_ACCOUNT = "key_account";

    private GoogleSignInClient signInClient;
    private TextView mStatusTextView;

    // Global instance of the 1 transport
    //private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    // Global instance of the JSON factory
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private Account mAccount;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_google_signin);
        Log.d(TAG,"onCreate: ");
        //final HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getResources().getString(R.string.server_client_id))
                .requestEmail()
                .build();

        signInClient = GoogleSignIn.getClient(this, gso);

        mStatusTextView = findViewById(R.id.status);


        // Restore instance state
        if (savedInstanceState != null) {
            mAccount = savedInstanceState.getParcelable(KEY_ACCOUNT);
        }


        // [START customize_button]
        // Set the dimensions of the sign-in button.
        /*SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);*/
        // [END customize_button]

    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ACCOUNT,mAccount);
        //wv.saveState(outState);
    }

    @Override
    protected void onStart(){
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        Log.d("ONSTART", "Prepare check permission");

    }
    //[START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            System.out.println("On Activity Result");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    //[END onActivityResult]

    // [START handleSignInResult]
    public void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        System.out.println("On Handle Result");
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            System.out.println(account.getDisplayName());
            System.out.println(account.getEmail());

            // Signed in successfully, show authenticated UI.
            updateUI(account);

            // Store the account from the result
            mAccount = account.getAccount();
            //getGmails();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }
    // [END handleSignInResult]

    public void signIn(View view){
        System.out.println("Sign In");
        Intent signInIntent = signInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void signOut(View view){
        System.out.println("Sign out");
        signInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@Nonnull Task<Void> task) {
                        // [START_EXCLUDE]
                        Log.d("SignOut ", "go update UI");
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }

    public void revokeAccess(View view){
        signInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        Log.d("RevokeAccess ", "go update UI");
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }

    public void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            System.out.println("UPDATEUI : Account is NOT null");
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName()));
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            System.out.println("UPDATEUI : Account is null");
            mStatusTextView.setText(R.string.signed_out);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    private void getGmails()  {
        try{
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();

            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(getString(R.string.app_name))
                    .build();

            List<Message> listMessage = listMessagesMatchingQuery(service,"me","");
            for(int i=0; i<listMessage.size();i++) System.out.println(listMessage.get(i));

        }catch (IOException e){
            System.out.println("WRONG AT GET GMAILS");
            e.printStackTrace();
        }
    }

    protected void onConnectionsLoadFinished(@Nullable List<Message> gmails) {
        if (gmails == null) {
            Log.d(TAG, "getContacts:connections: null");
           // mDetailTextView.setText(getString(R.string.connections_fmt, "None"));
            return;
        }

        Log.d(TAG, "getContacts:connections: size=" + gmails.size());

        // Get names of all connections
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < gmails.size(); i++)  System.out.println(gmails.get(i));
    }

    protected void onRecoverableAuthException(UserRecoverableAuthIOException recoverableException) {
        Log.w(TAG, "onRecoverableAuthException", recoverableException);
        startActivityForResult(recoverableException.getIntent(), RC_RECOVERABLE);
    }

    private static List<Message> listMessagesMatchingQuery(Gmail service, String userId, String query) throws IOException{
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(userId).setQ(query)
                            .setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }

            for (Message message : messages) {
                System.out.println(message.toPrettyString());
            }

            return messages;
    }


    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = this.getAssets().open("credentials.json");

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
}