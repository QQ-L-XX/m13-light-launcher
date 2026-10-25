package local.m13.home;
import android.content.*;
/** Protected by the voice application's signature permission. No remote key injection. */
public final class VoiceControlReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){try{
  if("local.m13.home.VOICE_CONTEXT".equals(i.getAction())){String data=LibreTvLibraryActivity.voiceContext();setResultCode(data==null?0:-1);setResultData(data);}
  else if("local.m13.home.VOICE_COMMAND_RESULT".equals(i.getAction())){String data=LibreTvLibraryActivity.voiceCommandResult(i.getStringExtra("requestId"));setResultCode(data==null?0:-1);setResultData(data);}
  else if("local.m13.home.VOICE_COMMAND".equals(i.getAction())){String error=LibreTvLibraryActivity.queueVoiceCommand(c,i);setResultCode(error==null?-1:0);setResultData(error==null?"已接收":error);}
 }catch(Exception e){setResultCode(0);setResultData("页面已变化，请返回后重新唤醒");}}
}
