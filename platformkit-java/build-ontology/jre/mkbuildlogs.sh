#!/bin/sh

cp build.log build.log.txt
grep "\[J2ME MIDP 1.0\]" build.log.txt > build.log.j2me-midp-1_0.txt
grep "\[J2ME MIDP 2.0\]" build.log.txt > build.log.j2me-midp-2_0.txt
grep "\[J2ME PP 1.0\]" build.log.txt > build.log.j2me-pp-1_0.txt
grep "\[J2ME PP 1.1\]" build.log.txt > build.log.j2me-pp-1_1.txt
grep "\[PersonalJava 1.1\]" build.log.txt > build.log.personaljava-1_1.txt
grep "\[JDK 1.1\]" build.log.txt > build.log.jdk-1_1.txt
grep "\[J2SE 1.2\]" build.log.txt > build.log.j2se-1_2.txt
grep "\[J2SE 1.3\]" build.log.txt > build.log.j2se-1_3.txt
grep "\[J2SE 1.4\]" build.log.txt > build.log.j2se-1_4.txt
grep "\[J2SE 5.0\]" build.log.txt > build.log.j2se-1_5.txt
grep "\[Java SE 6\]" build.log.txt > build.log.j2se-1_6.txt
