package local.m13.home;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** A granted read URI only exposes the two private installer files. */
public final class LauncherPackageProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException{String path=uri.getPath();if(!"/download.apk".equals(path)&&!"/previous.apk".equals(path))throw new FileNotFoundException();File f=new File(new File(getContext().getFilesDir(),"updates"),path.substring(1));if(!f.isFile())throw new FileNotFoundException();return f;}
    public String getType(Uri uri){return "application/vnd.android.package-archive";}
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=file(uri);MatrixCursor result=new MatrixCursor(new String[]{"_display_name","_size"});result.addRow(new Object[]{f.getName(),f.length()});return result;}catch(Exception e){return null;}}
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues values,String s,String[] args){throw new UnsupportedOperationException();}public int delete(Uri uri,String s,String[] args){throw new UnsupportedOperationException();}
}
