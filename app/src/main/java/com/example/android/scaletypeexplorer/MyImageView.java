package com.example.android.scaletypeexplorer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class MyImageView extends AppCompatImageView {
    // For scale type = matrix, where the focal point is located.
    private PointF mFocalPoint = new PointF(0.5f, 0.5f);

    // The image we will show. This should be some kind of bitmap file: jpg, png, etc.
    private final int mImageRes;

    // The real size of the image before scaling occurs.
    private int mUnderlyingImageWidth;
    private int mUnderlyingImageHeight;

    // True is we have initialized.
    private boolean mIsInited;

    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        @SuppressLint("CustomViewStyleable") final TypedArray a = context.obtainStyledAttributes(
            attrs, R.styleable.AppCompatImageView, 0, 0);
        mImageRes = a.getResourceId(R.styleable.MyImageView_android_src, NO_ID);
        if (mImageRes == NO_ID) {
            throw new IllegalStateException("`src` drawable must be defined.");
        }
        a.recycle();
    }

    private void prepImage(Context context) {
        // Set up the bitmap for drawing.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), mImageRes, options);
        Canvas c = new Canvas(b);

        // Set up the paint.
        Paint p = new Paint();
         p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(STROKE_WIDTH); // Something large enough to show if scaled way down.

        // Draw our lines.
        float imageWidth = b.getWidth();
        float imageHeight = b.getHeight();

        // Draw line in horizontal and vertical centers.
        p.setColor(context.getResources().getColor(android.R.color.white));
        c.drawLine(imageWidth / 2, 0f, imageWidth / 2, imageHeight, p);
        c.drawLine(0f, imageHeight / 2, imageWidth, imageHeight / 2, p);

        // Draw the matrix lines if needed.
        if (getScaleType() == ScaleType.MATRIX && mFocalPoint != null) {
            p.setColor(context.getResources().getColor(android.R.color.holo_blue_bright));
            // Horizontal line
            c.drawLine(0f, imageHeight * mFocalPoint.y, imageWidth, imageHeight * mFocalPoint.y, p);
            // Vertical line
            c.drawLine(imageWidth * mFocalPoint.x, 0f, imageWidth * mFocalPoint.x, imageHeight, p);
        }
        setImageBitmap(b);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!mIsInited) {
            mIsInited = true;
            Bitmap b = ((BitmapDrawable) getDrawable()).getBitmap();
            mUnderlyingImageWidth = b.getWidth();
            mUnderlyingImageHeight = b.getHeight();
        }
        if (getScaleType() == ScaleType.MATRIX) {
            setImageMatrix(prepareMatrix(getUnpaddedViewWidth(), (getUnpaddedViewHeight()),
                                         mUnderlyingImageWidth, mUnderlyingImageHeight,
                                         mFocalPoint, new Matrix()));
        }
    }

    public int getUnscaledWidth() {
        return mUnderlyingImageWidth;
    }

    public int getUnscaledHeight() {
        return mUnderlyingImageHeight;
    }

    public void setFocalPoint(@Nullable PointF focalPoint) {
        mFocalPoint = focalPoint;
        prepImage(this.getContext());
    }

    // The matrix scale type can do many things. Here, we are treating it as an alternate version
    // of ImageView.ScaleType.CROP_CENTER where the focal point is shifted from the center
    // of the view to a point defined as a percentage of the view from the top and left sides.
    private static Matrix prepareMatrix(float viewWidth, float viewHeight, float mediaWidth, float mediaHeight,
                                        PointF focalPoint, Matrix matrix) {
        float scaleFactorY = viewHeight / mediaHeight;
        float scaleFactor;
        float px;
        float py;
        if (mediaWidth * scaleFactorY >= viewWidth) {
            // Fit height
            scaleFactor = scaleFactorY;
            px = -(mediaWidth * scaleFactor - viewWidth) * focalPoint.x / (1 - scaleFactor);
            py = 0f;
        } else {
            // Fit width
            scaleFactor = viewWidth / mediaWidth;
            px = 0f;
            py = -(mediaHeight * scaleFactor - viewHeight) * focalPoint.y / (1 - scaleFactor);
        }
        matrix.postScale(scaleFactor, scaleFactor, px, py);
        return matrix;
    }

    private float getUnpaddedViewWidth() {
        return (getWidth() - getPaddingStart() - getPaddingEnd());
    }

    private float getUnpaddedViewHeight() {
        return (getHeight() - getPaddingTop() - getPaddingBottom());
    }

    private static final int STROKE_WIDTH = 80;
}
