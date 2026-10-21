package local.m13.home;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Small geometric glyphs; rendered locally without bitmap assets or fonts. */
public final class MinimalIcon extends Drawable {
 private final Paint p=new Paint(3);private final String kind;private int tint;
 public MinimalIcon(String kind,int tint){this.kind=kind;this.tint=tint;}
 public void color(int color){tint=color;invalidateSelf();}
 public void draw(Canvas canvas){int save=canvas.save();Rect b=getBounds();canvas.translate(b.left,b.top);canvas.scale(b.width()/64f,b.height()/64f);p.setColor(tint);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
  if(kind.equals("play")){canvas.drawRoundRect(new RectF(8,12,56,49),7,7,p);Path t=new Path();t.moveTo(27,23);t.lineTo(40,31);t.lineTo(27,39);t.close();p.setStyle(Paint.Style.FILL);canvas.drawPath(t,p);}
  else if(kind.equals("cast")){canvas.drawRoundRect(new RectF(15,12,57,44),4,4,p);p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(new RectF(5,30,24,55),3,3,p);p.setColor(0xff15345e);canvas.drawRoundRect(new RectF(8,33,21,50),1,1,p);p.setColor(tint);}
  else if(kind.equals("back")){Path path=new Path();path.moveTo(39,16);path.lineTo(22,32);path.lineTo(39,48);canvas.drawPath(path,p);}
  else{for(int y=0;y<2;y++)for(int x=0;x<2;x++)canvas.drawRoundRect(new RectF(12+x*23,12+y*23,28+x*23,28+y*23),3,3,p);}
  canvas.restoreToCount(save);
 }
 public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
