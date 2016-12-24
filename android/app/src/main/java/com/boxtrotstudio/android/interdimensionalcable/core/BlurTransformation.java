package com.boxtrotstudio.android.interdimensionalcable.core;


import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.boxtrotstudio.android.interdimensionalcable.CableApplication;
import com.squareup.picasso.Transformation;

public class BlurTransformation implements Transformation {
    private static final float RADIUS = 5;

    @Override
    public Bitmap transform(Bitmap source) {
        int width = Math.round((float)source.getWidth() * 0.5f);
        int height = Math.round((float)source.getHeight() * 0.5f);
        Bitmap inputBitmap = Bitmap.createScaledBitmap(source, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        RenderScript rs = RenderScript.create(CableApplication.getInstance());
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        source.recycle();
        return outputBitmap;
    }

    @Override
    public String key() {
        return "blur()";
    }
}
