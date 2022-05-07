# -*- coding: utf-8 -*-
"""
Created on Fri May  6 20:46:15 2022

@author: TwelveWings
"""
with open('big_text_file.txt', 'w') as f:
    blocks = 0
    for i in range(1024 * 1024 * 16):
        if i % (1024 * 1024 * 4) == 0:
            f.write(str(blocks + 1))
            blocks += 1
        else:
            f.write("a")