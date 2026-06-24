package com.example.digitalbinder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    public static Assignment currentAssignment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (currentAssignment == null) {
            Toast.makeText(this, "Error loading note.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView image = findViewById(R.id.detailImage);
        TextView title = findViewById(R.id.detailTitle);
        TextView subject = findViewById(R.id.detailSubject);
        TextView extractedText = findViewById(R.id.detailExtractedText);

        title.setText(currentAssignment.getTitle());
        subject.setText(currentAssignment.getSubject().toUpperCase());

        // Display the text the AI found!
        if (currentAssignment.getExtractedText() != null && !currentAssignment.getExtractedText().isEmpty()) {
            extractedText.setText(currentAssignment.getExtractedText());
        } else {
            extractedText.setText("No text detected in this document.");
        }

        try {
            byte[] decoded = Base64.decode(currentAssignment.getImageBase64(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            image.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentAssignment = null;
    }
}