package local.m13.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

/** The local LibreTV blue/rose palette, with brighter TV surfaces. */
public final class BlueRedTheme {
    public static final int BLUE=0xff00ccff,RED=0xffff3c78,TEXT=0xffe6f2ff,MUTED=0xffb2c8e6,FAINT=0xff98b4d9,DARK=0xff091a32;
    public static GradientDrawable background(int theme){
        int[][] colors={{0xff193d73,0xff152d54,0xff39224e},{0xff164e7a,0xff17365d,0xff282d61},{0xff352d65,0xff252c5b,0xff532643}};
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors[(theme%colors.length+colors.length)%colors.length]);
    }
    public static GradientDrawable focus(float radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{BLUE,0xff8174ee,RED});d.setCornerRadius(radius);return d;}
    public static final class BrandText extends TextView {
        private String previous="";private float previousWidth;
        public BrandText(Context context){super(context);}
        protected void onDraw(Canvas canvas){
            String value=getText().toString();float width=Math.max(1,getPaint().measureText(value));
            if(!value.equals(previous)||width!=previousWidth){previous=value;previousWidth=width;getPaint().setShader(new LinearGradient(getCompoundPaddingLeft(),0,getCompoundPaddingLeft()+width,0,BLUE,RED,Shader.TileMode.CLAMP));}
            super.onDraw(canvas);
        }
    }
}
