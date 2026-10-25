package local.m13.home;
/** Compare numeric stable release versions; refuse unrecognized tags. */
public final class UpdateVersion {
    public static boolean newer(String remote,String local){if(remote==null||local==null)return false;remote=remote.replaceFirst("^[vV]","");if(!remote.matches("[0-9]+(\\.[0-9]+){1,3}")||!local.matches("[0-9]+(\\.[0-9]+){1,3}"))return false;String[] a=remote.split("\\."),b=local.split("\\.");try{for(int n=0;n<Math.max(a.length,b.length);n++){int x=n<a.length?Integer.parseInt(a[n]):0,y=n<b.length?Integer.parseInt(b[n]):0;if(x!=y)return x>y;}}catch(Exception ignored){}return false;}
}
