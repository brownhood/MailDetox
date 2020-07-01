package com.codebrush.maildetox;
import com.codebrush.maildetox.helper.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class GmailActivity extends AppCompatActivity {
    private String TAG = "GmailActivity";

    protected GoogleAccountCredential mCredential;
    protected Gmail mService = null;

    private static final String PREF_ACCOUNT_NAME = "accountName";

    private static final String[] SCOPES = {
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.MAIL_GOOGLE_COM
    };

    private InternetDetector internetDetector;

    private TextView mStatusTextView;


    // Global instance of the 1 transport
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    // Global instance of the JSON factory
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_signin);
        Log.d(TAG,"onCreate: ");

        init();

        mStatusTextView = findViewById(R.id.status);

    }

    private void init(){
        internetDetector = new InternetDetector(getApplicationContext());

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    //[START onActivityResult]
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utils.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Log.d(TAG,"This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;

            case Utils.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        updateUI(mCredential);
                        getResultsFromApi();
                    }
                }
                break;
            case Utils.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.REQUEST_PERMISSION_GET_ACCOUNTS:
                chooseAccount(null);
                break;
        }
    }
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount(null);
        } else if (!internetDetector.checkMobileInternetConn()) {
            Log.d(TAG, "No network connection available.");
        } else {
            new MakeRequestTask(this, mCredential).execute();
        }
    }

    // Method for Checking Google Play Service is Available
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    // Method to Show Info, If Google Play Service is Not Available.
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }
    // Method for Google Play Services Error Info
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                GmailActivity.this,
                connectionStatusCode,
                Utils.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    // Storing Mail ID using Shared Preferences
    private void chooseAccount(View view) {
        if (Utils.checkPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                updateUI(mCredential);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), Utils.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            ActivityCompat.requestPermissions(GmailActivity.this,
                    new String[]{Manifest.permission.GET_ACCOUNTS}, Utils.REQUEST_PERMISSION_GET_ACCOUNTS);
        }
    }

    public void signIn(View view){
        Log.d("SIGN IN", "GO TO SIGN IN");
        chooseAccount(view);
    }

    public void signOut(View view){
        mCredential.setSelectedAccount(null);
        updateUI(mCredential);
    }


/*
    public void revokeAccess(View view){
        mCredential.
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
*/
    public void updateUI(@Nullable GoogleAccountCredential account) {
        if (account.getSelectedAccount() != null) {
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getSelectedAccountName()));
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, Message> {

        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        //private View view = sendFabButton;
        private GmailActivity activity;

        MakeRequestTask(GmailActivity activity, GoogleAccountCredential credential) {
            System.out.println("MakeRequestTask " + credential.getSelectedAccountName());

            mService = new com.google.api.services.gmail.Gmail.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(getResources().getString(R.string.app_name))
                    .build();

            this.activity = activity;
        }

        @Override
        protected Message doInBackground(Void... params) {
            try {
                //return getDataFromApi();
                System.out.println("Pre Going getGmail ");
                return getGmail();
            } catch (Exception e) {
                mLastError = e;
          //      cancel(true);
                return null;
            }
        }

        private Message getGmail() throws IOException{
            System.out.println("now We are getting gmail");
            Message message = null;
            try{
                message = mService.users().messages().get("me", "172ee0ad9d18e82f").execute();
                System.out.println("Run excute");
                System.out.println("Message snippet: " + message.getSnippet());
                System.out.println("HAHAHAA");
            }catch (UserRecoverableAuthIOException e){
                startActivityForResult(e.getIntent(), Utils.REQUEST_AUTHORIZATION);
            }catch (IOException e) {
                e.printStackTrace();
            }

            return message;
        }
/*
        private String getDataFromApi() throws IOException {
            // getting Values for to Address, from Address, Subject and Body
            String user = "me";
            String to = Utils.getString(edtToAddress);
            String from = mCredential.getSelectedAccountName();
            String subject = Utils.getString(edtSubject);
            String body = Utils.getString(edtMessage);
            MimeMessage mimeMessage;
            String response = "";
            try {
                mimeMessage = createEmail(to, from, subject, body);
                response = sendMessage(mService, user, mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return response;
        }

        // Method to send email
        private String sendMessage(Gmail service,
                                   String userId,
                                   MimeMessage email)
                throws MessagingException, IOException {
            Message message = createMessageWithEmail(email);
            // GMail's official method to send email with oauth2.0
            message = service.users().messages().send(userId, message).execute();

            System.out.println("Message id: " + message.getId());
            System.out.println(message.toPrettyString());
            return message.getId();
        }

        // Method to create email Params
        private MimeMessage createEmail(String to,
                                        String from,
                                        String subject,
                                        String bodyText) throws MessagingException {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            InternetAddress tAddress = new InternetAddress(to);
            InternetAddress fAddress = new InternetAddress(from);

            email.setFrom(fAddress);
            email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
            email.setSubject(subject);

            // Create Multipart object and add MimeBodyPart objects to this object
            Multipart multipart = new MimeMultipart();

            // Changed for adding attachment and text
            // email.setText(bodyText);

            BodyPart textBody = new MimeBodyPart();
            textBody.setText(bodyText);
            multipart.addBodyPart(textBody);

            if (!(activity.fileName.equals(""))) {
                // Create new MimeBodyPart object and set DataHandler object to this object
                MimeBodyPart attachmentBody = new MimeBodyPart();
                String filename = activity.fileName; // change accordingly
                DataSource source = new FileDataSource(filename);
                attachmentBody.setDataHandler(new DataHandler(source));
                attachmentBody.setFileName(filename);
                multipart.addBodyPart(attachmentBody);
            }

            //Set the multipart object to the message object
            email.setContent(multipart);
            return email;
        }

        private Message createMessageWithEmail(MimeMessage email)
                throws MessagingException, IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            email.writeTo(bytes);
            String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
            Message message = new Message();
            message.setRaw(encodedEmail);
            return message;
        }

*/
        @Override
        protected void onPostExecute(Message output) {
           // mProgress.hide();
            if (output == null ) {
                //showMessage(view, "No results returned.");
                Log.e(TAG, "No results returned");
            } else {
                //showMessage(view, output);
                //output.
                Log.v(TAG, "Done!!!!!!!!!!");
            }
        }


        @Override
        protected void onCancelled() {
           // mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Utils.REQUEST_AUTHORIZATION);
                } else {
                    //showMessage(view, "The following error occurred:\n" + mLastError);
                    Log.v("Error", mLastError + "");
                }
            } else {
                Log.e(TAG, "Request Cancelled.");
            }
        }
    }

}
