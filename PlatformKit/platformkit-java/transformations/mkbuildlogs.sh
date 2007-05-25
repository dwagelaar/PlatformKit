#!/bin/sh

cp build.log build.log.txt
grep "\[j2me-midp-1_0\]" build.log.txt > build.log.j2me-midp-1_0.txt
grep "\[j2me-pp-1_0\]" build.log.txt > build.log.j2me-pp-1_0.txt
grep "\[personaljava-1_1\]" build.log.txt > build.log.personaljava-1_1.txt
grep "\[jdk-1_1\]" build.log.txt > build.log.jdk-1_1.txt
grep "\[j2se-1_2\]" build.log.txt > build.log.j2se-1_2.txt
grep "\[j2se-1_3\]" build.log.txt > build.log.j2se-1_3.txt
grep "\[j2se-1_4\]" build.log.txt > build.log.j2se-1_4.txt
grep "\[j2se-1_5\]" build.log.txt > build.log.j2se-1_5.txt
