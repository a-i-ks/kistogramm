import os
import json
import base64
import logging
import redis
import httpx
from faster_whisper import WhisperModel

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger(__name__)

REDIS_HOST = os.environ.get("REDIS_HOST", "redis")
REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
CALLBACK_URL = os.environ["CALLBACK_URL"]
WEBHOOK_SECRET = os.environ["WEBHOOK_SECRET"]
OLLAMA_HOST = os.environ.get("OLLAMA_HOST", "http://host.docker.internal:11434")
QUEUE_KEY = "ai_jobs_queue"

PROMPT_TEMPLATE = (
    'You are an inventory management assistant. The user described the item as:\n'
    '"{transcript}"\n\n'
    'Analyze the image and the description. Output ONLY a valid JSON object with these fields:\n'
    '{{\n'
    '  "name": "short item name",\n'
    '  "description": "detailed description",\n'
    '  "category": "best matching category name or null",\n'
    '  "tags": ["tag1", "tag2"],\n'
    '  "quantity": 1,\n'
    '  "purchase_price": null\n'
    '}}'
)

log.info("Loading Whisper model (CPU)...")
whisper_model = WhisperModel("base", device="cpu", compute_type="int8")
log.info("Whisper model loaded.")


def transcribe(audio_path: str) -> str:
    segments, _ = whisper_model.transcribe(audio_path, beam_size=5)
    return " ".join(s.text for s in segments).strip()


def analyze_with_moondream(image_path: str, transcript: str) -> dict:
    with open(image_path, "rb") as f:
        img_b64 = base64.b64encode(f.read()).decode()

    payload = {
        "model": "moondream",
        "prompt": PROMPT_TEMPLATE.format(transcript=transcript),
        "images": [img_b64],
        "stream": False,
    }
    resp = httpx.post(f"{OLLAMA_HOST}/api/generate", json=payload, timeout=120)
    resp.raise_for_status()
    raw = resp.json()["response"]

    # Extract JSON robustly in case the model adds surrounding text
    start = raw.index("{")
    end = raw.rindex("}") + 1
    return json.loads(raw[start:end])


def send_callback(job_id: str, result: dict) -> None:
    payload = {"jobId": job_id, **result}
    headers = {
        "X-Webhook-Secret": WEBHOOK_SECRET,
        "Content-Type": "application/json",
    }
    # Rename purchase_price → purchasePrice to match Spring DTO
    if "purchase_price" in payload:
        payload["purchasePrice"] = payload.pop("purchase_price")

    resp = httpx.post(CALLBACK_URL, json=payload, headers=headers, timeout=30)
    resp.raise_for_status()


def process_job(job: dict) -> None:
    job_id = job["jobId"]
    log.info("Processing job %s", job_id)

    transcript = transcribe(job["audioPath"])
    log.info("Transcript for job %s: %s", job_id, transcript[:120])

    result = analyze_with_moondream(job["imagePath"], transcript)
    log.info("Moondream result for job %s: %s", job_id, result)

    send_callback(job_id, result)
    log.info("Job %s completed successfully", job_id)


def main() -> None:
    r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)
    log.info("Worker ready, listening on queue '%s'...", QUEUE_KEY)

    while True:
        try:
            item = r.blpop(QUEUE_KEY, timeout=0)
            if item is None:
                continue
            _, raw = item
            job = json.loads(raw)
            process_job(job)
        except Exception as exc:
            log.error("Job failed: %s", exc, exc_info=True)


if __name__ == "__main__":
    main()
