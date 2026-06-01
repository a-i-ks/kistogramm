import os
import json
import base64
import logging
import re
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
VLM_MODEL = os.environ.get("VLM_MODEL", "qwen2.5vl:7b")

PROMPT_INGESTION = """\
You are an inventory assistant. The user described this item as: "{transcript}"

Look at the image carefully and return a single valid JSON object with these fields:
- "name": the specific name of the object you see (e.g. "blue spray bottle", "wooden chair")
- "description": 1-2 sentences describing color, shape, material, and condition
- "category": exactly one of: Elektronik, Kleidung, Moebelstueck, Lebensmittel, Pflanze, Sonstiges
- "tags": list of 3-5 relevant lowercase tags
- "quantity": number of items visible (integer)
- "purchase_price": null

Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_DIMENSION = """\
You are an inventory assistant. Look at this item carefully and estimate its physical dimensions.
Return ONLY a JSON object with these fields:
- "width": estimated width as a number (e.g. 30.5)
- "widthUnit": unit string (e.g. "cm")
- "height": estimated height as a number
- "heightUnit": unit string
- "depth": estimated depth as a number
- "depthUnit": unit string
- "weight": estimated weight as a number
- "weightUnit": unit string (e.g. "kg" or "g")

Use centimeters for dimensions and kg/g for weight. Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_VALUE = """\
You are an inventory valuation assistant. Look at this item and estimate its current market value.
Return ONLY a JSON object with these fields:
- "minValue": conservative market value as a number (EUR)
- "maxValue": optimistic market value as a number (EUR)
- "currency": "EUR"
- "confidence": one of "low", "medium", "high"
- "reasoning": one sentence explaining the estimate

Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_CONDITION = """\
You are an inventory condition assessor. Examine this item carefully.
Return ONLY a JSON object with these fields:
- "condition": one of "neuwertig", "sehr gut", "gut", "akzeptabel", "schlecht"
- "conditionDetails": 1-2 sentences describing visible wear, damage, or notable features

Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_TAGS = """\
You are an inventory tagging assistant. Look at this item and suggest relevant tags.
Return ONLY a JSON object with:
- "tags": list of 3-6 relevant lowercase German tags (e.g. ["elektronik", "kabel", "usb"])

Respond with ONLY the JSON object, no markdown, no explanation."""

log.info("Loading Whisper model (CPU)...")
whisper_model = WhisperModel("base", device="cpu", compute_type="int8")
log.info("Whisper model loaded.")


def transcribe(audio_path: str) -> str:
    segments, _ = whisper_model.transcribe(audio_path, beam_size=5)
    return " ".join(s.text for s in segments).strip()


def analyze_with_vlm(image_path: str, transcript: str) -> dict:
    with open(image_path, "rb") as f:
        img_b64 = base64.b64encode(f.read()).decode()

    prompt = PROMPT_INGESTION.format(transcript=transcript)
    payload = {"model": VLM_MODEL, "prompt": prompt, "images": [img_b64], "stream": False}
    resp = httpx.post(f"{OLLAMA_HOST}/api/generate", json=payload, timeout=600)
    resp.raise_for_status()
    raw = resp.json()["response"].strip()
    log.info("VLM raw response: %s", raw)

    result = _parse_json_response(raw)

    result.setdefault("name", transcript)
    result.setdefault("description", "")
    result.setdefault("category", "Sonstiges")
    result.setdefault("tags", [])
    result.setdefault("quantity", 1)
    result.setdefault("purchase_price", None)
    return result


def analyze_with_prompt(image_path: str, prompt: str) -> dict:
    with open(image_path, "rb") as f:
        img_b64 = base64.b64encode(f.read()).decode()

    payload = {"model": VLM_MODEL, "prompt": prompt, "images": [img_b64], "stream": False}
    resp = httpx.post(f"{OLLAMA_HOST}/api/generate", json=payload, timeout=600)
    resp.raise_for_status()
    raw = resp.json()["response"].strip()
    log.info("Analysis raw response: %s", raw)
    return _parse_json_response(raw)


def _parse_json_response(raw: str) -> dict:
    raw = re.sub(r"^```(?:json)?\s*", "", raw)
    raw = re.sub(r"\s*```$", "", raw)
    start = raw.index("{")
    end = raw.rindex("}") + 1
    return json.loads(raw[start:end])


def send_callback(job_id: str, job_type: str, result: dict = None, proposal_data: str = None) -> None:
    if job_type == "INGESTION":
        payload = {"jobId": job_id, "jobType": job_type, **(result or {})}
        if "purchase_price" in payload:
            payload["purchasePrice"] = payload.pop("purchase_price")
    else:
        payload = {"jobId": job_id, "jobType": job_type, "proposalData": proposal_data}

    headers = {"X-Webhook-Secret": WEBHOOK_SECRET, "Content-Type": "application/json"}
    resp = httpx.post(CALLBACK_URL, json=payload, headers=headers, timeout=30)
    resp.raise_for_status()


ANALYSIS_PROMPTS = {
    "DIMENSION_ESTIMATION": PROMPT_DIMENSION,
    "VALUE_ESTIMATION": PROMPT_VALUE,
    "CONDITION_ASSESSMENT": PROMPT_CONDITION,
    "TAG_SUGGESTIONS": PROMPT_TAGS,
}


def process_job(job: dict) -> None:
    job_id = job["jobId"]
    job_type = job.get("jobType", "INGESTION")
    log.info("Processing job %s type=%s", job_id, job_type)

    if job_type == "INGESTION":
        audio_path = job.get("audioPath", "")
        transcript = transcribe(audio_path) if audio_path else ""
        log.info("Transcript for job %s: %s", job_id, transcript[:120])

        result = analyze_with_vlm(job["imagePath"], transcript)
        log.info("VLM result for job %s: %s", job_id, result)

        send_callback(job_id, job_type="INGESTION", result=result)

    elif job_type in ANALYSIS_PROMPTS:
        prompt = ANALYSIS_PROMPTS[job_type]
        proposal = analyze_with_prompt(job["imagePath"], prompt)
        log.info("Proposal for job %s: %s", job_id, proposal)

        send_callback(job_id, job_type=job_type, proposal_data=json.dumps(proposal))

    else:
        log.warning("Unknown job type '%s' for job %s — skipping", job_type, job_id)
        return

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
