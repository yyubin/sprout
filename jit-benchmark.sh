#!/bin/bash
# ================================================
# 🌱 Sprout JIT Profiling with Gatling
# -----------------------------------------------
# 1. IntelliJ에서 서버 실행 (JFR + JIT 로그)
# 2. Gatling 부하 테스트 실행
# 3. 결과 분석: JFR, Hotspot 로그, Gatling 리포트
# ================================================

OUTPUT_DIR="./jit-profile"
PORT=8080

# ================================================
# 준비
# ================================================
mkdir -p "$OUTPUT_DIR"

echo "════════════════════════════════════════════════════════════════"
echo "  🌱 Sprout JIT Profiling with Gatling"
echo "════════════════════════════════════════════════════════════════"
echo ""

# ================================================
# 서버 구동 확인
# ================================================
echo "[1/3] Checking if Sprout server is running..."
if ! curl -s "http://localhost:$PORT/benchmark/hello" > /dev/null 2>&1; then
  echo ""
  echo "Server is NOT running on port $PORT"
  echo ""
  echo "Start the server from IntelliJ with these VM options:"
  echo "────────────────────────────────────────────────────────────────"
  cat << 'VMEOF'
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
-XX:StartFlightRecording=filename=jit-profile/recording.jfr,duration=300s,settings=profile
-XX:+UnlockDiagnosticVMOptions
-XX:+LogCompilation
-XX:LogFile=jit-profile/hotspot_%p.log
-XX:+PrintInlining
-XX:+PrintCompilation
-XX:+PrintCodeCache
-XX:+PrintAssembly
-XX:PrintAssemblyOptions=intel
-XX:CompileCommand=print,*benchmark*
-XX:+DebugNonSafepoints
VMEOF
  echo "────────────────────────────────────────────────────────────────"
  echo ""
  echo "Note: -XX:+PrintAssembly requires hsdis library"
  echo "   Download: https://chriswhocodes.com/hsdis/"
  echo "   Place at: \$JAVA_HOME/lib/server/"
  echo ""
  exit 1
fi

echo "✓ Server is running on port $PORT"
echo ""

# ================================================
# Gatling 부하 테스트
# ================================================
echo "[2/3] Running Gatling Heavy Load test..."
echo "────────────────────────────────────────────────────────────────"
echo "  Simulation: HeavyLoadSimulation"
echo "  Warm-up:    ~20,000 requests"
echo "  Load test:  ~80,000 requests"
echo "  Total:      ~100,000 requests"
echo "────────────────────────────────────────────────────────────────"
echo ""

# Gatling 실행
./gradlew gatlingRun --simulation=benchmark.HeavyLoadSimulation

GATLING_EXIT_CODE=$?

if [ $GATLING_EXIT_CODE -ne 0 ]; then
  echo ""
  echo "Gatling test failed with exit code: $GATLING_EXIT_CODE"
  exit $GATLING_EXIT_CODE
fi

echo ""
echo "✓ Gatling test completed"
echo ""

# ================================================
# 결과 정리
# ================================================
echo "[3/3] Collecting profiling results..."
echo ""

# Gatling 리포트 경로 찾기
LATEST_GATLING_REPORT=$(find build/reports/gatling -type d -name "*HeavyLoadSimulation*" 2>/dev/null | sort -r | head -n 1)

echo "════════════════════════════════════════════════════════════════"
echo "  Profiling Complete!"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Profiling Data:"
echo "  • JIT Compilation Log:  $OUTPUT_DIR/hotspot_*.log"
echo "  • JFR Recording:        $OUTPUT_DIR/recording.jfr"
echo "  • Assembly Output:      $OUTPUT_DIR/hotspot_*.log (if hsdis available)"
echo ""
echo "Gatling Report:"
if [ -n "$LATEST_GATLING_REPORT" ]; then
  echo "  • HTML Report:          $LATEST_GATLING_REPORT/index.html"
  echo ""
  echo "  Open report:"
  echo "    open $LATEST_GATLING_REPORT/index.html"
else
  echo "  • Check: build/reports/gatling/"
fi
echo ""
echo "Analysis Tools"
echo "  1. JITWatch:     Analyze hotspot_*.log for JIT compilation details"
echo "  2. JDK Mission Control: Open recording.jfr for performance profiling"
echo "  3. Gatling:      View HTML report for load test metrics"
echo ""
echo "════════════════════════════════════════════════════════════════"
