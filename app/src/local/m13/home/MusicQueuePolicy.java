package local.m13.home;

import java.util.Random;

/** Auto completion differs from a deliberate next/previous press. */
public final class MusicQueuePolicy {
    public static int next(int index,int count,int direction,boolean automatic,int mode,Random random){
        if(count<=0)return -1;
        if(automatic&&mode==1)return Math.max(0,index);
        if(mode==2&&count>1){int value=random.nextInt(count-1);return value>=index?value+1:value;}
        if(automatic&&mode==0&&index>=count-1)return -1;
        return (index+direction+count)%count;
    }
}
