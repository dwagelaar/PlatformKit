#!/bin/sh

grep "\[Android 1.5\]" build.log > build.log.android-1_5.txt
grep "\[Android 1.6\]" build.log > build.log.android-1_6.txt
grep "\[Android 2.1\]" build.log > build.log.android-2_1.txt
grep "\[Android 2.2\]" build.log > build.log.android-2_2.txt
grep "\[Android 2.3\]" build.log > build.log.android-2_3.txt
