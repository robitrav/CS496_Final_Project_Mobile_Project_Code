/*
    http://stackoverflow.com/questions/29058973/android-load-button-background-image-from-web-via-picasso
    This class creates a button that picasso can load images into (via implementing Target)
 */

package com.trcs496.android.cs496_final_project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by Travis on 11/30/2016.
 */
public class PicassoButton extends Button implements Target {

        public PicassoButton(Context context) {
            super(context);
        }

        public PicassoButton(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public PicassoButton(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public PicassoButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            setBackground(new BitmapDrawable(getContext().getResources(), bitmap));//set background of button to loaded image from picasso
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
}
