#!/bin/bash
# Sprout 서버 부하테스트 + Async Profiler 통합 스크립트

SERVER_PORT=8080
DURATION=30
ASYNC_PROFILER_HOME=/Users/mac/IdeaProjects/async-profiler/build
SIMULATION_CLASS=benchmark.HelloWorldSimulation

PID=$(lsof -i :$SERVER_PORT -t)
if [ -z "$PID" ]; then
  echo "서버가 실행 중이 아닙니다. 먼저 Sprout 서버를 실행하세요."
  exit 1
fi
echo "Sprout 서버 PID = $PID"

ASPROF="$ASYNC_PROFILER_HOME/bin/asprof"
if [ ! -f "$ASPROF" ]; then
  echo "asprof 실행 파일을 찾을 수 없습니다. 경로를 확인하세요."
  exit 1
fi

echo "1) Gatling 부하테스트 시작..."
./gradlew gatlingRun --simulation=benchmark.HelloWorldSimulation &
GATLING_PID=$!

sleep 3

echo "2) Async Profiler로 $DURATION초간 프로파일링..."
env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF -d $DURATION -e cpu   -o flamegraph -f cpu-flamegraph.html   $PID
env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF -d $DURATION -e alloc -o flamegraph -f alloc-flamegraph.html $PID

wait $GATLING_PID

echo "완료! 결과 파일"
echo " - cpu-flamegraph.html"
echo " - alloc-flamegraph.html"
open cpu-flamegraph.html
open alloc-flamegraph.html
