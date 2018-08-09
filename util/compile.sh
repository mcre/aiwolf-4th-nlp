#!/bin/sh

javac -d bin -cp lib/aiwolf-client.jar:lib/aiwolf-common.jar:lib/aiwolf-server.jar:lib/aiwolf-viewer.jar:lib/jackson-core-2.8.1.jar:lib/jackson-databind-2.8.5.jar:lib/java-juman-knp.jar:lib/jsonic-1.3.10.jar:lib/McrePlayer.jar:lib/jackson-annotations-2.8.0.jar src/net/mchs_u/mc/aiwolf/nlp/starter/*.java src/net/mchs_u/mc/aiwolf/nlp/util/*.java src/net/mchs_u/mc/aiwolf/nlp/human/*.java src/net/mchs_u/mc/aiwolf/nlp/common/*.java src/net/mchs_u/mc/aiwolf/nlp/chaser/*.java
