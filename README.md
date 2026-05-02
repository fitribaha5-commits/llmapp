# LLM GPU Server App

แอป Android ที่รัน LLM ด้วย GPU แล้วเปิด HTTP API ให้ Termux เรียกใช้ได้

## วิธีใช้งาน

### 1. เตรียมโมเดล
วางไฟล์ `model.bin` (MediaPipe format) ใน `/sdcard/Download/model.bin`

โมเดลที่รองรับ: Gemma 2B, Phi-2, Falcon 1B
ดาวน์โหลดได้จาก: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference

### 2. ติดตั้ง APK
ดาวน์โหลด APK จาก GitHub Actions แล้วติดตั้ง

### 3. เปิดแอป
เปิดแอป "LLM Server" แล้วรอจนเห็น ✅ Server รันอยู่ที่ port 8080

### 4. ใช้งานจาก Termux

#### เช็คสถานะ
```bash
curl http://localhost:8080/health
```

#### ถามคำถาม (GET)
```bash
curl "http://localhost:8080/generate?prompt=สวัสดี+บอกฉันเกี่ยวกับ+AI+หน่อย"
```

#### ถามคำถาม (POST + JSON)
```bash
curl -X POST http://localhost:8080/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "อธิบาย machine learning ให้เข้าใจง่ายๆ"}'
```

#### รับ Response เป็น JSON
```json
{
  "prompt": "คำถามที่ส่งไป",
  "response": "คำตอบจาก AI",
  "model": "mediapipe-gpu"
}
```

#### ใช้ใน Script Termux
```bash
#!/bin/bash
PROMPT="$1"
RESULT=$(curl -s -X POST http://localhost:8080/generate \
  -H "Content-Type: application/json" \
  -d "{\"prompt\": \"$PROMPT\"}")

echo $RESULT | python3 -c "import sys,json; print(json.load(sys.stdin)['response'])"
```

## Build เอง

```bash
git clone https://github.com/YOUR_USERNAME/llmapp
cd llmapp
# Push ขึ้น GitHub แล้วดู Actions tab
```
