package com.codebrush.maildetox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    public static final int RC_SIGN_IN = 30232;
    GoogleSignInClient signInClient;
    String TAG = "signin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        signInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
    }

    private void signIn(){
        Intent signIn = signInClient.getSignInIntent();
        startActivityForResult(signIn, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSigninResult(task);
        }
    }

    private void handleSigninResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            updateUI(account);
        } catch(ApiException error) {
            Log.w("account", "signInResult: failed code = " + error.getStatusCode());
            updateUI(null);
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        if(account == null) {
            SignInButton button = findViewById(R.id.sign_in_button);
            button.setSize(SignInButton.SIZE_STANDARD);
            button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch(view.getId()){
                            case R.id.sign_in_button:
                                signIn();
                                break;
                        }
                    }
                });
        }
        else {
            Log.d(TAG, "updateUI: " + account.getEmail());
            Intent mail = new Intent(this, MailView.class);
            mail.putExtra("account", account);
            startActivity(mail);
        }
    }
}