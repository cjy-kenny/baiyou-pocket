# -*- coding: utf-8 -*-
"""抓取首页 HTML 的 head 段落检查 CSS 是否完好。"""
import io
import urllib.request

t = urllib.request.urlopen('http://127.0.0.1:8082/', timeout=5).read().decode('utf-8', 'replace')
i = t.find('<style>')
j = t.find('</style>')
print('style idx:', i, j)
print(t[max(0, i - 40):i + 500])
print('...')
print(t[j - 200:j + 120])
io.open(r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\android\build\home_dump.html', 'w', encoding='utf-8').write(t)
