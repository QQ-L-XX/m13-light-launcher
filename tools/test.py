"""Offline solar and schedule regression checks; requires only a JDK."""
from pathlib import Path
import os, shutil, subprocess
root=Path(__file__).resolve().parent.parent
java_home=os.environ.get('JAVA_HOME')
suffix='.exe' if os.name=='nt' else ''
def tool(name):
    value=str(Path(java_home)/'bin'/(name+suffix)) if java_home else shutil.which(name)
    if not value: raise SystemExit('JDK missing: set JAVA_HOME')
    return value
dest=root/'build/tests';dest.mkdir(parents=True,exist_ok=True)
sources=[root/'app/src/local/m13/home'/name for name in ['CrayonSolar.java','CrayonTheme.java']]+[root/'tests/M13SolarCheck.java']
subprocess.run([tool('javac'),'-encoding','UTF-8','-d',str(dest),*map(str,sources)],check=True)
subprocess.run([tool('java'),'-cp',str(dest),'M13SolarCheck'],check=True)
subprocess.run([tool('javac'),'-encoding','UTF-8','-d',str(dest),str(root/'app/src/local/m13/home/MusicLyrics.java'),str(root/'tests/M13MusicChecks.java')],check=True)
subprocess.run([tool('java'),'-cp',str(dest),'M13MusicChecks'],check=True)
subprocess.run([tool('javac'),'-encoding','UTF-8','-d',str(dest),str(root/'app/src/local/m13/home/CrayonTimetable.java'),str(root/'tests/M13TimetableChecks.java')],check=True)
subprocess.run([tool('java'),'-cp',str(dest),'M13TimetableChecks'],check=True)
