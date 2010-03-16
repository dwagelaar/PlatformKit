#!/bin/sh

cp build.log build.log.txt
grep "\[SWT 3.0\]" build.log.txt > build.log.swt-3_0.txt
grep "\[SWT 3.1\]" build.log.txt > build.log.swt-3_1.txt
grep "\[SWT 3.2\]" build.log.txt > build.log.swt-3_2.txt
grep "\[SWT 3.3\]" build.log.txt > build.log.swt-3_3.txt
grep "\[SWT 3.4\]" build.log.txt > build.log.swt-3_4.txt
grep "\[SWT 3.5\]" build.log.txt > build.log.swt-3_5.txt
