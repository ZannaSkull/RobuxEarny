/*
 * Created by FakeException on 8/5/23, 11:58 AM
 * Copyright (c) 2023. All rights reserved.
 * Last modified 8/5/23, 11:58 AM
 */

package com.robuxearny.official.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.robuxearny.official.R;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class TicketActivity extends BaseActivity {
    private AdManagerInterstitialAd interstitialAd;
    private Set<Button> scratchedBlocks;

    private int totalPoints;
    private TextView totalPointsTextView;
    private Set<Integer> winningNumbers;
    private String uid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            uid = currentUser.getUid();
        }

        loadAd();

        this.totalPointsTextView = findViewById(R.id.totalPointsTextView);

        Button confirmButton = findViewById(R.id.confirmButton);

        initializeGame();

        final Button block1 = findViewById(R.id.block1);
        final Button block2 = findViewById(R.id.block2);
        final Button block3 = findViewById(R.id.block3);
        final Button block4 = findViewById(R.id.block4);
        final Button block5 = findViewById(R.id.block5);
        final Button block6 = findViewById(R.id.block6);
        final Button block7 = findViewById(R.id.block7);
        final Button block8 = findViewById(R.id.block8);
        final Button block9 = findViewById(R.id.block9);

        AdView adView = findViewById(R.id.adView);
        AdView adView2 = findViewById(R.id.adView2);
        AdView adView3 = findViewById(R.id.adView3);
        AdView adView4 = findViewById(R.id.adView4);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView2.loadAd(adRequest);
        adView3.loadAd(adRequest);
        adView4.loadAd(adRequest);

        this.scratchedBlocks = new HashSet<>();

        block1.setOnClickListener(view -> processBlockClick(block1));

        block2.setOnClickListener(view -> processBlockClick(block2));

        block3.setOnClickListener(view -> processBlockClick(block3));

        block4.setOnClickListener(view -> processBlockClick(block4));

        block5.setOnClickListener(view -> processBlockClick(block5));

        block6.setOnClickListener(view -> processBlockClick(block6));

        block7.setOnClickListener(view -> processBlockClick(block7));

        block8.setOnClickListener(view -> processBlockClick(block8));

        block9.setOnClickListener(view -> processBlockClick(block9));

        confirmButton.setOnClickListener(view -> {
            try {
                confirmTicket();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        updateWinningNumbersTextView();
    }

    private void initializeGame() {
        this.winningNumbers = generateWinningNumbers();
        getCurrentUserCoins(uid);
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

    private void getCurrentUserCoins(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("Coins", "Current UID: " + uid);

        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Map<String, Object> data = document.getData();
                    Log.d("Coins", "Document Data: " + data);

                    Long coinsLong = document.getLong("coins");
                    if (coinsLong != null) {
                        long coins = coinsLong;
                        this.totalPoints = (int) coins;
                        updateTotalPointsTextView();
                        Log.d("Coins", "Current User Coins: " + coins);
                    } else {
                        this.totalPoints = 0;
                        Log.d("Coins", "Coins field does not exist in the document.");
                    }
                } else {
                    this.totalPoints = 0;
                    Log.d("Coins", "Document does not exist");
                }
            } else {
                this.totalPoints = 0;
                Log.d("Coins", "Error fetching document: " + task.getException());
            }
        });
    }


    private Set<Integer> generateWinningNumbers() {
        Set<Integer> numbers = new HashSet<>();
        Random random = new Random();
        while (numbers.size() < 3) {
            int number = random.nextInt(9) + 1;
            numbers.add(number);
        }
        return numbers;
    }

    private void processBlockClick(Button block) {
        if (scratchedBlocks.contains(block)) {
            return;
        }

        int randomNumber = generateRandomNumber();
        block.setText(String.valueOf(randomNumber));
        block.setTextColor(Color.BLACK);

        if (this.winningNumbers.contains(randomNumber)) {
            int points = generateRandomPoints();
            this.totalPoints += points;
        }

        this.scratchedBlocks.add(block);
        updateTotalPointsTextView();
    }

    private int generateRandomNumber() {
        Random random = new Random();
        return random.nextInt(9) + 1;
    }

    private int generateRandomPoints() {
        Random random = new Random();
        return random.nextInt(11) + 1;
    }
    private void updateWinningNumbersTextView() {
        TextView winningNumbersTextView = findViewById(R.id.winningNumbersTextView);
        StringBuilder numbersBuilder = new StringBuilder();
        for (Integer num : this.winningNumbers) {
            int number = num;
            numbersBuilder.append(number).append(" ");
        }
        String numbers = getString(R.string.winning_numbers, numbersBuilder.toString());
        winningNumbersTextView.setText(numbers);
    }

    private void updateTotalPointsTextView() {
        this.totalPointsTextView.setText(getString(R.string.total_points, this.totalPoints));
    }

    private void confirmTicket() throws ExecutionException, InterruptedException {
        if (this.scratchedBlocks.size() == 9) {
            this.scratchedBlocks.clear();
            showInterstitial();
            for (int i = 1; i <= 9; i++) {
                Button block = findViewById(getResources().getIdentifier("block" + i, "id", getPackageName()));
                block.setText(getString(R.string.x));
                block.setTextColor(Color.WHITE);
            }
            this.winningNumbers = generateWinningNumbers();
            updateWinningNumbersTextView();
            updateCoins(uid, totalPoints);
            return;
        }
        Toast.makeText(this, getString(R.string.ticket_finish), Toast.LENGTH_LONG).show();
    }

    public void loadAd() {
        AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
        AdManagerInterstitialAd.load(this, "ca-app-pub-6202710455352099/5121141458", adRequest, new AdManagerInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AdManagerInterstitialAd interstitialAd) {
                TicketActivity.this.interstitialAd = interstitialAd;
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        TicketActivity.this.interstitialAd = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Log.d("TAG", "The ad failed to show.");
                        TicketActivity.this.interstitialAd = null;
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d("TAG", "The ad was shown.");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                TicketActivity.this.interstitialAd = null;
            }
        });
    }

    private void showInterstitial() {
        AdManagerInterstitialAd adManagerInterstitialAd = this.interstitialAd;
        if (adManagerInterstitialAd != null) {
            adManagerInterstitialAd.show(this);
        } else {
            loadAd();
        }
    }
}