# -*- coding: utf-8 -*-
"""直接实例化每一款游戏，抓真实异常（不依赖网页壳）。"""
import os
import subprocess
import sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(BASE, 'dist', 'baiyou-pocket-desktop-v1.0.jar')

JAVA = '''
import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;
import com.baiyou.pocket.game.Game;

public class T {
    public static void main(String[] a) {
        for (int n = 1; n <= 100; n++) {
            String id = String.format("g%03d", n);
            try {
                GameMeta m = Registry.get(id);
                if (m == null || !m.playable()) { System.out.println(id + " not-playable"); continue; }
                Game g = ((Class<? extends Game>) m.impl).newInstance();
                g.bind(null, null);
            } catch (Throwable t) {
                System.out.println(id + " THROW " + t);
                StackTraceElement[] st = t.getStackTrace();
                if (st.length > 0) System.out.println("   at " + st[0]);
                Throwable c = t.getCause();
                while (c != null) {
                    System.out.println("   cause: " + c);
                    if (c.getStackTrace().length > 0) System.out.println("     at " + c.getStackTrace()[0]);
                    c = c.getCause();
                }
            }
        }
        System.out.println("scan done");
    }
}
'''
src = os.path.join(BASE, 'tools', 'T.java')
open(src, 'w', encoding='utf-8').write(JAVA)
cls = os.path.join(BASE, 'tools')
subprocess.run(['javac', '-cp', JAR, '-d', cls, src], check=True)
r = subprocess.run(['java', '-cp', os.path.join(cls) + ';' + JAR, 'T'],
                   capture_output=True)
out = (r.stdout or b'').decode('gbk', 'replace')
err = (r.stderr or b'').decode('gbk', 'replace')
print(out)
print(err[:2000])
