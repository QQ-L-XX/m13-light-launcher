"""Build the complete native APK using an installed Android SDK and JDK."""
from pathlib import Path
import hashlib, os, shutil, subprocess, zipfile
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
app=root/'app';build=root/'build/apk';dist=root/'dist'
suffix='.exe' if os.name=='nt' else ''
sdk=Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or '.sdk-not-configured')
androidjar=Path(os.environ.get('ANDROID_JAR',str(sdk/'platforms/android-22/android.jar')))
bt=Path(os.environ.get('ANDROID_BUILD_TOOLS',str(sdk/'build-tools/35.0.0')))
jhome=os.environ.get('JAVA_HOME')
def jtool(name):
    value=str(Path(jhome)/'bin'/(name+suffix)) if jhome else shutil.which(name)
    if not value or not Path(value).is_file():raise SystemExit('JDK missing: set JAVA_HOME')
    return value
def run(args,cwd=None):
    subprocess.run(list(map(str,args)),cwd=cwd,check=True)
for required in [androidjar,bt/('aapt'+suffix),bt/('zipalign'+suffix),bt/'lib/d8.jar',bt/'lib/apksigner.jar']:
    if not required.is_file():raise SystemExit('SDK component missing: '+str(required))
if build.exists():
    if build.is_symlink() or build.resolve().parent!=(root/'build').resolve():raise SystemExit('Unexpected build output path')
    shutil.rmtree(build)
classes=build/'classes';dex=build/'dex'
for p in [classes,dex,dist]:p.mkdir(parents=True,exist_ok=True)
java=jtool('java')
run([jtool('javac'),'-encoding','UTF-8','-source','8','-target','8','-bootclasspath',androidjar,'-d',classes,*sorted((app/'src').rglob('*.java'))])
run([java,'-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--min-api','21','--lib',androidjar,'--output',dex,*sorted(classes.rglob('*.class'))])
unsigned=build/'unsigned.apk';aligned=build/'aligned.apk'
run([bt/('aapt'+suffix),'package','-f','-M',app/'AndroidManifest.xml','-S',app/'res','-A',app/'assets','-I',androidjar,'-F',unsigned])
with zipfile.ZipFile(unsigned) as z:
    with z.open('assets/title-index.db') as f:assert f.read(16)==b'SQLite format 3\0'
run([bt/('aapt'+suffix),'add',unsigned,'classes.dex'],cwd=dex)
run([bt/('zipalign'+suffix),'-f','4',unsigned,aligned])
release=os.environ.get('M13_KEYSTORE')
if release:
    for key in ['M13_KEY_ALIAS','M13_STORE_PASSWORD','M13_KEY_PASSWORD']:
        if not os.environ.get(key):raise SystemExit('Release signing requires '+key)
    keystore=Path(release);alias=os.environ['M13_KEY_ALIAS'];store_env='M13_STORE_PASSWORD';key_env='M13_KEY_PASSWORD'
else:
    keystore=root/'.signing/debug.jks';keystore.parent.mkdir(parents=True,exist_ok=True)
    alias='androiddebugkey';store_env=key_env='M13_DEBUG_PASSWORD';os.environ[store_env]='android'
    if not keystore.exists():
        run([jtool('keytool'),'-genkeypair','-keystore',keystore,'-storetype','JKS','-alias',alias,'-storepass:env',store_env,'-keypass:env',key_env,'-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=Android Debug'])
    print('Development signing: this APK cannot update the official release key.')
version=ET.parse(app/'AndroidManifest.xml').getroot().get('{http://schemas.android.com/apk/res/android}versionName')
apk=dist/('M13-Light-Launcher-'+version+'.apk')
run([java,'-jar',bt/'lib/apksigner.jar','sign','--ks',keystore,'--ks-key-alias',alias,'--ks-pass','env:'+store_env,'--key-pass','env:'+key_env,'--min-sdk-version','21','--out',apk,aligned])
run([java,'-jar',bt/'lib/apksigner.jar','verify','--verbose',apk])
run([bt/('zipalign'+suffix),'-c','4',apk])
with zipfile.ZipFile(apk) as z:assert z.testzip() is None
digest=hashlib.file_digest(apk.open('rb'),'sha256').hexdigest()
(dist/'SHA256SUMS.txt').write_text(digest+'  '+apk.name+'\n',encoding='utf-8')
print('Built',apk.name,'bytes',apk.stat().st_size,'SHA256',digest)
