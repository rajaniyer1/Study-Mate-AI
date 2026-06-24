package com.example.digitalbinder;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// ML Kit Imports
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private android.net.Uri highResImageUri;
    private List<Assignment> masterList = new ArrayList<>();

    // UI Containers
    private View viewBinder, viewCheatSheet, viewWhiteboard, viewAnalytics;
    private RecyclerView recyclerBinder, recyclerDeck;
    private DrawingView drawingCanvas;
    private TextView textCheatSheet, textStats;

    // Adapters
    private BinderAdapter binderAdapter;
    private DeckAdapter deckAdapter;

    // --- HIGH-RES CAMERA LAUNCHER ---
    private final ActivityResultLauncher<android.net.Uri> takeHighResPictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && highResImageUri != null) {
                    Toast.makeText(this, "Running NLP on High-Res Image...", Toast.LENGTH_SHORT).show();
                    try {
                        Bitmap bitmap;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            bitmap = android.graphics.ImageDecoder.decodeBitmap(
                                    android.graphics.ImageDecoder.createSource(getContentResolver(), highResImageUri));
                        } else {
                            bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), highResImageUri);
                        }

                        // Convert hardware bitmap to software bitmap for ML Kit
                        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        runAIProcessing(bitmap);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load high-res image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        viewBinder = findViewById(R.id.viewBinder);
        recyclerDeck = findViewById(R.id.recyclerDeck);
        viewCheatSheet = findViewById(R.id.viewCheatSheet);
        viewWhiteboard = findViewById(R.id.viewWhiteboard);
        viewAnalytics = findViewById(R.id.viewAnalytics);
        textCheatSheet = findViewById(R.id.textCheatSheet);
        textStats = findViewById(R.id.textStats);
        drawingCanvas = findViewById(R.id.drawingCanvas);

        // Setup Tab 1: Binder
        recyclerBinder = findViewById(R.id.recyclerBinder);
        recyclerBinder.setLayoutManager(new GridLayoutManager(this, 2));
        binderAdapter = new BinderAdapter();
        recyclerBinder.setAdapter(binderAdapter);

        // Setup Tab 2: Deck (Horizontal Pager)
        recyclerDeck.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        deckAdapter = new DeckAdapter();
        recyclerDeck.setAdapter(deckAdapter);
        new PagerSnapHelper().attachToRecyclerView(recyclerDeck);

        // Buttons
        findViewById(R.id.btnSnapNote).setOnClickListener(v -> {
            highResImageUri = createTempImageUri();
            takeHighResPictureLauncher.launch(highResImageUri);
        });

        findViewById(R.id.btnClearCanvas).setOnClickListener(v -> drawingCanvas.clearCanvas());

        setupTabs();
        fetchData();
    }

    // --- SECURE FILE PROVIDER BRIDGE ---
    private android.net.Uri createTempImageUri() {
        java.io.File imagePath = new java.io.File(getCacheDir(), "images");
        imagePath.mkdirs();
        java.io.File newFile = new java.io.File(imagePath, "scan_" + System.currentTimeMillis() + ".jpg");
        return androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", newFile);
    }

    // --- TAB NAVIGATION ---
    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewBinder.setVisibility(View.GONE);
                recyclerDeck.setVisibility(View.GONE);
                viewCheatSheet.setVisibility(View.GONE);
                viewWhiteboard.setVisibility(View.GONE);
                viewAnalytics.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0: viewBinder.setVisibility(View.VISIBLE); break;
                    case 1: recyclerDeck.setVisibility(View.VISIBLE); filterDeckByLeitner(); break;
                    case 2: viewCheatSheet.setVisibility(View.VISIBLE); generateCheatSheet(); break;
                    case 3: viewWhiteboard.setVisibility(View.VISIBLE); break;
                    case 4: viewAnalytics.setVisibility(View.VISIBLE); calculateAnalytics(); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // --- DIALOG BOX FOR USER INPUT ---
    private void showSaveDialog(String imageBase64, String extractedText, String summary) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Save Flashcard");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final android.widget.EditText titleBox = new android.widget.EditText(this);
        titleBox.setHint("Enter Title (e.g. Mitosis)");
        layout.addView(titleBox);

        final android.widget.EditText subjectBox = new android.widget.EditText(this);
        subjectBox.setHint("Enter Subject (e.g. Biology)");
        layout.addView(subjectBox);

        builder.setView(layout);

        builder.setPositiveButton("Save to Binder", (dialog, which) -> {
            String userTitle = titleBox.getText().toString();
            String userSubject = subjectBox.getText().toString();

            if (userTitle.isEmpty()) userTitle = "Untitled Note";
            if (userSubject.isEmpty()) userSubject = "General";

            Assignment assignment = new Assignment(userTitle, userSubject, imageBase64, extractedText, summary);
            db.collection("assignments").add(assignment).addOnSuccessListener(doc -> fetchData());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- LEVEL 9: NLP TOPIC SUMMARY ENGINE ---
    private String generateTopicSummary(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) return "No summary available.";

        String[] stopWordsArray = {"the", "and", "is", "in", "to", "of", "a", "it", "for", "on",
                "this", "that", "with", "as", "if", "by", "an", "be", "or", "are", "from", "at"};
        HashSet<String> stopWords = new HashSet<>(Arrays.asList(stopWordsArray));

        String cleanText = ocrText.replaceAll("[^a-zA-Z ]", " ").toLowerCase();
        String[] words = cleanText.split("\\s+");

        HashMap<String, Integer> wordCounts = new HashMap<>();
        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }

        PriorityQueue<Map.Entry<String, Integer>> maxHeap = new PriorityQueue<>(
                (a, b) -> b.getValue() - a.getValue()
        );
        maxHeap.addAll(wordCounts.entrySet());

        StringBuilder summary = new StringBuilder("Key Terms: ");
        int count = 0;
        while (!maxHeap.isEmpty() && count < 5) {
            summary.append(maxHeap.poll().getKey());
            count++;
            if (count < 5 && !maxHeap.isEmpty()) summary.append(", ");
        }
        return summary.toString();
    }

    // --- ML KIT OCR ENGINE (WITH REGEX CLEANER) ---
    private void runAIProcessing(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();

                    // THE CLEANER: Strips out weird random line breaks and double spaces
                    String extractedText = rawText.replaceAll("(\\r|\\n)+", " ").replaceAll("\\s+", " ").trim();

                    String summary = generateTopicSummary(extractedText);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                    showSaveDialog(imageBase64, extractedText, summary);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "AI Engine Failed to read image", Toast.LENGTH_SHORT).show();
                });
    }

    // --- FIREBASE CLOUD SYNC ---
    private void fetchData() {
        db.collection("assignments").get().addOnSuccessListener(snapshots -> {
            masterList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Assignment a = doc.toObject(Assignment.class);
                a.setId(doc.getId());
                masterList.add(a);
            }

            binderAdapter.setList(masterList);
            deckAdapter.setList(masterList);

            Toast.makeText(this, "Binder synced: " + masterList.size() + " notes", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("BinderApp", "Database Error: ", e);
        });
    }

    // --- TAB 2: LEITNER ALGORITHM ---
    // --- TAB 2: LEITNER ALGORITHM ---
    private void filterDeckByLeitner() {
        long today = System.currentTimeMillis();
        List<Assignment> dueCards = new ArrayList<>();

        // DEVELOPER OVERRIDE: Change this to 'true' if you want to force-load all cards while testing.
        // Change it back to 'false' when you are ready to publish the app to Google Play.
        boolean isTestingMode = true;

        for (Assignment a : masterList) {
            if (isTestingMode || a.getNextReviewDate() <= today) {
                dueCards.add(a);
            }
        }

        deckAdapter.setList(dueCards);

        // Visual feedback so the user knows WHY the screen is blank
        if (dueCards.isEmpty()) {
            Toast.makeText(this, "🎉 You're all caught up for today!", Toast.LENGTH_LONG).show();
        }
    }
    // --- TAB 2: LEITNER ALGORITHM (PUNISHING MODE) ---
    private void processLeitnerAnswer(Assignment a, boolean knewIt) {
        if (knewIt) {
            // They knew it. Level up and push to the future.
            a.setMasteryLevel(a.getMasteryLevel() + 1);
            long intervalDays = (long) Math.pow(2, a.getMasteryLevel() - 1);
            long nextReview = System.currentTimeMillis() + (intervalDays * 24 * 60 * 60 * 1000);
            a.setNextReviewDate(nextReview);
            Toast.makeText(this, "Nice! Reviewing in " + intervalDays + " days.", Toast.LENGTH_SHORT).show();
        } else {
            // They forgot it. Drop to level 1, keep it due TODAY, and move it to the back.
            a.setMasteryLevel(1);
            a.setNextReviewDate(System.currentTimeMillis() - 1000); // Set timestamp to the past so it stays due
            Toast.makeText(this, "Sent to the back of the pile.", Toast.LENGTH_SHORT).show();

            // Physically move the card to the back of the master list
            masterList.remove(a);
            masterList.add(a);
        }

        // Save the update to Firebase
        db.collection("assignments").document(a.getId()).set(a);

        // Refresh the deck. If they forgot it, it will still be there, just at the end!
        filterDeckByLeitner();
    }

    // --- TAB 3: CHEAT SHEET GENERATOR (TRUNCATED) ---
    private void generateCheatSheet() {
        StringBuilder html = new StringBuilder();

        for (Assignment a : masterList) {
            html.append("<h2 style=\"color:#00E5FF;\">").append(a.getTitle()).append("</h2>");
            html.append("<h4 style=\"color:#808B96;\">Subject: ").append(a.getSubject()).append("</h4>");
            html.append("<b>").append(a.getTopicSummary()).append("</b><br><br>");

            String bodyText = (a.getExtractedText() != null && !a.getExtractedText().trim().isEmpty())
                    ? a.getExtractedText()
                    : "[No readable text found in image]";

            // THE CHOP: Limit the cheat sheet raw text to 150 characters
            if (bodyText.length() > 150) {
                bodyText = bodyText.substring(0, 150) + "...";
            }

            html.append("<i>").append(bodyText).append("</i><br><br>");
            html.append("<hr><br>");
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textCheatSheet.setText(android.text.Html.fromHtml(html.toString(), android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            textCheatSheet.setText(android.text.Html.fromHtml(html.toString()));
        }
    }

    // --- TAB 5: ANALYTICS DASHBOARD ---
    private void calculateAnalytics() {
        int totalWords = 0;
        int totalMastery = 0;
        for (Assignment a : masterList) {
            if (a.getExtractedText() != null) {
                totalWords += a.getExtractedText().split("\\s+").length;
            }
            totalMastery += a.getMasteryLevel();
        }
        double avgMastery = masterList.isEmpty() ? 0 : (double) totalMastery / masterList.size();

        String stats = "📚 Total Notes: " + masterList.size() + "\n\n" +
                "🧠 AI Words Scanned: " + totalWords + "\n\n" +
                "🔥 Avg Deck Mastery Level: " + String.format("%.1f", avgMastery);
        textStats.setText(stats);
    }

    // ==========================================
    // RECYCLER VIEW ADAPTERS (INNER CLASSES)
    // ==========================================

    class BinderAdapter extends RecyclerView.Adapter<BinderAdapter.ViewHolder> {
        private List<Assignment> list = new ArrayList<>();
        public void setList(List<Assignment> list) { this.list = list; notifyDataSetChanged(); }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_binder, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Assignment a = list.get(position);
            holder.textTitle.setText(a.getTitle());
            holder.textSubject.setText(a.getSubject());

            if (a.getImageBase64() != null && !a.getImageBase64().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(a.getImageBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.imgPhoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.imgPhoto.setBackgroundColor(android.graphics.Color.DKGRAY);
                }
            }
        }
        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView imgPhoto;
            TextView textTitle, textSubject;
            ViewHolder(View v) {
                super(v);
                imgPhoto = v.findViewById(R.id.imgBinderPhoto);
                textTitle = v.findViewById(R.id.textBinderTitle);
                textSubject = v.findViewById(R.id.textBinderSubject);
            }
        }
    }

    class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.ViewHolder> {
        private List<Assignment> list = new ArrayList<>();
        public void setList(List<Assignment> list) { this.list = list; notifyDataSetChanged(); }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flashcard, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Assignment a = list.get(position);
            holder.textFrontSubject.setText(a.getSubject());
            holder.textFrontTitle.setText(a.getTitle());
            holder.textBackSummary.setText(a.getTopicSummary());

            // THE CHOP: Limit the flashcard back text to 100 characters max
            String backFull = a.getExtractedText();
            if (backFull != null && backFull.length() > 100) {
                backFull = backFull.substring(0, 100) + "...";
            }
            holder.textBackFull.setText(backFull);

            // Reset Card State (NOW WITH VISIBILITY TOGGLES)
            holder.cardFront.setVisibility(View.VISIBLE);
            holder.cardFront.setAlpha(1f);
            holder.cardFront.setRotationY(0f);

            holder.cardBack.setVisibility(View.GONE);
            holder.cardBack.setAlpha(0f);
            holder.cardBack.setRotationY(-180f);

            // 3D Flip Animation Logic
            holder.cardFront.setOnClickListener(v -> {
                holder.cardFront.animate().rotationY(180f).setDuration(300).withEndAction(() -> {
                    holder.cardFront.setAlpha(0f);
                    holder.cardFront.setVisibility(View.GONE); // THE FIX: Remove the invisible wall
                }).start();

                holder.cardBack.setVisibility(View.VISIBLE);
                holder.cardBack.setAlpha(1f);
                holder.cardBack.animate().rotationY(0f).setDuration(300).start();
            });

            // Leitner Buttons
            holder.btnKnew.setOnClickListener(v -> processLeitnerAnswer(a, true));
            holder.btnForgot.setOnClickListener(v -> processLeitnerAnswer(a, false));
        }
        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            View cardFront, cardBack;
            TextView textFrontSubject, textFrontTitle, textBackSummary, textBackFull;
            Button btnKnew, btnForgot;

            ViewHolder(View v) {
                super(v);
                cardFront = v.findViewById(R.id.cardFront);
                cardBack = v.findViewById(R.id.cardBack);
                textFrontSubject = v.findViewById(R.id.textFrontSubject);
                textFrontTitle = v.findViewById(R.id.textFrontTitle);
                textBackSummary = v.findViewById(R.id.textBackSummary);
                textBackFull = v.findViewById(R.id.textBackFull);
                btnKnew = v.findViewById(R.id.btnKnew);
                btnForgot = v.findViewById(R.id.btnForgot);
            }
        }
    }
}