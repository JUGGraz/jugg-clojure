#!/bin/bash

jar="code/target/workshopstats-1.0.0-standalone.jar"

set -e

read -p "RET to start demo..."

cp prepped/antworten.csv .
open -a LibreOffice antworten.csv
read -p "RET when ready to continue..."

echo "Copied prepared answers. Extracting questions:"
set -x
java -jar "$jar" -i antworten.csv questions
set +x

read -p "RET to see results..."
open -a TextEdit questions.txt prepped/questions.txt
read -p "RET when ready to continue..."

cp prepped/questions.txt .
echo "Copied prepared questions... Generating empty scores:"
set -x
java -jar "$jar" -i antworten.csv scorestruct
set +x

read -p "RET to see results..."
open -a TextEdit scores.json prepped/scores.json
read -p "RET when ready to continue..."

cp prepped/scores.json .
echo "Copied prepared scores... Scoring participants:"
set -x
java -jar "$jar" -i antworten.csv scores
set +x

read -p "RET to see results..."
open -a LibreOffice scored.csv
read -p "Demo done. RET to exit."
