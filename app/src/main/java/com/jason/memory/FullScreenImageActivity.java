package com.jason.memory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;

public class FullScreenImageActivity extends AppCompatActivity {

    private static final String TAG = "FullScreenImageActivity";
    private ViewPager2 viewPager;
    private ArrayList<String> imageUrls;
    private int currentPosition;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1.0f;
    private boolean isProfileImage;
    private TextView tvImageCount;
    private View mControlsView;
    private boolean mVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        viewPager = findViewById(R.id.viewPager);
        tvImageCount = findViewById(R.id.tvImageCount);
        mControlsView = findViewById(R.id.bottomControls);

        String singleImageUrl = getIntent().getStringExtra("IMAGE_URL");
        ArrayList<String> multipleImageUrls = getIntent().getStringArrayListExtra("IMAGE_URLS");

        isProfileImage = getIntent().getBooleanExtra("IS_PROFILE_IMAGE", false);
        currentPosition = getIntent().getIntExtra("POSITION", 0);

        if (multipleImageUrls != null && !multipleImageUrls.isEmpty()) {
            imageUrls = multipleImageUrls;
            Log.d(TAG, "--m-- Received multiple images: " + imageUrls.size());
        } else if (singleImageUrl != null) {
            imageUrls = new ArrayList<>();
            imageUrls.add(singleImageUrl);
            Log.d(TAG, "--m-- Received single image: " + singleImageUrl);
        } else {
            Log.e(TAG, "--m-- No images received");
            finish();
            return;
        }

        FullScreenImageAdapter adapter = new FullScreenImageAdapter(this, imageUrls, isProfileImage);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        updateImageCountText();

        Log.d(TAG, "--m-- Setting up ViewPager with " + imageUrls.size() + " images, current position: " + currentPosition);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureListener());

        viewPager.setUserInputEnabled(!isProfileImage && imageUrls.size() > 1);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                updateImageCountText();
                Log.d(TAG, "--m-- Page changed to position: " + position);
            }
        });

        mVisible = true;
        mControlsView.setOnClickListener(view -> toggle());
    }

    private void updateImageCountText() {
        String countText = (currentPosition + 1) + "/" + imageUrls.size();
        tvImageCount.setText(countText);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        mControlsView.setVisibility(View.GONE);
        tvImageCount.setVisibility(View.GONE);
        mVisible = false;
    }

    private void show() {
        mControlsView.setVisibility(View.VISIBLE);
        tvImageCount.setVisibility(View.VISIBLE);
        mVisible = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
            ImageView imageView = viewPager.findViewWithTag("image_" + currentPosition);
            if (imageView != null) {
                imageView.setScaleX(scaleFactor);
                imageView.setScaleY(scaleFactor);
                Log.d(TAG, "--m-- Image scaled: " + scaleFactor);
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!isProfileImage && imageUrls.size() > 1) {
                if (velocityX > 0 && currentPosition > 0) {
                    viewPager.setCurrentItem(currentPosition - 1, true);
                    Log.d(TAG, "--m-- Swiped to previous image");
                } else if (velocityX < 0 && currentPosition < imageUrls.size() - 1) {
                    viewPager.setCurrentItem(currentPosition + 1, true);
                    Log.d(TAG, "--m-- Swiped to next image");
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            toggle();
            return true;
        }
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        super.finish();
    }
}