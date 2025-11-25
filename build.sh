#!/bin/bash

echo "cleaning up bin/ directory..."
rm -rf bin/
mkdir bin/

echo "compiling java files..."
javac -cp "lib/com.microsoft.z3.jar" src/com/unischeduler/*.java -d bin/

if [ $? -eq 0 ]; then
    echo "compilation successful!"
else
    echo "compilation failed"
fi
