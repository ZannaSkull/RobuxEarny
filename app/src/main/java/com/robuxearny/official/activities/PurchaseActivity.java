/*
 * Created by FakeException on 8/5/23, 11:58 AM
 * Copyright (c) 2023. All rights reserved.
 * Last modified 8/5/23, 11:58 AM
 */

package com.robuxearny.official.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.robuxearny.official.R;

import java.util.HashMap;
import java.util.Map;

public class PurchaseActivity extends BaseActivity {

    private int cost;
    private int coins;
    private int robux;
    private EditText gamepass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);
        TextView guide = findViewById(R.id.guide);
        gamepass = findViewById(R.id.gamepass);

        cost = getIntent().getIntExtra("price", 0);
        coins = getIntent().getIntExtra("coins", 0);

        robux = Math.round(getIntent().getIntExtra("robux", 0) / 0.70F);

        guide.setText(getString(R.string.guide, robux));
    }

    public void purchase(View view) {
        if (!gamepass.getText().toString().isEmpty()) {
            showConfirmation(this);
        }
    }

    private void showConfirmation(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.confirmation));
        builder.setMessage(getString(R.string.confirm_desc));
        builder.setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFunctions functions = FirebaseFunctions.getInstance();

                Map<String, Object> data = new HashMap<>();
                data.put("messageContent", "Email: " + user.getEmail() + " Robux: " + robux + " Gamepass: " + gamepass.getText());

                functions
                        .getHttpsCallable("redeem")
                        .call(data)
                        .addOnSuccessListener(result -> {
                            // Handle success
                            Toast.makeText(this, "Request sent! You will receive your Robux soon.", Toast.LENGTH_LONG).show();

                            updateCoins(user.getUid(), coins - cost);
                            Intent menu = new Intent(this, MainMenuActivity.class);
                            startActivity(menu);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // Handle failure
                            Log.e("Cloud Function", "Error: " + e.getMessage());
                        });
            }
        });
        builder.setNegativeButton("No", ((dialogInterface, i) -> dialogInterface.cancel()));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void updateCoins(String uid, int newCoins) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {

                    userRef.update("coins", newCoins).addOnSuccessListener(obj -> {
                        Log.d("Coins", "Coins updated");
                    }).addOnFailureListener(exc -> {
                        Log.d("Coins", exc.getMessage());
                    });
                }
            }

        });

    }
}