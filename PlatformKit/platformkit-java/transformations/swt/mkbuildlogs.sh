#!/bin/sh

cp build.log build.log.txt
grep "\[swt-3_0\]" build.log.txt > build.log.swt-3_0.txt
grep "\[swt-3_1\]" build.log.txt > build.log.swt-3_1.txt
grep "\[swt-3_2\]" build.log.txt > build.log.swt-3_2.txt
grep "\[swt-3_3\]" build.log.txt > build.log.swt-3_3.txt
grep "\[swt-3_4\]" build.log.txt > build.log.swt-3_4.txt
