#!/bin/sh

# cd ~/aiwolf-4th-nlp/dic
~/usr/libexec/juman/makeint usr.dic
~/usr/libexec/juman/dicsort usr.int > jumandic.dat
~/usr/libexec/juman/makepat usr.int
