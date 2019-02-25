package com.example.android.scaletypeexplorer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private ViewGroup mImageWrapper;
    private MyImageView mImageView;
    private ImageView mDirection;
    private AppCompatTextView mScaleType;

    private int mInitialViewWidth;
    private int mInitialViewHeight;
    private int mMaxViewWidth;
    private int mMaxViewHeight;

    private int mLockDirection = DIRECTIONAL_LOCK_NOT_SET;

    private int mTouchSlop;
    private float mLastX;
    private float mLastY;

    private boolean mIsOptionsSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        applyOptionsToImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            mIsOptionsSelected = true;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mIsOptionsSelected) {
            mIsOptionsSelected = false;
            applyOptionsToImage();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                mLockDirection = DIRECTIONAL_LOCK_NOT_SET;
                mLastX = event.getX();
                mLastY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - mLastX;
                float deltaY = event.getY() - mLastY;
                if (mLockDirection == DIRECTIONAL_LOCK_NOT_SET &&
                    (Math.abs(deltaX) >= mTouchSlop || Math.abs(deltaY) >= mTouchSlop)) {
                    if (Math.abs(deltaX) >= Math.abs(deltaY)) {
                        mLockDirection = DIRECTIONAL_LOCK_HORIZONTAL;
                        mDirection.setImageResource(R.drawable.ic_scale_width_black_24dp);
                        mDirection.setVisibility(View.VISIBLE);
                    } else {
                        mLockDirection = DIRECTIONAL_LOCK_VERTICAL;
                        mDirection.setImageResource(R.drawable.ic_scale_height_black_24dp);
                        mDirection.setVisibility(View.VISIBLE);
                    }
                }
                if (mLockDirection == DIRECTIONAL_LOCK_HORIZONTAL) {
                    changeViewSize(mImageWrapper, (int) deltaX, 0);
                    mLastX = event.getX();
                } else if (mLockDirection == DIRECTIONAL_LOCK_VERTICAL) {
                    changeViewSize(mImageWrapper, 0, (int) deltaY);
                    mLastY = event.getY();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDirection.setVisibility(View.INVISIBLE);
                break;

            default:
                break;
        }
        return true;
    }

    private void applyOptionsToImage() {
        // Reset the layout to the base.
        setContentView(R.layout.activity_main);

        mImageWrapper = findViewById(R.id.imageWrapper);
        mImageView = findViewById(R.id.imageView);
        mScaleType = findViewById(R.id.scaleType);
        mImageView.setOnTouchListener(this);
        mDirection = findViewById(R.id.direction);

        mDirection.setVisibility(View.INVISIBLE);

        // Deal with preferences.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String scaleType = sharedPref.getString(SettingsActivity.PREF_SCALE_TYPE,
                                                SettingsActivity.PREF_SCALE_TYPE_DEFAULT);
        if (scaleType.equals(SettingsActivity.PREF_SCALE_TYPE_MATRIX)) {
            String widthPct = sharedPref.getString(SettingsActivity.PREF_WIDTH_PERCENTAGE,
                                                   SettingsActivity.PREF_WIDTH_PERCENTAGE_DEFAULT);
            String heightPct = sharedPref.getString(SettingsActivity.PREF_HEIGHT_PERCENTAGE,
                                                    SettingsActivity.PREF_HEIGHT_PERCENTAGE_DEFAULT);
            mScaleType.setText(String.format("%s\n(%s%%, %s%%)", scaleType, widthPct, heightPct));
        } else {
            mScaleType.setText(scaleType);
        }

        ImageView.ScaleType scaleTypeEnum = ImageView.ScaleType.valueOf(scaleType);
        mImageView.setScaleType(scaleTypeEnum);

        View horizontalLine = findViewById(R.id.horizontalLine);
        View verticalLine = findViewById(R.id.verticalLine);
        PointF focalPoint = null;
        if (scaleTypeEnum == ImageView.ScaleType.MATRIX) {
            horizontalLine.setVisibility(View.VISIBLE);
            verticalLine.setVisibility(View.VISIBLE);
            String pref;
            focalPoint = new PointF();
            pref = sharedPref.getString(SettingsActivity.PREF_WIDTH_PERCENTAGE,
                                        SettingsActivity.PREF_WIDTH_PERCENTAGE_DEFAULT);
            focalPoint.x = Float.valueOf(pref) / 100f;
            pref = sharedPref.getString(SettingsActivity.PREF_HEIGHT_PERCENTAGE,
                                        SettingsActivity.PREF_HEIGHT_PERCENTAGE_DEFAULT);
            focalPoint.y = Float.valueOf(pref) / 100f;

            ConstraintLayout.LayoutParams lp;
            lp = (ConstraintLayout.LayoutParams) horizontalLine.getLayoutParams();
            lp.verticalBias = focalPoint.y;
            lp = (ConstraintLayout.LayoutParams) verticalLine.getLayoutParams();
            lp.horizontalBias = focalPoint.x;
        } else {
            horizontalLine.setVisibility(View.INVISIBLE);
            verticalLine.setVisibility(View.INVISIBLE);
        }
        mImageView.setFocalPoint(focalPoint);

        final ViewGroup layout = findViewById(R.id.layout);
        layout.post(new Runnable() {
            @Override
            public void run() {
                // Once layout is complete, capture key measurements and adjust the initial size
                // to fit 100% on the screen. Start with trying to fill a percentage of the width.
                mMaxViewWidth = (int) getUnpaddedWidth(layout);
                mMaxViewHeight = (int) getUnpaddedHeight(layout);

                mInitialViewWidth = (int) getUnpaddedWidth(mImageWrapper);
                mInitialViewHeight = (int) getUnpaddedHeight(mImageWrapper);

                float aspectRatio = (float) mImageView.getUnscaledWidth() / mImageView.getUnscaledHeight();

                float newWidth = mMaxViewWidth * INITIAL_IMAGE_PERCENTAGE_OF_WIDTH;
                float newHeight = newWidth / aspectRatio;
                if (newHeight > mMaxViewHeight) {
                    newHeight = mMaxViewHeight;
                    newWidth = newHeight * aspectRatio;
                }
                ViewGroup.LayoutParams lp = mImageWrapper.getLayoutParams();
                lp.width = (int) newWidth;
                lp.height = (int) newHeight;
                mImageWrapper.setLayoutParams(lp);

                // Let layout happen again and capture the actual values set although they
                // should not differ.
                layout.post(new Runnable() {
                    @Override
                    public void run() {
                        mInitialViewWidth = (int) getUnpaddedWidth(mImageWrapper);
                        mInitialViewHeight = (int) getUnpaddedHeight(mImageWrapper);
                    }
                });
            }
        });
    }

    private void changeViewSize(View view, int deltaW, int deltaH) {
        // ConstraintLayout doesn't restrict the view size to the size of the ConstraintLayout
        // while RelativeLayout and other ViewGroups do. Make sure the image size fits within
        // the ViewGroup and is not negative.
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = Math.min(mMaxViewWidth, Math.max(0, lp.width + deltaW));
        lp.height = Math.min(mMaxViewHeight, Math.max(0, lp.height + deltaH));
        view.setLayoutParams(lp);
    }

    private float getUnpaddedWidth(@NonNull View view) {
        return view.getWidth() - view.getPaddingStart() - view.getPaddingEnd();
    }

    private float getUnpaddedHeight(@NonNull View view) {
        return view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
    }

    public void resetSize(View view) {
        ViewGroup.LayoutParams lp = mImageWrapper.getLayoutParams();
        lp.width = mInitialViewWidth;
        lp.height = mInitialViewHeight;
        mImageWrapper.setLayoutParams(lp);
    }

    private static final int DIRECTIONAL_LOCK_NOT_SET = 0;
    private static final int DIRECTIONAL_LOCK_VERTICAL = 1;
    private static final int DIRECTIONAL_LOCK_HORIZONTAL = 2;

    private static final float INITIAL_IMAGE_PERCENTAGE_OF_WIDTH = 0.5f;
}
