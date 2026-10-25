package app.controllerbridge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Local vector illustration. Animates verified state changes, never an optimistic tap. */
public final class ControllerVisual extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path body=new Path();
    private ValueAnimator animator;
    private boolean initialized,connected;
    private float amount,pulse;

    public ControllerVisual(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void setConnected(boolean value,boolean animate){
        if(initialized&&connected==value)return;
        boolean wasInitialized=initialized;initialized=true;connected=value;
        if(animator!=null){animator.cancel();animator=null;}
        float target=value?1f:0f;
        if(!wasInitialized||!animate||!ValueAnimator.areAnimatorsEnabled()){
            amount=target;pulse=0;invalidate();return;
        }
        float from=amount;
        animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(value?700:450);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a->{
            float progress=(float)a.getAnimatedValue();amount=from+(target-from)*progress;
            pulse=(float)Math.sin(progress*Math.PI)*(value?1f:.45f);invalidate();
        });animator.start();
    }
    public void stopAnimation(){
        if(animator!=null){animator.cancel();animator=null;}
        amount=connected?1f:0f;pulse=0;invalidate();
    }
    @Override protected void onDetachedFromWindow(){stopAnimation();super.onDetachedFromWindow();}
    private int blend(int off,int on){
        return Color.rgb((int)(Color.red(off)+(Color.red(on)-Color.red(off))*amount),
                (int)(Color.green(off)+(Color.green(on)-Color.green(off))*amount),
                (int)(Color.blue(off)+(Color.blue(on)-Color.blue(off))*amount));
    }
    private void fill(Canvas canvas,int color,float left,float top,float right,float bottom,float radius){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);canvas.drawRoundRect(left,top,right,bottom,radius,radius,paint);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();
        float scale=Math.min(getWidth()/320f,getHeight()/220f);
        canvas.translate(getWidth()/2f,getHeight()/2f);canvas.scale(scale,scale);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int)(18*amount+12*pulse),101,227,196));canvas.drawCircle(0,0,91+10*pulse,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(blend(0xff233147,0xff317e72));canvas.drawCircle(0,0,92,paint);
        if(pulse>0){paint.setColor(Color.argb((int)(90*pulse),101,227,196));canvas.drawCircle(0,0,96+18*pulse,paint);}
        canvas.scale(1+.025f*pulse,1+.025f*pulse);
        body.reset();body.moveTo(-61,-35);body.cubicTo(-86,-34,-99,22,-100,48);
        body.cubicTo(-100,69,-82,72,-70,54);body.lineTo(-48,30);body.quadTo(0,20,48,30);
        body.lineTo(70,54);body.cubicTo(82,72,100,69,100,48);body.cubicTo(99,22,86,-34,61,-35);
        body.cubicTo(30,-41,-30,-41,-61,-35);body.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(blend(0xff536176,0xffe6edf4));canvas.drawPath(body,paint);
        fill(canvas,0xff142033,-29,-29,29,-1,8);
        fill(canvas,blend(0xff263b51,0xff65e3c4),-28,-34,28,-30,2);
        fill(canvas,0xff142033,-69,-20,-61,10,2);fill(canvas,0xff142033,-80,-9,-50,-1,2);
        paint.setColor(0xff142033);canvas.drawCircle(-30,21,13,paint);canvas.drawCircle(30,21,13,paint);
        paint.setColor(blend(0xff7c8ca1,0xff40556a));canvas.drawCircle(-30,21,8,paint);canvas.drawCircle(30,21,8,paint);
        paint.setColor(0xff142033);canvas.drawCircle(65,-19,5,paint);canvas.drawCircle(79,-5,5,paint);canvas.drawCircle(65,9,5,paint);canvas.drawCircle(51,-5,5,paint);
        paint.setColor(blend(0xff34445a,0xff65e3c4));canvas.drawCircle(0,13,3,paint);
        canvas.restore();
    }
}
